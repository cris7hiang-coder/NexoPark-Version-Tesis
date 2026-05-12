package com.example.parking.Client
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.parking.Perfil.PerfilDropdown
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.parking.Admin.EspacioParqueo
import com.example.parking.Admin.EstadoEspacio
import com.example.parking.Client.ViewParking.BottomSheetParqueo
import com.example.parking.Client.ViewParking.ParqueoSharedViewModel
import com.example.parking.Client.ViewParking.parqueoTieneDisponiblesConEspacios
import com.example.parking.Client.ViewParking.snapshotToParqueoModel
import com.example.parking.ui.theme.AzulPetroleo
import com.example.parking.ui.theme.EmptyStateView
import com.example.parking.ui.theme.FondoCard
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.Locale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteHomeScreen(
    navController: NavHostController,
    sharedViewModel: ParqueoSharedViewModel
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    var locationText by remember { mutableStateOf("Obteniendo ubicación...") }

    var parqueos by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var userName by remember { mutableStateOf("Usuario") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var filtros by remember { mutableStateOf(FiltrosCliente()) }
    var mostrarSelectorTipos by remember { mutableStateOf(false) }

    var espaciosPorParqueo by remember {
        mutableStateOf<Map<String, List<EspacioParqueo>>>(emptyMap())
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val mapaViewModel: ClienteMapaViewModel = viewModel(
        key = "cliente_mapa_vm"
    )
    val cameraPositionState = mapaViewModel.cameraPositionState

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        locationText = if (hasPermission) {
            obtenerUbicacionTexto(context)
        } else {
            "Sin permiso de ubicación"
        }
    }

    LaunchedEffect(uid) {
        uid?.let { currentUid ->
            loading = true
            error = null

            try {
                val userSnapshot = db.collection("users")
                    .document(currentUid)
                    .get()
                    .await()

                userName = userSnapshot.getString("name") ?: "Usuario"
                photoUrl = userSnapshot.getString("photoUrl")
                    ?: auth.currentUser?.photoUrl?.toString()

                val parqueoSnapshot = db.collection("parqueos")
                    .get()
                    .await()

                parqueos = parqueoSnapshot.documents

                val espaciosMap = mutableMapOf<String, List<EspacioParqueo>>()

                parqueoSnapshot.documents.forEach { doc ->
                    val espaciosSnap = db.collection("parqueos")
                        .document(doc.id)
                        .collection("espacios")
                        .get()
                        .await()

                    val espacios = espaciosSnap.documents.mapNotNull { espacioDoc ->
                        val tipo = espacioDoc.getString("tipoVehiculo") ?: return@mapNotNull null
                        val indice = espacioDoc.getLong("indice")?.toInt() ?: return@mapNotNull null
                        val estadoStr = espacioDoc.getString("estado")?.uppercase() ?: "DISPONIBLE"
                        val estado = EstadoEspacio.valueOf(estadoStr)

                        EspacioParqueo(tipo, indice, estado)
                    }

                    espaciosMap[doc.id] = espacios
                }

                espaciosPorParqueo = espaciosMap

            } catch (e: Exception) {
                error = e.message ?: "Error al cargar la información"
            } finally {
                loading = false
            }
        }
    }

    val zonasDisponibles = remember(parqueos) {
        parqueos.mapNotNull { it.getString("zona") }.distinct()
    }

    val parqueosFiltrados = remember(parqueos, filtros, espaciosPorParqueo) {
        parqueos.filter { parqueo ->
            val capacidades = parqueo.get("capacidad") as? Map<*, *> ?: emptyMap<String, Any>()
            val tarifas = parqueo.get("tarifas") as? Map<*, *> ?: emptyMap<String, Any>()
            val zona = parqueo.getString("zona") ?: ""
            val calificacion = parqueo.getDouble("calificacion") ?: 0.0

            val tiposOk = filtros.tiposVehiculo.isEmpty() ||
                    capacidades.keys.any { it.toString() in filtros.tiposVehiculo }

            val espacios = espaciosPorParqueo[parqueo.id] ?: emptyList()

            val disponibilidadOk = parqueoTieneDisponiblesConEspacios(
                espacios = espacios,
                tiposSeleccionados = filtros.tiposVehiculo,
                soloDisponibles = filtros.soloDisponibles
            )

            val zonaOk = filtros.zonaSeleccionada.isNullOrBlank() ||
                    zona.contains(filtros.zonaSeleccionada!!, ignoreCase = true)

            val precioOk = if (filtros.precioMaximo != null) {
                tarifas.values.any {
                    val precioHora = (it as? Map<*, *>)?.get("hora") as? Number
                    precioHora?.toDouble()?.let { valor ->
                        valor <= filtros.precioMaximo!!
                    } ?: false
                }
            } else {
                true
            }

            val califOk = filtros.calificacionMinima?.let { calificacion >= it } ?: true

            tiposOk && zonaOk && precioOk && califOk && disponibilidadOk
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!loading) {
            MapaClienteScreen(
                parqueos = parqueosFiltrados.map { snapshotToParqueoModel(it) },
                navController = navController,
                sharedViewModel = sharedViewModel,
                cameraPositionState = cameraPositionState
            )
        }

        AnimatedVisibility(
            visible = !loading && parqueos.isNotEmpty()
        ) {
            val totalFiltros = filtros.tiposVehiculo.size +
                    (if (filtros.zonaSeleccionada != null) 1 else 0) +
                    (if (filtros.precioMaximo != null) 1 else 0) +
                    (if (filtros.calificacionMinima != null) 1 else 0) +
                    (if (filtros.soloDisponibles) 1 else 0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PerfilDropdown(
                            photoUrl = photoUrl,
                            navController = navController
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Hola, $userName",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(8.dp)
                                ) {}

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    text = locationText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.width(64.dp)
                ) {
                    NotificacionesButton(
                        cantidad = 3,
                        onClick = {
                            navController.navigate("cliente_notificaciones")
                        }
                    )

                    FilterFloatingButton(
                        totalFiltros = totalFiltros,
                        onClick = {
                            mostrarSelectorTipos = true
                        }
                    )
                }
            }
        }
        if (mostrarSelectorTipos) {
            ModalBottomSheet(
                onDismissRequest = { mostrarSelectorTipos = false },
                containerColor = MaterialTheme.colorScheme.surface,
                sheetState = bottomSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                FiltroParqueoModal(
                    filtros = filtros,
                    zonas = zonasDisponibles,
                    onAplicar = {
                        filtros = it
                        mostrarSelectorTipos = false
                    },
                    onCancelar = {
                        mostrarSelectorTipos = false
                    }
                )
            }
        }

        if (!loading && parqueosFiltrados.isEmpty() && error == null) {
            EmptyStateView(
                title = "No se encontraron parqueos",
                message = "Intenta ajustar los filtros o limpiarlos para ver más resultados.",
                onClearFilters = {
                    filtros = FiltrosCliente()
                }
            )
        }

        error?.let { mensaje ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Ocurrió un error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = mensaje,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                loading = true
                                error = null
                                loadParqueos(
                                    db = db,
                                    onSuccess = {
                                        parqueos = it
                                        loading = false
                                    },
                                    onError = {
                                        error = it
                                        loading = false
                                    }
                                )
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun FilterFloatingButton(
    totalFiltros: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.size(58.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onClick()
                }
        ) {
            BadgedBox(
                badge = {
                    if (totalFiltros > 0) {
                        Badge {
                            Text(
                                text = totalFiltros.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Filtros",
                    tint = if (totalFiltros > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}
@Composable
private fun NotificacionesButton(
    cantidad: Int,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.size(58.dp)
    ) {
        IconButton(
            onClick = onClick
        ) {
            BadgedBox(
                badge = {
                    if (cantidad > 0) {
                        Badge {
                            Text(
                                text = cantidad.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = "Notificaciones",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
private fun loadParqueos(
    db: FirebaseFirestore,
    onSuccess: (List<DocumentSnapshot>) -> Unit,
    onError: (String) -> Unit
) {
    db.collection("parqueos")
        .get()
        .addOnSuccessListener { result ->
            onSuccess(result.documents)
        }
        .addOnFailureListener {
            onError(it.message ?: "Error desconocido")
        }
}
suspend fun obtenerUbicacionTexto(context: Context): String {
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasPermission) return "Sin permiso"

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    return try {
        val location = fusedLocationClient.lastLocation.await()

        if (location == null) return "Sin ubicación"

        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(
            location.latitude,
            location.longitude,
            1
        )

        if (!addresses.isNullOrEmpty()) {
            val city = addresses[0].locality ?: ""
            val country = addresses[0].countryName ?: ""

            when {
                city.isNotBlank() && country.isNotBlank() -> "$city, $country"
                country.isNotBlank() -> country
                else -> "Ubicación desconocida"
            }
        } else {
            "Ubicación desconocida"
        }
    } catch (_: Exception) {
        "Error ubicación"
    }
}
