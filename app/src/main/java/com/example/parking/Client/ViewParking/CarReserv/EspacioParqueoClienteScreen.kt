package com.example.parking.Client.ViewParking.CarReserv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parking.Admin.EspacioParqueo
import com.example.parking.Admin.EstadoEspacio
import com.example.parking.Admin.getIconoVehiculo
import com.example.parking.model.Dimensiones
import com.example.parking.model.TipoVehiculoDimension
import com.example.parking.ui.theme.RojoCoral

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EspacioParqueoClienteScreen(
    tipoVehiculo: String,
    capacidad: Map<String, Int>,
    espacios: List<EspacioParqueo>,
    onEspacioClick: (tipo: String, indice: Int) -> Unit

) {

    val total = capacidad[tipoVehiculo] ?: 0
    val lista = espacios.filter { it.tipoVehiculo == tipoVehiculo }
    val espaciosOcupados = lista.associateBy { it.indice }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {

        DimensionesVehiculoCard(tipoVehiculo)

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Selecciona tu espacio",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )


        Spacer(Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(total) { index ->
                val numero = index + 1
                val espacio = espaciosOcupados[numero]

                if (espacio != null) {
                    EspacioSlotCliente(
                        indice = numero,
                        tipo = espacio.tipoVehiculo,
                        estado = espacio.estado,
                        onClick = {
                            if (espacio.estado == EstadoEspacio.DISPONIBLE) {
                                onEspacioClick(espacio.tipoVehiculo, espacio.indice)
                            }
                        }
                    )
                } else {
                    EspacioSlotCliente(
                        indice = numero,
                        tipo = tipoVehiculo,
                        estado = EstadoEspacio.DISPONIBLE,
                        onClick = {
                            onEspacioClick(tipoVehiculo, numero)
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun EspacioSlotCliente(
    indice: Int,
    tipo: String,
    estado: EstadoEspacio,
    onClick: () -> Unit
) {
    val icono = getIconoVehiculo(tipo)

    val (colorFondo, iconColor) = when (estado) {
        EstadoEspacio.DISPONIBLE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        EstadoEspacio.RESERVADO -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        EstadoEspacio.OCUPADO -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
    }

    val clickable = estado == EstadoEspacio.DISPONIBLE

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colorFondo)
            .clickable(enabled = clickable, onClick = onClick)
            .border(
                width = if (clickable) 1.dp else 0.dp,
                color = if (clickable) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icono,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "%02d".format(indice),
                style = MaterialTheme.typography.labelSmall,
                color = iconColor
            )
        }
    }
}
@Composable
fun DimensionesVehiculoCard(tipo: String) {

    val dims = obtenerDimensiones(tipo)

    val icono = when (tipo.lowercase()) {
        "moto" -> Icons.Default.TwoWheeler
        "auto" -> Icons.Default.DirectionsCar
        "camion" -> Icons.Default.LocalShipping
        else -> Icons.Default.DirectionsCar
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = RojoCoral
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 🔵 ICONO DENTRO DE CÍRCULO ELEGANTE
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 1.dp,
                        color = Color(0xFFE0DED8),
                        shape = CircleShape
                    )
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = tipo,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // TÍTULO VEHÍCULO
                Text(
                    text = tipo.uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Dimensiones del espacio",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF6A6A6A)
                    )
                )

                Spacer(Modifier.height(10.dp))

                // ➤ ANCHO
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.OpenInFull,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Ancho: ${dims.ancho} m",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(6.dp))

                // ➤ LARGO
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Height,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Largo: ${dims.largo} m",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

fun obtenerDimensiones(tipo: String): Dimensiones {
    return when (tipo.lowercase()) {
        "moto" -> TipoVehiculoDimension.MOTO.dimensiones
        "auto" -> TipoVehiculoDimension.AUTO.dimensiones
        "camion" -> TipoVehiculoDimension.CAMION.dimensiones
        else -> TipoVehiculoDimension.AUTO.dimensiones // default
    }
}
