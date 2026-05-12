package com.example.parking.Client.CarClient
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.outlined.AirportShuttle
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parking.Login.PrimaryButton
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroVehiculoScreen(navController: NavController) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val db = FirebaseFirestore.getInstance()
    val storageRef = FirebaseStorage.getInstance().reference

    val tipos = listOf("auto", "moto", "camion")
    var tipoSeleccionado by remember { mutableStateOf("auto") }

    val usos = listOf("Personal", "Familiar/Amigo", "Alquiler", "Empresa")
    var usoSeleccionado by remember { mutableStateOf("") }

    var placa by remember { mutableStateOf("") }
    var nombreVehiculo by remember { mutableStateOf("") }
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("") }

    val colores = listOf("Negro", "Blanco", "Gris", "Rojo", "Azul", "Amarillo", "Verde", "Plateado")
    var colorSeleccionado by remember { mutableStateOf("Negro") }

    var imagenUri by remember { mutableStateOf<Uri?>(null) }
    var imagenUrlSubida by remember { mutableStateOf<String?>(null) }
    var subiendoImagen by remember { mutableStateOf(false) }

    var mostrarPersonalizacion by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    var placaError by remember { mutableStateOf<String?>(null) }
    var usoError by remember { mutableStateOf<String?>(null) }
    var anioError by remember { mutableStateOf<String?>(null) }

    val subirImagenLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val fileName = "vehiculo_${UUID.randomUUID()}.jpg"
                val ref = storageRef.child("vehiculos/$uid/$fileName")

                subiendoImagen = true

                ref.putFile(it)
                    .continueWithTask { task ->
                        if (task.isSuccessful) ref.downloadUrl
                        else throw (task.exception ?: Exception("Error al subir imagen"))
                    }
                    .addOnSuccessListener { url ->
                        imagenUrlSubida = url.toString()
                        imagenUri = it
                        subiendoImagen = false
                    }
                    .addOnFailureListener {
                        subiendoImagen = false
                        Toast.makeText(context, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                    }
            }
        }

    Scaffold(
        topBar = {
            MinimalTopBarCompact(onBack = { navController.popBackStack() })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ScreenHeader(
                title = "Registrar vehículo",
                subtitle = "Agrega un vehículo para usarlo en reservas y accesos."
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CustomFieldModern(
                            label = "Placa *",
                            value = placa,
                            onChange = {
                                placa = it.uppercase()
                                placaError = when {
                                    placa.isBlank() -> "Ingresa la placa"
                                    placa.length < 6 -> "Mínimo 6 caracteres"
                                    else -> null
                                }
                            },
                            leadingIcon = Icons.Default.ConfirmationNumber,
                            isError = placaError != null,
                            supportingText = placaError,
                            modifier = Modifier.weight(1f)
                        )

                        CustomFieldModern(
                            label = "Alias",
                            value = nombreVehiculo,
                            onChange = { nombreVehiculo = it },
                            leadingIcon = Icons.Default.DriveFileRenameOutline,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SectionLabel("Tipo de vehículo")

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tipos.forEach { tipo ->
                            TipoVehiculoChip(
                                tipo = tipo,
                                seleccionado = tipo == tipoSeleccionado,
                                onClick = { tipoSeleccionado = tipo },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    SectionLabel("Uso del vehículo *")

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        usos.forEach { uso ->
                            UsoItem(
                                texto = uso,
                                seleccionado = uso == usoSeleccionado,
                                onClick = {
                                    usoSeleccionado = uso
                                    usoError = null
                                }
                            )
                        }
                    }

                    if (usoError != null) {
                        Text(
                            text = usoError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        mostrarPersonalizacion = !mostrarPersonalizacion
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Personalización opcional",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = if (mostrarPersonalizacion) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (mostrarPersonalizacion) {
                                Spacer(modifier = Modifier.height(14.dp))

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
                                    colores = colores,
                                    colorSeleccionado = colorSeleccionado,
                                    onSeleccionarColor = { colorSeleccionado = it }
                                )

                                VehicleImagePickerCard(
                                    imagenUri = imagenUri,
                                    imagenUrl = imagenUrlSubida.orEmpty(),
                                    loading = subiendoImagen,
                                    onPickImage = { subirImagenLauncher.launch("image/*") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    PrimaryButton(
                        text = "Guardar vehículo",
                        loading = loading,
                        icon = Icons.Default.Save,
                        onClick = {
                            placaError = when {
                                placa.isBlank() -> "Ingresa la placa"
                                placa.length < 6 -> "Mínimo 6 caracteres"
                                else -> null
                            }

                            usoError = if (usoSeleccionado.isBlank()) {
                                "Selecciona el uso del vehículo"
                            } else {
                                null
                            }

                            anioError = when {
                                anio.isBlank() -> null
                                anio.length < 4 -> "Ingresa un año válido"
                                else -> null
                            }

                            val hasError = listOf(placaError, usoError, anioError).any { it != null }

                            if (hasError) return@PrimaryButton

                            if (subiendoImagen) {
                                Toast.makeText(
                                    context,
                                    "Espera a que termine la subida de imagen",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@PrimaryButton
                            }

                            loading = true

                            val id = UUID.randomUUID().toString()

                            val data = mapOf(
                                "id" to id,
                                "clienteId" to uid,
                                "placa" to placa.trim(),
                                "nombreVehiculo" to nombreVehiculo.trim().ifBlank { null },
                                "tipo" to tipoSeleccionado,
                                "uso" to usoSeleccionado,
                                "marca" to marca.trim().ifBlank { null },
                                "modelo" to modelo.trim().ifBlank { null },
                                "anio" to anio.toIntOrNull(),
                                "color" to colorSeleccionado,
                                "imagenUrl" to imagenUrlSubida,
                                "registradoEn" to FieldValue.serverTimestamp()
                            )

                            db.collection("users")
                                .document(uid)
                                .collection("vehiculos")
                                .document(id)
                                .set(data)
                                .addOnSuccessListener {
                                    loading = false
                                    Toast.makeText(
                                        context,
                                        "Vehículo registrado",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                }
                                .addOnFailureListener {
                                    loading = false
                                    Toast.makeText(
                                        context,
                                        "Error al registrar vehículo",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading
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
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
fun CustomFieldModern(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    supportingText: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    val supportingColor = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = leadingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        isError = isError,
        supportingText = {
            supportingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = supportingColor
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,

            focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            errorBorderColor = MaterialTheme.colorScheme.error,

            focusedLabelColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            errorLabelColor = MaterialTheme.colorScheme.error,

            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            errorTextColor = MaterialTheme.colorScheme.onSurface,

            focusedLeadingIconColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            errorLeadingIconColor = MaterialTheme.colorScheme.error,

            cursorColor = MaterialTheme.colorScheme.primary,
            errorCursorColor = MaterialTheme.colorScheme.error
        )
    )
}

@Composable
fun TipoVehiculoChip(
    tipo: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "tipo_chip_scale"
    )

    val bg = if (seleccionado) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (seleccionado) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val icon = getVehiculoIcon(tipo)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(
            1.dp,
            if (seleccionado) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tipo,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = tipo.replaceFirstChar { it.uppercase() },
                color = contentColor,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

fun getVehiculoIcon(tipo: String): ImageVector {
    return when (tipo.lowercase()) {
        "auto" -> Icons.Default.DirectionsCar
        "moto" -> Icons.Default.TwoWheeler
        "camion" -> Icons.Default.LocalShipping
        else -> Icons.Default.HelpOutline
    }
}

@Composable
fun UsoItem(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    val bg = if (seleccionado) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = if (seleccionado) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val textColor = if (seleccionado) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            if (seleccionado) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}



@Composable
fun VehicleImagePickerCard(
    imagenUri: Uri?,
    imagenUrl: String,
    loading: Boolean,
    onPickImage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Imagen del vehículo",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPickImage() },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    imagenUri != null || imagenUrl.isNotBlank() -> {
                        AsyncImage(
                            model = imagenUri ?: imagenUrl,
                            contentDescription = "Imagen del vehículo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }

                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Toca para subir una imagen",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
