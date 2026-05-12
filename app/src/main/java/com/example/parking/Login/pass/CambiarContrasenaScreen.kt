package com.example.parking.Login.pass

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parking.Login.AppPasswordField
import com.example.parking.Login.PrimaryButton
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CambiarContrasenaScreen(
    onPasswordChanged: () -> Unit,
    onCancel: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    var currentPasswordError by rememberSaveable { mutableStateOf<String?>(null) }
    var newPasswordError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmPasswordError by rememberSaveable { mutableStateOf<String?>(null) }

    var isLoading by rememberSaveable { mutableStateOf(false) }
    var feedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var showContent by remember { mutableStateOf(false) }

    fun isValidPassword(password: String): Boolean = password.length >= 6

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

    Scaffold(
        topBar = {
            MinimalTopBarCompact(
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            AnimatedVisibility(
                visible = showContent,
                enter = enter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CambiarContrasenaHeader()

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
                            AppPasswordField(
                                label = "Contraseña actual",
                                value = currentPassword,
                                onValueChange = {
                                    currentPassword = it
                                    currentPasswordError = if (it.isBlank()) {
                                        "Ingresa tu contraseña actual"
                                    } else {
                                        null
                                    }
                                    if (feedbackMessage != null) feedbackMessage = null
                                },
                                isError = currentPasswordError != null,
                                supportingText = currentPasswordError
                            )

                            AppPasswordField(
                                label = "Nueva contraseña",
                                value = newPassword,
                                onValueChange = {
                                    newPassword = it
                                    newPasswordError = when {
                                        it.isBlank() -> "Ingresa tu nueva contraseña"
                                        !isValidPassword(it) -> "Mínimo 6 caracteres"
                                        else -> null
                                    }

                                    confirmPasswordError = when {
                                        confirmPassword.isBlank() -> confirmPasswordError
                                        confirmPassword != it -> "No coincide con la nueva contraseña"
                                        else -> null
                                    }

                                    if (feedbackMessage != null) feedbackMessage = null
                                }
                                ,
                                isError = newPasswordError != null,
                                supportingText = newPasswordError
                            )

                            AppPasswordField(
                                label = "Confirmar nueva contraseña",
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    confirmPasswordError = when {
                                        it.isBlank() -> "Confirma la nueva contraseña"
                                        it != newPassword -> "No coincide con la nueva contraseña"
                                        else -> null
                                    }
                                    if (feedbackMessage != null) feedbackMessage = null
                                },
                                leading = Icons.Default.CheckCircle,
                                isError = confirmPasswordError != null,
                                supportingText = confirmPasswordError
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            PrimaryButton(
                                text = "Actualizar contraseña",
                                icon = Icons.Default.Check,
                                loading = isLoading,
                                onClick = {
                                    currentPasswordError = if (currentPassword.isBlank()) {
                                        "Ingresa tu contraseña actual"
                                    } else {
                                        null
                                    }

                                    newPasswordError = when {
                                        newPassword.isBlank() -> "Ingresa tu nueva contraseña"
                                        !isValidPassword(newPassword) -> "Mínimo 6 caracteres"
                                        else -> null
                                    }

                                    confirmPasswordError = when {
                                        confirmPassword.isBlank() -> "Confirma la nueva contraseña"
                                        confirmPassword != newPassword -> "No coincide con la nueva contraseña"
                                        else -> null
                                    }

                                    val hasError = listOf(
                                        currentPasswordError,
                                        newPasswordError,
                                        confirmPasswordError
                                    ).any { it != null }

                                    if (hasError || isLoading) return@PrimaryButton

                                    val user = auth.currentUser
                                    val email = user?.email

                                    if (user == null || email.isNullOrBlank()) {
                                        feedbackMessage = "Error: No se pudo validar la cuenta actual."
                                        return@PrimaryButton
                                    }

                                    isLoading = true
                                    feedbackMessage = null

                                    val credential = EmailAuthProvider.getCredential(
                                        email,
                                        currentPassword
                                    )

                                    user.reauthenticate(credential)
                                        .addOnSuccessListener {
                                            user.updatePassword(newPassword)
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    Toast.makeText(
                                                        context,
                                                        "Contraseña actualizada correctamente",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    onPasswordChanged()
                                                }
                                                .addOnFailureListener {
                                                    isLoading = false
                                                    feedbackMessage = "Error: ${it.message}"
                                                }
                                        }
                                        .addOnFailureListener {
                                            isLoading = false
                                            feedbackMessage = "Error: La contraseña actual es incorrecta."
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
                            text = "¿No deseas continuar? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                        )

                        Text(
                            text = "Cancelar",
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onCancel()
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    feedbackMessage?.let { texto ->
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
                                        Icons.Default.CheckCircle
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
private fun CambiarContrasenaHeader() {
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
            text = "Cambiar contraseña",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Actualiza tu contraseña para mantener tu cuenta protegida y segura.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
    }
}