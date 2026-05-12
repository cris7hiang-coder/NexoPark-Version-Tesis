package com.example.parking.Admin.form

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parking.Login.AppTextField
import com.example.parking.Login.PrimaryButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CapacidadesYTarifasDinamico(
    tiposSeleccionados: List<String>,
    capacidadesIniciales: Map<String, Int> = emptyMap(),
    tarifasIniciales: Map<String, Map<String, Double>> = emptyMap(),
    onBack: () -> Unit,
    deshabilitarCapacidad: Boolean = false,
    onGuardar: (capacidad: Map<String, Int>, tarifas: Map<String, Map<String, Double>>) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val capacidades = remember {
        mutableStateMapOf<String, String>().apply {
            capacidadesIniciales.forEach { (tipo, valor) ->
                this[tipo] = valor.toString()
            }
        }
    }

    val tarifasHora = remember {
        mutableStateMapOf<String, String>().apply {
            tarifasIniciales.forEach { (tipo, mapa) ->
                mapa["hora"]?.let { this[tipo] = it.toString() }
            }
        }
    }

    val tarifasMediaHora = remember {
        mutableStateMapOf<String, String>().apply {
            tarifasIniciales.forEach { (tipo, mapa) ->
                mapa["mediaHora"]?.let { this[tipo] = it.toString() }
            }
        }
    }

    val totalEspacios = remember(capacidades.toMap()) {
        capacidades.values.sumOf { it.toIntOrNull() ?: 0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WizardStepHeaderCompact(
                title = "Capacidad y tarifas",
                subtitle = "Define espacios disponibles y cobro por bloques de 30 minutos.",
                stepLabel = "Paso 4 de 7"
            )
        }

        item {
            if (tiposSeleccionados.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalParking,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Resumen actual",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "$totalEspacios espacios configurados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Regla de cobro",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "El cálculo será automático al finalizar la ocupación. Se cobra por bloques de 30 minutos usando la tarifa de media hora y de hora según el tiempo total usado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(tiposSeleccionados) { tipo ->
            TarifaVehiculoCardLibre(
                tipo = tipo,
                capacidad = capacidades[tipo].orEmpty(),
                tarifaHora = tarifasHora[tipo].orEmpty(),
                tarifaMediaHora = tarifasMediaHora[tipo].orEmpty(),
                deshabilitarCapacidad = deshabilitarCapacidad,
                onCapacidadChange = {
                    if (!deshabilitarCapacidad) {
                        capacidades[tipo] = it.filter(Char::isDigit)
                    }
                },
                onTarifaHoraChange = { tarifasHora[tipo] = filtrarDecimal(it) },
                onTarifaMediaHoraChange = { tarifasMediaHora[tipo] = filtrarDecimal(it) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "Atrás",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                PrimaryButton(
                    text = "Guardar",
                    icon = Icons.Default.Check,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val capacidadFinal = mutableMapOf<String, Int>()

                        for (tipo in tiposSeleccionados) {
                            val valor = capacidades[tipo]?.toIntOrNull()

                            if (valor == null || valor <= 0) {
                                Toast.makeText(
                                    context,
                                    "Capacidad inválida en ${tipo.replaceFirstChar { it.uppercase() }}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@PrimaryButton
                            }

                            val hora = tarifasHora[tipo]?.toDoubleOrNull() ?: 0.0
                            val media = tarifasMediaHora[tipo]?.toDoubleOrNull() ?: 0.0

                            if (hora <= 0 || media <= 0) {
                                Toast.makeText(
                                    context,
                                    "Tarifas inválidas en ${tipo.replaceFirstChar { it.uppercase() }}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@PrimaryButton
                            }

                            capacidadFinal[tipo] = valor
                        }

                        val tarifasFinal = tiposSeleccionados.associateWith { tipo ->
                            mapOf(
                                "hora" to (tarifasHora[tipo]?.toDoubleOrNull() ?: 0.0),
                                "mediaHora" to (tarifasMediaHora[tipo]?.toDoubleOrNull() ?: 0.0)
                            )
                        }

                        onGuardar(capacidadFinal, tarifasFinal)

                        tiposSeleccionados.forEach { tipo ->
                            db.collection("parqueos")
                                .document(uid)
                                .collection("espacios")
                                .whereEqualTo("tipoVehiculo", tipo)
                                .get()
                                .addOnSuccessListener { docs ->
                                    docs.forEach { it.reference.delete() }

                                    val cantidad = capacidadFinal[tipo] ?: 0

                                    for (i in 1..cantidad) {
                                        val espacio = hashMapOf(
                                            "parqueoId" to uid,
                                            "tipoVehiculo" to tipo,
                                            "indice" to i,
                                            "estado" to "DISPONIBLE",
                                            "cliente" to null
                                        )

                                        db.collection("parqueos")
                                            .document(uid)
                                            .collection("espacios")
                                            .add(espacio)
                                    }
                                }
                        }

                        Toast.makeText(
                            context,
                            "Capacidad y tarifas guardadas correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }
}
@Composable
private fun TarifaVehiculoCardLibre(
    tipo: String,
    capacidad: String,
    tarifaHora: String,
    tarifaMediaHora: String,
    deshabilitarCapacidad: Boolean,
    onCapacidadChange: (String) -> Unit,
    onTarifaHoraChange: (String) -> Unit,
    onTarifaMediaHoraChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = vehiculoIcon(tipo),
                            contentDescription = tipo,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = tipo.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = descripcionTipoVehiculo(tipo),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AppTextField(
                label = "Cantidad de espacios",
                value = capacidad,
                onValueChange = onCapacidadChange,
                leading = Icons.Default.LocalParking,
                keyboardType = KeyboardType.Number,
                modifier = Modifier.fillMaxWidth(),
                enabled = !deshabilitarCapacidad
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppTextField(
                    label = "Hora (Bs)",
                    value = tarifaHora,
                    onValueChange = onTarifaHoraChange,
                    leading = Icons.Default.AttachMoney,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )

                AppTextField(
                    label = "30 min (Bs)",
                    value = tarifaMediaHora,
                    onValueChange = onTarifaMediaHoraChange,
                    leading = Icons.Default.Schedule,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ejemplo de cálculo",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "• 0–30 min: media hora\n• 31–60 min: una hora\n• 61–90 min: una hora + media hora\n• 91–120 min: dos horas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
private fun filtrarDecimal(valor: String): String {
    return valor.filterIndexed { index, c ->
        c.isDigit() || (c == '.' && '.' !in valor.take(index))
    }
}

private fun vehiculoIcon(tipo: String): ImageVector {
    return when (tipo.lowercase()) {
        "moto" -> Icons.Default.TwoWheeler
        "camion" -> Icons.Default.LocalShipping
        else -> Icons.Default.DirectionsCar
    }
}