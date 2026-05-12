package com.example.parking.Client

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parking.Admin.EspacioParqueo
import com.example.parking.Admin.EstadoEspacio
import com.example.parking.Client.ViewParking.ParqueoModel
import com.example.parking.Client.ViewParking.snapshotToParqueoModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ClienteHomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    val loading = mutableStateOf(true)
    val error = mutableStateOf<String?>(null)
    val usuario = mutableStateOf<Usuario?>(null)
    val parqueos = mutableStateOf<List<ParqueoModel>>(emptyList())
    val espaciosPorParqueo = mutableStateMapOf<String, List<EspacioParqueo>>()

    // Cargar usuario
    fun cargarUsuario(uid: String) {
        viewModelScope.launch {
            loading.value = true
            try {
                val doc = db.collection("users").document(uid).get().await()
                usuario.value = Usuario(
                    uid = uid,
                    nombre = doc.getString("name") ?: "Usuario",
                    photoUrl = doc.getString("photoUrl")
                )
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                loading.value = false
            }
        }
    }

    // Cargar parqueos dentro de un área
    fun cargarParqueosEnArea(
        latMin: Double, latMax: Double,
        lngMin: Double, lngMax: Double
    ) {
        viewModelScope.launch {
            loading.value = true
            try {
                val snapshot = db.collection("parqueos")
                    .whereGreaterThanOrEqualTo("latitud", latMin)
                    .whereLessThanOrEqualTo("latitud", latMax)
                    .get()
                    .await()

                parqueos.value = snapshot.documents.map { snapshotToParqueoModel(it) }
            } catch (e: Exception) {
                error.value = e.message
            } finally {
                loading.value = false
            }
        }
    }

    // Lazy loading: cargar espacios solo al seleccionar parqueo
    fun cargarEspaciosParqueo(parqueoId: String) {
        if (espaciosPorParqueo.containsKey(parqueoId)) return // ya cargado
        viewModelScope.launch {
            try {
                val snap = db.collection("parqueos")
                    .document(parqueoId)
                    .collection("espacios")
                    .get()
                    .await()

                val lista = snap.documents.mapNotNull { doc ->
                    val tipo = doc.getString("tipoVehiculo") ?: return@mapNotNull null
                    val indice = doc.getLong("indice")?.toInt() ?: return@mapNotNull null
                    val estado = EstadoEspacio.valueOf(doc.getString("estado")?.uppercase() ?: "DISPONIBLE")
                    EspacioParqueo(tipo, indice, estado)
                }

                espaciosPorParqueo[parqueoId] = lista
            } catch (_: Exception) { }
        }
    }
}

data class Usuario(val uid: String, val nombre: String, val photoUrl: String?)
