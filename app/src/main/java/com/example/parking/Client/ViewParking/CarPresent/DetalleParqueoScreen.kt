package com.example.parking.Client.ViewParking.CarPresent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.AssistChip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parking.Client.ViewParking.CarReserv.PreReservaCheckFAB
import com.example.parking.ui.theme.FondoCard
import com.example.parking.ui.theme.TextoSecundario
import com.example.parking.Admin.EspacioParqueo
import com.example.parking.Admin.EstadoEspacio
import com.example.parking.ui.theme.FondoClaro
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

@Composable
fun DetalleParqueoScreen(
    navController: NavController,
    parqueoId: String,
    onBack: () -> Unit,
    viewModel: DetalleParqueoViewModel = viewModel()
) {
    LaunchedEffect(parqueoId) {
        viewModel.cargarDatos(parqueoId)
    }

    val parqueo = viewModel.parqueo

    if (parqueo == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val espacios by produceState<List<EspacioParqueo>>(
        initialValue = emptyList(),
        key1 = parqueo.id
    ) {
        value = try {
            FirebaseFirestore.getInstance()
                .collection("parqueos")
                .document(parqueo.id)
                .collection("espacios")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val tipo = doc.getString("tipoVehiculo") ?: return@mapNotNull null
                    val indice = doc.getLong("indice")?.toInt() ?: return@mapNotNull null
                    val estadoStr = doc.getString("estado")?.uppercase() ?: "DISPONIBLE"
                    val estado = runCatching { EstadoEspacio.valueOf(estadoStr) }
                        .getOrElse { EstadoEspacio.DISPONIBLE }

                    EspacioParqueo(
                        tipoVehiculo = tipo,
                        indice = indice,
                        estado = estado
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            PreReservaCheckFAB(
                parqueoId = parqueo.id,
                navController = navController,
                onReservar = {
                    navController.navigate("pantalla_reserva/${parqueo.id}")
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TopImageHeaderGaleria(
                    imagenes = parqueo.imagenes,
                    onBack = onBack,
                    esFavorito = viewModel.isFavorito,
                    onToggleFavorito = { viewModel.toggleFavorito() }
                )
            }

            item {
                DetalleResumenCard(
                    nombre = parqueo.nombre,
                    direccion = parqueo.direccion,
                    horario = formatearHorario(
                        parqueo.horaInicio,
                        parqueo.horaFin
                    ),
                    encargado = parqueo.encargado
                )
            }

            item {
                MiniSatisfaccionHeader(
                    promedio = viewModel.promedio,
                    total = viewModel.reseñas.size
                )
            }

            item {
                SimpleDetailSection(
                    title = "Sobre el parqueo"
                ) {
                    Text(
                        text = parqueo.descripcion.ifBlank {
                            "Sin descripción disponible."
                        },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                SimpleSectionHeader("Capacidades y tarifas")
                TarifasSection(
                    capacidades = parqueo.capacidades,
                    tarifas = parqueo.tarifas,
                    disponibles = contarDisponiblesPorTipo(espacios)
                )
            }

            item {
                SimpleSectionHeader("Características y reglas")
                CaracteristicasScreenSection(
                    parqueo.caracteristicas,
                    parqueo.reglas
                )
            }

            item {
                SimpleSectionHeader("Opiniones")
                CalificacionesSection(
                    reseñas = viewModel.reseñas,
                    onVerTodas = {
                        navController.navigate("todas_reseñas/${parqueo.id}")
                    }
                )
            }
        }
    }
}
@Composable
fun DetalleResumenCard(
    nombre: String,
    direccion: String,
    horario: String,
    encargado: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = nombre,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            DetailInfoRow(
                icon = Icons.Default.Place,
                title = "Dirección",
                value = direccion
            )

            DetailInfoRow(
                icon = Icons.Default.AccessTime,
                title = "Horario",
                value = horario
            )

            DetailInfoRow(
                icon = Icons.Default.Person,
                title = "Encargado",
                value = encargado.ifBlank { "No especificado" }
            )
        }
    }
}
@Composable
fun DetailInfoRow(
    title: String,
    icon: ImageVector,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
fun SimpleSectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
    )
}
@Composable
fun SimpleDetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SimpleSectionHeader(title)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                content = content
            )
        }
    }
}
@Composable
fun CaracteristicaChipResumen(
    texto: String,
    icono: ImageVector
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = texto.humanize(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MiniSatisfaccionHeader(
    total: Int,
    promedio: Float,
) {
    val promedioSeguro = promedio.coerceIn(0f, 1f)
    val porcentaje = (promedioSeguro * 100).roundToInt()

    val estadoColor = when {
        total == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        promedioSeguro >= 0.7f -> MaterialTheme.colorScheme.secondary
        promedioSeguro >= 0.5f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    val icono = when {
        total == 0 -> Icons.Default.RateReview
        promedioSeguro >= 0.5f -> Icons.Default.ThumbUp
        else -> Icons.Default.ThumbDown
    }

    val estadoTexto = when {
        total == 0 -> "Sin reseñas registradas"
        promedioSeguro >= 0.7f -> "Muy buena satisfacción"
        promedioSeguro >= 0.5f -> "Satisfacción favorable"
        else -> "Satisfacción por mejorar"
    }

    val supportingText = when {
        total == 0 -> "Cuando los clientes califiquen este parqueo, aquí verás el resumen general."
        promedioSeguro >= 0.7f -> "La mayoría de las opiniones reflejan una experiencia positiva."
        promedioSeguro >= 0.5f -> "Las opiniones son mayormente aceptables, con margen de mejora."
        else -> "Las reseñas muestran observaciones importantes que requieren atención."
    }

        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = estadoColor.copy(alpha = 0.10f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icono,
                            contentDescription = null,
                            tint = estadoColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Satisfacción de clientes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = estadoTexto,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (total == 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Aún no hay opiniones publicadas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$porcentaje%",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = estadoColor
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "$total reseñas registradas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                LinearProgressIndicator(
                    progress = { promedioSeguro },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = estadoColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }

fun String.humanize(): String =
    this.replace("_", " ").replaceFirstChar { it.uppercase() }
