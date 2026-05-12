package com.example.parking.Admin.form
import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.parking.Login.PrimaryButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalTime
@Composable
fun HorarioMinimalistaSelector(
    valueInicio: LocalTime,
    valueFin: LocalTime,
    abierto24Value: Boolean,
    onChange: (inicio: LocalTime, fin: LocalTime, abierto24: Boolean) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Horario de atención",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (abierto24Value) {
                    "Disponible las 24 horas"
                } else {
                    "${valueInicio.formatHm()} - ${valueFin.formatHm()}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = { showSheet = true },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Cambiar horario",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    if (showSheet) {
        HorarioBottomSheet(
            initialInicio = valueInicio,
            initialFin = valueFin,
            initial24 = abierto24Value,
            onClose = { showSheet = false },
            onConfirm = { hIni, hFin, is24 ->
                onChange(hIni, hFin, is24)
                showSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HorarioBottomSheet(
    initialInicio: LocalTime,
    initialFin: LocalTime,
    initial24: Boolean,
    onClose: () -> Unit,
    onConfirm: (LocalTime, LocalTime, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var abierto24 by remember { mutableStateOf(initial24) }
    var inicio by remember { mutableStateOf(initialInicio) }
    var fin by remember { mutableStateOf(initialFin) }

    val horarioInvalido = !abierto24 && !fin.isAfter(inicio)

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Selecciona el horario",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Configura el rango de atención del parqueo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Abierto 24 horas",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Activa esta opción si el parqueo funciona todo el día.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = abierto24,
                        onCheckedChange = {
                            abierto24 = it
                            if (it) {
                                inicio = LocalTime.MIDNIGHT
                                fin = LocalTime.of(23, 59)
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = !abierto24,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TimeWheelGroup(
                                title = "Apertura",
                                time = inicio,
                                onTimeChange = { inicio = it }
                            )

                            TimeWheelGroup(
                                title = "Cierre",
                                time = fin,
                                onTimeChange = { fin = it }
                            )
                        }
                    }

                    if (horarioInvalido) {
                        Text(
                            text = "La hora de cierre debe ser posterior a la de apertura.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            PrimaryButton(
                text = "Confirmar horario",
                onClick = {
                    val finCorregido = if (!abierto24 && fin <= inicio) {
                        inicio.plusHours(1)
                    } else {
                        fin
                    }

                    onConfirm(inicio, finCorregido, abierto24)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
                icon = Icons.Default.Check
            )
        }
    }
}

@Composable
private fun TimeWheelGroup(
    title: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    val hours = remember { (0..23).toList() }
    val minutes = remember { listOf(0, 30) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPicker(
                items = hours,
                initialIndex = time.hour,
                itemLabel = { "%02d".format(it) },
                onSelected = { selectedHour ->
                    onTimeChange(LocalTime.of(selectedHour, time.minute))
                }
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            WheelPicker(
                items = minutes,
                initialIndex = minutes.indexOf(time.minute).coerceAtLeast(0),
                itemLabel = { "%02d".format(it) },
                onSelected = { selectedMin ->
                    onTimeChange(LocalTime.of(time.hour, selectedMin))
                }
            )
        }
    }
}

@Composable
private fun <T> WheelPicker(
    items: List<T>,
    initialIndex: Int,
    visibleItemsCount: Int = 5,
    rowHeight: Dp = 42.dp,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val pickerHeight = rowHeight * visibleItemsCount

    var selectedIndex by remember(items, initialIndex) { mutableStateOf(initialIndex) }

    LaunchedEffect(items, initialIndex) {
        val safeIndex = initialIndex.coerceIn(items.indices)
        listState.scrollToItem(safeIndex)
        selectedIndex = safeIndex
        onSelected(items[safeIndex])
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset to listState.firstVisibleItemIndex }
            .collectLatest {
                val center = findCenteredItemIndex(listState)
                if (center != null && center in items.indices && center != selectedIndex) {
                    selectedIndex = center
                    onSelected(items[center])
                }
            }
    }

    Box(
        modifier = Modifier
            .width(68.dp)
            .height(pickerHeight),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = (pickerHeight - rowHeight) / 2)
        ) {
            itemsIndexed(items) { index, item ->
                val isSelected = index == selectedIndex

                val animatedAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.42f,
                    label = "wheel_alpha"
                )

                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.88f,
                    label = "wheel_scale"
                )

                Text(
                    text = itemLabel(item),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .height(rowHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = animatedAlpha
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(index)
                                selectedIndex = index
                                onSelected(item)
                            }
                        },
                    style = if (isSelected) {
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier.matchParentSize()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        RoundedCornerShape(14.dp)
                    )
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            Color.Transparent,
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    )
                )
        )
    }
}

private fun findCenteredItemIndex(listState: LazyListState): Int? {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val center = layoutInfo.viewportStartOffset +
            (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) / 2

    return visibleItems.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        kotlin.math.abs(itemCenter - center)
    }?.index
}

private fun LocalTime.formatHm(): String = "%02d:%02d".format(hour, minute)