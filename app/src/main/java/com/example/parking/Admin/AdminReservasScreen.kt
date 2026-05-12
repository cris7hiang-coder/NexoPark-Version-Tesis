package com.example.parking.Admin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.DocumentSnapshot
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parking.Admin.form.RegistroParqueoWizard
import com.example.parking.Client.Record.formatHora
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReservasScreen(
    reservas: List<DocumentSnapshot>,
    modifier: Modifier = Modifier
) {
    var reservaSeleccionada by remember { mutableStateOf<DocumentSnapshot?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Agrupar reservas por fecha de inicio
    val reservasPorDia = reservas
        .sortedByDescending { it.getTimestamp("fechaInicio")?.toDate() }
        .groupBy { doc ->
            val fecha = doc.getTimestamp("fechaInicio")?.toDate()
            SimpleDateFormat("dd MMM yyyy", Locale("es")).format(fecha ?: Date())
        }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "📅 Historial de reservas",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            reservasPorDia.forEach { (dia, lista) ->

                // ---- ENCABEZADO DEL DÍA ----
                item {
                    Text(
                        dia,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF444444),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // ---- ITEMS DEL DÍA ----
                items(lista, key = { it.id }) { doc ->
                    ReservaMinimalCard(doc) {
                        reservaSeleccionada = doc
                        scope.launch { bottomSheetState.show() }
                    }
                }
            }
        }
    }

    // BOTTOM SHEET ↓
    if (reservaSeleccionada != null) {
        ModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = { reservaSeleccionada = null }
        ) {
            DetalleReservaAdmin(reservaSeleccionada!!)
        }
    }
}
@Composable
fun ReservaMinimalCard(doc: DocumentSnapshot, onClick: () -> Unit) {
    val estado = doc.getString("estado") ?: "—"
    val fecha = doc.getTimestamp("fechaInicio")?.toDate()
    val costo = doc.getDouble("costoFinal") ?: doc.getDouble("costoEstimado") ?: 0.0
    val clienteId = doc.getString("clienteId") ?: "—"
    val vehiculoId = doc.getString("vehiculoId") ?: ""

    var vehiculo by remember { mutableStateOf<DocumentSnapshot?>(null) }

    LaunchedEffect(vehiculoId) {
        if (vehiculoId.isNotEmpty()) {
            vehiculo = Firebase.firestore.collection("users")
                .document(clienteId)
                .collection("vehiculos")
                .document(vehiculoId)
                .get()
                .await()
        }
    }

    val placa = vehiculo?.getString("placa") ?: "—"
    val tipo = vehiculo?.getString("tipo") ?: "—"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(formatHora(fecha), fontWeight = FontWeight.SemiBold, color = Color.Black)
                EstadoChip(estado)
            }

            Spacer(Modifier.height(6.dp))

            Text("Cliente: $clienteId", color = Color.DarkGray, fontSize = 13.sp)
            Text("Placa: $placa", color = Color.DarkGray, fontSize = 13.sp)
            Text("Tipo: ${tipo.replaceFirstChar { it.uppercase() }}", color = Color.DarkGray, fontSize = 13.sp)
            Text("Costo: Bs %.2f".format(costo), fontWeight = FontWeight.Medium, fontSize = 14.sp)
        }
    }
}

@Composable
fun EstadoChip(estado: String) {
    val (bg, fg) = when (estado.lowercase()) {
        "finalizada" -> Color(0xFFDFF7F0) to Color(0xFF0B7A4B)
        "cancelada"  -> Color(0xFFFFE6E6) to Color(0xFFC62828)
        "expirada"   -> Color(0xFFFFF4D6) to Color(0xFFB37E00)
        else         -> Color(0xFFEAEAEA) to Color.DarkGray
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            estado.replaceFirstChar { it.uppercase() },
            color = fg,
            fontSize = 12.sp
        )
    }
}

fun formatFecha(date: Date?): String {
    if (date == null) return "—"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))
    return sdf.format(date)
}

