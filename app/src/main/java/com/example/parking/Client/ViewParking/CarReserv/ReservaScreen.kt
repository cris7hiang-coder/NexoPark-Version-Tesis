package com.example.parking.Client.ViewParking.CarReserv
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parking.Admin.EspacioParqueo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.parking.Admin.EstadoEspacio
import com.example.parking.Admin.getIconoVehiculo
import com.example.parking.Client.CarClient.ScreenHeader
import com.example.parking.Client.ViewParking.CarReserv.Detall.ReservaEstado
import com.example.parking.components.nav.AppRoutes
import com.example.parking.ui.theme.AzulPetroleo
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale
import java.util.UUID
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservaScreen(
    parqueoId: String,
    navController: NavController,
    viewModel: ReservaViewModel = viewModel()
) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val espacioSeleccionadoHandle = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<EspacioParqueo?>("espacioSeleccionado", null)
        ?.collectAsState()

    LaunchedEffect(espacioSeleccionadoHandle?.value) {
        espacioSeleccionadoHandle?.value?.let { espacio ->
            viewModel.seleccionarEspacio(espacio)

            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<EspacioParqueo>("espacioSeleccionado")
        }
    }

    LaunchedEffect(parqueoId) {
        viewModel.cargarDatos(
            parqueoId = parqueoId,
            uid = uid,
            onError = {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                navController.popBackStack()
            }
        )
    }

    val tiposPermitidos = remember(viewModel.parqueo) {
        (viewModel.parqueo?.get("tiposVehiculo") as? List<*>)
            ?.mapNotNull { it as? String }
            ?: emptyList()
    }
    val espacioSeleccionadoFlow = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<EspacioParqueo?>("espacioSeleccionado", null)

    val espacioSeleccionadoDesdeNav by espacioSeleccionadoFlow
        ?.collectAsState()
        ?: remember { mutableStateOf(null) }

    LaunchedEffect(espacioSeleccionadoDesdeNav) {
        espacioSeleccionadoDesdeNav?.let { espacio ->
            viewModel.seleccionarEspacio(espacio)

            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<EspacioParqueo>("espacioSeleccionado")
        }
    }
    Scaffold(
        topBar = {
            MinimalTopBarCompact(
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            ScreenHeader(
                title = "Reserva tu espacio",
                subtitle = "Selecciona tu vehículo, genera tu QR y presenta tu acceso al llegar."
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SelectorVehiculoSection(
                        vehiculos = viewModel.vehiculos,
                        tiposPermitidos = tiposPermitidos,
                        vehiculoSeleccionado = viewModel.vehiculoSeleccionado,
                        onVehiculoSeleccionado = { viewModel.seleccionarVehiculo(it) }
                    )
                    val vehiculoActual = viewModel.vehiculoSeleccionado

                    val tipoVehiculoActual = vehiculoActual
                        ?.getString("tipo")
                        ?.trim()
                        ?.lowercase(Locale.ROOT)

                    AnimatedVisibility(
                        visible = vehiculoActual != null
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (!tipoVehiculoActual.isNullOrBlank()) {
                                        navController.navigate(
                                            AppRoutes.seleccionarEspacioRoute(
                                                parqueoId = parqueoId,
                                                vehiculoId = vehiculoActual!!.id,
                                                tipoVehiculo = tipoVehiculoActual
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalParking,
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = if (viewModel.espacioSeleccionado == null) {
                                        "Elegir espacio"
                                    } else {
                                        "Cambiar espacio"
                                    }
                                )
                            }

                            viewModel.espacioSeleccionado?.let { espacio ->
                                EspacioSeleccionadoReservaCard(espacio)
                            }
                        }
                    }
                }
            }

            InfoReserva(
                toleranciaMin = viewModel.toleranciaMin
            )
            BotonReserva(
                estado = viewModel.estado,
                enabled = viewModel.vehiculoSeleccionado != null &&
                        viewModel.espacioSeleccionado != null,
                onClick = {
                    viewModel.crearReserva(
                        parqueoId = parqueoId,
                        uid = uid,
                        onSuccess = { reservaId ->
                            navController.navigate("detalle_reserva/$reservaId")
                        },
                        onError = {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
class ReservaViewModel : ViewModel() {

    var espacioSeleccionado by mutableStateOf<EspacioParqueo?>(null)
        private set

    fun seleccionarEspacio(espacio: EspacioParqueo) {
        espacioSeleccionado = espacio
    }

    fun limpiarEspacioSeleccionado() {
        espacioSeleccionado = null
    }
    var parqueo by mutableStateOf<DocumentSnapshot?>(null)
        private set

    var vehiculos by mutableStateOf<List<DocumentSnapshot>>(emptyList())
        private set

    var vehiculoSeleccionado by mutableStateOf<DocumentSnapshot?>(null)
        private set

    var estado by mutableStateOf("idle")
        private set

    var toleranciaMin by mutableStateOf(10)
        private set

    fun seleccionarVehiculo(vehiculo: DocumentSnapshot) {
        vehiculoSeleccionado = vehiculo
    }

    fun cargarDatos(
        parqueoId: String,
        uid: String,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()

                val parqueoDoc = db.collection("parqueos")
                    .document(parqueoId)
                    .get()
                    .await()

                parqueo = parqueoDoc

                vehiculos = db.collection("users")
                    .document(uid)
                    .collection("vehiculos")
                    .get()
                    .await()
                    .documents

                toleranciaMin = (
                        parqueoDoc.getLong("toleranciaMin")
                            ?: parqueoDoc.getLong("tiempoTolerancia")
                            ?: 10L
                        ).toInt()

            } catch (e: Exception) {
                onError(e.message ?: "Error al cargar datos")
            }
        }
    }

    fun crearReserva(
        parqueoId: String,
        uid: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (estado == "loading") return

        val vehiculo = vehiculoSeleccionado ?: return
        val espacio = espacioSeleccionado ?: run {
            onError("Selecciona un espacio antes de reservar")
            return
        }

        viewModelScope.launch {
            estado = "loading"

            try {
                val db = FirebaseFirestore.getInstance()

                val reservasActivas = db.collection("reservas")
                    .whereEqualTo("clienteId", uid)
                    .whereIn(
                        "estado",
                        listOf(
                            ReservaEstado.PENDIENTE,
                            ReservaEstado.ACTIVA
                        )
                    )
                    .get()
                    .await()

                if (!reservasActivas.isEmpty) {
                    throw Exception("Ya tienes una reserva activa")
                }

                val tipoVehiculo = vehiculo.getString("tipo")
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    ?: "auto"

                val espaciosRef = db.collection("parqueos")
                    .document(parqueoId)
                    .collection("espacios")

                val espacioQuery = espaciosRef
                    .whereEqualTo("tipoVehiculo", espacio.tipoVehiculo)
                    .whereEqualTo("indice", espacio.indice)
                    .limit(1)
                    .get()
                    .await()

                val espacioRef = espacioQuery.documents.firstOrNull()?.reference
                    ?: espaciosRef.document("${espacio.tipoVehiculo}_${espacio.indice}")

                val reservaRef = db.collection("reservas").document()

                val ahora = Date()
                val toleranciaMs = toleranciaMin * 60_000L
                val fechaExpiracion = Date(ahora.time + toleranciaMs)

                db.runTransaction { tx ->
                    val espacioSnap = tx.get(espacioRef)

                    if (espacioSnap.exists()) {
                        val estadoEspacio = espacioSnap.getString("estado")
                            ?: EstadoEspacio.DISPONIBLE.name

                        if (estadoEspacio != EstadoEspacio.DISPONIBLE.name) {
                            throw Exception("El espacio ya no está disponible")
                        }
                    }

                    val reserva = hashMapOf(
                        "id" to reservaRef.id,
                        "clienteId" to uid,
                        "parqueoId" to parqueoId,
                        "vehiculoId" to vehiculo.id,
                        "tipoVehiculo" to tipoVehiculo,
                        "espacioId" to espacioRef.id,
                        "indice" to espacio.indice,
                        "modoLibre" to true,
                        "estado" to ReservaEstado.PENDIENTE,
                        "qrActivo" to true,
                        "presente" to false,
                        "cobroProcesado" to false,
                        "toleranciaMin" to toleranciaMin,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "fechaExpiracion" to Timestamp(fechaExpiracion)
                    )

                    tx.set(reservaRef, reserva)

                    tx.set(
                        espacioRef,
                        mapOf(
                            "tipoVehiculo" to espacio.tipoVehiculo,
                            "indice" to espacio.indice,
                            "estado" to EstadoEspacio.RESERVADO.name,
                            "cliente" to uid,
                            "reservaId" to reservaRef.id
                        ),
                        SetOptions.merge()
                    )
                }.await()

                estado = "success"
                delay(700)
                onSuccess(reservaRef.id)

                limpiarEspacioSeleccionado()
                estado = "idle"

            } catch (e: Exception) {
                estado = "idle"
                onError(e.message ?: "Error al reservar")
            }
        }
    }
}
@Composable
fun BotonReserva(
    estado: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val isLoading = estado == "loading"
    val isSuccess = estado == "success"
    val isEnabled = enabled && !isLoading && !isSuccess

    val containerColor by animateColorAsState(
        targetValue = when {
            isLoading -> MaterialTheme.colorScheme.surfaceVariant
            isSuccess -> MaterialTheme.colorScheme.primary
            enabled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "botonReservaContainer"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            isLoading -> MaterialTheme.colorScheme.onSurfaceVariant
            isSuccess || enabled -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "botonReservaContent"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isLoading -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            isSuccess -> MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
            enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.0f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
        },
        label = "botonReservaBorder"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = containerColor,
        border = BorderStroke(
            width = if (enabled || isSuccess) 0.dp else 1.dp,
            color = borderColor
        ),
        tonalElevation = if (enabled && !isLoading) 2.dp else 0.dp,
        shadowElevation = if (enabled && !isLoading) 6.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = isEnabled,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .padding(horizontal = 18.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = Triple(isLoading, isSuccess, enabled),
                label = "botonReservaAnimated"
            ) { (loading, success, active) ->

                when {
                    loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = contentColor
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Creando reserva",
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Espera un momento",
                                    color = contentColor.copy(alpha = 0.88f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    success -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Reserva confirmada",
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Tu acceso fue generado correctamente",
                                    color = contentColor.copy(alpha = 0.90f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    else -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(30.dp),
                                shape = CircleShape,
                                color = if (active) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (active) {
                                            Icons.Default.QrCode2
                                        } else {
                                            Icons.Default.DirectionsCar
                                        },
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = if (active) "Confirmar reserva" else "Selecciona un vehículo",
                                    color = contentColor,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Text(
                                    text = if (active) {
                                        "Genera tu acceso QR"
                                    } else {
                                        "Necesitas un vehículo compatible"
                                    },
                                    color = contentColor.copy(alpha = 0.90f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun EspacioSeleccionadoReservaCard(
    espacio: EspacioParqueo
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getIconoVehiculo(espacio.tipoVehiculo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Espacio seleccionado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${espacio.tipoVehiculo.replaceFirstChar { it.uppercase() }} #${"%02d".format(espacio.indice)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Disponible",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}