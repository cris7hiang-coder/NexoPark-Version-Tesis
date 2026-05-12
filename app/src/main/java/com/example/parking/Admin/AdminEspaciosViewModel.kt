package com.example.parking.Admin

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
class AdminEspaciosViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    companion object {
        private const val ORIGEN_ADMIN = "ADMIN"

        private const val ESTADO_ACTIVA = "activa"
        private const val ESTADO_PENDIENTE = "pendiente"
        private const val ESTADO_FINALIZADA = "finalizada"
    }

    private fun parqueoRef(parqueoId: String) =
        db.collection("parqueos").document(parqueoId)

    private fun espaciosRef(parqueoId: String) =
        parqueoRef(parqueoId).collection("espacios")

    private fun pagosRef(parqueoId: String) =
        parqueoRef(parqueoId).collection("pagos")

    private fun reservasRef() =
        db.collection("reservas")

    private fun usersRef() =
        db.collection("users")

    suspend fun cargarDetalleOcupacion(
        parqueoId: String,
        tipoVehiculo: String,
        indice: Int
    ): DetalleOcupacion? = withContext(Dispatchers.IO) {
        val espacioId = obtenerIdDeEspacioDesdeFirestore(
            parqueoId = parqueoId,
            tipoVehiculo = tipoVehiculo,
            indice = indice
        ) ?: return@withContext null

        val reservaSnap = reservasRef()
            .whereEqualTo("espacioId", espacioId)
            .whereIn("estado", listOf(ESTADO_ACTIVA, ESTADO_PENDIENTE))
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?: return@withContext null

        val origen = reservaSnap.getString("origen")

        if (origen == ORIGEN_ADMIN) {
            return@withContext DetalleOcupacion(
                reservaId = reservaSnap.id,
                cliente = null,
                vehiculo = null,
                reserva = reservaSnap
            )
        }

        val clienteId = reservaSnap.getString("clienteId") ?: return@withContext null
        val vehiculoId = reservaSnap.getString("vehiculoId") ?: return@withContext null

        val clienteSnap = usersRef()
            .document(clienteId)
            .get()
            .await()

        val vehiculoSnap = usersRef()
            .document(clienteId)
            .collection("vehiculos")
            .document(vehiculoId)
            .get()
            .await()

        DetalleOcupacion(
            reservaId = reservaSnap.id,
            cliente = clienteSnap,
            vehiculo = vehiculoSnap,
            reserva = reservaSnap
        )
    }

    suspend fun finalizarReservaYLiberarEspacio(
        reservaId: String,
        parqueoId: String,
        tipoVehiculo: String,
        indice: Int,
        minutosUsados: Int,
        costoFinal: Double
    ) = withContext(Dispatchers.IO) {
        val espacioId = obtenerIdDeEspacioDesdeFirestore(parqueoId, tipoVehiculo, indice)
            ?: throw IllegalStateException("No se encontró espacioId")

        val reservaRef = reservasRef().document(reservaId)
        val espacioRef = espaciosRef(parqueoId).document(espacioId)

        db.runTransaction { tx ->
            val reservaSnap = tx.get(reservaRef)
            val estadoActual = reservaSnap.getString("estado")

            if (estadoActual != ESTADO_ACTIVA && estadoActual != ESTADO_PENDIENTE) {
                throw IllegalStateException("La reserva ya no está activa o pendiente")
            }

            tx.update(
                reservaRef,
                mapOf(
                    "estado" to ESTADO_FINALIZADA,
                    "horaSalida" to Timestamp.now(),
                    "minutosUsados" to minutosUsados,
                    "costoFinal" to costoFinal,
                    "qrActivo" to false,
                    "cobroProcesado" to true
                )
            )

            tx.update(
                espacioRef,
                mapOf(
                    "estado" to EstadoEspacio.DISPONIBLE.name,
                    "cliente" to null,
                    "placa" to null,
                    "reservaId" to null
                )
            )
        }.await()
    }

    suspend fun obtenerIdDeEspacioDesdeFirestore(
        parqueoId: String,
        tipoVehiculo: String,
        indice: Int
    ): String? = withContext(Dispatchers.IO) {
        espaciosRef(parqueoId)
            .whereEqualTo("tipoVehiculo", tipoVehiculo)
            .whereEqualTo("indice", indice)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.id
    }

    suspend fun crearReservaManual(
        parqueoId: String,
        tipoVehiculo: String,
        indice: Int,
        placa: String?,
        comentario: String? = null
    ) = withContext(Dispatchers.IO) {
        val espacioId = obtenerIdDeEspacioDesdeFirestore(parqueoId, tipoVehiculo, indice)
            ?: throw IllegalStateException("No se encontró el espacio")

        val espacioRef = espaciosRef(parqueoId).document(espacioId)
        val reservaRef = reservasRef().document()

        db.runTransaction { tx ->
            val espacioSnap = tx.get(espacioRef)

            if (espacioSnap.getString("estado") != EstadoEspacio.DISPONIBLE.name) {
                throw IllegalStateException("El espacio no está disponible")
            }

            val reservaData = mapOf(
                "id" to reservaRef.id,
                "parqueoId" to parqueoId,
                "tipoVehiculo" to tipoVehiculo,
                "espacioId" to espacioId,
                "indice" to indice,
                "placa" to placa,
                "comentario" to comentario,
                "clienteId" to null,
                "origen" to ORIGEN_ADMIN,
                "estado" to ESTADO_ACTIVA,
                "horaLlegada" to FieldValue.serverTimestamp(),
                "fechaInicio" to FieldValue.serverTimestamp(),
                "qrActivo" to false,
                "presente" to true,
                "modoLibre" to true,
                "cobroProcesado" to false
            )

            tx.set(reservaRef, reservaData)

            tx.update(
                espacioRef,
                mapOf(
                    "estado" to EstadoEspacio.OCUPADO.name,
                    "placa" to placa,
                    "cliente" to null,
                    "reservaId" to reservaRef.id
                )
            )
        }.await()
    }

    suspend fun liberarEspacioDirecto(
        parqueoId: String,
        tipoVehiculo: String,
        indice: Int,
        reservaId: String? = null
    ) = withContext(Dispatchers.IO) {
        val espacioId = obtenerIdDeEspacioDesdeFirestore(parqueoId, tipoVehiculo, indice)
            ?: throw IllegalStateException("No se encontró el espacio")

        val espacioRef = espaciosRef(parqueoId).document(espacioId)

        db.runTransaction { tx ->
            val espacioSnap = tx.get(espacioRef)
            val reservaActualId = reservaId ?: espacioSnap.getString("reservaId")

            if (reservaActualId != null) {
                val reservaRef = reservasRef().document(reservaActualId)
                tx.update(
                    reservaRef,
                    mapOf(
                        "estado" to ESTADO_FINALIZADA,
                        "horaSalida" to Timestamp.now(),
                        "qrActivo" to false,
                        "cobroProcesado" to true
                    )
                )
            }

            tx.update(
                espacioRef,
                mapOf(
                    "estado" to EstadoEspacio.DISPONIBLE.name,
                    "cliente" to null,
                    "placa" to null,
                    "reservaId" to null
                )
            )
        }.await()
    }

    suspend fun registrarPago(
        reservaId: String,
        parqueoId: String,
        clienteId: String?,
        monto: Double,
        metodo: String,
        minutosUsados: Int,
        estado: String = "completado"
    ) = withContext(Dispatchers.IO) {
        val pago = hashMapOf(
            "reservaId" to reservaId,
            "clienteId" to clienteId,
            "monto" to monto,
            "tipoPago" to metodo,
            "minutosUsados" to minutosUsados,
            "estado" to estado,
            "fecha" to FieldValue.serverTimestamp(),
            "fechaRegistro" to FieldValue.serverTimestamp(),
            "mes" to SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
            "adminId" to Firebase.auth.currentUser?.uid
        )

        pagosRef(parqueoId)
            .add(pago)
            .await()
    }

    suspend fun obtenerTarifas(
        parqueoId: String
    ): Map<String, Map<String, Double>> = withContext(Dispatchers.IO) {
        val doc = parqueoRef(parqueoId).get().await()

        val tarifasRaw = doc.get("tarifas") as? Map<*, *> ?: return@withContext emptyMap()

        tarifasRaw.entries.associate { (tipoAny, valoresAny) ->
            val tipo = tipoAny.toString().trim().lowercase()
            val valoresMap = valoresAny as? Map<*, *> ?: emptyMap<String, Any>()

            val hora = (valoresMap["hora"] as? Number)?.toDouble() ?: 0.0
            val mediaHora = (valoresMap["mediaHora"] as? Number)?.toDouble() ?: 0.0

            tipo to mapOf(
                "hora" to hora,
                "mediaHora" to mediaHora
            )
        }
    }
}