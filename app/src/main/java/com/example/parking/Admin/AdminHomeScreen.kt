package com.example.parking.Admin

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parking.Admin.form.RegistroParqueoWizard
import com.example.parking.Client.ViewParking.CarPresent.CalificacionesSection
import com.example.parking.Client.ViewParking.CarPresent.MiniSatisfaccionHeader
import com.example.parking.Client.ViewParking.CarReserv.point.ReseñaModel
import com.example.parking.Client.ViewParking.CarReserv.point.calcularStats
import com.example.parking.Client.ViewParking.ParqueoModel
import com.example.parking.Client.ViewParking.snapshotToParqueoModel
import com.example.parking.Login.PrimaryButton
import com.google.firebase.firestore.DocumentSnapshot

@Composable
fun AdminHomeScreen(
    navController: NavController,
    viewModel: AdminViewModel = viewModel(),
    onBottomBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val parqueo by viewModel.parqueo.collectAsState()
    val reservas by viewModel.reservas.collectAsState()
    val reseñas by viewModel.reseñas.collectAsState()
    val promedio by viewModel.promedio.collectAsState()

    val mostrarDashboard = viewModel.isParqueoCompleto() && parqueo != null
    val mostrarWizard = !isLoading && !mostrarDashboard

    LaunchedEffect(mostrarWizard) {
        onBottomBarVisibilityChanged(!mostrarWizard)
    }

    DisposableEffect(Unit) {
        onDispose { onBottomBarVisibilityChanged(true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        when {
            isLoading -> {
                DashboardLoadingState(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            mostrarDashboard -> {
                AdminDashboardContent(
                    parqueo = snapshotToParqueoModel(parqueo!!),
                    reservas = reservas,
                    reseñas = reseñas,
                    promedio = promedio,
                    navController = navController
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        shadowElevation = 2.dp,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                    ) {
                        RegistroParqueoWizard(
                            onFinishRegistro = { viewModel.cargarDatos() }
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun AdminDashboardContent(
    parqueo: ParqueoModel,
    reservas: List<DocumentSnapshot>,
    reseñas: List<ReseñaModel>,
    promedio: Float,
    navController: NavController,
    viewModel: AdminViewModel = viewModel()
) {
    val ingresosMensuales by viewModel.ingresosMensuales.collectAsState()

    val capacidadTotal = listOf("auto", "moto", "camion").sumOf {
        parqueo.capacidades[it] ?: 0
    }

    val reservasActivas = reservas.count { it.getString("estado") == "activa" }
    val reservasFinalizadas = reservas.count { it.getString("estado") == "finalizada" }
    val porcentajeOcupacion = if (capacidadTotal > 0) {
        ((reservasActivas.toFloat() / capacidadTotal.toFloat()) * 100f).toInt()
    } else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 12.dp, bottom = 112.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AdminDashboardHeader(
            nombre = parqueo.nombre,
            direccion = parqueo.direccion
        )

        Text(
            text = "Resumen general",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminMetricCard(
                icon = Icons.Default.LocalParking,
                title = "Ocupación",
                content = "$reservasActivas / $capacidadTotal",
                supporting = if (capacidadTotal > 0) {
                    "$porcentajeOcupacion% en uso"
                } else {
                    "Capacidad pendiente"
                },
                modifier = Modifier.weight(1f)
            )

            AdminMetricCard(
                icon = Icons.Default.TaskAlt,
                title = "Finalizadas",
                content = reservasFinalizadas.toString(),
                supporting = "Cierres registrados",
                modifier = Modifier.weight(1f)
            )
        }

        DashboardSectionCard(
            title = "Operación rápida",
            subtitle = "Accesos directos de uso frecuente"
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    text = "Escanear QR",
                    icon = Icons.Default.QrCodeScanner,
                    onClick = { navController.navigate("escanear_qr_admin") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { navController.navigate("reporte_reservas") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Generar reporte",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (ingresosMensuales.isNotEmpty()) {
            DashboardSectionCard(
                title = "Ingresos mensuales",
                subtitle = "Resumen consolidado por mes"
            ) {
                IngresosMensualesCard(ingresosMensuales)
            }
        }

        DashboardSectionCard(
            title = "Satisfacción de clientes",
            subtitle = if (reseñas.isEmpty()) {
                "Aún no hay reseñas registradas"
            } else {
                "Percepción general del servicio"
            }
        ) {
            MiniSatisfaccionHeader(
                total = reseñas.size,
                promedio = promedio
            )
        }
    }
}
@Composable
private fun AdminDashboardHeader(
    nombre: String,
    direccion: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 3.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Panel administrativo",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = nombre.ifBlank { "Mi parqueo" },
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            if (direccion.isNotBlank()) {
                Text(
                    text = direccion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun DashboardLoadingState(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Cargando dashboard...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
private fun AdminMetricCard(
    icon: ImageVector,
    title: String,
    content: String,
    supporting: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = content,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardSectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            content()
        }
    }
}