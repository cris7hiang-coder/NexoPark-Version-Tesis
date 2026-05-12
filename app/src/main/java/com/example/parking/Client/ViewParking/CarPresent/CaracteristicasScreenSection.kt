package com.example.parking.Client.ViewParking.CarPresent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.AssistantPhoto
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wc
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaracteristicasScreenSection(
    caracteristicas: List<String>,
    reglas: List<String>,
    modifier: Modifier = Modifier
) {
    val caracteristicasDisponibles = remember {
        mapOf(
            "camaras" to Icons.Default.Videocam,
            "guardias" to Icons.Default.Security,
            "iluminacion" to Icons.Default.Lightbulb,
            "techo" to Icons.Default.Roofing,
            "limpieza" to Icons.Default.CleanHands,
            "senalizacion" to Icons.Default.Directions,
            "pago qr" to Icons.Default.QrCodeScanner,
            "accesibilidad" to Icons.Default.Accessible,
            "bano" to Icons.Default.Wc,
            "area espera" to Icons.Default.EventSeat
        )
    }

    val reglasDisponibles = remember {
        mapOf(
            "no mascotas" to Icons.Default.Pets,
            "solo clientes" to Icons.Default.Store,
            "no fumar" to Icons.Default.SmokingRooms,
            "no pernoctar" to Icons.Default.NightsStay,
            "no lavado" to Icons.Default.LocalCarWash,
            "silencio noche" to Icons.Default.VolumeOff
        )
    }

    val caracteristicasValidas = caracteristicas.mapNotNull { key ->
        caracteristicasDisponibles[key]?.let { icono -> key to icono }
    }

    val reglasValidas = reglas.mapNotNull { key ->
        reglasDisponibles[key]?.let { icono -> key to icono }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        if (caracteristicasValidas.isEmpty() && reglasValidas.isEmpty()) {
            EmptyCaracteristicasState()
            return
        }

        if (caracteristicasValidas.isNotEmpty()) {
            CaracteristicasBlock(
                title = "Características",
                items = caracteristicasValidas
            )
        }

        if (caracteristicasValidas.isNotEmpty() && reglasValidas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (reglasValidas.isNotEmpty()) {
            CaracteristicasBlock(
                title = "Reglas",
                items = reglasValidas
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaracteristicasBlock(
    title: String,
    items: List<Pair<String, ImageVector>>
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
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { (texto, icono) ->
                    CaracteristicaChipResumen(
                        texto = texto,
                        icono = icono
                    )
                }
            }
        }
    }
}

@Composable
fun CaracteristicaChipResumen(
    texto: String,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = texto.humanize(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyCaracteristicasState() {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Icon(
                    imageVector = Icons.Default.FactCheck,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Sin información adicional",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Este parqueo todavía no registró características ni reglas.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
