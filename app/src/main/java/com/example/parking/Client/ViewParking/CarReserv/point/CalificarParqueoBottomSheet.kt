package com.example.parking.Client.ViewParking.CarReserv.point

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parking.Login.PrimaryButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalificarParqueoBottomSheet(
    reservaId: String,
    parqueoId: String,
    uid: String,
    viewModel: ReseñaViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var conformidad by remember { mutableStateOf<Int?>(null) }
    var comentario by remember { mutableStateOf("") }
    var tipoOpinion by remember { mutableStateOf("Positiva") }
    val selectedChips = remember { mutableStateListOf<String>() }
    var enviarAnonimo by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }

    val opcionesPositivas = listOf(
        "Instalación limpia",
        "Atención amable",
        "Ingreso rápido",
        "Se sintió seguro",
        "Precio adecuado"
    )

    val opcionesNegativas = listOf(
        "Instalación descuidada",
        "Atención deficiente",
        "Proceso lento",
        "Poca seguridad",
        "Precio elevado"
    )

    val opcionesActuales = if (tipoOpinion == "Positiva") opcionesPositivas else opcionesNegativas

    ModalBottomSheet(
        onDismissRequest = { if (!isSending) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            ReviewSheetHeader()

            Spacer(modifier = Modifier.height(18.dp))

            ReviewReactionSelector(
                conformidad = conformidad,
                onSelect = { valor, label ->
                    conformidad = valor
                    tipoOpinion = label
                    selectedChips.clear()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ReviewOptionsGuide(
                tipoOpinion = tipoOpinion,
                seleccionRealizada = conformidad != null
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opcionesActuales.forEach { chip ->
                    val selected = selectedChips.contains(chip)
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.98f else 1f,
                        label = "review_chip_scale"
                    )

                    Surface(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (selected) selectedChips.remove(chip) else selectedChips.add(chip)
                            },
                        shape = RoundedCornerShape(50),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.50f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = comentario,
                onValueChange = { comentario = it },
                label = {
                    Text(
                        text = "Comentario opcional",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                placeholder = {
                    Text(
                        text = if (tipoOpinion == "Positiva") {
                            "Cuéntanos qué te dejó una buena impresión"
                        } else {
                            "Indica qué debería mejorar"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            ReviewAnonymousCard(
                checked = enviarAnonimo,
                onCheckedChange = { enviarAnonimo = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            PrimaryButton(
                text = if (isSending) {
                    "Enviando..."
                } else if (tipoOpinion == "Positiva") {
                    "Enviar valoración"
                } else {
                    "Enviar comentario"
                },
                loading = isSending,
                enabled = conformidad != null && !isSending,
                onClick = {
                    if (conformidad == null || isSending) return@PrimaryButton

                    isSending = true

                    scope.launch {
                        viewModel.agregarReseña(
                            reservaId = reservaId,
                            parqueoId = parqueoId,
                            conformidad = conformidad!!,
                            chips = selectedChips.toList(),
                            comentario = comentario,
                            anonimo = enviarAnonimo
                        ) { success ->
                            isSending = false
                            if (success) onDismiss()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
@Composable
private fun ReviewSheetHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Califica tu experiencia",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Tu opinión ayuda a mejorar el servicio y orientar mejor a otros usuarios.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun ReviewReactionSelector(
    conformidad: Int?,
    onSelect: (Int, String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ReviewReactionCard(
            modifier = Modifier.weight(1f),
            selected = conformidad == 1,
            title = "Positiva",
            subtitle = "Experiencia satisfactoria",
            icon = Icons.Default.ThumbUp,
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
            borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.40f),
            iconTint = MaterialTheme.colorScheme.secondary,
            onClick = { onSelect(1, "Positiva") }
        )

        ReviewReactionCard(
            modifier = Modifier.weight(1f),
            selected = conformidad == -1,
            title = "Negativa",
            subtitle = "Experiencia negativa",
            icon = Icons.Default.ThumbDown,
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.35f),
            iconTint = MaterialTheme.colorScheme.error,
            onClick = { onSelect(-1, "Negativa") }
        )
    }
}
@Composable
private fun ReviewReactionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    containerColor: Color,
    borderColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.98f
            selected -> 1.01f
            else -> 1f
        },
        label = "review_reaction_scale"
    )

    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) containerColor else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) borderColor else MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = if (selected) iconTint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (selected) iconTint else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun ReviewAnonymousCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onCheckedChange(!checked)
                }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Privacidad de reseña",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Privacidad de la reseña",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (checked) {
                        "Se enviará sin mostrar tu identidad a otros usuarios."
                    } else {
                        "Puedes ocultar tu identidad al publicar esta reseña."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                )
            )
        }
    }
}
@Composable
private fun ReviewOptionsGuide(
    tipoOpinion: String,
    seleccionRealizada: Boolean
) {
    val esPositiva = tipoOpinion == "Positiva"

    val title = if (!seleccionRealizada) {
        "Aspectos de la experiencia"
    } else if (esPositiva) {
        "Aspectos a destacar"
    } else {
        "Aspectos a mejorar"
    }

    val subtitle = if (!seleccionRealizada) {
        "Selecciona primero el tipo de experiencia y luego marca lo que mejor la describa."
    } else if (esPositiva) {
        "Marca los puntos que consideras más valiosos del servicio."
    } else {
        "Marca los aspectos que consideras que deberían corregirse o mejorar."
    }

    val accentColor = if (esPositiva) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            accentColor.copy(alpha = 0.18f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}