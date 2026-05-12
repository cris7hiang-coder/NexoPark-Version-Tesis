package com.example.parking.Client.ViewParking

import androidx.lifecycle.ViewModel
import com.example.parking.Admin.EspacioParqueo
import com.example.parking.Client.ViewParking.CarPresent.contarDisponiblesPorTipo
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

data class ParqueoModel(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val direccion: String = "",
    val encargado: String = "",
    val imagenUrl: String = "",
    val imagenes: List<String> = emptyList(),
    val horaInicio: String = "",
    val horaFin: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val zona: String? = null,
    val capacidades: Map<String, Int> = emptyMap(),
    val tarifas: Map<String, Map<String, Double>> = emptyMap(),
    val caracteristicas: List<String> = emptyList(),
    val reglas: List<String> = emptyList(),
    val calificacion: Double = 0.0
)
fun snapshotToParqueoModel(snapshot: DocumentSnapshot): ParqueoModel {
    val nombre = snapshot.getString("nombre").orEmpty()
    val imagenUrl = snapshot.getString("imagenUrl").orEmpty()
    val descripcion = snapshot.getString("descripcion").orEmpty()
    val direccion = snapshot.getString("direccion").orEmpty()
    val horaInicio = snapshot.getString("hora_inicio") ?: "08:00"
    val horaFin = snapshot.getString("hora_fin") ?: "20:00"
    val encargado = snapshot.getString("encargado").orEmpty()
    val zona = snapshot.getString("zona")

    val capacidadesRaw =
        (snapshot["capacidades"] as? Map<*, *>)
            ?: (snapshot["capacidad"] as? Map<*, *>)
            ?: emptyMap<Any?, Any?>()

    val capacidades = capacidadesRaw.mapNotNull { (key, value) ->
        val tipo = key as? String ?: return@mapNotNull null
        val cantidad = when (value) {
            is Long -> value.toInt()
            is Int -> value
            is Double -> value.toInt()
            else -> null
        } ?: return@mapNotNull null

        tipo to cantidad
    }.toMap()

    val tarifasRaw = snapshot["tarifas"] as? Map<*, *> ?: emptyMap<Any?, Any?>()

    val tarifas = tarifasRaw.mapNotNull { (tipoKey, datosValue) ->
        val tipo = tipoKey as? String ?: return@mapNotNull null
        val datos = datosValue as? Map<*, *> ?: return@mapNotNull null

        val hora = (datos["hora"] as? Number)?.toDouble() ?: 0.0
        val mediaHora = (datos["mediaHora"] as? Number)?.toDouble() ?: 0.0

        tipo to mapOf(
            "hora" to hora,
            "mediaHora" to mediaHora
        )
    }.toMap()

    val imagenes = (snapshot["imagenes"] as? List<*>)?.mapNotNull { it as? String }
        ?.takeIf { it.isNotEmpty() }
        ?: listOfNotNull(imagenUrl.takeIf { it.isNotBlank() })

    val lat = snapshot.getDouble("latitud") ?: 0.0
    val lng = snapshot.getDouble("longitud") ?: 0.0

    val caracteristicas = (snapshot.get("caracteristicas") as? List<*>)
        ?.mapNotNull { it as? String }
        ?: emptyList()

    val reglas = (snapshot.get("reglas") as? List<*>)
        ?.mapNotNull { it as? String }
        ?: emptyList()

    val calificacion = snapshot.getDouble("calificacion") ?: 0.0

    return ParqueoModel(
        id = snapshot.id,
        nombre = nombre,
        descripcion = descripcion,
        direccion = direccion,
        encargado = encargado,
        imagenUrl = imagenUrl,
        imagenes = imagenes,
        horaInicio = horaInicio,
        horaFin = horaFin,
        latitud = lat,
        longitud = lng,
        zona = zona,
        capacidades = capacidades,
        tarifas = tarifas,
        caracteristicas = caracteristicas,
        reglas = reglas,
        calificacion = calificacion
    )
}
class ParqueoSharedViewModel : ViewModel() {

    private val _parqueoSeleccionado = MutableStateFlow<ParqueoModel?>(null)
    val parqueoSeleccionado: StateFlow<ParqueoModel?> = _parqueoSeleccionado

    private val _parqueos = MutableStateFlow<List<ParqueoModel>>(emptyList())
    val parqueos: StateFlow<List<ParqueoModel>> = _parqueos

    private val _zonas = MutableStateFlow<List<String>>(emptyList())
    val zonas: StateFlow<List<String>> = _zonas

    fun onParqueoSeleccionado(parqueo: ParqueoModel) {
        _parqueoSeleccionado.value = parqueo
    }

    fun limpiarParqueo() {
        _parqueoSeleccionado.value = null
    }

    fun actualizarParqueos(lista: List<ParqueoModel>) {
        _parqueos.value = lista
        _zonas.value = lista
            .mapNotNull { it.zona?.trim()?.takeIf(String::isNotBlank) }
            .distinct()
            .sorted()
    }
}

fun parqueoTieneDisponiblesConEspacios(
    espacios: List<EspacioParqueo>,
    tiposSeleccionados: List<String>,
    soloDisponibles: Boolean
): Boolean {
    if (!soloDisponibles) return true

    val disponiblesPorTipo = contarDisponiblesPorTipo(espacios)

    val tiposARevisar = if (tiposSeleccionados.isEmpty()) {
        disponiblesPorTipo.keys
    } else {
        tiposSeleccionados
    }

    return tiposARevisar.any { tipo ->
        (disponiblesPorTipo[tipo] ?: 0) > 0
    }
}
fun agregarAFavoritos(uid: String, parqueo: ParqueoModel) {
    val db = FirebaseFirestore.getInstance()

    val data = mapOf(
        "id" to parqueo.id,
        "nombre" to parqueo.nombre,
        "direccion" to parqueo.direccion,
        "imagenUrl" to parqueo.imagenUrl,
        "latitud" to parqueo.latitud,
        "longitud" to parqueo.longitud,
        "timestamp" to FieldValue.serverTimestamp()
    )

    db.collection("users")
        .document(uid)
        .collection("favoritos")
        .document(parqueo.id)
        .set(data)
}

fun eliminarDeFavoritos(uid: String, parqueoId: String) {
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .collection("favoritos")
        .document(parqueoId)
        .delete()
}

suspend fun esFavorito(uid: String, parqueoId: String): Boolean {
    val doc = FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .collection("favoritos")
        .document(parqueoId)
        .get()
        .await()

    return doc.exists()
}