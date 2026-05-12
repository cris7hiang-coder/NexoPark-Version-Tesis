package com.example.parking.Client.ViewParking.CarReserv.Detall

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parking.Admin.CalculadoraCobroReserva
import com.example.parking.components.nav.AppRoutes
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleReservaScreen(
    reservaId: String,
    navController: NavController,
    viewModel: DetalleReservaViewModel = viewModel()
) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    LaunchedEffect(reservaId) {
        viewModel.start(reservaId, uid)
    }

    val reserva = viewModel.reserva
    val parqueo = viewModel.parqueo
    val vehiculo = viewModel.vehiculo
    val espacio = viewModel.espacio

    LaunchedEffect(viewModel.reservaExpirada) {
        if (viewModel.reservaExpirada) {
            Toast.makeText(context, "Reserva expirada", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    var countdownState by remember {
        mutableStateOf<QrCountdownUiState>(QrCountdownUiState.Hidden)
    }

    LaunchedEffect(
        reserva?.id,
        reserva?.getString("estado"),
        reserva?.getBoolean("qrActivo"),
        reserva?.getTimestamp("fechaExpiracion")
    ) {
        val res = reserva ?: return@LaunchedEffect
        val estado = res.getString("estado") ?: return@LaunchedEffect
        val qrActivo = res.getBoolean("qrActivo") ?: false

        if (estado != ReservaEstado.PENDIENTE || !qrActivo) {
            countdownState = QrCountdownUiState.Hidden
            return@LaunchedEffect
        }

        val expiracion = res.getTimestamp("fechaExpiracion")?.toDate() ?: run {
            countdownState = QrCountdownUiState.Hidden
            return@LaunchedEffect
        }

        while (isActive) {
            val ahora = System.currentTimeMillis()
            val restante = expiracion.time - ahora

            if (restante <= 0L) {
                countdownState = QrCountdownUiState.AwaitingServerUpdate
                break
            }

            val minutos = restante / 60000
            val segundos = (restante / 1000) % 60

            countdownState = QrCountdownUiState.Running(
                text = "%02d:%02d".format(minutos, segundos)
            )

            delay(1000)
        }
    }

    val formato = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    val detalleUi = remember(reserva, parqueo, vehiculo, espacio) {
        if (reserva == null || parqueo == null || vehiculo == null) {
            null
        } else {
            val nombreParqueo = parqueo.getString("nombre").orEmpty()
            val direccion = parqueo.getString("direccion").orEmpty()
            val placa = vehiculo.getString("placa").orEmpty()
            val estado = reserva.getString("estado").orEmpty()
            val qrActivo = reserva.getBoolean("qrActivo") ?: false
            val latitud = parqueo.getDouble("latitud")
            val longitud = parqueo.getDouble("longitud")
            val puedeVerUbicacion = latitud != null && longitud != null

            val horaIngreso = reserva.getTimestamp("horaLlegada")
                ?.toDate()
                ?.let(formato::format)
                ?: "Pendiente"

            val horaSalida = reserva.getTimestamp("horaSalida")
                ?.toDate()
                ?.let(formato::format)
                ?: "Pendiente"

            val textoEspacio = espacio?.getLong("indice")
                ?.toInt()
                ?.let { "N° $it" }
                ?: "Se asigna al llegar"

            val costoFinal = reserva.getDouble("costoFinal") ?: 0.0
            val costoEstimadoActual = calcularCostoEstimadoCliente(reserva, parqueo)

            val costoTexto = when (estado) {
                ReservaEstado.FINALIZADA -> "Bs ${"%.2f".format(costoFinal)}"
                ReservaEstado.ACTIVA -> {
                    if (costoEstimadoActual != null) {
                        "Bs ${"%.2f".format(costoEstimadoActual)}"
                    } else {
                        "Se calculará al finalizar"
                    }
                }
                ReservaEstado.PENDIENTE -> "Aún no generado"
                ReservaEstado.EXPIRADA -> "Sin cobro"
                else -> "—"
            }

            DetalleReservaUi(
                nombreParqueo = nombreParqueo,
                direccion = direccion,
                placa = placa,
                estado = estado,
                qrActivo = qrActivo,
                latitud = latitud,
                longitud = longitud,
                puedeVerUbicacion = puedeVerUbicacion,
                textoEspacio = textoEspacio,
                horaIngreso = horaIngreso,
                horaSalida = horaSalida,
                costoFinal = costoFinal,
                costoTexto = costoTexto
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Detalle de reserva",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.navigate(AppRoutes.ClienteReservas) {
                                launchSingleTop = true
                                popUpTo(AppRoutes.ClienteReservas) { inclusive = false }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        bottomBar = {
            val ui = detalleUi
            if (ui != null) {
                val bloqueoTemporalPorUi =
                    countdownState is QrCountdownUiState.AwaitingServerUpdate

                val puedeAbrirQR = ui.qrActivo && (
                        ui.estado == ReservaEstado.ACTIVA ||
                                (ui.estado == ReservaEstado.PENDIENTE && !bloqueoTemporalPorUi)
                        )

                FloatingReservaActionsBar(
                    puedeAbrirQR = puedeAbrirQR,
                    puedeVerUbicacion = ui.puedeVerUbicacion,
                    onVerQr = {
                        navController.navigate("qr_reserva/$reservaId")
                    },
                    onVerMapa = {
                        if (ui.puedeVerUbicacion && ui.latitud != null && ui.longitud != null) {
                            navController.navigate(
                                "mapa_parqueo_completo/${ui.latitud}/${ui.longitud}/${Uri.encode(ui.nombreParqueo)}/${Uri.encode(ui.direccion)}"
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val ui = detalleUi
            if (ui == null) {
                ComprobanteSkeleton()
                return@Column
            }

            ComprobanteReservaCard(
                reservaId = reservaId,
                nombreParqueo = ui.nombreParqueo,
                direccion = ui.direccion,
                placa = ui.placa,
                estado = ui.estado,
                countdownState = countdownState,
                textoEspacio = ui.textoEspacio,
                horaIngreso = ui.horaIngreso,
                horaSalida = ui.horaSalida,
                costoTexto = ui.costoTexto
            )

            when (ui.estado) {
                ReservaEstado.EXPIRADA -> {
                    EstadoNoticeCard(
                        text = "La reserva expiró y el acceso QR quedó inhabilitado.",
                        tone = NoticeTone.Error
                    )
                }

                ReservaEstado.ACTIVA -> {
                    EstadoNoticeCard(
                        text = "La reserva está activa. Conserva este comprobante para el control de salida.",
                        tone = NoticeTone.Info
                    )
                }

                ReservaEstado.FINALIZADA -> {
                    EstadoNoticeCard(
                        text = "Servicio finalizado correctamente.",
                        tone = NoticeTone.Success
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ComprobanteReservaCard(
    reservaId: String,
    nombreParqueo: String,
    direccion: String,
    placa: String,
    estado: String,
    countdownState: QrCountdownUiState,
    textoEspacio: String,
    horaIngreso: String,
    horaSalida: String,
    costoTexto: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ComprobanteHeaderBlock(
                estado = estado,
                countdownState = countdownState
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ComprobanteSectionTitle(
                    title = "Datos de la reserva",
                    subtitle = "Información principal del servicio y control de acceso."
                )

                ComprobanteInfoRow(
                    icon = Icons.Default.LocalParking,
                    title = "Parqueo",
                    value = nombreParqueo.ifBlank { "No disponible" },
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )

                ComprobanteInfoRow(
                    icon = Icons.Default.Place,
                    title = "Dirección",
                    value = direccion.ifBlank { "No disponible" },
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )

                ComprobanteInfoRow(
                    icon = Icons.Default.DirectionsCar,
                    title = "Vehículo",
                    value = placa.ifBlank { "No registrado" },
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                ComprobanteInfoRow(
                    icon = Icons.Default.GridView,
                    title = "Espacio",
                    value = textoEspacio,
                    iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ComprobanteSectionTitle(
                    title = "Control operativo",
                    subtitle = "Registro de ingreso y salida del servicio."
                )

                ComprobanteInfoRow(
                    icon = Icons.Default.Login,
                    title = "Hora de ingreso",
                    value = horaIngreso,
                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    iconTint = MaterialTheme.colorScheme.primary
                )

                ComprobanteInfoRow(
                    icon = Icons.Default.Logout,
                    title = "Hora de salida",
                    value = horaSalida,
                    iconContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }

            MontoServicioCard(
                estado = estado,
                costoTexto = costoTexto
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Código de reserva",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = reservaId,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
@Composable
private fun ComprobanteHeaderBlock(
    estado: String,
    countdownState: QrCountdownUiState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Comprobante de reserva",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Documento de control para acceso, seguimiento y validación del servicio.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EstadoBadge(estado)
            CountdownStatusBadge(state = countdownState)
        }
    }
}
@Composable
private fun ComprobanteSectionTitle(
    title: String,
    subtitle: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(
                lineHeight = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun MontoServicioCard(
    estado: String,
    costoTexto: String
) {
    val isFinalizado = estado == ReservaEstado.FINALIZADA
    val isActivo = estado == ReservaEstado.ACTIVA
    val isPendiente = estado == ReservaEstado.PENDIENTE
    val isExpirada = estado == ReservaEstado.EXPIRADA

    val backgroundColor = when {
        isFinalizado -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        isActivo -> Color(0xFF3FA36C).copy(alpha = 0.08f)
        isPendiente -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
        isExpirada -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val borderColor = when {
        isFinalizado -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        isActivo -> Color(0xFF3FA36C).copy(alpha = 0.18f)
        isPendiente -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f)
        isExpirada -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    val accentColor = when {
        isFinalizado -> MaterialTheme.colorScheme.primary
        isActivo -> Color(0xFF2E8B57)
        isPendiente -> MaterialTheme.colorScheme.tertiary
        isExpirada -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val title = when {
        isFinalizado -> "Cobro final"
        isActivo -> "Estimación actual"
        isPendiente -> "Cobro pendiente"
        isExpirada -> "Cobro no generado"
        else -> "Cobro del servicio"
    }

    val supportingText = when {
        isFinalizado -> "El monto final fue calculado al cierre de la ocupación según el tiempo real utilizado."
        isActivo -> "Estimación actual calculada con la misma tarifa operativa del parqueo. El monto final será confirmado al registrar la salida."
        isPendiente -> "La reserva todavía no inició. El monto se calculará al finalizar el servicio."
        isExpirada -> "La reserva venció antes del uso del espacio, por lo que no se generó un cobro operativo."
        else -> "Resumen económico del servicio."
    }

    val footerText = when {
        isFinalizado -> "Importe final emitido"
        isActivo -> "Cálculo pendiente de cierre"
        isPendiente -> "Aún sin generación de importe"
        isExpirada -> "Sin cargo aplicado"
        else -> null
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = accentColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "RESUMEN ECONÓMICO",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp
                    ),
                    color = accentColor
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.12f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = costoTexto,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = accentColor
                    )
                }
            }

            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            footerText?.let {
                HorizontalDivider(
                    color = borderColor.copy(alpha = 0.8f)
                )

                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = accentColor
                )
            }
        }
    }
}
@Composable
private fun ComprobanteInfoRow(
    icon: ImageVector,
    title: String,
    value: String,
    iconContainerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = iconContainerColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 1.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
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
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
private fun FloatingReservaActionsBar(
    puedeAbrirQR: Boolean,
    puedeVerUbicacion: Boolean,
    onVerQr: () -> Unit,
    onVerMapa: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            ),
            tonalElevation = 2.dp,
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingReservaPrimaryButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.QrCode2,
                    title = "Ver QR",
                    enabled = puedeAbrirQR,
                    onClick = onVerQr
                )

                FloatingReservaSecondaryButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocationOn,
                    title = "Ubicación",
                    enabled = puedeVerUbicacion,
                    onClick = onVerMapa
                )
            }
        }
    }
}
@Composable
private fun FloatingReservaPrimaryButton(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.98f
            hovered -> 1.01f
            else -> 1f
        },
        label = "primaryButtonScale"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant
            pressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
            hovered -> MaterialTheme.colorScheme.primary.copy(alpha = 0.94f)
            else -> MaterialTheme.colorScheme.primary
        },
        label = "primaryButtonContainer"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "primaryButtonContent"
    )

    val iconContainerColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = if (hovered) 0.22f else 0.16f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
        },
        label = "primaryButtonIconBg"
    )

    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = if (enabled && hovered) 6.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(
                    enabled = enabled,
                    interactionSource = interactionSource
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = iconContainerColor,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor
            )
        }
    }
}
@Composable
private fun FloatingReservaSecondaryButton(
    icon: ImageVector,
    title: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.985f
            hovered -> 1.01f
            else -> 1f
        },
        label = "secondaryButtonScale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            hovered -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.34f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
        },
        label = "secondaryButtonBorder"
    )

    val containerColor by animateColorAsState(
        targetValue = when {
            !enabled -> MaterialTheme.colorScheme.surfaceVariant
            hovered -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
            pressed -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "secondaryButtonContainer"
    )

    val contentColor by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "secondaryButtonContent"
    )

    val iconTint by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.tertiary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "secondaryButtonIconTint"
    )

    val iconBg by animateColorAsState(
        targetValue = if (enabled) {
            MaterialTheme.colorScheme.tertiary.copy(alpha = if (hovered) 0.16f else 0.10f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
        },
        label = "secondaryButtonIconBg"
    )

    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
        shadowElevation = if (enabled && hovered) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(
                    enabled = enabled,
                    interactionSource = interactionSource
                )
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = iconBg,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = contentColor
            )
        }
    }
}

