package com.example.parking.Admin
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModel
import com.example.parking.Client.ViewParking.CarReserv.point.ReseñaModel
import com.example.parking.Client.ViewParking.CarReserv.point.calcularStats
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _parqueo = MutableStateFlow<DocumentSnapshot?>(null)
    val parqueo: StateFlow<DocumentSnapshot?> = _parqueo

    private val _reservas = MutableStateFlow<List<DocumentSnapshot>>(emptyList())
    val reservas: StateFlow<List<DocumentSnapshot>> = _reservas

    private val _reseñas = MutableStateFlow<List<ReseñaModel>>(emptyList())
    val reseñas: StateFlow<List<ReseñaModel>> = _reseñas

    private val _promedio = MutableStateFlow(0f)
    val promedio: StateFlow<Float> = _promedio

    private val _espacios = MutableStateFlow<List<EspacioParqueo>>(emptyList())
    val espacios: StateFlow<List<EspacioParqueo>> = _espacios

    data class IngresoMensual(val mes: String, val total: Double)

    private val _ingresosMensuales = MutableStateFlow<List<IngresoMensual>>(emptyList())
    val ingresosMensuales: StateFlow<List<IngresoMensual>> = _ingresosMensuales

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _isLoading.value = true

            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                _isLoading.value = false
                return@launch
            }

            try {
                val parqueoQuery = db.collection("parqueos")
                    .whereEqualTo("adminId", uid)
                    .limit(1)
                    .get()
                    .await()

                val parqueoDoc = parqueoQuery.documents.firstOrNull()
                _parqueo.value = parqueoDoc

                if (parqueoDoc != null) {
                    val parqueoId = parqueoDoc.id

                    val reservasSnap = db.collection("reservas")
                        .whereEqualTo("parqueoId", parqueoId)
                        .get()
                        .await()

                    _reservas.value = reservasSnap.documents

                    val reseñasSnap = db.collection("parqueos")
                        .document(parqueoId)
                        .collection("reseñas")
                        .orderBy("fecha", Query.Direction.DESCENDING)
                        .get()
                        .await()

                    val listaReseñas = reseñasSnap.documents.map { doc ->
                        ReseñaModel(
                            userName = doc.getString("userName") ?: "Anónimo",
                            photoUrl = doc.getString("photoUrl"),
                            conformidad = doc.getLong("conformidad")?.toInt() ?: 1,
                            comentario = doc.getString("comentario").orEmpty(),
                            chips = (doc.get("chips") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            fecha = doc.getTimestamp("fecha")
                                ?.toDate()
                                ?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) }
                                ?: ""
                        )
                    }

                    _reseñas.value = listaReseñas
                    _promedio.value = calcularStats(listaReseñas).first

                    db.collection("parqueos")
                        .document(parqueoId)
                        .collection("pagos")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null || snapshot == null) return@addSnapshotListener

                            val ingresos = snapshot.documents
                                .groupBy { it.getString("mes") ?: "N/A" }
                                .map { (mes, docs) ->
                                    IngresoMensual(
                                        mes = mes,
                                        total = docs.sumOf { it.getDouble("monto") ?: 0.0 }
                                    )
                                }
                                .sortedBy { it.mes }

                            _ingresosMensuales.value = ingresos
                        }
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al cargar datos", e)
            }

            _isLoading.value = false
        }
    }

    private var espaciosListener: ListenerRegistration? = null

    fun cargarEspacios(parqueoId: String) {
        espaciosListener?.remove()

        espaciosListener = db.collection("parqueos")
            .document(parqueoId)
            .collection("espacios")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    Log.e("AdminViewModel", "Error cargando espacios: ${e?.message}")
                    return@addSnapshotListener
                }

                val lista = snapshot.documents.mapNotNull { doc ->
                    val tipo = doc.getString("tipoVehiculo") ?: return@mapNotNull null
                    val indice = (doc.getLong("indice") ?: 0L).toInt()

                    val estado = when (doc.getString("estado")) {
                        "RESERVADO" -> EstadoEspacio.RESERVADO
                        "OCUPADO" -> EstadoEspacio.OCUPADO
                        else -> EstadoEspacio.DISPONIBLE
                    }

                    EspacioParqueo(tipo, indice, estado)
                }

                _espacios.value = lista
            }
    }
    override fun onCleared() {
        espaciosListener?.remove()
        super.onCleared()
    }

    fun isParqueoCompleto(): Boolean {
        val doc = _parqueo.value ?: return false

        val tieneCapacidades = doc.contains("capacidades") || doc.contains("capacidad")

        return doc.contains("latitud") &&
                doc.contains("longitud") &&
                tieneCapacidades &&
                doc.contains("tarifas") &&
                doc.contains("tiposVehiculo")
    }

}
