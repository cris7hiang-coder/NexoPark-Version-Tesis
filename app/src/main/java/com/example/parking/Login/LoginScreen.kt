    package com.example.parking.Login

    import android.app.Activity
    import android.util.Log
    import android.util.Patterns
    import android.widget.Toast
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.interaction.MutableInteractionSource
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.AccountCircle
    import androidx.compose.material.icons.filled.Email
    import androidx.compose.material.icons.filled.LocalParking
    import androidx.compose.material.icons.filled.Lock
    import androidx.compose.material.icons.filled.Login
    import androidx.compose.material3.*
    import androidx.compose.ui.graphics.Color
    import androidx.compose.runtime.*
    import androidx.compose.runtime.saveable.rememberSaveable
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.input.KeyboardType
    import androidx.compose.ui.text.input.PasswordVisualTransformation
    import androidx.compose.ui.unit.dp
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavController
    import com.example.parking.Login.GoogleSignInHelper.updateFcmToken
    import com.example.parking.components.auth.AuthViewModel
    import com.google.firebase.Firebase
    import com.google.firebase.auth.AuthCredential
    import com.google.firebase.auth.EmailAuthProvider
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FirebaseFirestore

    import kotlinx.coroutines.tasks.await
    @Composable
    fun LoginScreen(
        navController: NavController,
        onLoginSuccess: () -> Unit,
        onNavigateToRegister: () -> Unit
    ) {
        val context = LocalContext.current
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        var email by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var isLoading by rememberSaveable { mutableStateOf(false) }

        var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
        var passwordForLinking by rememberSaveable { mutableStateOf("") }
        var googleCredential by remember { mutableStateOf<AuthCredential?>(null) }
        var googleEmail by rememberSaveable { mutableStateOf("") }

        var emailError by rememberSaveable { mutableStateOf<String?>(null) }
        var passwordError by rememberSaveable { mutableStateOf<String?>(null) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                GoogleSignInHelper.handleGoogleSignInResult(
                    context = context,
                    data = result.data,
                    onSuccess = { onLoginSuccess() },
                    onError = { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    onRequirePassword = { emailResult, credential ->
                        googleEmail = emailResult
                        googleCredential = credential
                        passwordForLinking = ""
                        showPasswordDialog = true
                    }
                )
            }
        }

        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { showPasswordDialog = false },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "Vincular cuenta existente",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Ya existe una cuenta con este correo. Ingresa tu contraseña para vincularla con Google.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AppPasswordField(
                            label = "Contraseña",
                            value = passwordForLinking,
                            onValueChange = { passwordForLinking = it }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (passwordForLinking.isBlank()) return@TextButton

                            auth.signInWithEmailAndPassword(googleEmail, passwordForLinking)
                                .addOnSuccessListener { emailResult ->
                                    googleCredential?.let { credential ->
                                        emailResult.user?.linkWithCredential(credential)
                                            ?.addOnSuccessListener {
                                                db.collection("users")
                                                    .document(emailResult.user!!.uid)
                                                    .get()
                                                    .addOnSuccessListener {
                                                        onLoginSuccess()
                                                    }
                                            }
                                            ?.addOnFailureListener {
                                                Toast.makeText(
                                                    context,
                                                    "Error al vincular cuentas",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Contraseña incorrecta",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            showPasswordDialog = false
                        }
                    ) {
                        Text(
                            text = "Vincular",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasswordDialog = false }) {
                        Text(
                            text = "Cancelar",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }

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
                Spacer(modifier = Modifier.height(56.dp))

                LoginHeader()

                Spacer(modifier = Modifier.height(36.dp))

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
                                passwordError = if (it.isBlank()) "Ingresa tu contraseña" else null
                            },
                            isError = passwordError != null,
                            supportingText = passwordError
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { navController.navigate("recuperar_contrasena") },
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    text = "¿Olvidaste tu contraseña?",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        PrimaryButton(
                            text = "Iniciar sesión",
                            icon = Icons.Default.Login,
                            loading = isLoading,
                            onClick = {
                                emailError = when {
                                    email.isBlank() -> "Ingresa tu correo"
                                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Correo inválido"
                                    else -> null
                                }

                                passwordError = if (password.isBlank()) {
                                    "Ingresa tu contraseña"
                                } else {
                                    null
                                }

                                val hasError = listOf(emailError, passwordError).any { it != null }
                                if (hasError || isLoading) return@PrimaryButton

                                isLoading = true

                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnSuccessListener { result ->
                                        val uid = result.user?.uid
                                        if (uid != null) {
                                            updateFcmToken(uid)
                                        }
                                        isLoading = false
                                        onLoginSuccess()
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

                        GoogleButton(
                            text = "Continuar con Google",
                            onClick = {
                                val intent = GoogleSignInHelper.getGoogleClient(context).signInIntent
                                launcher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "¿No tienes cuenta? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                    )
                    Text(
                        text = "Regístrate",
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onNavigateToRegister()
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
    private fun LoginHeader() {
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
                    imageVector = Icons.Default.LocalParking,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Bienvenido",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Accede para gestionar reservas, ingresos y salidas de forma rápida y segura.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
            )
        }
    }