class DetalleReservaViewModel : ViewModel() {

    var reserva by mutableStateOf<DocumentSnapshot?>(null)
        private set

    var parqueo by mutableStateOf<DocumentSnapshot?>(null)
        private set

    var vehiculo by mutableStateOf<DocumentSnapshot?>(null)
        private set

    var espacio by mutableStateOf<DocumentSnapshot?>(null)
        private set

    var showRatingSheet by mutableStateOf(false)
        private set

    var reservaExpirada by mutableStateOf(false)
        private set

    private var listener: ListenerRegistration? = null
    private var yaMostroRating = false

    private var lastParqueoId: String? = null
    private var lastVehiculoId: String? = null
    private var lastEspacioId: String? = null

    fun start(reservaId: String, uid: String) {
        listener?.remove()

        reserva = null
        parqueo = null
        vehiculo = null
        espacio = null

        reservaExpirada = false
        showRatingSheet = false
        yaMostroRating = false

        lastParqueoId = null
        lastVehiculoId = null
        lastEspacioId = null

        val db = FirebaseFirestore.getInstance()

        listener = db.collection("reservas")
            .document(reservaId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) return@addSnapshotListener

                reserva = snapshot

                when (snapshot.getString("estado")) {
                    ReservaEstado.EXPIRADA -> {
                        reservaExpirada = true
                        showRatingSheet = false
                    }

                    ReservaEstado.FINALIZADA -> {
                        if (!yaMostroRating) {
                            showRatingSheet = true
                            yaMostroRating = true
                        }
                    }
                }

                cargarRelaciones(snapshot, uid)
            }
    }
    private fun cargarRelaciones(res: DocumentSnapshot, uid: String) {
        viewModelScope.launch {

            val db = FirebaseFirestore.getInstance()

            val parqueoId = res.getString("parqueoId") ?: return@launch
            val vehiculoId = res.getString("vehiculoId") ?: return@launch
            val espacioId = res.getString("espacioId")

            try {

                if (parqueoId != lastParqueoId) {
                    parqueo = db.collection("parqueos").document(parqueoId).get().await()
                    lastParqueoId = parqueoId
                }

                if (vehiculoId != lastVehiculoId) {
                    vehiculo = db.collection("users")
                        .document(uid)
                        .collection("vehiculos")
                        .document(vehiculoId)
                        .get()
                        .await()

                    lastVehiculoId = vehiculoId
                }
                if (espacioId != lastEspacioId) {
                    espacio = if (!espacioId.isNullOrBlank()) {
                        db.collection("parqueos")
                            .document(parqueoId)
                            .collection("espacios")
                            .document(espacioId)
                            .get()
                            .await()
                    } else {
                        null
                    }
                    lastEspacioId = espacioId
                }

            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}
@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFFEFEFEF),
        Color(0xFFDFDFDF),
        Color(0xFFEFEFEF)
    )

    val transition = rememberInfiniteTransition()
    val animX = transition.animateFloat(
        initialValue = -400f,
        targetValue = 800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Genera un gradiente que se mueve horizontalmente
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(animX.value - 200f, 0f),
        end = Offset(animX.value, 0f)
    )
}

