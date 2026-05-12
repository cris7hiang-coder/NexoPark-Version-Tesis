package com.example.parking.Client.ViewParking.CarReserv

import android.graphics.fonts.FontStyle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parking.Admin.getIconoVehiculo
import com.example.parking.ui.theme.AzulPetroleo
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Locale
import kotlin.collections.forEach
@Composable
fun SelectorVehiculoSection(
    vehiculos: List<DocumentSnapshot>,
    tiposPermitidos: List<String>,
    vehiculoSeleccionado: DocumentSnapshot?,
    onVehiculoSeleccionado: (DocumentSnapshot) -> Unit
) {
    val tiposPermitidosNormalizados = remember(tiposPermitidos) {
        tiposPermitidos.map {
            it.trim().lowercase(Locale.ROOT)
        }
    }

    val vehiculosCompatibles = remember(vehiculos, tiposPermitidosNormalizados) {
        vehiculos.filter { vehiculo ->
            val tipo = vehiculo.getString("tipo")
                ?.trim()
                ?.lowercase(Locale.ROOT)
                .orEmpty()

            tipo in tiposPermitidosNormalizados
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when {
            vehiculos.isEmpty() -> {
                EmptyVehiculoSelectionState(
                    text = "No tienes vehículos registrados."
                )
            }

            vehiculosCompatibles.isEmpty() -> {
                EmptyVehiculoSelectionState(
                    text = "Ninguno de tus vehículos es compatible con este parqueo."
                )
            }

            else -> {
                vehiculosCompatibles.forEach { vehiculo ->
                    val placa = vehiculo.getString("placa") ?: "Sin placa"
                    val tipo = vehiculo.getString("tipo") ?: "auto"
                    val alias = vehiculo.getString("nombreVehiculo").orEmpty()
                    val seleccionado = vehiculoSeleccionado?.id == vehiculo.id

                    val containerColor = if (seleccionado) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }

                    val borderColor = if (seleccionado) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }

                    val iconContainerColor = if (seleccionado) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onVehiculoSeleccionado(vehiculo)
                            }
                            .animateContentSize(),
                        shape = RoundedCornerShape(18.dp),
                        color = containerColor,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            width = if (seleccionado) 1.6.dp else 1.dp,
                            color = borderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape,
                                color = iconContainerColor
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconoVehiculo(tipo),
                                        contentDescription = tipo,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                if (alias.isNotBlank()) {
                                    Text(
                                        text = alias,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                Text(
                                    text = "Placa: $placa",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = tipo.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Crossfade(
                                targetState = seleccionado,
                                label = "selectState"
                            ) { checked ->
                                if (checked) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Vehículo seleccionado",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(16.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyVehiculoSelectionState(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
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
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}