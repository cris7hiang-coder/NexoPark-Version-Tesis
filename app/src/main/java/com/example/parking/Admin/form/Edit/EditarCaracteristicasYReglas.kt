package com.example.parking.Admin.form.Edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parking.Admin.form.ChipResumenConIcono
import com.example.parking.Admin.form.ChipSectionVariant
import com.example.parking.Admin.form.SeccionChips
import com.example.parking.Login.PrimaryButton
import com.google.accompanist.flowlayout.FlowRow


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditarCaracteristicasYReglas(
    caracteristicasActuales: List<String> = emptyList(),
    reglasActuales: List<String> = emptyList(),
    onBack: () -> Unit,
    onGuardar: (caracteristicas: List<String>, reglas: List<String>) -> Unit
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
        mutableStateListOf<String>().apply { addAll(caracteristicasActuales) }
    }

    val reglasSeleccionadas = remember {
        mutableStateListOf<String>().apply { addAll(reglasActuales) }
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
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Editar características y reglas",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Actualiza los servicios y condiciones visibles para los clientes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                                text = "Resumen actual",
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
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    text = "Guardar cambios",
                    onClick = {
                        onGuardar(
                            caracteristicasSeleccionadas.toList(),
                            reglasSeleccionadas.toList()
                        )
                    },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Check
                )
            }
        }
    }
}