@Composable
fun DetalleReservaAdmin(doc: DocumentSnapshot) {
    val clienteId = doc.getString("clienteId") ?: "—"
    val vehiculoId = doc.getString("vehiculoId") ?: "—"
    val parqueoId = doc.getString("parqueoId") ?: return

    val db = Firebase.firestore
    var cliente by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var vehiculo by remember { mutableStateOf<DocumentSnapshot?>(null) }

    var pago by remember { mutableStateOf<DocumentSnapshot?>(null) }
    val tipoPago = pago?.getString("tipoPago")
    val monto = pago?.getDouble("monto")

    LaunchedEffect(doc.id) {
        // cliente y vehículo (ya lo tienes)
        cliente = db.collection("users").document(clienteId).get().await()
        vehiculo = db.collection("users").document(clienteId)
            .collection("vehiculos").document(vehiculoId).get().await()

        // 🔥 cargar pago desde /parqueos/{parqueoId}/pagos
        val pagosSnap = db.collection("parqueos")
            .document(parqueoId)
            .collection("pagos")
            .whereEqualTo("reservaId", doc.id)
            .limit(1)
            .get()
            .await()

        if (!pagosSnap.isEmpty) pago = pagosSnap.documents[0]
    }


    Column(Modifier.padding(24.dp)) {
        Text("Detalle de Reserva", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(16.dp))
        Text("Comprobante de pago", fontWeight = FontWeight.SemiBold)
        // CLIENTE
        Text("Cliente", fontWeight = FontWeight.Bold)
        Text("Nombre: ${cliente?.getString("name") ?: "—"}")
        Text("Email: ${cliente?.getString("email") ?: "—"}")
        Spacer(Modifier.height(12.dp))

        // VEHÍCULO
        Text("Vehículo", fontWeight = FontWeight.Bold)
        Text("Placa: ${vehiculo?.getString("placa") ?: "—"}")
        Text("Modelo: ${vehiculo?.getString("modelo") ?: "—"}")
        Text("Color: ${vehiculo?.getString("color") ?: "—"}")
        Text("Tipo: ${vehiculo?.getString("tipo")?.replaceFirstChar { it.uppercase() } ?: "—"}")

        Spacer(Modifier.height(12.dp))

        // RESERVA
        val inicio = doc.getTimestamp("fechaInicio")?.toDate()
        val salida = doc.getTimestamp("horaSalida")?.toDate()
        val costoFinal = doc.getDouble("costoFinal")
        val costoEstimado = doc.getDouble("costoEstimado") ?: 0.0

        Text("🕒 Reserva", fontWeight = FontWeight.Bold)
        Text("Inicio: ${inicio?.let { formatFecha(it) } ?: "—"}")
        Text("Salida: ${salida?.let { formatFecha(it) } ?: "—"}")
        Text("Costo: Bs %.2f".format(costoFinal ?: costoEstimado))
        Text("Minutos usados: ${doc.getLong("minutosUsados") ?: "—"}")

        Spacer(Modifier.height(20.dp))
        Text("Pago", fontWeight = FontWeight.Bold)
// Estado para mostrar el Dialog
        var mostrarImagenCompleta by remember { mutableStateOf(false) }

        if (pago == null) {
            Text("No se registró pago aún.", color = Color.Gray)
        } else {
            Text("Tipo: ${tipoPago ?: "—"}")
            Text("Monto: Bs ${String.format("%.2f", monto ?: 0.0)}")

            Spacer(Modifier.height(10.dp))

            val comprobanteUrl = pago?.getString("comprobanteUrl")

            if (comprobanteUrl.isNullOrEmpty()) {
                Text("Sin comprobante subido.", color = Color.Gray)
            } else {
                // Card tocable
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clickable { mostrarImagenCompleta = true }, // <-- aquí
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    AsyncImage(
                        model = comprobanteUrl,
                        contentDescription = "Comprobante de pago",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Dialog para mostrar imagen completa
                if (mostrarImagenCompleta) {
                    Dialog(onDismissRequest = { mostrarImagenCompleta = false }) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = comprobanteUrl,
                                contentDescription = "Comprobante de pago",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable { mostrarImagenCompleta = false } // cerrar al tocar
                            )
                        }
                    }
                }
            }
        }
    }
}