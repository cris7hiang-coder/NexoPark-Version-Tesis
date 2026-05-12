package com.example.parking.components.nav

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.parking.Admin.AdminHomeScreen
import com.example.parking.SupAdmin.SuperAdminHomeScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.parking.Admin.AdminReservasScreen
import com.example.parking.Admin.AdminViewModel
import com.example.parking.Admin.EspacioParqueoScreen
import com.example.parking.Admin.form.Edit.EditarParqueoScreen
import com.example.parking.Client.CarClient.RegistroVehiculoScreen
import com.example.parking.Client.ViewParking.CarPresent.DetalleParqueoScreen
import com.example.parking.Client.ViewParking.CarReserv.Detall.PantallaDetalleReservaScreen
import com.example.parking.Client.ViewParking.CarReserv.Detall.PantallaQRReservaScreen
import com.example.parking.Client.ViewParking.CarReserv.ReservaScreen
import com.example.parking.Client.ViewParking.CarReserv.Rutas
import com.example.parking.Client.ViewParking.CarReserv.SeleccionarEspacioScreen
import com.example.parking.Client.Reserv.ClienteReservasScreen
import com.example.parking.Client.Record.HistorialReservasScreen
import com.example.parking.Client.CarClient.DetalleMiVehiculoScreen
import com.example.parking.Perfil.Perfil
import com.example.parking.Client.CarClient.EditarVehiculoScreen
import com.example.parking.Client.CarClient.MiVehiculoScreen
import com.example.parking.Client.Security.AutenticacionVerificacionScreen
import com.example.parking.Client.Security.Country.VerificacionTelefonoScreen
import com.example.parking.Login.pass.CambiarContrasenaScreen
import com.example.parking.ui.screens.auth.RecuperarContrasenaScreen