// ----------------- SKELETON ITEM -----------------
@Composable
fun SkeletonItem(
    modifier: Modifier = Modifier,
    shapeRadius: Int = 6
) {
    val brush = rememberShimmerBrush()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(shapeRadius.dp))
            .background(brush = brush)
    )
}

// ----------------- SKELETON COMPLETO DEL COMPROBANTE -----------------
@Composable
fun ComprobanteSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        // Card simulada
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column {
                // Título
                SkeletonItem(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(24.dp)
                )
                Spacer(Modifier.height(18.dp))

                // Fila superior: cliente + QR
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SkeletonItem(modifier = Modifier.fillMaxWidth(0.7f).height(18.dp))
                        Spacer(Modifier.height(8.dp))
                        SkeletonItem(modifier = Modifier.fillMaxWidth(0.5f).height(18.dp))
                    }

                    Spacer(Modifier.width(16.dp))

                    SkeletonItem(
                        modifier = Modifier
                            .size(64.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Varias filas de datos (parqueo, placa, fecha, costo...)
                repeat(5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonItem(modifier = Modifier.fillMaxWidth(0.45f).height(18.dp))
                        SkeletonItem(modifier = Modifier.fillMaxWidth(0.45f).height(18.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(12.dp))
                SkeletonItem(modifier = Modifier.fillMaxWidth().height(14.dp))
                Spacer(Modifier.height(12.dp))

                // Código reserva + estado simulado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonItem(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
                    SkeletonItem(modifier = Modifier.width(80.dp).height(24.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // MiniMapa (simulado)
        SkeletonItem(modifier = Modifier.fillMaxWidth().height(140.dp), shapeRadius = 12)
        Spacer(Modifier.height(12.dp))

        // Temporizador (simulado)
        SkeletonItem(modifier = Modifier.fillMaxWidth().height(56.dp), shapeRadius = 12)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMapaParqueoCompleto(
    latitud: Double,
    longitud: Double,
    nombre: String,
    direccion: String,
    onBack: () -> Unit
) {
    val ubicacion = LatLng(latitud, longitud)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(latitud, longitud), 16f)
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubicación del Parqueo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = false
                )
            ) {
                Marker(
                    state = MarkerState(position = ubicacion),
                    title = nombre,
                    snippet = direccion
                )
            }

        }
    }
}
object ReservaEstado {
    const val PENDIENTE = "pendiente"
    const val ACTIVA = "activa"
    const val FINALIZADA = "finalizada"
    const val EXPIRADA = "expirada"
}
@Composable
private fun EstadoNoticeCard(
    text: String,
    tone: NoticeTone
) {
    val bg = when (tone) {
        NoticeTone.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        NoticeTone.Info -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
        NoticeTone.Success -> Color(0xFF3FA36C).copy(alpha = 0.10f)
    }

    val border = when (tone) {
        NoticeTone.Error -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
        NoticeTone.Info -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
        NoticeTone.Success -> Color(0xFF3FA36C).copy(alpha = 0.18f)
    }

    val textColor = when (tone) {
        NoticeTone.Error -> MaterialTheme.colorScheme.error
        NoticeTone.Info -> MaterialTheme.colorScheme.primary
        NoticeTone.Success -> Color(0xFF2E8B57)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp
            ),
            color = textColor,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private enum class NoticeTone {
    Error, Info, Success
}
@Composable
fun EstadoBadge(estado: String) {
    val (bgTarget, textTarget, label) = when (estado.lowercase()) {
        ReservaEstado.PENDIENTE -> Triple(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.tertiary,
            "Pendiente"
        )

        ReservaEstado.ACTIVA -> Triple(
            Color(0xFF3FA36C).copy(alpha = 0.16f),
            Color(0xFF3FA36C),
            "Activa"
        )

        ReservaEstado.FINALIZADA -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.primary,
            "Finalizada"
        )

        ReservaEstado.EXPIRADA -> Triple(
            MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            MaterialTheme.colorScheme.error,
            "Expirada"
        )

        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            estado
        )
    }

    val bg by animateColorAsState(bgTarget, label = "estadoBadgeBg")
    val textColor by animateColorAsState(textTarget, label = "estadoBadgeText")

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor
        )
    }
}


