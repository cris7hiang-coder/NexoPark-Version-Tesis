package com.example.parking.SupAdmin

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SuperAdminViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _users = MutableStateFlow<List<DocumentSnapshot>>(emptyList())
    val users: StateFlow<List<DocumentSnapshot>> = _users

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadUsers()
    }

    fun loadUsers() {
        db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = "Error al escuchar cambios"
                    return@addSnapshotListener
                }
                _users.value = snapshot?.documents ?: emptyList()
                _isLoading.value = false
            }
    }



    fun cambiarRol(uid: String, nuevoRol: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("users").document(uid)
            .update("role", nuevoRol)
            .addOnSuccessListener {
                loadUsers()
                onSuccess()
            }
            .addOnFailureListener {
                onError("Error al cambiar rol")
            }
    }
}
