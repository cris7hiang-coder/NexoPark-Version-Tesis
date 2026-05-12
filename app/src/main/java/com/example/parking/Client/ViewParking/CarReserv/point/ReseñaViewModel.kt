package com.example.parking.Client.ViewParking.CarReserv.point

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReseñaViewModel : ViewModel() {

    fun agregarReseña(
        reservaId: String,
        parqueoId: String,
        conformidad: Int,
        chips: List<String>,
        comentario: String,
        anonimo: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: run {
            onResult(false)
            return
        }

        val db = FirebaseFirestore.getInstance()

        viewModelScope.launch {
            try {
                val userSnap = db.collection("users")
                    .document(uid)
                    .get()
                    .await()

                val userName = if (anonimo) {
                    "Anónimo"
                } else {
                    userSnap.getString("nombre")
                        ?: userSnap.getString("name")
                        ?: auth.currentUser?.displayName
                        ?: "Usuario"
                }

                val photoUrl = if (anonimo) {
                    null
                } else {
                    userSnap.getString("photoUrl")
                        ?: auth.currentUser?.photoUrl?.toString()
                }

                val reseñaRef = db.collection("parqueos")
                    .document(parqueoId)
                    .collection("reseñas")
                    .document()

                db.runBatch { batch ->
                    batch.set(
                        reseñaRef,
                        mapOf(
                            "uid" to if (anonimo) null else uid,
                            "reservaId" to reservaId,
                            "parqueoId" to parqueoId,
                            "userName" to userName,
                            "photoUrl" to photoUrl,
                            "conformidad" to conformidad,
                            "chips" to chips,
                            "comentario" to comentario.trim(),
                            "anonimo" to anonimo,
                            "fecha" to FieldValue.serverTimestamp()
                        )
                    )

                    batch.update(
                        db.collection("reservas").document(reservaId),
                        mapOf(
                            "reseñaEnviada" to true,
                            "fechaReseña" to FieldValue.serverTimestamp()
                        )
                    )
                }.await()

                onResult(true)
            } catch (e: Exception) {
                Log.e("ReseñaViewModel", "Error al guardar reseña: ${e.message}", e)
                onResult(false)
            }
        }
    }
}
data class ReseñaModel(
    val userName: String = "Anónimo",
    val photoUrl: String? = null,
    val conformidad: Int = 1,
    val comentario: String = "",
    val chips: List<String> = emptyList(),
    val fecha: String = ""
)

fun calcularStats(reseñas: List<ReseñaModel>): Pair<Float, Map<Int, Int>> {
    val total = reseñas.size
    val positivas = reseñas.count { it.conformidad == 1 }
    val negativas = reseñas.count { it.conformidad == -1 }

    val promedioPositivo = if (total == 0) {
        0f
    } else {
        positivas.toFloat() / total
    }

    val stats = mapOf(
        1 to positivas,
        -1 to negativas
    )

    return promedioPositivo to stats
}