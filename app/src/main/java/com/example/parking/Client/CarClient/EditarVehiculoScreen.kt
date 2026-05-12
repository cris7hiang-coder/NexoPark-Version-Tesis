package com.example.parking.Client.CarClient

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parking.Login.PrimaryButton
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarVehiculoScreen(navController: NavController) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().reference

    val vehiculoId =
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<String>("vehiculoId")

    val datos =
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<Map<String, Any>>("vehiculoDetalle")

    val uid = FirebaseAuth.getInstance().currentUser?.uid

    if (vehiculoId == null || datos == null || uid == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No se pudo cargar el vehículo",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    var alias by remember { mutableStateOf(datos["nombreVehiculo"]?.toString().orEmpty()) }
    var placa by remember { mutableStateOf(datos["placa"]?.toString().orEmpty()) }
    var marca by remember { mutableStateOf(datos["marca"]?.toString().orEmpty()) }
    var modelo by remember { mutableStateOf(datos["modelo"]?.toString().orEmpty()) }
    var anio by remember { mutableStateOf(datos["anio"]?.toString().orEmpty()) }
    var color by remember { mutableStateOf(datos["color"]?.toString().orEmpty()) }

    var imagenUrl by remember { mutableStateOf(datos["imagenUrl"]?.toString().orEmpty()) }
    var imagenUri by remember { mutableStateOf<Uri?>(null) }

    var loading by remember { mutableStateOf(false) }

    var aliasError by remember { mutableStateOf<String?>(null) }
    var placaError by remember { mutableStateOf<String?>(null) }
    var anioError by remember { mutableStateOf<String?>(null) }

    val coloresDisponibles = listOf(
        "Negro",
        "Blanco",
        "Gris",
        "Rojo",
        "Azul",
        "Amarillo",
        "Verde",
        "Plateado"
    )

    val isValid = alias.isNotBlank() && placa.isNotBlank() && !loading

    val subirImagenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val fileName = "vehiculo_${UUID.randomUUID()}.jpg"
            val imagenRef = storageRef.child("vehiculos/$vehiculoId/$fileName")

            loading = true

            imagenRef.putFile(it)
                .continueWithTask { task ->
                    if (task.isSuccessful) {
                        imagenRef.downloadUrl
                    } else {
                        throw task.exception ?: Exception("Error al subir imagen")
                    }
                }
                .addOnSuccessListener { url ->
                    imagenUrl = url.toString()
                    imagenUri = it
                    loading = false
                }
                .addOnFailureListener { e ->
                    loading = false
                    Toast.makeText(
                        context,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ScreenHeader(
                title = "Editar vehículo",
                subtitle = "Actualiza la información y la imagen de tu vehículo."
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                    CustomFieldModern(
                        label = "Alias",
                        value = alias,
                        onChange = {
                            alias = it
                            aliasError = if (it.isBlank()) "Ingresa un alias" else null
                        },
                        leadingIcon = Icons.Default.DriveFileRenameOutline,
                        isError = aliasError != null,
                        supportingText = aliasError
                    )

                    CustomFieldModern(
                        label = "Placa",
                        value = placa,
                        onChange = {
                            placa = it.uppercase()
                            placaError = if (it.isBlank()) "Ingresa la placa" else null
                        },
                        leadingIcon = Icons.Default.ConfirmationNumber,
                        isError = placaError != null,
                        supportingText = placaError
                    )

                    CustomFieldModern(
                        label = "Marca",
                        value = marca,
                        onChange = { marca = it },
                        leadingIcon = Icons.Default.Business
                    )

                    CustomFieldModern(
                        label = "Modelo",
                        value = modelo,
                        onChange = { modelo = it },
                        leadingIcon = Icons.Default.Badge
                    )

                    CustomFieldModern(
                        label = "Año",
                        value = anio,
                        onChange = {
                            anio = it.filter(Char::isDigit).take(4)
                            anioError = when {
                                anio.isBlank() -> null
                                anio.length < 4 -> "Ingresa un año válido"
                                else -> null
                            }
                        },
                        leadingIcon = Icons.Default.DateRange,
                        keyboardType = KeyboardType.Number,
                        isError = anioError != null,
                        supportingText = anioError
                    )

                    SelectorDeColorVehiculo(
                        colores = coloresDisponibles,
                        colorSeleccionado = color,
                        onSeleccionarColor = { color = it }
                    )

                    VehicleImagePickerCard(
                        imagenUri = imagenUri,
                        imagenUrl = imagenUrl,
                        loading = loading,
                        onPickImage = { subirImagenLauncher.launch("image/*") }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    PrimaryButton(
                        text = "Guardar cambios",
                        loading = loading,
                        icon = Icons.Default.Save,
                        onClick = {
                            aliasError = if (alias.isBlank()) "Ingresa un alias" else null
                            placaError = if (placa.isBlank()) "Ingresa la placa" else null
                            anioError = when {
                                anio.isBlank() -> null
                                anio.length < 4 -> "Ingresa un año válido"
                                else -> null
                            }

                            val hasError = listOf(aliasError, placaError, anioError).any { it != null }
                            if (hasError || loading) return@PrimaryButton

                            loading = true

                            val updates = mapOf(
                                "nombreVehiculo" to alias.trim(),
                                "placa" to placa.trim(),
                                "marca" to marca.trim(),
                                "modelo" to modelo.trim(),
                                "anio" to anio.toIntOrNull(),
                                "color" to color,
                                "imagenUrl" to imagenUrl
                            )

                            db.collection("users")
                                .document(uid)
                                .collection("vehiculos")
                                .document(vehiculoId)
                                .update(updates)
                                .addOnSuccessListener {
                                    loading = false
                                    Toast.makeText(
                                        context,
                                        "Vehículo actualizado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                }
                                .addOnFailureListener {
                                    loading = false
                                    Toast.makeText(
                                        context,
                                        it.message ?: "Error al actualizar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isValid
                    )

                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "Cancelar",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
    }
}