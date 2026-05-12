package com.example.parking.Client.Security.Country
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await
@Composable
fun VerificacionTelefonoScreen(navController: NavController) {
    val context = LocalContext.current

    var yaVerificado by remember { mutableStateOf<Boolean?>(null) }
    var phoneNumber by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var verificationId by remember { mutableStateOf("") }
    var smsCode by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf<Country?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var resendTimer by remember { mutableStateOf(60) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Inicializar con un país por defecto (ej: España)
    LaunchedEffect(Unit) {
        if (selectedCountry == null) {
            selectedCountry = countries.find { it.code == "ES" } ?: countries.first()
        }

        val user = FirebaseAuth.getInstance().currentUser
        yaVerificado = if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
                .await()
                .getBoolean("telefonoVerificado") == true
        } else false
    }

    // Contador de reenvío
    LaunchedEffect(codeSent) {
        if (codeSent) {
            resendTimer = 60
            while (resendTimer > 0) {
                delay(1000)
                resendTimer--
            }
        }
    }
    when (yaVerificado) {
        null -> {
            // Cargando...
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        true -> {
            // Mostrar animación de éxito y redirigir
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LottieAnimation(
                    composition = rememberLottieComposition(LottieCompositionSpec.Asset("success_check.json")).value,
                    iterations = 1,
                    modifier = Modifier.size(200.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text("Tu número ya está verificado ", style = MaterialTheme.typography.headlineSmall)
            }
        }
        false -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (!codeSent) "Verifica tu número" else "Ingresa el código SMS",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(Modifier.height(16.dp))

            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (!codeSent) {

                //  Selector de país
                CountrySelector(
                    selectedCountry = selectedCountry,
                    onCountrySelected = { country ->
                        selectedCountry = country
                        phoneNumber = ""
                        errorMessage = null
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                //  Campo de número
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { newValue ->
                        phoneNumber = newValue.filter { it.isDigit() }

                        selectedCountry?.let { country ->
                            val (isValid, error) = validatePhoneNumber(phoneNumber, country)
                            errorMessage = if (!isValid) error else null
                        }
                    },
                    label = { Text("Número telefónico") },
                    leadingIcon = {
                        Text(
                            text = selectedCountry?.dialCode ?: "+",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (errorMessage == null)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )


                Spacer(Modifier.height(8.dp))

                // Mostrar información del país seleccionado
                selectedCountry?.let { country ->
                    Text(
                        text = "Formato: ${country.minLength}-${country.maxLength} dígitos${
                            if (country.validStart.isNotEmpty())
                                ", comienza con: ${country.validStart.joinToString(", ")}"
                            else ""
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Botón enviar
                Button(
                    onClick = {
                        val country = selectedCountry ?: run {
                            errorMessage = "Selecciona un país"
                            return@Button
                        }

                        val validation = validatePhoneNumber(phoneNumber, country)
                        if (!validation.first) {
                            errorMessage = validation.second
                            return@Button
                        }

                        isSending = true
                        errorMessage = null

                        val fullNumber = country.dialCode + phoneNumber

                        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                            .setPhoneNumber(fullNumber)
                            .setTimeout(60L, TimeUnit.SECONDS)
                            .setActivity(context.findActivity())
                            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                    FirebaseAuth.getInstance().currentUser
                                        ?.linkWithCredential(credential)
                                        ?.addOnSuccessListener {
                                            guardarTelefonoEnFirestore(
                                                phone = fullNumber,
                                                context = context,
                                                navController = navController
                                            )
                                        }
                                        ?.addOnFailureListener {
                                            errorMessage = "Error al verificar automáticamente"
                                        }
                                }


                                override fun onVerificationFailed(e: FirebaseException) {
                                    isSending = false
                                    errorMessage = when (e) {
                                        is FirebaseAuthInvalidCredentialsException -> "Número inválido"
                                        is FirebaseTooManyRequestsException -> "Demasiados intentos"
                                        else -> e.message ?: "Error desconocido"
                                    }
                                }

                                override fun onCodeSent(
                                    verificationIdParam: String,
                                    token: PhoneAuthProvider.ForceResendingToken
                                ) {
                                    verificationId = verificationIdParam
                                    codeSent = true
                                    resendTimer = 60
                                    isSending = false
                                }
                            }).build()

                        PhoneAuthProvider.verifyPhoneNumber(options)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending &&
                            selectedCountry != null &&
                            phoneNumber.isNotEmpty() &&
                            errorMessage == null
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Enviando...")
                    } else {
                        Text("Enviar código SMS")
                    }
                }

            } else {
                // --------- VERIFICACIÓN DE CÓDIGO SMS ---------
                OutlinedTextField(
                    value = smsCode,
                    onValueChange = { smsCode = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Código SMS de 6 dígitos") },
                    placeholder = { Text("123456") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(Modifier.height(16.dp))

                // Contador de reenvío
                Text(
                    text = if (resendTimer > 0)
                        "Puedes reenviar en $resendTimer segundos"
                    else "¿No recibiste el código?",
                    color = if (resendTimer > 0)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(enabled = resendTimer <= 0) {
                            codeSent = false
                            resendTimer = 60
                            smsCode = ""
                        }
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (smsCode.length != 6) {
                            errorMessage = "El código debe tener 6 dígitos"
                            return@Button
                        }
                        if (verificationId.isBlank()) {
                            errorMessage = "Error de verificación. Intenta nuevamente."
                            return@Button
                        }

                        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
                        FirebaseAuth.getInstance().currentUser?.linkWithCredential(credential)
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    guardarTelefonoEnFirestore(
                                        phone = (selectedCountry?.dialCode ?: "") + phoneNumber,
                                        context = context,
                                        navController = navController
                                    )
                                } else {
                                    errorMessage = when (task.exception) {
                                        is FirebaseAuthInvalidCredentialsException -> "Código incorrecto"
                                        is FirebaseAuthUserCollisionException -> "Este número ya está vinculado a otra cuenta"
                                        else -> "Error: ${task.exception?.message ?: "Intenta nuevamente"}"
                                    }
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = smsCode.length == 6
                ) {
                    Text("Verificar código")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Función mejorada de validación
fun validatePhoneNumber(phoneNumber: String, country: Country): Pair<Boolean, String> {
    if (phoneNumber.isEmpty()) {
        return Pair(false, "Ingresa un número telefónico")
    }

    // Validar longitud
    if (phoneNumber.length < country.minLength || phoneNumber.length > country.maxLength) {
        return Pair(false, "El número debe tener entre ${country.minLength} y ${country.maxLength} dígitos")
    }

    // Validar inicio si está especificado
    if (country.validStart.isNotEmpty() && !country.validStart.contains(phoneNumber.first())) {
        return Pair(false, "El número debe comenzar con: ${country.validStart.joinToString(", ")}")
    }

    // Validar que solo contenga dígitos (ya filtrado, pero por si acaso)
    if (!phoneNumber.all { it.isDigit() }) {
        return Pair(false, "Solo se permiten números")
    }

    return Pair(true, "")
}

// Función para guardar en Firestore (sin cambios)
fun guardarTelefonoEnFirestore(
    phone: String,
    context: Context,
    navController: NavController
) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    if (user != null) {
        db.collection("users").document(user.uid)
            .update(
                mapOf(
                    "phone" to phone,
                    "telefonoVerificado" to true
                )
            )
            .addOnSuccessListener {
                Toast.makeText(context, "✅ Teléfono verificado exitosamente", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al guardar: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

// Extensión para obtener Activity
fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    throw IllegalStateException("No se pudo encontrar Activity desde el Context")
}
