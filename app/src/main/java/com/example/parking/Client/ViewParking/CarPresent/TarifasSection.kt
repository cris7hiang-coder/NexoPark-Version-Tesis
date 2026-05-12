package com.example.parking.Client.ViewParking.CarPresent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.parking.ui.theme.AzulPetroleoFuerte
import com.example.parking.ui.theme.RojoCoral

@Composable
fun TarifasSection(
    capacidades: Map<String, Int>,
    tarifas: Map<String, Map<String, Double>>,
    disponibles: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val tiposOrdenados = remember(capacidades, tarifas) {
        val prioridad = listOf("auto", "moto", "camion")
        (capacidades.keys + tarifas.keys)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedWith(
                compareBy<String> { tipo ->
                    val index = prioridad.indexOf(tipo.lowercase())
                    if (index == -1) Int.MAX_VALUE else index
                }.thenBy { it.lowercase() }
            )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (tiposOrdenados.isEmpty()) {
            EmptyTarifasState()
            return@Column
        }

        tiposOrdenados.forEach { tipo ->
            val datos = tarifas[tipo].orEmpty()
            val capacidad = capacidades[tipo] ?: 0
            val disponiblesTipo = disponibles[tipo] ?: 0
            val precioHora = datos["hora"] ?: 0.0
            val precioMediaHora = datos["mediaHora"] ?: 0.0

            TarifaVehiculoCard(
                tipo = tipo,
                capacidad = capacidad,
                disponibles = disponiblesTipo,
                estadoTexto = disponibilidadTexto(disponiblesTipo, capacidad),
                estadoColor = disponibilidadColor(disponiblesTipo, capacidad),
                precioMediaHora = precioMediaHora,
                precioHora = precioHora
            )
        }
    }
}

@Composable
private fun TarifaVehiculoCard(
    tipo: String,
    capacidad: Int,
    disponibles: Int,
    estadoTexto: String,
    estadoColor: Color,
    precioMediaHora: Double,
    precioHora: Double
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = vehiculoIcon(tipo),
                            contentDescription = tipo,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tipo.formatearTipoVehiculo(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    EstadoDisponibilidadBadge(
                        texto = estadoTexto,
                        color = estadoColor
                    )
                }

                DisponibilidadBox(
                    disponibles = disponibles,
                    capacidad = capacidad,
                    color = estadoColor,
                    modifier = Modifier.widthIn(min = 96.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TarifaMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Media hora",
                    value = precioMediaHora.toBs()
                )

                TarifaMetricItem(
                    modifier = Modifier.weight(1f),
                    label = "Hora completa",
                    value = precioHora.toBs()
                )
            }
        }
    }
}
@Composable
private fun DisponibilidadBox(
    disponibles: Int,
    capacidad: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.18f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = disponibles.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = color,
                maxLines = 1
            )

            Text(
                text = "/$capacidad",
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = color.copy(alpha = 0.86f),
                maxLines = 1
            )
        }

        Text(
            text = "disponibles",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = color.copy(alpha = 0.78f),
            maxLines = 1
        )
    }
}
@Composable
private fun EstadoDisponibilidadBadge(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.10f)
    ) {
        Text(
            text = texto,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = color
        )
    }
}

@Composable
private fun TarifaMetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun EmptyTarifasState() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tarifas no disponibles",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Este parqueo aún no registró precios para los tipos de vehículo configurados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun disponibilidadColor(disponibles: Int, capacidad: Int): Color {
    return when {
        capacidad <= 0 -> MaterialThemeFallbackColors.Warning
        disponibles <= 0 -> MaterialThemeFallbackColors.Error
        disponibles <= 2 -> MaterialThemeFallbackColors.Warning
        else -> MaterialThemeFallbackColors.Success
    }
}

private fun disponibilidadTexto(disponibles: Int, capacidad: Int): String {
    return when {
        capacidad <= 0 -> "Sin capacidad configurada"
        disponibles <= 0 -> "Sin espacios disponibles"
        disponibles <= 2 -> "Pocos espacios disponibles"
        else -> "Disponibilidad alta"
    }
}

private fun vehiculoIcon(tipo: String): ImageVector {
    return when (tipo.lowercase().trim()) {
        "auto" -> Icons.Default.DirectionsCar
        "moto" -> Icons.Default.TwoWheeler
        "camion" -> Icons.Default.LocalShipping
        else -> Icons.Default.DirectionsCar
    }
}

private fun String.formatearTipoVehiculo(): String {
    return trim()
        .lowercase()
        .replaceFirstChar { it.titlecase() }
}

private fun Double.toBs(): String {
    return "Bs ${String.format(java.util.Locale.US, "%.2f", this)}"
}

private object MaterialThemeFallbackColors {
    val Success = AzulPetroleoFuerte
    val Warning = RojoCoral
    val Error = RojoCoral
}