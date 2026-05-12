package com.example.parking.Client.ViewParking.CarPresent

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.parking.Client.ViewParking.CarReserv.point.ReseñaModel
import com.example.parking.ui.theme.AzulPetroleo
import com.example.parking.ui.theme.FondoCard
import com.example.parking.ui.theme.TextoBase
import com.example.parking.ui.theme.TextoSecundario
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodasLasReseñasScreen(
    parqueoId: String,
    onBack: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()

    var reseñas by remember { mutableStateOf<List<ReseñaModel>>(emptyList()) }
    var lastVisible by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isPaginating by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var filtro by remember { mutableStateOf(FiltroReseña.TODAS) }

    LaunchedEffect(parqueoId) {
        isLoading = true
        cargarReseñas(
            db = db,
            parqueoId = parqueoId,
            lastVisible = null
        ) { lista, last, more ->
            reseñas = lista
            lastVisible = last
            hasMore = more
            isLoading = false
        }
    }

    val reseñasFiltradas = remember(reseñas, filtro) {
        when (filtro) {
            FiltroReseña.TODAS -> reseñas
            FiltroReseña.FAVORABLES -> reseñas.filter { it.conformidad == 1 }
            FiltroReseña.CRITICAS -> reseñas.filter { it.conformidad == -1 }
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(
        listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index,
        reseñas.size,
        isPaginating,
        hasMore,
        filtro
    ) {
        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@LaunchedEffect
        val totalItems = listState.layoutInfo.totalItemsCount

        val estaCercaDelFinal = totalItems > 0 && lastVisibleIndex >= totalItems - 2

        if (
            estaCercaDelFinal &&
            !isLoading &&
            !isPaginating &&
            hasMore
        ) {
            isPaginating = true
            cargarReseñas(
                db = db,
                parqueoId = parqueoId,
                lastVisible = lastVisible
            ) { lista, last, more ->
                reseñas = reseñas + lista
                lastVisible = last
                hasMore = more
                isPaginating = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Opiniones del parqueo",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "${reseñas.size} reseñas registradas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading && reseñas.isEmpty() -> {
                ReviewsLoadingState(
                    modifier = Modifier.padding(padding)
                )
            }

            !isLoading && reseñas.isEmpty() -> {
                ReviewsEmptyState(
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ReviewsFilterBar(
                            filtro = filtro,
                            onFiltroChange = { filtro = it }
                        )
                    }

                    item {
                        ReviewsSummaryCard(
                            total = reseñas.size,
                            visibles = reseñasFiltradas.size,
                            filtro = filtro
                        )
                    }

                    if (reseñasFiltradas.isEmpty()) {
                        item {
                            ReviewsFilterEmptyState(
                                filtro = filtro
                            )
                        }
                    } else {
                        items(
                            count = reseñasFiltradas.size,
                            key = { index ->
                                val reseña = reseñasFiltradas[index]
                                "${reseña.userName}-${reseña.fecha}-${reseña.comentario.hashCode()}-$index"
                            }
                        ) { index ->
                            ReseñaCard(
                                reseña = reseñasFiltradas[index]
                            )
                        }
                    }

                    if (isPaginating) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.5.dp
                                )
                            }
                        }
                    }

                    if (!hasMore && reseñas.isNotEmpty()) {
                        item {
                            Text(
                                text = "No hay más reseñas para mostrar.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
fun cargarReseñas(
    db: FirebaseFirestore,
    parqueoId: String,
    lastVisible: DocumentSnapshot? = null,
    onResult: (List<ReseñaModel>, DocumentSnapshot?, Boolean) -> Unit
) {
    val baseQuery = db.collection("parqueos")
        .document(parqueoId)
        .collection("reseñas")
        .orderBy("fecha", Query.Direction.DESCENDING)
        .limit(10)

    val finalQuery = lastVisible?.let { baseQuery.startAfter(it) } ?: baseQuery

    finalQuery.get()
        .addOnSuccessListener { snapshot ->
            val lista = snapshot.documents.map { doc ->
                ReseñaModel(
                    userName = doc.getString("userName") ?: "Anónimo",
                    photoUrl = doc.getString("photoUrl"),
                    conformidad = doc.getLong("conformidad")?.toInt() ?: 1,
                    comentario = doc.getString("comentario") ?: "",
                    chips = (doc.get("chips") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    fecha = doc.getTimestamp("fecha")?.toDate()?.let {
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                    } ?: ""
                )
            }

            onResult(
                lista,
                snapshot.documents.lastOrNull(),
                snapshot.size() >= 10
            )
        }
        .addOnFailureListener {
            onResult(emptyList(), null, false)
        }
}
private enum class FiltroReseña {
    TODAS,
    FAVORABLES,
    CRITICAS
}
@Composable
private fun ReviewsFilterBar(
    filtro: FiltroReseña,
    onFiltroChange: (FiltroReseña) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReviewFilterChip(
            modifier = Modifier.weight(1f),
            text = "Todas",
            selected = filtro == FiltroReseña.TODAS,
            onClick = { onFiltroChange(FiltroReseña.TODAS) }
        )

        ReviewFilterChip(
            modifier = Modifier.weight(1f),
            text = "Favorables",
            selected = filtro == FiltroReseña.FAVORABLES,
            onClick = { onFiltroChange(FiltroReseña.FAVORABLES) }
        )

        ReviewFilterChip(
            modifier = Modifier.weight(1f),
            text = "Críticas",
            selected = filtro == FiltroReseña.CRITICAS,
            onClick = { onFiltroChange(FiltroReseña.CRITICAS) }
        )
    }
}
@Composable
private fun ReviewFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "reviewFilterContainer"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        label = "reviewFilterBorder"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "reviewFilterText"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = textColor
            )
        }
    }
}
@Composable
private fun ReviewsSummaryCard(
    total: Int,
    visibles: Int,
    filtro: FiltroReseña
) {
    val texto = when (filtro) {
        FiltroReseña.TODAS -> "Mostrando las opiniones más recientes registradas para este parqueo."
        FiltroReseña.FAVORABLES -> "Mostrando únicamente las valoraciones favorables."
        FiltroReseña.CRITICAS -> "Mostrando únicamente las opiniones críticas."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$visibles de $total reseñas visibles",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun ReviewsEmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Icon(
                        imageVector = Icons.Default.RateReview,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Aún no hay reseñas",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Todavía no existen opiniones visibles registradas para este parqueo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
@Composable
private fun ReviewsFilterEmptyState(
    filtro: FiltroReseña
) {
    val mensaje = when (filtro) {
        FiltroReseña.TODAS -> "No hay reseñas disponibles."
        FiltroReseña.FAVORABLES -> "No hay reseñas favorables para mostrar."
        FiltroReseña.CRITICAS -> "No hay opiniones críticas para mostrar."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FilterAltOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
private fun ReviewsLoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.6.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Cargando reseñas...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}