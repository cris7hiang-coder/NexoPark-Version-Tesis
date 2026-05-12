package com.example.parking.Client.CarClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
@Composable
fun SelectorDeColorVehiculo(
    colores: List<String>,
    colorSeleccionado: String,
    onSeleccionarColor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val colorMap = remember {
        mapOf(
            "Negro" to Color(0xFF2E2E2E),
            "Blanco" to Color(0xFFF8FAFC),
            "Gris" to Color(0xFF9CA3AF),
            "Rojo" to Color(0xFFEF4444),
            "Azul" to Color(0xFF2563EB),
            "Amarillo" to Color(0xFFF59E0B),
            "Verde" to Color(0xFF34D399),
            "Plateado" to Color(0xFFD1D5DB)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "Color",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(colores) { colorName ->
                val swatchColor = colorMap[colorName] ?: MaterialTheme.colorScheme.outline
                val isSelected = colorName == colorSeleccionado
                val interactionSource = remember { MutableInteractionSource() }

                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 1f,
                    label = "selector_color_scale"
                )

                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                    label = "selector_color_border"
                )

                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    label = "selector_color_container"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(74.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSeleccionarColor(colorName)
                            },
                        shape = CircleShape,
                        color = containerColor,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = borderColor
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (colorName == "Blanco" || colorName == "Plateado") 1.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        shape = CircleShape
                                    )
                            )

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Color seleccionado",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = colorName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1
                    )
                }
            }
        }

        if (colorSeleccionado.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Seleccionado: $colorSeleccionado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}