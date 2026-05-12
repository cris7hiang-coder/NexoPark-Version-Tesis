package com.example.parking.Client.ViewParking
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.zIndex
import com.example.parking.Admin.EspacioParqueo
import com.example.parking.Admin.EstadoEspacio
import com.example.parking.Client.ViewParking.CarPresent.contarDisponiblesPorTipo
import com.example.parking.ui.theme.AmarilloAdvertencia
import com.example.parking.ui.theme.AzulPetroleo
import com.example.parking.ui.theme.RojoCoral
import com.example.parking.ui.theme.VerdeExito

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetParqueo(
    parqueoId: String,
    onDismiss: () -> Unit,
    navController: NavHostController,
    sharedViewModel: ParqueoSharedViewModel
) {
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Cargar parqueo y espacios desde Firestore
    val parqueo by produceState<ParqueoModel?>(initialValue = null, parqueoId) {
        try {
            val snap = FirebaseFirestore.getInstance()
                .collection("parqueos")
                .document(parqueoId)
                .get()
                .await()
            value = snap.takeIf { it.exists() }?.let { snapshotToParqueoModel(it) }
        } catch (e: Exception) {
            value = null
        }
    }


    val espacios by produceState<List<EspacioParqueo>>(initialValue = emptyList(), parqueoId) {
        val snap = FirebaseFirestore.getInstance()
            .collection("parqueos")
            .document(parqueoId)
            .collection("espacios")
            .get().await()
        value = snap.documents.mapNotNull { doc ->
            val tipo = doc.getString("tipoVehiculo") ?: return@mapNotNull null
            val indice = doc.getLong("indice")?.toInt() ?: return@mapNotNull null
            val estado = EstadoEspacio.valueOf(doc.getString("estado") ?: "DISPONIBLE")
            EspacioParqueo(
                tipoVehiculo = tipo,
                indice = indice,
                estado = estado
            )        }
    }

    var isFavorito by remember { mutableStateOf(false) }

    // Verificar si es favorito
    LaunchedEffect(parqueoId, user?.uid) {
        user?.uid?.let { uid ->
            isFavorito = esFavorito(uid, parqueoId)
        }
    }

    fun toggleFavorito() {
        val uid = user?.uid ?: return
        scope.launch {
            if (isFavorito) eliminarDeFavoritos(uid, parqueoId)
            else parqueo?.let { agregarAFavoritos(uid, it) }
            isFavorito = !isFavorito
        }
    }

    ModalBottomSheet(
        modifier = Modifier.zIndex(2f),
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.Transparent,
        dragHandle = null
    ) {
        if (espacios.isNotEmpty()) {
            parqueo?.let { p ->
                ParqueoPreviewCard(
                    nombre = p.nombre,
                    tarifas = p.tarifas,
                    imagenUrl = p.imagenUrl,
                    disponiblesPorTipo = contarDisponiblesPorTipo(espacios),
                    isFavorito = isFavorito,
                    onToggleFavorito = { toggleFavorito() },
                    onClick = {
                        sharedViewModel.onParqueoSeleccionado(p)
                        onDismiss()
                        navController.navigate("detalle_parqueo/${p.id}")
                    },
                    isBottomSheet = true,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
@Composable
fun ParqueoPreviewCard(
    nombre: String,
    tarifas: Map<String, Map<String, Double>>,
    imagenUrl: String,
    disponiblesPorTipo: Map<String, Int>,
    isFavorito: Boolean,
    onToggleFavorito: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isBottomSheet: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val interactionSource = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> scale.animateTo(0.985f)
                is PressInteraction.Release,
                is PressInteraction.Cancel -> scale.animateTo(1f)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clickable(
                enabled = onClick != null,
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick?.invoke()
            },
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        border = if (isBottomSheet) {
            null
        } else {
            BorderStroke(1.dp, colors.outlineVariant)
        }
    ) {
        Column {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        if (isBottomSheet) 180.dp else 188.dp
                    )
                    .clip(
                        RoundedCornerShape(
                            topStart = 22.dp,
                            topEnd = 22.dp
                        )
                    )
            ) {

                if (imagenUrl.isNotBlank()) {
                    AsyncImage(
                        model = imagenUrl,
                        contentDescription = "Imagen de $nombre",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = colors.outline,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )

                if (onToggleFavorito != null) {
                    IconButton(
                        onClick = onToggleFavorito,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(if (isBottomSheet) 36.dp else 40.dp)
                            .background(
                                colors.surface.copy(alpha = 0.88f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isFavorito)
                                Icons.Default.Favorite
                            else
                                Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorito)
                                colors.error
                            else
                                colors.onSurface
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(
                    if (isBottomSheet) 8.dp else 14.dp
                )
            )

            if (tarifas.isEmpty() && disponiblesPorTipo.isEmpty()) {
                ParqueoPreviewCardEmptyInfo()
            } else {
                TarifasTotal(
                    tarifas = tarifas,
                    disponiblesPorTipo = disponiblesPorTipo,
                    compact = true
                )
            }

            Spacer(
                modifier = Modifier.height(
                    if (isBottomSheet) 10.dp else 14.dp
                )
            )
        }
    }
}
@Composable
private fun ParqueoPreviewCardEmptyInfo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Información general",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Este parqueo no tiene tarifas o disponibilidad visibles en esta vista.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
@Composable
fun TarifasTotal(
    tarifas: Map<String, Map<String, Double>>,
    disponiblesPorTipo: Map<String, Int>,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    val tarifasOrdenadas = tarifas.toList()
        .sortedByDescending { disponiblesPorTipo[it.first] ?: 0 }

    if (tarifasOrdenadas.isEmpty()) {
        EmptyTarifasResumen(modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        tarifasOrdenadas.forEachIndexed { index, (tipo, datos) ->
            val precioHora = datos["hora"] ?: 0.0
            val disponibles = disponiblesPorTipo[tipo] ?: 0

            val estadoColor = disponibilidadColor(disponibles)
            val estadoTexto = disponibilidadTexto(disponibles)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (compact) 8.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(2f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primary.copy(alpha = 0.08f)
                    ) {
                        Icon(
                            imageVector = vehiculoIcon(tipo),
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(if (compact) 16.dp else 18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = tipo.replaceFirstChar { it.uppercase() },
                        style = if (compact) typography.bodyMedium else typography.bodyLarge,
                        color = colors.onSurface
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = disponibles.toString(),
                        style = typography.titleSmall,
                        color = estadoColor
                    )

                    Text(
                        text = estadoTexto,
                        style = typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier.weight(1.5f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Bs ${"%.2f".format(precioHora)}",
                        style = if (compact) {
                            typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        } else {
                            typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        },
                        color = colors.primary
                    )

                    Text(
                        text = "por hora",
                        style = typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            if (index < tarifasOrdenadas.lastIndex) {
                HorizontalDivider(
                    color = colors.outlineVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
@Composable
private fun EmptyTarifasResumen(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Sin tarifas visibles por el momento",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun disponibilidadColor(disponibles: Int): Color {
    return when {
        disponibles == 0 -> MaterialTheme.colorScheme.error
        disponibles <= 2 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
}

fun disponibilidadTexto(disponibles: Int): String {
    return when {
        disponibles == 0 -> "Lleno"
        disponibles <= 2 -> "Pocos"
        else -> "Disponible"
    }
}

fun vehiculoIcon(tipo: String): ImageVector {
    return when (tipo.lowercase()) {
        "auto" -> Icons.Default.DirectionsCar
        "moto" -> Icons.Default.TwoWheeler
        "camion" -> Icons.Default.LocalShipping
        else -> Icons.Default.DirectionsCar
    }
}