package com.example.parking.Admin.form
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.parking.Login.PrimaryButton
import com.example.parking.ui.theme.TextoSecundario
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalTime
data class DatosParqueoForm(
    val nombre: String = "",
    val descripcion: String = "",
    val encargado: String = "",
    val telefono: String = "",
    val horaInicio: LocalTime = LocalTime.of(8, 0),
    val horaFin: LocalTime = LocalTime.of(18, 0),
    val abierto24: Boolean = false
) {
    fun isValid(): Boolean {
        return nombre.isNotBlank() &&
                encargado.isNotBlank() &&
                telefono.isNotBlank() &&
                (abierto24 || horaFin.isAfter(horaInicio))
    }

    fun toFirestoreMap(uid: String) = mapOf(
        "nombre" to nombre.trim(),
        "descripcion" to descripcion.trim(),
        "encargado" to encargado.trim(),
        "telefono" to telefono.trim(),
        "abierto24" to abierto24,
        "hora_inicio" to if (abierto24) "00:00" else horaInicio.toString(),
        "hora_fin" to if (abierto24) "23:59" else horaFin.toString(),
        "adminId" to uid,
        "creadoEn" to FieldValue.serverTimestamp()
    )
}

@Composable
fun RegistrarParqueoForm(
    uid: String,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val focusManager = LocalFocusManager.current

    var isSaving by remember { mutableStateOf(false) }

    var formState by rememberSaveable(
        stateSaver = Saver(
            save = {
                listOf(
                    it.nombre,
                    it.descripcion,
                    it.encargado,
                    it.telefono,
                    it.horaInicio.toString(),
                    it.horaFin.toString(),
                    it.abierto24
                )
            },
            restore = {
                DatosParqueoForm(
                    nombre = it[0] as String,
                    descripcion = it[1] as String,
                    encargado = it[2] as String,
                    telefono = it[3] as String,
                    horaInicio = LocalTime.parse(it[4] as String),
                    horaFin = LocalTime.parse(it[5] as String),
                    abierto24 = it[6] as Boolean
                )
            }
        )
    ) { mutableStateOf(DatosParqueoForm()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(
            start = 4.dp,
            end = 4.dp,
            top = 4.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            FormCompactHeader(
                title = "Información del parqueo",
                subtitle = "Completa los datos base para continuar con la configuración."
            )
        }

        item {
            WizardField(
                value = formState.nombre,
                onValueChange = { formState = formState.copy(nombre = it) },
                label = "Nombre del parqueo",
                placeholder = "Ej. Parqueo Central",
                isError = formState.nombre.isBlank(),
                supportingText = if (formState.nombre.isBlank()) {
                    "Este campo es obligatorio"
                } else {
                    null
                },
                leadingIcon = Icons.Default.LocalParking,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                    }
                )
            )
        }

        item {
            WizardField(
                value = formState.encargado,
                onValueChange = { formState = formState.copy(encargado = it) },
                label = "Nombre del encargado",
                placeholder = "Ej. Juan Pérez",
                isError = formState.encargado.isBlank(),
                supportingText = if (formState.encargado.isBlank()) {
                    "Este campo es obligatorio"
                } else {
                    null
                },
                leadingIcon = Icons.Default.Badge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                    }
                )
            )
        }

        item {
            WizardField(
                value = formState.telefono,
                onValueChange = {
                    formState = formState.copy(
                        telefono = it
                            .filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' }
                            .take(15)
                    )
                },
                label = "Teléfono",
                placeholder = "+591 70000000",
                isError = formState.telefono.isBlank(),
                supportingText = if (formState.telefono.isBlank()) {
                    "Este campo es obligatorio"
                } else {
                    null
                },
                leadingIcon = Icons.Default.Phone,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                    }
                )
            )
        }

        item {
            WizardField(
                value = formState.descripcion,
                onValueChange = { formState = formState.copy(descripcion = it) },
                label = "Descripción",
                placeholder = "Opcional: referencias, servicios o detalles importantes",
                leadingIcon = Icons.Default.Description,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                keyboardActions = KeyboardActions.Default,
                singleLine = false,
                minLines = 3
            )
        }

        item {
            HorarioCompactBlock(
                valueInicio = formState.horaInicio,
                valueFin = formState.horaFin,
                abierto24Value = formState.abierto24
            ) { inicio, fin, abierto24 ->
                formState = formState.copy(
                    horaInicio = inicio,
                    horaFin = fin,
                    abierto24 = abierto24
                )
            }
        }

        item {
            PrimaryButton(
                text = "Siguiente",
                onClick = {
                    if (!formState.isValid()) {
                        Toast.makeText(
                            context,
                            "Completa los campos obligatorios y verifica el horario.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@PrimaryButton
                    }

                    isSaving = true

                    db.collection("parqueos")
                        .document(uid)
                        .set(formState.toFirestoreMap(uid))
                        .addOnSuccessListener {
                            isSaving = false
                            Toast.makeText(
                                context,
                                "Datos guardados. Continuemos.",
                                Toast.LENGTH_SHORT
                            ).show()
                            onNext()
                        }
                        .addOnFailureListener { e ->
                            isSaving = false
                            Toast.makeText(
                                context,
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                loading = isSaving,
                icon = Icons.Default.ArrowForward
            )
        }
    }
}

@Composable
private fun FormCompactHeader(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HorarioCompactBlock(
    valueInicio: LocalTime,
    valueFin: LocalTime,
    abierto24Value: Boolean,
    onHorarioChange: (LocalTime, LocalTime, Boolean) -> Unit
) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Horario de atención",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Define cuándo estará disponible el parqueo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorarioMinimalistaSelector(
                valueInicio = valueInicio,
                valueFin = valueFin,
                abierto24Value = abierto24Value
            ) { inicio, fin, abierto24 ->
                onHorarioChange(inicio, fin, abierto24)
            }
        }
    }
}

@Composable
private fun WizardField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            unfocusedBorderColor = if (isError) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outline
            },
            errorBorderColor = MaterialTheme.colorScheme.error,
            focusedLabelColor = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            errorLabelColor = MaterialTheme.colorScheme.error,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            errorTextColor = MaterialTheme.colorScheme.onSurface,
            cursorColor = MaterialTheme.colorScheme.primary,
            errorCursorColor = MaterialTheme.colorScheme.error
        )
    )
}