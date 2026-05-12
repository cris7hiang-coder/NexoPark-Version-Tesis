package com.example.parking.ui.screens.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parking.Login.AppTextField
import com.example.parking.Login.PrimaryButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun RecuperarContrasenaScreen(
    onBackToLogin: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var mensaje by rememberSaveable { mutableStateOf<String?>(null) }
    var emailError by rememberSaveable { mutableStateOf<String?>(null) }
    var showContent by remember { mutableStateOf(false) }

    fun isEmailValid(value: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(value.trim()).matches()
    }

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    val enter = fadeIn(
        animationSpec = tween(300)
    ) + slideInVertically(
        animationSpec = tween(300),
        initialOffsetY = { it / 10 }
    )

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
                .imePadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = enter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RecuperarContrasenaHeader()

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
                                label = "Correo electrónico",
                                value = email,
                                onValueChange = {
                                    email = it.trim()
                                    emailError = when {
                                        it.isBlank() -> "Ingresa tu correo"
                                        !isEmailValid(it) -> "Correo inválido"
                                        else -> null
                                    }
                                    if (mensaje != null) mensaje = null
                                },
                                leading = Icons.Default.Email,
                                keyboardType = KeyboardType.Email,
                                isError = emailError != null,
                                supportingText = emailError
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            PrimaryButton(
                                text = "Enviar correo",
                                loading = isLoading,
                                icon = Icons.Default.Send,
                                onClick = {
                                    emailError = when {
                                        email.isBlank() -> "Ingresa tu correo"
                                        !isEmailValid(email) -> "Correo inválido"
                                        else -> null
                                    }

                                    if (emailError != null || isLoading) return@PrimaryButton

                                    isLoading = true
                                    mensaje = null

                                    auth.sendPasswordResetEmail(email.trim())
                                        .addOnSuccessListener {
                                            mensaje = "Te enviamos un correo para restablecer tu contraseña."
                                        }
                                        .addOnFailureListener {
                                            mensaje = "Error: ${it.message}"
                                        }
                                        .addOnCompleteListener {
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
                            text = "¿Recordaste tu contraseña? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                        )

                        Text(
                            text = "Inicia sesión",
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onBackToLogin()
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    mensaje?.let { texto ->
                        Spacer(modifier = Modifier.height(24.dp))

                        val esError = texto.startsWith("Error")

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (esError) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (esError) {
                                        Icons.Default.ErrorOutline
                                    } else {
                                        Icons.Default.MarkEmailRead
                                    },
                                    contentDescription = null,
                                    tint = if (esError) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = texto,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (esError) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun RecuperarContrasenaHeader() {
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
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Recuperar contraseña",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ingresa tu correo y te enviaremos un enlace para restablecer tu contraseña de forma segura.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
    }
}