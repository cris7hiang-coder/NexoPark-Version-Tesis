package com.example.parking.Client.Reserv
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parking.Client.ViewParking.CarReserv.Detall.EstadoBadge
import com.example.parking.Client.ViewParking.CarReserv.Detall.ReservaEstado
import com.example.parking.Client.ViewParking.CarReserv.point.CalificarParqueoBottomSheet
import com.example.parking.components.nav.AppRoutes
import com.example.parking.components.nav.detalleReservaRoute
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClienteReservasScreen(
    navController: NavController,
    viewModel: ClienteReservasViewModel = viewModel()
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    LaunchedEffect(uid) {
        viewModel.start(uid)
    }

    if (viewModel.showRatingSheet && viewModel.reserva != null) {
        CalificarParqueoBottomSheet(
            reservaId = viewModel.reserva?.id.orEmpty(),
            parqueoId = viewModel.reserva?.getString("parqueoId").orEmpty(),
            uid = uid,
            onDismiss = {
                viewModel.cerrarRating()
            }
        )
    }

    Scaffold(
        topBar = {
            MinimalTopBarCompact(
                onBack = {
                    navController.navigate(AppRoutes.ClienteHome) {
                        launchSingleTop = true
                        popUpTo(AppRoutes.ClienteHome) { inclusive = false }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        when {
            viewModel.cargando -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            viewModel.reserva == null && !viewModel.showRatingSheet -> {
                EmptyReservasState(
                    modifier = Modifier.padding(padding)
                )
            }

            else -> {
                val reserva = viewModel.reserva
                val estado = reserva?.getString("estado").orEmpty()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
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
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Tienes una reserva",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            EstadoBadge(estado)

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Puedes ver el detalle completo, tu QR y el estado actual de la reserva.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    reserva?.id?.let {
                                        navController.navigate(detalleReservaRoute(it))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ver detalle")
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun EmptyReservasState(
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
                        imageVector = Icons.Default.EventBusy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "No tienes reservas activas",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Cuando realices una reserva o tengas una pendiente, la verás aquí automáticamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
class ClienteReservasViewModel : ViewModel() {

    var cargando by mutableStateOf(true)
        private set

    var reserva by mutableStateOf<DocumentSnapshot?>(null)
        private set

    var showRatingSheet by mutableStateOf(false)
        private set

    var totalReservasActivas by mutableStateOf(0)
        private set

    private var listener: ListenerRegistration? = null

    // Evita reapertura inmediata del sheet en la misma sesión
    private var reservaIdRatingCerrada: String? = null

    fun start(uid: String) {
        listener?.remove()

        cargando = true
        reserva = null
        showRatingSheet = false
        totalReservasActivas = 0
        reservaIdRatingCerrada = null

        val db = FirebaseFirestore.getInstance()

        listener = db.collection("reservas")
            .whereEqualTo("clienteId", uid)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    cargando = false
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents.orEmpty()

                totalReservasActivas = docs.count {
                    val estado = it.getString("estado").orEmpty()
                    estado == ReservaEstado.ACTIVA || estado == ReservaEstado.PENDIENTE
                }

                val reservaRelevante = seleccionarReservaRelevante(docs)
                reserva = reservaRelevante
                cargando = false

                manejarEstado(reservaRelevante)
            }
    }

    private fun seleccionarReservaRelevante(
        docs: List<DocumentSnapshot>
    ): DocumentSnapshot? {
        if (docs.isEmpty()) return null

        val activasOPendientes = docs
            .filter {
                val estado = it.getString("estado").orEmpty()
                estado == ReservaEstado.ACTIVA || estado == ReservaEstado.PENDIENTE
            }
            .sortedByDescending {
                it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
            }

        if (activasOPendientes.isNotEmpty()) {
            return activasOPendientes.first()
        }

        val finalizadasSinResena = docs
            .filter {
                val estado = it.getString("estado").orEmpty()
                val resenaEnviada = it.getBoolean("reseñaEnviada") ?: false
                estado == ReservaEstado.FINALIZADA && !resenaEnviada
            }
            .sortedByDescending {
                val horaSalida = it.getTimestamp("horaSalida")?.toDate()?.time ?: 0L
                val createdAt = it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                maxOf(horaSalida, createdAt)
            }

        return finalizadasSinResena.firstOrNull()
    }

    private fun manejarEstado(res: DocumentSnapshot?) {
        if (res == null) {
            showRatingSheet = false
            return
        }

        val estado = res.getString("estado").orEmpty()
        val resenaEnviada = res.getBoolean("reseñaEnviada") ?: false
        val reservaId = res.id

        showRatingSheet = (
                estado == ReservaEstado.FINALIZADA &&
                        !resenaEnviada &&
                        reservaIdRatingCerrada != reservaId
                )
    }

    fun cerrarRating() {
        reservaIdRatingCerrada = reserva?.id
        showRatingSheet = false
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}