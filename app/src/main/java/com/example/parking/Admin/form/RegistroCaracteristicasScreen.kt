package com.example.parking.Admin.form


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AssistantPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parking.Login.PrimaryButton
import kotlin.collections.addAll
import kotlin.collections.remove

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistroCaracteristicasScreen(
    caracteristicasIniciales: List<String> = emptyList(),
    reglasIniciales: List<String> = emptyList(),
    onBack: () -> Unit,
    onNext: (List<String>, List<String>) -> Unit
) {
    val caracteristicasDisponibles = remember {
        listOf(
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
        listOf(
            "no mascotas" to Icons.Default.Pets,
            "solo clientes" to Icons.Default.Store,
            "no fumar" to Icons.Default.SmokingRooms,
            "no pernoctar" to Icons.Default.NightsStay,
            "no lavado" to Icons.Default.LocalCarWash,
            "silencio noche" to Icons.Default.VolumeOff
        )
    }

    val caracteristicasSeleccionadas = remember {
        mutableStateListOf<String>().apply { addAll(caracteristicasIniciales) }
    }

    val reglasSeleccionadas = remember {
        mutableStateListOf<String>().apply { addAll(reglasIniciales) }
    }

    val totalSeleccionados = caracteristicasSeleccionadas.size + reglasSeleccionadas.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp),
            contentPadding = PaddingValues(
                start = 4.dp,
                end = 4.dp,
                top = 4.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                WizardStepHeaderCompact(
                    title = "Características y reglas",
                    subtitle = "Selecciona los servicios y condiciones que definen tu parqueo.",
                    stepLabel = "Paso 5 de 7"
                )
            }

            if (totalSeleccionados > 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Resumen de selección",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "$totalSeleccionados elemento(s) seleccionado(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                (caracteristicasDisponibles + reglasDisponibles).forEach { (clave, icono) ->
                                    if (clave in caracteristicasSeleccionadas || clave in reglasSeleccionadas) {
                                        ChipResumenConIcono(
                                            texto = clave,
                                            icono = icono,
                                            onRemove = {
                                                caracteristicasSeleccionadas.remove(clave)
                                                reglasSeleccionadas.remove(clave)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SeccionChips(
                    titulo = "Características del parqueo",
                    descripcion = "Selecciona servicios, equipamiento y comodidades disponibles.",
                    items = caracteristicasDisponibles,
                    seleccionados = caracteristicasSeleccionadas,
                    variant = ChipSectionVariant.Features
                )
            }

            item {
                SeccionChips(
                    titulo = "Reglas del parqueo",
                    descripcion = "Selecciona condiciones o restricciones aplicables.",
                    items = reglasDisponibles,
                    seleccionados = reglasSeleccionadas,
                    variant = ChipSectionVariant.Rules
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
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
                        text = "Atrás",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                PrimaryButton(
                    text = "Guardar y continuar",
                    onClick = {
                        onNext(
                            caracteristicasSeleccionadas.toList(),
                            reglasSeleccionadas.toList()
                        )
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowForward
                )
            }
        }
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeccionChips(
    titulo: String,
    descripcion: String,
    items: List<Pair<String, ImageVector>>,
    seleccionados: SnapshotStateList<String>,
    variant: ChipSectionVariant
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
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { (texto, icono) ->
                    val activo = texto in seleccionados

                    CaracteristicaChipMinimal(
                        label = texto,
                        icon = icono,
                        activo = activo,
                        variant = variant
                    ) {
                        if (activo) {
                            seleccionados.remove(texto)
                        } else {
                            seleccionados.add(texto)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun CaracteristicaChipMinimal(
    label: String,
    icon: ImageVector,
    activo: Boolean,
    variant: ChipSectionVariant,
    onClick: () -> Unit
) {
    val activeColor = when (variant) {
        ChipSectionVariant.Features -> MaterialTheme.colorScheme.primary
        ChipSectionVariant.Rules -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            if (activo) {
                activeColor.copy(alpha = 0.24f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        color = if (activo) {
            activeColor.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        tonalElevation = 0.dp,
        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (activo) {
                    activeColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Text(
                text = label.humanize(),
                style = MaterialTheme.typography.labelMedium,
                color = if (activo) {
                    activeColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            if (activo) {
                Surface(
                    shape = CircleShape,
                    color = activeColor
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(12.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun ChipResumenConIcono(
    texto: String,
    icono: ImageVector,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onRemove() },
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = texto.humanize(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Quitar",
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
enum class ChipSectionVariant {
    Features,
    Rules
}
private fun String.humanize(): String {
    return replace("_", " ").replaceFirstChar { it.uppercase() }
}