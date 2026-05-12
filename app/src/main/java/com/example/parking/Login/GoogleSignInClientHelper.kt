package com.example.parking.Login

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.parking.R
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessaging
object GoogleSignInHelper {

    /** Obtiene el cliente de Google SignIn configurado con ID de cliente web */
    fun getGoogleClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.WEB_CLIENT_ID))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Maneja el resultado de Google SignIn
     * onSuccess: login exitoso (sin parámetros)
     * onRequirePassword: cuenta previa con email+password
     */
    fun handleGoogleSignInResult(
        context: Context,
        data: Intent?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onRequirePassword: (String, AuthCredential) -> Unit
    ) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)

        try {
            val account = task.getResult(ApiException::class.java)
                ?: return onError("Cuenta de Google inválida")

            val emailFromGoogle = account.email
            if (emailFromGoogle.isNullOrEmpty()) {
                return onError("No se pudo obtener el correo desde Google.")
            }

            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val auth = FirebaseAuth.getInstance()

            // Verificar si ya existe una cuenta con email/password
            auth.fetchSignInMethodsForEmail(emailFromGoogle)
                .addOnSuccessListener { result ->
                    val methods = result.signInMethods ?: emptyList()

                    if (methods.contains(EmailAuthProvider.EMAIL_PASSWORD_SIGN_IN_METHOD)) {
                        // EXISTE CUENTA NORMAL → pedir contraseña
                        onRequirePassword(emailFromGoogle, credential)
                    } else {
                        // NO existe → login directo con Google
                        signInWithGoogle(auth, credential, account, onSuccess, onError)
                    }
                }
                .addOnFailureListener {
                    onError("Error verificando método de inicio: ${it.message}")
                }

        } catch (e: ApiException) {
            onError("Error al procesar Google SignIn: ${e.message}")
        }
    }

    private fun signInWithGoogle(
        auth: FirebaseAuth,
        credential: AuthCredential,
        account: GoogleSignInAccount,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val user = authResult.user ?: return@addOnSuccessListener onError("Usuario inválido")

                val finalEmail = user.email ?: account.email ?: "no-email"
                val finalName = user.displayName ?: account.displayName ?: "Sin nombre"

                checkOrCreateUserInFirestore(
                    user.uid,
                    finalEmail,
                    finalName,
                    onSuccess,
                    onError
                )
            }
            .addOnFailureListener {
                onError("Error al iniciar sesión con Google: ${it.message}")
            }
    }

    private fun checkOrCreateUserInFirestore(
        uid: String,
        email: String,
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(uid)

        ref.get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    val userData = mapOf(
                        "email" to email,
                        "name" to name,
                        "role" to "cliente"
                    )
                    ref.set(userData)
                        .addOnFailureListener {
                            onError("Error guardando usuario en Firestore: ${it.message}")
                        }
                }

                updateFcmToken(uid)
                onSuccess()   // YA SIN ROLE
            }
            .addOnFailureListener {
                onError("Error obteniendo usuario en Firestore: ${it.message}")
            }
    }

    /** Actualiza el token FCM */
    fun updateFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Firebase.firestore.collection("users")
                    .document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
            }
        }
    }
}
