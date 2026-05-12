package com.example.parking.Client.ViewParking.CarPresent

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parking.Admin.EspacioParqueo
import com.example.parking.Admin.EstadoEspacio
import com.example.parking.Client.ViewParking.CarReserv.point.ReseñaModel
import com.example.parking.Client.ViewParking.CarReserv.point.calcularStats
import com.example.parking.Client.ViewParking.ParqueoModel
import com.example.parking.Client.ViewParking.agregarAFavoritos
import com.example.parking.Client.ViewParking.eliminarDeFavoritos
import com.example.parking.Client.ViewParking.esFavorito
import com.example.parking.Client.ViewParking.snapshotToParqueoModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
class DetalleParqueoViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var parqueo by mutableStateOf<ParqueoModel?>(null)
        private set

    var promedio by mutableStateOf(0f)
        private set

    var reseñas by mutableStateOf<List<ReseñaModel>>(emptyList())
        private set

    var isFavorito by mutableStateOf(false)
        private set

    var cargando by mutableStateOf(false)
        private set

    fun cargarDatos(parqueoId: String) {
        val uid = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            cargando = true
            try {
                cargarParqueo(parqueoId)
                cargarReseñas(parqueoId)
                isFavorito = esFavorito(uid, parqueoId)
            } catch (e: Exception) {
                Log.e("DetalleParqueoVM", "Error al cargar datos", e)
            } finally {
                cargando = false
            }
        }
    }

    private suspend fun cargarParqueo(parqueoId: String) {
        val snap = db.collection("parqueos")
            .document(parqueoId)
            .get()
            .await()

        if (snap.exists()) {
            parqueo = snapshotToParqueoModel(snap)
        }
    }

    private suspend fun cargarReseñas(parqueoId: String) {
        val reseñasSnap = db.collection("parqueos")
            .document(parqueoId)
            .collection("reseñas")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .await()

        val lista = reseñasSnap.documents.map { doc ->
            doc.toReseñaModel()
        }

        reseñas = lista
        promedio = calcularStats(lista).first
    }

    fun toggleFavorito() {
        val uid = auth.currentUser?.uid ?: return
        val currentParqueo = parqueo ?: return

        viewModelScope.launch {
            if (isFavorito) {
                eliminarDeFavoritos(uid, currentParqueo.id)
            } else {
                agregarAFavoritos(uid, currentParqueo)
            }
            isFavorito = !isFavorito
        }
    }
}
fun contarDisponiblesPorTipo(espacios: List<EspacioParqueo>): Map<String, Int> {
    return espacios
        .filter { it.estado == EstadoEspacio.DISPONIBLE }
        .groupingBy { it.tipoVehiculo }
        .eachCount()
}

fun formatearHorario(horaInicio: String, horaFin: String): String {
    val inicio = horaInicio.trim().replace(".", ":")
    val fin = horaFin.trim().replace(".", ":")

    return if (inicio == "00:00" && (fin == "23:59" || fin == "23:59")) {
        "24 Hrs"
    } else {
        "$inicio - $fin"
    }
}
fun DocumentSnapshot.toReseñaModel(): ReseñaModel {
    return ReseñaModel(
        userName = getString("userName") ?: "Anónimo",
        photoUrl = getString("photoUrl"),
        conformidad = getLong("conformidad")?.toInt() ?: 1,
        comentario = getString("comentario") ?: "",
        chips = (get("chips") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        fecha = getTimestamp("fecha")?.toDate()?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
        } ?: ""
    )
}