package com.example.parking.Admin

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.parking.Client.ViewParking.CarReserv.Detall.SkeletonItem
import com.example.parking.ui.theme.AzulPetroleoFuerte
import com.example.parking.ui.theme.DivisorSuave
import com.example.parking.ui.theme.FondoCard
import com.example.parking.ui.theme.TextoSecundario
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReporteReservasScreen(
    viewModel: AdminViewModel = viewModel(),
    navController: NavController? = null
) {
    val context = LocalContext.current
    val reservas by viewModel.reservas.collectAsState()
    var pdfGenerado by remember { mutableStateOf<File?>(null) }
    var mostrarSheetPdf by remember { mutableStateOf(false) }
    // ---------- FILTROS ----------
    var fechaDesde by remember { mutableStateOf<Date?>(null) }
    var fechaHasta by remember { mutableStateOf<Date?>(null) }

    var tipoVehiculoSeleccionado by remember { mutableStateOf("todos") }
    val tiposVehiculo = listOf("todos", "auto", "moto", "camion")

    var tipoPagoSeleccionado by remember { mutableStateOf("todos") }
    val tiposPago = listOf("todos", "fisico", "digital")

    // ---------- MAPAS ----------
    var tipoVehiculoPorReserva by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var tipoPagoPorReserva by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var cargandoDatosExtra by remember { mutableStateOf(false) }
    // ---------- CARGA DE DATOS ----------
    LaunchedEffect(reservas) {
        if (reservas.isEmpty()) {
            tipoVehiculoPorReserva = emptyMap()
            tipoPagoPorReserva = emptyMap()
            cargandoDatosExtra = false
            return@LaunchedEffect
        }

        cargandoDatosExtra = true

        val db = FirebaseFirestore.getInstance()
        val mapVehiculo = mutableMapOf<String, String>()
        val mapPago = mutableMapOf<String, String>()

        try {
            reservas.forEach { reserva ->
                val clienteId = reserva.getString("clienteId")
                val vehiculoId = reserva.getString("vehiculoId")
                val parqueoId = reserva.getString("parqueoId")

                // 1. Primero usar tipoVehiculo directo de la reserva
                val tipoDesdeReserva = reserva.getString("tipoVehiculo")
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    ?.normalizarTipoVehiculoReporte()

                if (!tipoDesdeReserva.isNullOrBlank()) {
                    mapVehiculo[reserva.id] = tipoDesdeReserva
                } else if (!clienteId.isNullOrBlank() && !vehiculoId.isNullOrBlank()) {
                    // 2. Fallback: buscar vehículo del cliente
                    try {
                        val vehiculoSnap = db.collection("users")
                            .document(clienteId)
                            .collection("vehiculos")
                            .document(vehiculoId)
                            .get()
                            .await()

                        mapVehiculo[reserva.id] = vehiculoSnap.getString("tipo")
                            ?.trim()
                            ?.lowercase(Locale.ROOT)
                            ?.normalizarTipoVehiculoReporte()
                            ?: "otros"

                    } catch (e: Exception) {
                        mapVehiculo[reserva.id] = "otros"
                    }
                } else {
                    // 3. Si no hay nada, recién ahí usar otros
                    mapVehiculo[reserva.id] = "otros"
                }

                // Pago
                if (!parqueoId.isNullOrBlank()) {
                    try {
                        val pagoSnap = db.collection("parqueos")
                            .document(parqueoId)
                            .collection("pagos")
                            .whereEqualTo("reservaId", reserva.id)
                            .limit(1)
                            .get()
                            .await()

                        mapPago[reserva.id] = pagoSnap.documents.firstOrNull()
                            ?.getString("tipoPago")
                            ?.trim()
                            ?.lowercase(Locale.ROOT)
                            ?: "sin_registrar"

                    } catch (e: Exception) {
                        mapPago[reserva.id] = "sin_registrar"
                    }
                } else {
                    mapPago[reserva.id] = "sin_registrar"
                }
            }

            tipoVehiculoPorReserva = mapVehiculo
            tipoPagoPorReserva = mapPago

        } finally {
            cargandoDatosExtra = false
        }
    }
    val cargando = cargandoDatosExtra
    // ---------- FILTRO FINAL ----------
    val reservasFiltradas = remember(
        reservas,
        fechaDesde,
        fechaHasta,
        tipoVehiculoSeleccionado,
        tipoPagoSeleccionado,
        tipoVehiculoPorReserva,
        tipoPagoPorReserva
    ) {
        reservas.filter { doc ->
            val fecha = doc.getTimestamp("fechaInicio")?.toDate()
            val tipoVehiculo = tipoVehiculoPorReserva[doc.id]
            val tipoPago = tipoPagoPorReserva[doc.id]

            val pasaFecha = when {
                fechaDesde != null && fechaHasta != null ->
                    fecha != null && !fecha.before(fechaDesde) && !fecha.after(fechaHasta)
                fechaDesde != null ->
                    fecha != null && !fecha.before(fechaDesde)
                fechaHasta != null ->
                    fecha != null && !fecha.after(fechaHasta)
                else -> true
            }

            val pasaVehiculo =
                tipoVehiculoSeleccionado == "todos" ||
                        tipoVehiculo?.normalizarTipoVehiculoReporte() == tipoVehiculoSeleccionado
            val pasaPago =
                tipoPagoSeleccionado == "todos" || tipoPago == tipoPagoSeleccionado

            pasaFecha && pasaVehiculo && pasaPago
        }
    }

    // ---------- MÉTRICAS ----------
    val total = reservasFiltradas.size
    val finalizadas = reservasFiltradas.count { it.getString("estado") == "finalizada" }
    val canceladas = reservasFiltradas.count { it.getString("estado") == "cancelada" }
    val expiradas = reservasFiltradas.count { it.getString("estado") == "expirada" }

    val ingresosTotales = reservasFiltradas
        .filter { it.getString("estado") == "finalizada" }
        .sumOf {
            it.getDouble("costoFinal")
                ?: it.getDouble("costoEstimado")
                ?: 0.0
        }

    // ---------------------------------------------------------------------
    // ------------------------------  UI  ---------------------------------
    // ---------------------------------------------------------------------
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reporte de Reservas",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController?.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            if (cargando) {
                repeat(3) {
                    SkeletonItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        shapeRadius = 16
                    )
                }
                return@Column
            }

            // ---------- FECHA ----------
            Text(
                "Rango de fechas",
                style = MaterialTheme.typography.labelLarge,
                color = TextoSecundario
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectorFecha(
                    label = "Desde",
                    fecha = fechaDesde,
                    modifier = Modifier.weight(1f)
                ) { fechaDesde = it }

                SelectorFecha(
                    label = "Hasta",
                    fecha = fechaHasta,
                    modifier = Modifier.weight(1f)
                ) { fechaHasta = it }
            }

            // ---------- VEHÍCULO ----------
            Text(
                "Tipo de vehículo",
                style = MaterialTheme.typography.labelLarge,
                color = TextoSecundario
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tiposVehiculo.forEach { tipo ->
                    FilterChip(
                        selected = tipoVehiculoSeleccionado == tipo,
                        onClick = { tipoVehiculoSeleccionado = tipo },
                        label = {
                            Text(
                                if (tipo == "todos") "Todos"
                                else tipo.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // ---------- PAGO ----------
            Text(
                "Tipo de pago",
                style = MaterialTheme.typography.labelLarge,
                color = TextoSecundario
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tiposPago.forEach { tipo ->
                    FilterChip(
                        selected = tipoPagoSeleccionado == tipo,
                        onClick = { tipoPagoSeleccionado = tipo },
                        label = {
                            Text(
                                when (tipo) {
                                    "fisico" -> "Físico"
                                    "digital" -> "Digital"
                                    else -> "Todos"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // ---------- RESUMEN ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = FondoCard
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReporteItem("Total", total)
                    ReporteItem("Finalizadas", finalizadas)
                    ReporteItem("Canceladas", canceladas)
                    ReporteItem("Expiradas", expiradas)

                    Divider(color = DivisorSuave)

                    Text(
                        "Ingresos Totales: Bs ${"%.2f".format(ingresosTotales)}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = AzulPetroleoFuerte
                        )
                    )
                }
            }

            // ---------- EXPORTAR ----------
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                onClick = {
                    val file = generarReporteReservasPdfFile(
                        context = context,
                        reservas = reservasFiltradas,
                        tipoVehiculoPorReserva = tipoVehiculoPorReserva,
                        tipoPagoPorReserva = tipoPagoPorReserva
                    )

                    if (file != null) {
                        pdfGenerado = file
                        mostrarSheetPdf = true
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzulPetroleoFuerte,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)

                Spacer(Modifier.width(10.dp))

                Text(
                    "Generar PDF",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }
    }
    if (mostrarSheetPdf && pdfGenerado != null) {
        ModalBottomSheet(
            onDismissRequest = { mostrarSheetPdf = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ReporteReservasPdfSheet(
                file = pdfGenerado!!,
                onCompartir = {
                    compartirReportePdf(
                        context = context,
                        file = pdfGenerado!!
                    )
                    mostrarSheetPdf = false
                },
                onCerrar = {
                    mostrarSheetPdf = false
                }
            )
        }
    }
}

@Composable
fun ReporteItem(label: String, value: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
        Text(text = value.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SelectorFecha(
    label: String,
    fecha: Date?,
    modifier: Modifier = Modifier,
    onFechaSeleccionada: (Date) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, anio, mes, dia ->
            calendar.set(anio, mes, dia, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            onFechaSeleccionada(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    OutlinedTextField(
        value = fecha?.let { SimpleDateFormat("dd/MM/yyyy", Locale("es")).format(it) } ?: "",
        onValueChange = {},
        label = { Text(label) },
        modifier = modifier,
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { datePickerDialog.show() }) {
                Icon(Icons.Default.CalendarToday, contentDescription = null)
            }
        }
    )
}
private fun String.normalizarTipoVehiculoReporte(): String {
    return when (this.trim().lowercase(Locale.ROOT)) {
        "auto", "carro", "coche", "vehiculo", "vehículo" -> "auto"
        "moto", "motocicleta" -> "moto"
        "camion", "camión", "camioneta", "truck" -> "camion"
        else -> this.trim().lowercase(Locale.ROOT)
    }
}