import com.example.parking.ui.screens.perfil.PerfilScreenWrapper
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.parking.Admin.AdminEspaciosViewModel
import com.example.parking.Admin.CalculadoraCobroReserva
import com.example.parking.Admin.EscanearQRAdminScreen
import com.example.parking.Admin.ReporteReservasScreen
import com.example.parking.Client.ViewParking.CarPresent.TodasLasReseñasScreen
import com.example.parking.Client.ViewParking.CarReserv.Detall.PantallaMapaParqueoCompleto
import com.example.parking.Client.ViewParking.ParqueoSharedViewModel
import com.example.parking.Client.ClienteHomeScreen
import com.example.parking.Client.Favorit.FavoritosScreen
import com.example.parking.Client.NotificacionesScreen
import com.example.parking.Client.Record.PantallaDetalleReservaSimple
import com.example.parking.Client.ViewParking.CarReserv.Detall.ReservaEstado
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavigationScreen(
    role: String,
    onLogout: () -> Unit) {
    val navController = rememberAnimatedNavController()
    val sharedViewModel: ParqueoSharedViewModel = viewModel()
    val adminViewModel: AdminViewModel = viewModel()
    val saveableStateHolder = rememberSaveableStateHolder()
    var forceHideBottomBar by rememberSaveable { mutableStateOf(false) }

    val adminIsLoading by adminViewModel.isLoading.collectAsState()
    val adminParqueo by adminViewModel.parqueo.collectAsState()

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var reservasBadge by remember { mutableStateOf(0) }

    DisposableEffect(role, uid) {
        if (role != "cliente" || uid.isNullOrBlank()) {
            onDispose { }
        } else {
            val listener = FirebaseFirestore.getInstance()
                .collection("reservas")
                .whereEqualTo("clienteId", uid)
                .addSnapshotListener { snapshot, _ ->
                    val docs = snapshot?.documents.orEmpty()
                    reservasBadge = docs.count {
                        val estado = it.getString("estado").orEmpty()
                        estado == ReservaEstado.ACTIVA || estado == ReservaEstado.PENDIENTE
                    }
                }

            onDispose { listener.remove() }
        }
    }
    val navItems = remember(role, reservasBadge) {
        when (role) {
            "cliente" -> listOf(
                BottomNavItem(
                    label = "Inicio",
                    icon = Icons.Default.Home,
                    route = AppRoutes.ClienteHome
                ),
                BottomNavItem(
                    label = "Favorito",
                    icon = Icons.Default.Favorite,
                    route = AppRoutes.ClienteFavorito
                ),
                BottomNavItem(
                    label = "Reservas",
                    icon = Icons.Default.ReceiptLong,
                    route = AppRoutes.ClienteReservas,
                    badgeCount = reservasBadge
                ) ,
                BottomNavItem(
                    label = "Perfil",
                    icon = Icons.Default.Person,
                    route = AppRoutes.Perfil
                )
            )

            "admin" -> listOf(
                BottomNavItem(
                    label = "Panel",
                    icon = Icons.Default.Home,
                    route = AppRoutes.AdminHome
                ),
                BottomNavItem(
                    label = "Reservas",
                    icon = Icons.Default.DirectionsCar,
                    route = AppRoutes.AdminReservas
                ),
                BottomNavItem(
                    label = "Editar",
                    icon = Icons.Default.Edit,
                    route = AppRoutes.AdminEditarParqueo,
                    matchRoutes = setOf(
                        AppRoutes.AdminEditarParqueo,
                        AppRoutes.AdminEditarParqueoArg
                    )
                ),
                BottomNavItem(
                    label = "Control",
                    icon = Icons.Default.PlayArrow,
                    route = AppRoutes.AdminEspacio
                ),
                BottomNavItem(
                    label = "Perfil",
                    icon = Icons.Default.Person,
                    route = AppRoutes.Perfil
                )
            )

            "superadmin" -> listOf(
                BottomNavItem(
                    label = "Panel",
                    icon = Icons.Default.Menu,
                    route = AppRoutes.SuperAdminHome
                ),
                BottomNavItem(
                    label = "Usuarios",
                    icon = Icons.Default.AccountCircle,
                    route = AppRoutes.SuperAdminUsuarios
                ),
                BottomNavItem(
                    label = "Perfil",
                    icon = Icons.Default.Person,
                    route = AppRoutes.Perfil
                )
            )

            else -> emptyList()
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = remember(role) {
        when (role) {
            "cliente" -> setOf(
                AppRoutes.ClienteHome,
                AppRoutes.ClienteFavorito,
                AppRoutes.ClienteReservas,
            )

            "admin" -> setOf(
                AppRoutes.AdminHome,
                AppRoutes.AdminReservas,
                AppRoutes.AdminEditarParqueo,
                AppRoutes.AdminEditarParqueoArg,
                AppRoutes.AdminEspacio,

            )

            "superadmin" -> setOf(
                AppRoutes.SuperAdminHome,
                AppRoutes.SuperAdminUsuarios,
            )

            else -> emptySet()
        }
    }

    val baseShowBottomBar = when {
        currentRoute == null -> false
        currentRoute !in bottomBarRoutes -> false
        role == "admin" && currentRoute == AppRoutes.AdminHome ->
            !adminIsLoading && adminViewModel.isParqueoCompleto() && adminParqueo != null
        else -> true
    }

    val showBottomBar = baseShowBottomBar && !forceHideBottomBar

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedNavHost(
            navController = navController,
            startDestination = obtenerStartDestination(role),
            modifier = Modifier.fillMaxSize(),
            enterTransition = { slideInHorizontally(initialOffsetX = { it / 6 }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 6 }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 6 }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it / 6 }) + fadeOut() }
        ) {
            composable(AppRoutes.ClienteFavorito) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                FavoritosScreen(
                    navController = navController,
                    uid = uid
                )
            }

            composable(AppRoutes.ClienteNotificaciones) {
                NotificacionesScreen(navController)
            }

            composable(AppRoutes.ClienteReservas) {
                ClienteReservasScreen(navController)
            }

            composable(AppRoutes.ClienteHome) {
                saveableStateHolder.SaveableStateProvider(AppRoutes.ClienteHome) {
                    var visible by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) { visible = true }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(400, delayMillis = 70)) +
                                scaleIn(initialScale = 0.97f)
                    ) {
                        ClienteHomeScreen(
                            navController = navController,
                            sharedViewModel = sharedViewModel
                        )
                    }
                }
            }

            composable(AppRoutes.EscanearQrAdmin) {
                EscanearQRAdminScreen(navController)
            }

            composable(AppRoutes.ReporteReservas) {
                ReporteReservasScreen()
            }

            composable(AppRoutes.AdminHome) {
                AdminHomeScreen(
                    navController = navController,
                    onBottomBarVisibilityChanged = { visible ->
                        forceHideBottomBar = !visible
                    }
                )
            }

            composable(AppRoutes.AdminReservas) {
                val reservas by adminViewModel.reservas.collectAsState()
                AdminReservasScreen(reservas = reservas)
            }

            composable(AppRoutes.AdminEditarParqueo) {
                val parqueoId = FirebaseAuth.getInstance().currentUser?.uid
                if (parqueoId.isNullOrBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se pudo identificar el parqueo",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LaunchedEffect(parqueoId) {
                        navController.navigate(adminEditarParqueoRoute(parqueoId)) {
                            popUpTo(AppRoutes.AdminEditarParqueo) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }

            composable(
                route = AppRoutes.AdminEditarParqueoArg,
                arguments = listOf(navArgument("parqueoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val parqueoId = backStackEntry.arguments?.getString("parqueoId") ?: return@composable
                EditarParqueoScreen(
                    parqueoId = parqueoId,
                    onVolver = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.AdminEspacio) {
                val context = LocalContext.current
                val parqueo by adminViewModel.parqueo.collectAsState()
                val espacios by adminViewModel.espacios.collectAsState()

                LaunchedEffect(parqueo?.id) {
                    parqueo?.id?.let(adminViewModel::cargarEspacios)
                }

                if (parqueo != null) {
                    val parqueoId = parqueo!!.id
                    val tipos = parqueo?.get("tiposVehiculo") as? List<String> ?: emptyList()

                    val capacidadesRaw = parqueo?.get("capacidades") as? Map<String, Long>
                        ?: parqueo?.get("capacidad") as? Map<String, Long>
                        ?: emptyMap()

                    val capacidades = capacidadesRaw.mapValues { it.value.toInt() }

                    EspacioParqueoScreen(
                        parqueoId = parqueoId,
                        tiposVehiculo = tipos,
                        capacidades = capacidades,
                        espacios = espacios,
                        modoAdmin = true,
                        onEspacioClick = { _, _ ->
                            // No se usa en modoAdmin.
                            // El flujo real está en el bottom sheet y PagoDialog.
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            composable(AppRoutes.SuperAdminHome) {
                SuperAdminHomeScreen()
            }

            composable(AppRoutes.SuperAdminUsuarios) {
                Text("Gestión de Usuarios")
            }

            composable("perfil") {
                PerfilScreenWrapper(
                    navController = navController,
                    onLogout = onLogout
                )
            }

            composable(AppRoutes.RecuperarContrasena) {
                RecuperarContrasenaScreen(
                    onBackToLogin = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.CambiarContrasena) {
                CambiarContrasenaScreen(
                    navController = navController,
                    onPasswordChanged = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            composable(AppRoutes.AutenticacionVerificacion) {
                AutenticacionVerificacionScreen(navController)
            }

            composable(AppRoutes.VerificacionTelefono) {
                VerificacionTelefonoScreen(navController)
            }

            composable(AppRoutes.EditarPerfil) {
                Perfil(navController = navController)
            }

            composable(AppRoutes.RegistrarVehiculo) {
                RegistroVehiculoScreen(navController)
            }

            composable(AppRoutes.MiVehiculo) {
                MiVehiculoScreen(navController)
            }

            composable(AppRoutes.EditarVehiculo) {
                EditarVehiculoScreen(navController)
            }

            composable(
                route = AppRoutes.PantallaReserva,
                arguments = listOf(navArgument("parqueoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val parqueoId = backStackEntry.arguments?.getString("parqueoId") ?: return@composable
                ReservaScreen(
                    parqueoId = parqueoId,
                    navController = navController
                )
            }

            composable(
                route = AppRoutes.DetalleParqueo,
                arguments = listOf(navArgument("parqueoId") { type = NavType.StringType })
            ) { backStackEntry ->
                val parqueoId = backStackEntry.arguments?.getString("parqueoId") ?: return@composable
                DetalleParqueoScreen(
                    navController = navController,
                    parqueoId = parqueoId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppRoutes.SeleccionarEspacio,
                arguments = listOf(
                    navArgument("parqueoId") { type = NavType.StringType },
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("tipoVehiculo") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val parqueoId = backStackEntry.arguments?.getString("parqueoId") ?: ""
                val tipoVehiculo = backStackEntry.arguments?.getString("tipoVehiculo") ?: ""

                SeleccionarEspacioScreen(
                    parqueoId = parqueoId,
                    tipoVehiculo = tipoVehiculo,
                    navController = navController
                )
            }

            composable(
                route = AppRoutes.DetalleReserva,
                arguments = listOf(navArgument("reservaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val reservaId = backStackEntry.arguments?.getString("reservaId") ?: return@composable
                PantallaDetalleReservaScreen(
                    reservaId = reservaId,
                    navController = navController
                )
            }

            composable(
                route = AppRoutes.QrReserva,
                arguments = listOf(navArgument("reservaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val reservaId = backStackEntry.arguments?.getString("reservaId") ?: return@composable
                PantallaQRReservaScreen(
                    reservaId = reservaId,
                    navController = navController
                )
            }

            composable(AppRoutes.HistorialReservas) {
                HistorialReservasScreen(navController)
            }

            composable(
                route = AppRoutes.DetalleReservaSimple,
                arguments = listOf(navArgument("reservaId") { type = NavType.StringType })
            ) { backStackEntry ->
                val reservaId = backStackEntry.arguments?.getString("reservaId") ?: return@composable
                PantallaDetalleReservaSimple(
                    reservaId = reservaId,
                    navController = navController
                )
            }

            composable(AppRoutes.DetalleVehiculo) {
                DetalleMiVehiculoScreen(navController)
            }

            composable(
                route = AppRoutes.TodasResenas,
                arguments = listOf(navArgument("parqueoId") { type = NavType.StringType })
            ) { backStackEntry ->
                TodasLasReseñasScreen(
                    parqueoId = backStackEntry.arguments?.getString("parqueoId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppRoutes.MapaParqueoCompleto,
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType },
                    navArgument("lng") { type = NavType.StringType },
                    navArgument("nombre") { type = NavType.StringType },
                    navArgument("direccion") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
                val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0
                val nombre = backStackEntry.arguments?.getString("nombre") ?: ""
                val direccion = backStackEntry.arguments?.getString("direccion") ?: ""

                PantallaMapaParqueoCompleto(
                    latitud = lat,
                    longitud = lng,
                    nombre = nombre,
                    direccion = direccion,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        if (showBottomBar && navItems.isNotEmpty()) {
            BottomBar(
                navItems = navItems,
                navController = navController,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

object AppRoutes {
    const val SeleccionarEspacio =
        "seleccionar_espacio/{parqueoId}/{vehiculoId}/{tipoVehiculo}"

    fun seleccionarEspacioRoute(
        parqueoId: String,
        vehiculoId: String,
        tipoVehiculo: String
    ): String {
        return "seleccionar_espacio/$parqueoId/$vehiculoId/$tipoVehiculo"
    }
    const val ClienteHome = "cliente_home"
    const val ClienteFavorito = "cliente_favorito"
    const val ClienteReservas = "cliente_reservas"
    const val ClienteNotificaciones = "cliente_notificaciones"

    const val AdminHome = "admin_home"
    const val AdminReservas = "admin_reservas"
    const val AdminEspacio = "admin_espacio"
    const val AdminEditarParqueo = "admin_editar_parqueo"
    const val AdminEditarParqueoArg = "admin_editar_parqueo/{parqueoId}"

    const val SuperAdminHome = "superadmin_home"
    const val SuperAdminUsuarios = "superadmin_usuarios"

    const val Perfil = "perfil"
    const val RecuperarContrasena = "recuperar_contrasena"
    const val CambiarContrasena = "cambiar_contrasena"
    const val AutenticacionVerificacion = "autenticacion_verificacion"
    const val VerificacionTelefono = "verificacion_telefono"
    const val EditarPerfil = "editar_perfil"

    const val RegistrarVehiculo = "registrar_vehiculo"
    const val MiVehiculo = "mi_vehiculo"
    const val EditarVehiculo = "editar_vehiculo"
    const val DetalleVehiculo = "detalle_vehiculo"

    const val EscanearQrAdmin = "escanear_qr_admin"
    const val ReporteReservas = "reporte_reservas"
    const val HistorialReservas = "historial_reservas"

    const val PantallaReserva = "pantalla_reserva/{parqueoId}"
    const val DetalleParqueo = "detalle_parqueo/{parqueoId}"
    const val DetalleReserva = "detalle_reserva/{reservaId}"
    const val DetalleReservaSimple = "detalle_reserva_simple/{reservaId}"
    const val QrReserva = "qr_reserva/{reservaId}"
    const val TodasResenas = "todas_reseñas/{parqueoId}"
    const val MapaParqueoCompleto = "mapa_parqueo_completo/{lat}/{lng}/{nombre}/{direccion}"
}

fun adminEditarParqueoRoute(parqueoId: String) = "admin_editar_parqueo/$parqueoId"
fun pantallaReservaRoute(parqueoId: String) = "pantalla_reserva/$parqueoId"
fun detalleReservaRoute(reservaId: String) = "detalle_reserva/$reservaId"
fun qrReservaRoute(reservaId: String) = "qr_reserva/$reservaId"


fun obtenerStartDestination(role: String): String = when (role) {
    "cliente" -> AppRoutes.ClienteHome
    "admin" -> AppRoutes.AdminHome
    "superadmin" -> AppRoutes.SuperAdminHome
    else -> AppRoutes.Perfil
}
