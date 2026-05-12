package com.example.parking.components.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parking.Login.GoogleSignInHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole

    private var roleListener: ListenerRegistration? = null

    init {
        // Cargar estado inicial
        // IMPORTANTE: siempre marcar loading false al terminar
        viewModelScope.launch {
            _isLoading.value = true
        }
    }

    fun checkUserStatus(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            _isLoggedIn.value = true        // 🔥 Importante
            listenToUserRole(currentUser.uid)
            return
        }

        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount != null) {

            val credential = GoogleAuthProvider.getCredential(googleAccount.idToken, null)

            auth.signInWithCredential(credential)
                .addOnSuccessListener {
                    _isLoggedIn.value = true      // 🔥 Importante
                    listenToUserRole(it.user!!.uid)
                }
                .addOnFailureListener {
                    clearSession()
                }

            return
        }

        clearSession()
    }


    fun setLoginSuccess(context: Context) {
        _isLoading.value = true     // 🔥 Esto evita el loading eterno
        checkUserStatus(context)
    }


    private fun listenToUserRole(uid: String) {
        roleListener?.remove()

        roleListener = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    clearSession()
                    return@addSnapshotListener
                }

                val role = snapshot?.getString("role")

                _userRole.value = role

                // Usuario YA está autenticado
                _isLoggedIn.value = true

                // Terminar loading aunque rol tarde
                _isLoading.value = false
            }
    }

    fun logout(context: Context) {
        GoogleSignInHelper.getGoogleClient(context).signOut()
        FirebaseAuth.getInstance().signOut()
        clearSession()
    }

    private fun clearSession() {
        roleListener?.remove()
        roleListener = null

        _isLoggedIn.value = false
        _userRole.value = null
        _isLoading.value = false
    }
}
