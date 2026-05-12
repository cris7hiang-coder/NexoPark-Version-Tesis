package com.example.parking.Login

import android.util.Patterns
import android.widget.Toast

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun RegisterScreen(
    navController: NavHostController,
    onRegisterSuccess: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var fullName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var passwordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }
    var fullNameError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            RegisterHeader()

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    AppTextField(
                        label = "Nombre completo",
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            fullNameError = if (it.trim().isBlank()) {
                                "Ingresa tu nombre"
                            } else {
                                null
                            }
                        },
                        leading = Icons.Default.Person,
                        isError = fullNameError != null,
                        supportingText = fullNameError
                    )

                    AppTextField(
                        label = "Correo electrónico",
                        value = email,
                        onValueChange = {
                            email = it.trim()
                            emailError = when {
                                it.isBlank() -> "Ingresa tu correo"
                                !Patterns.EMAIL_ADDRESS.matcher(it).matches() -> "Correo inválido"
                                else -> null
                            }
                        },
                        leading = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        isError = emailError != null,
                        supportingText = emailError
                    )

                    AppPasswordField(
                        label = "Contraseña",
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = when {
                                it.isBlank() -> "Ingresa tu contraseña"
                                it.length < 6 -> "Mínimo 6 caracteres"
                                else -> null
                            }

                            confirmPasswordError = when {
                                confirmPassword.isBlank() -> confirmPasswordError
                                confirmPassword != it -> "No coincide con la contraseña"
                                else -> null
                            }
                        },
                        isError = passwordError != null,
                        supportingText = passwordError
                    )

                    AppPasswordField(
                        label = "Confirmar contraseña",
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            confirmPasswordError = when {
                                it.isBlank() -> "Confirma tu contraseña"
                                it != password -> "No coincide con la contraseña"
                                else -> null
                            }
                        },
                        isError = confirmPasswordError != null,
                        supportingText = confirmPasswordError
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    PrimaryButton(
                        text = "Crear cuenta",
                        loading = isLoading,
                        onClick = {
                            fullNameError = if (fullName.trim().isBlank()) {
                                "Ingresa tu nombre"
                            } else {
                                null
                            }

                            emailError = when {
                                email.isBlank() -> "Ingresa tu correo"
                                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Correo inválido"
                                else -> null
                            }

                            passwordError = when {
                                password.isBlank() -> "Ingresa tu contraseña"
                                password.length < 6 -> "Mínimo 6 caracteres"
                                else -> null
                            }

                            confirmPasswordError = when {
                                confirmPassword.isBlank() -> "Confirma tu contraseña"
                                confirmPassword != password -> "No coincide con la contraseña"
                                else -> null
                            }

                            val hasError = listOf(
                                fullNameError,
                                emailError,
                                passwordError,
                                confirmPasswordError
                            ).any { it != null }

                            if (hasError || isLoading) return@PrimaryButton

                            isLoading = true

                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid
                                    if (uid == null) {
                                        Toast.makeText(
                                            context,
                                            "UID no disponible",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isLoading = false
                                        return@addOnSuccessListener
                                    }

                                    val userData = mapOf(
                                        "uid" to uid,
                                        "name" to fullName.trim(),
                                        "email" to email.trim(),
                                        "role" to "cliente"
                                    )

                                    db.collection("users")
                                        .document(uid)
                                        .set(userData)
                                        .addOnSuccessListener {
                                            isLoading = false
                                            onRegisterSuccess()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                context,
                                                "Error al guardar usuario",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isLoading = false
                                        }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error: ${it.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    isLoading = false
                                }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "¿Ya tienes cuenta? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                )

                Text(
                    text = "Inicia sesión",
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        navController.popBackStack()
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RegisterHeader() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PersonAddAlt1,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Regístrate para reservar estacionamientos y gestionar tus ingresos y salidas de forma simple y segura.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
    }
}
