package com.example.parking.Client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltroParqueoModal(
    filtros: FiltrosCliente,
    zonas: List<String>,
    onAplicar: (FiltrosCliente) -> Unit,
    onCancelar: () -> Unit
) {
    var filtrosState by remember { mutableStateOf(filtros) }
    var query by remember { mutableStateOf(filtros.zonaSeleccionada.orEmpty()) }

    val maxPrecio = 30f

    val sugerencias = remember(query, zonas) {
        if (query.isBlank()) {
            emptyList()
        } else {
            zonas.filter {
                it.contains(query, ignoreCase = true)
            }.distinct().take(5)
        }
    }

    val totalFiltrosActivos = remember(filtrosState) {
        filtrosState.tiposVehiculo.size +
                (if (!filtrosState.zonaSeleccionada.isNullOrBlank()) 1 else 0) +
                (if (filtrosState.precioMaximo != null && filtrosState.precioMaximo!! > 0f) 1 else 0) +
                (if (filtrosState.calificacionMinima != null) 1 else 0) +
                (if (filtrosState.soloDisponibles) 1 else 0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Filtros de búsqueda",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (totalFiltrosActivos == 0) {
                        "Ajusta los criterios para encontrar el parqueo ideal."
                    } else {
                        "$totalFiltrosActivos filtros activos"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (totalFiltrosActivos > 0) {
                TextButton(
                    onClick = {
                        filtrosState = FiltrosCliente()
                        query = ""
                    }
                ) {
                    Text("Limpiar")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        FiltroSectionCard(
            title = "Tipo de vehículo"
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("auto", "moto", "camion").forEach { tipo ->
                    val seleccionado = tipo in filtrosState.tiposVehiculo

                    FilterChip(
                        selected = seleccionado,
                        onClick = {
                            filtrosState = filtrosState.copy(
                                tiposVehiculo = filtrosState.tiposVehiculo.toMutableList().apply {
                                    if (contains(tipo)) remove(tipo) else add(tipo)
                                }
                            )
                        },
                        label = {
                            Text(
                                text = tipo.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = when (tipo) {
                                    "auto" -> Icons.Default.DirectionsCar
                                    "moto" -> Icons.Default.TwoWheeler
                                    "camion" -> Icons.Default.LocalShipping
                                    else -> Icons.Default.DirectionsCar
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = seleccionado,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FiltroSectionCard(
            title = "Buscar zona"
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    filtrosState = filtrosState.copy(
                        zonaSeleccionada = it.ifBlank { null }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("Ej: Equipetrol, Centro...")
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            if (sugerencias.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        sugerencias.forEachIndexed { index, zona ->
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            query = zona
                                            filtrosState = filtrosState.copy(
                                                zonaSeleccionada = zona
                                            )
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = zona,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (index < sugerencias.lastIndex) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FiltroSectionCard(
            title = "Precio máximo por hora"
        ) {
            Text(
                text = "Bs ${((filtrosState.precioMaximo ?: 0f).toInt())}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Slider(
                value = filtrosState.precioMaximo ?: 0f,
                onValueChange = {
                    filtrosState = filtrosState.copy(
                        precioMaximo = if (it <= 0f) null else it
                    )
                },
                valueRange = 0f..maxPrecio,
                steps = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Hasta Bs ${maxPrecio.toInt()} por hora",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        FiltroSectionCard(
            title = "Disponibilidad"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = filtrosState.soloDisponibles,
                    onCheckedChange = {
                        filtrosState = filtrosState.copy(
                            soloDisponibles = it
                        )
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Mostrar solo parqueos disponibles",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = {
                onAplicar(
                    filtrosState.copy(
                        zonaSeleccionada = filtrosState.zonaSeleccionada?.trim()?.ifBlank { null }
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp
            )
        ) {
            Text(
                text = "Aplicar filtros",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onCancelar,
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

@Composable
private fun FiltroSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                content()
            }
        )
    }
}

data class FiltrosCliente(
    val tiposVehiculo: List<String> = emptyList(),
    val zonaSeleccionada: String? = null,
    val precioMaximo: Float? = null,
    val calificacionMinima: Float? = null,
    val soloDisponibles: Boolean = false
)