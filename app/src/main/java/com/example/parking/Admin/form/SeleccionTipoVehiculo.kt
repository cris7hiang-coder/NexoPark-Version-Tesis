package com.example.parking.Admin.form

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.parking.Login.PrimaryButton
import com.example.parking.model.Dimensiones
import com.example.parking.model.TipoVehiculoDimension
import com.example.parking.ui.theme.AzulPetroleoFuerte
import com.example.parking.ui.theme.FondoCard
import com.example.parking.ui.theme.TextoBase
import com.example.parking.ui.theme.TextoSecundario
import com.example.parking.ui.theme.VerdeExito

@Composable
fun SeleccionTipoVehiculo(
    onTiposSeleccionados: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val tipos = listOf(
        "moto" to Icons.Default.TwoWheeler,
        "auto" to Icons.Default.DirectionsCar,
        "camion" to Icons.Default.LocalShipping
    )

    val seleccionados = remember {
        mutableStateMapOf<String, Boolean>().apply {
            tipos.forEach { this[it.first] = false }
        }
    }

    val totalSeleccionados = seleccionados.count { it.value }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WizardStepHeaderCompact(
                title = "Tipos de vehículos aceptados",
                subtitle = "Selecciona los vehículos que podrá recibir tu parqueo.",
                stepLabel = "Paso 3 de 7"
            )
        }

        item {
            if (totalSeleccionados > 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    )
                ) {
                    Text(
                        text = "$totalSeleccionados tipo(s) seleccionado(s)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        items(tipos) { (tipo, icono) ->
            val isSelected = seleccionados[tipo] == true

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                animationSpec = tween(250),
                label = "tipo_bg"
            )

            val borderColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = tween(250),
                label = "tipo_border"
            )

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.02f else 1f,
                animationSpec = tween(250),
                label = "tipo_scale"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable {
                        seleccionados[tipo] = !isSelected
                    },
                shape = RoundedCornerShape(20.dp),
                color = backgroundColor,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Icon(
                            imageVector = icono,
                            contentDescription = tipo,
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
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

                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Seleccionado",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                ) {
                    Text(
                        text = "Atrás",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                PrimaryButton(
                    text = "Siguiente",
                    onClick = {
                        val elegidos = seleccionados
                            .filterValues { it }
                            .keys
                            .toList()

                        if (elegidos.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Selecciona al menos un tipo",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onTiposSeleccionados(elegidos)
                        }
                    },
                    enabled = totalSeleccionados > 0,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowForward
                )
            }
        }
    }
}
@Composable
internal fun WizardStepHeaderCompact(
    title: String,
    subtitle: String,
    stepLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(
            text = stepLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(6.dp))

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

internal fun descripcionTipoVehiculo(tipo: String): String {
    return when (tipo.lowercase()) {
        "moto" -> "Espacios para motocicletas y vehículos ligeros"
        "auto" -> "Espacios estándar para automóviles"
        "camion" -> "Espacios amplios para camionetas o vehículos grandes"
        else -> "Tipo de vehículo"
    }
}