private sealed interface QrCountdownUiState {
    data object Hidden : QrCountdownUiState
    data class Running(val text: String) : QrCountdownUiState
    data object AwaitingServerUpdate : QrCountdownUiState
}
@Composable
private fun CountdownStatusBadge(
    state: QrCountdownUiState
) {
    when (state) {
        QrCountdownUiState.Hidden -> Unit

        is QrCountdownUiState.Running -> {
            val isCritical = state.text.startsWith("00:")

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isCritical) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isCritical) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(15.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = state.text,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isCritical) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }

        QrCountdownUiState.AwaitingServerUpdate -> {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Actualizando estado…",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
private data class DetalleReservaUi(
    val nombreParqueo: String,
    val direccion: String,
    val placa: String,
    val estado: String,
    val qrActivo: Boolean,
    val latitud: Double?,
    val longitud: Double?,
    val puedeVerUbicacion: Boolean,
    val textoEspacio: String,
    val horaIngreso: String,
    val horaSalida: String,
    val costoFinal: Double,
    val costoTexto: String
)
private fun calcularCostoEstimadoCliente(
    reserva: DocumentSnapshot,
    parqueo: DocumentSnapshot
): Double? {
    val tipoVehiculo = reserva.getString("tipoVehiculo")
        ?.trim()
        ?.lowercase()
        ?: return null

    val tarifas = parqueo.get("tarifas") as? Map<*, *> ?: return null
    val tarifaTipo = tarifas[tipoVehiculo] as? Map<*, *> ?: return null

    val tarifaHora = (tarifaTipo["hora"] as? Number)?.toDouble() ?: 0.0
    val tarifaMediaHora = (tarifaTipo["mediaHora"] as? Number)?.toDouble() ?: 0.0

    return CalculadoraCobroReserva.calcularCostoFinal(
        reserva = reserva,
        tarifaHora = tarifaHora,
        tarifaMediaHora = tarifaMediaHora
    )
}