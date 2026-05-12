package com.example.parking.Client.Record

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.parking.Admin.EstadoChip
import com.example.parking.Admin.formatFecha
import com.example.parking.Client.ViewParking.CarReserv.Detall.ComprobanteSkeleton
import com.example.parking.ui.theme.AzulPetroleo
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialReservasScreen(navController: NavController) {
    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var historial by remember { mutableStateOf<List<ReservaConDetalle>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        val db = Firebase.firestore

        db.collection("reservas")
            .whereEqualTo("clienteId", uid)
            .whereIn("estado", listOf("finalizada", "cancelada", "expirada"))
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    cargando = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        val detalles = snapshot.documents.map { doc ->
                            async {
                                val parqueoId = doc.getString("parqueoId") ?: ""
                                val vehiculoId = doc.getString("vehiculoId") ?: ""

                                val parqueo = db.collection("parqueos")
                                    .document(parqueoId)
                                    .get()
                                    .await()

                                val vehiculo = db.collection("users")
                                    .document(uid)
                                    .collection("vehiculos")
                                    .document(vehiculoId)
                                    .get()
                                    .await()

                                ReservaConDetalle(
                                    reserva = doc,
                                    nombreParqueo = parqueo.getString("nombre") ?: "Parqueo desconocido",
                                    placa = vehiculo.getString("placa") ?: "Sin placa",
                                    modelo = vehiculo.getString("modelo") ?: "Vehículo",
                                    tipoVehiculo = vehiculo.getString("tipo") ?: "desconocido"
                                )
                            }
                        }.awaitAll()

                        withContext(Dispatchers.Main) {
                            historial = detalles.sortedByDescending {
                                it.reserva.getTimestamp("horaSalida")?.toDate()?.time
                                    ?: it.reserva.getTimestamp("fechaInicio")?.toDate()?.time
                                    ?: 0L
                            }
                            cargando = false
                        }
                    }
                }
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
        ) {
            HistorialHeader()

            when {
                cargando -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                historial.isEmpty() -> {
                    EmptyHistorialState()
                }

                else -> {
                    val historialPorDia = historial
                        .sortedByDescending {
                            it.reserva.getTimestamp("fechaInicio")?.toDate()
                        }
                        .groupBy { detalle ->
                            val fecha = detalle.reserva.getTimestamp("fechaInicio")?.toDate()
                            SimpleDateFormat("dd MMM yyyy", Locale("es"))
                                .format(fecha ?: Date())
                        }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        historialPorDia.forEach { (dia, lista) ->
                            item {
                                Text(
                                    text = dia.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                )
                            }

                            items(lista, key = { it.reserva.id }) { detalle ->
                                ReservaHistorialMinimalCard(
                                    detalle = detalle,
                                    navController = navController
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
private fun HistorialHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Historial de reservas",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Consulta tus reservas finalizadas, canceladas o expiradas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyHistorialState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
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
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Aún no tienes historial",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Aquí aparecerán tus reservas finalizadas, canceladas o expiradas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ReservaHistorialMinimalCard(
    detalle: ReservaConDetalle,
    navController: NavController
) {
    val r = detalle.reserva
    val fecha = r.getTimestamp("fechaInicio")?.toDate()
    val salida = r.getTimestamp("horaSalida")?.toDate()

    val estado = r.getString("estado") ?: "—"
    val costoFinal = r.getDouble("costoFinal")
    val costoEstimado = r.getDouble("costoEstimado") ?: 0.0
    val costo = costoFinal ?: costoEstimado

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                navController.navigate("detalle_reserva_simple/${r.id}")
            },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatHora(fecha),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                EstadoChipHistorial(estado)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = detalle.nombreParqueo,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${detalle.modelo} • Placa: ${detalle.placa}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Tipo: ${detalle.tipoVehiculo.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Costo: Bs ${"%.2f".format(costo)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
            )

            salida?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Salida: ${formatHora(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EstadoChipHistorial(estado: String) {
    val bgColor = when (estado.lowercase()) {
        "finalizada" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        "cancelada" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        "expirada" -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = when (estado.lowercase()) {
        "finalizada" -> MaterialTheme.colorScheme.primary
        "cancelada" -> MaterialTheme.colorScheme.error
        "expirada" -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Text(
            text = estado.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleReservaSimple(
    reservaId: String,
    navController: NavController
) {
    val db = Firebase.firestore
    val context = LocalContext.current

    var reserva by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var parqueo by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var vehiculo by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var espacio by remember { mutableStateOf<DocumentSnapshot?>(null) }

    LaunchedEffect(reservaId) {
        try {
            val res = db.collection("reservas")
                .document(reservaId)
                .get()
                .await()

            reserva = res

            val parqueoId = res.getString("parqueoId") ?: return@LaunchedEffect
            val clienteId = res.getString("clienteId") ?: return@LaunchedEffect
            val vehiculoId = res.getString("vehiculoId") ?: return@LaunchedEffect
            val espacioId = res.getString("espacioId") ?: return@LaunchedEffect

            parqueo = db.collection("parqueos")
                .document(parqueoId)
                .get()
                .await()

            vehiculo = db.collection("users")
                .document(clienteId)
                .collection("vehiculos")
                .document(vehiculoId)
                .get()
                .await()

            espacio = db.collection("parqueos")
                .document(parqueoId)
                .collection("espacios")
                .document(espacioId)
                .get()
                .await()

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Detalle de reserva")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (reserva == null || parqueo == null || vehiculo == null || espacio == null) {
            ComprobanteSkeleton()
            return@Scaffold
        }

        val nombreParqueo = parqueo!!.getString("nombre") ?: "Parqueo"
        val placa = vehiculo!!.getString("placa") ?: "Placa"
        val indiceEspacio = espacio!!.getLong("indice")?.toInt() ?: -1
        val tipoReserva = reserva!!.getString("tipoReserva") ?: "directa"
        val costo = reserva!!.getDouble("costoEstimado")
            ?: reserva!!.getDouble("costoFinal")
            ?: 0.0
        val estado = reserva!!.getString("estado") ?: "-"
        val direccion = parqueo!!.getString("direccion") ?: "-"

        val formato = remember {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        }

        val inicio = reserva!!.getTimestamp("fechaInicio")?.toDate()
            ?.let { formato.format(it) } ?: "-"

        val fin = reserva!!.getTimestamp("fechaFinal")?.toDate()
            ?.let { formato.format(it) } ?: "-"

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Comprobante de reserva",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    EstadoChipHistorial(estado)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    CampoDobleSimple(
                        t1 = "Parqueo",
                        v1 = nombreParqueo,
                        t2 = "Espacio",
                        v2 = if (indiceEspacio > 0) indiceEspacio.toString() else "—"
                    )

                    CampoDobleSimple(
                        t1 = "Dirección",
                        v1 = direccion,
                        t2 = "Placa",
                        v2 = placa
                    )

                    CampoDobleSimple(
                        t1 = "Tipo de reserva",
                        v1 = tipoReserva.replaceFirstChar { it.uppercase() },
                        t2 = "Monto",
                        v2 = "Bs ${"%.2f".format(costo)}"
                    )

                    CampoDobleSimple(
                        t1 = "Estado",
                        v1 = estado.replaceFirstChar { it.uppercase() },
                        t2 = "Código",
                        v2 = reservaId.takeLast(6).uppercase()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    CampoDobleSimple(
                        t1 = "Inicio",
                        v1 = inicio,
                        t2 = "Fin",
                        v2 = fin
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun CampoDobleSimple(
    t1: String,
    v1: String,
    t2: String,
    v2: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = t1,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = v1,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = t2,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = v2,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun formatHora(date: Date?): String {
    if (date == null) return "—"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

data class ReservaConDetalle(
    val reserva: DocumentSnapshot,
    val nombreParqueo: String,
    val placa: String,
    val modelo: String,
    val tipoVehiculo: String
)