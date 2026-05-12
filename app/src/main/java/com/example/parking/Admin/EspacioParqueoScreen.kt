package com.example.parking.Admin

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EspacioParqueoScreen(
    parqueoId: String,
    tiposVehiculo: List<String>,
    capacidades: Map<String, Int>,
    espacios: List<EspacioParqueo>,
    modoAdmin: Boolean = false,
    onEspacioClick: (tipo: String, indice: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val viewModel: AdminEspaciosViewModel = viewModel()

    var filtroTipo by remember { mutableStateOf<String?>(null) }
    var filtroEstado by remember { mutableStateOf<EstadoEspacio?>(null) }

    var mostrarDialogoPago by remember { mutableStateOf(false) }
    var reservaFinalizar by remember { mutableStateOf<DetalleOcupacion?>(null) }

    var espacioSeleccionado by remember { mutableStateOf<EspacioParqueo?>(null) }
    var detalleOcupacion by remember { mutableStateOf<DetalleOcupacion?>(null) }
    var cargandoDetalle by remember { mutableStateOf(false) }
    var errorDetalle by remember { mutableStateOf<String?>(null) }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val espaciosFiltrados = remember(espacios, filtroTipo, filtroEstado) {
        espacios
            .filter {
                (filtroTipo == null || it.tipoVehiculo == filtroTipo) &&
                        (filtroEstado == null || it.estado == filtroEstado)
            }
            .groupBy { it.tipoVehiculo }
    }

    suspend fun cerrarSheet() {
        espacioSeleccionado = null
        detalleOcupacion = null
        errorDetalle = null
        bottomSheetState.hide()
    }

    LaunchedEffect(espacioSeleccionado) {
        val e = espacioSeleccionado ?: return@LaunchedEffect
        if (!modoAdmin) return@LaunchedEffect

        if (e.estado == EstadoEspacio.DISPONIBLE) {
            detalleOcupacion = null
            return@LaunchedEffect
        }

        cargandoDetalle = true
        errorDetalle = null

        try {
            detalleOcupacion = viewModel.cargarDetalleOcupacion(
                parqueoId = parqueoId,
                tipoVehiculo = e.tipoVehiculo,
                indice = e.indice
            )
        } catch (ex: Exception) {
            errorDetalle = ex.message
        } finally {
            cargandoDetalle = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        EspaciosHeader()

        Spacer(modifier = Modifier.height(16.dp))

        FiltrosEspaciosSection(
            tiposVehiculo = tiposVehiculo,
            filtroTipo = filtroTipo,
            onFiltroTipoChange = { filtroTipo = it },
            filtroEstado = filtroEstado,
            onFiltroEstadoChange = { filtroEstado = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        tiposVehiculo.forEach { tipo ->
            GridEspaciosSection(
                tipo = tipo,
                total = capacidades[tipo] ?: 0,
                lista = espaciosFiltrados[tipo].orEmpty(),
                modoAdmin = modoAdmin,
                onEspacioSelected = { espacio ->
                    espacioSeleccionado = espacio
                    scope.launch {
                        delay(80)
                        bottomSheetState.show()
                    }
                }
            )
        }
    }

    if (mostrarDialogoPago && reservaFinalizar != null && espacioSeleccionado != null) {
        PagoDialog(
            context = context,
            parqueoId = parqueoId,
            espacio = espacioSeleccionado!!,
            detalle = reservaFinalizar!!,
            viewModel = viewModel,
            onDismiss = { mostrarDialogoPago = false },
            onDone = {
                mostrarDialogoPago = false
                reservaFinalizar = null
                detalleOcupacion = null
                espacioSeleccionado = null
            }
        )
    }

    if (espacioSeleccionado != null) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { cerrarSheet() }
            },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            EspacioDetalleBottomSheet(
                parqueoId = parqueoId,
                espacio = espacioSeleccionado!!,
                modoAdmin = modoAdmin,
                detalleOcupacion = detalleOcupacion,
                cargandoDetalle = cargandoDetalle,
                errorDetalle = errorDetalle,
                viewModel = viewModel,
                context = context,
                onEspacioClick = onEspacioClick,
                onCerrar = {
                    scope.launch { cerrarSheet() }
                },
                onFinalizar = { detalle ->
                    reservaFinalizar = detalle
                    mostrarDialogoPago = true
                }
            )
        }
    }
}

@Composable
private fun EspaciosHeader() {
    Text(
        text = "Espacios por tipo de vehículo",
        style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun FiltrosEspaciosSection(
    tiposVehiculo: List<String>,
    filtroTipo: String?,
    onFiltroTipoChange: (String?) -> Unit,
    filtroEstado: EstadoEspacio?,
    onFiltroEstadoChange: (EstadoEspacio?) -> Unit
) {
    Text(
        text = "Filtro por tipo",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChipActivo(
                label = "Todos",
                activo = filtroTipo == null,
                onClick = { onFiltroTipoChange(null) }
            )
        }

        tiposVehiculo.forEach { tipo ->
            item {
                FilterChipActivo(
                    label = tipo.replaceFirstChar { it.uppercase() },
                    activo = filtroTipo == tipo,
                    onClick = { onFiltroTipoChange(tipo) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Filtro por estado",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            EstadoChipActivo(
                label = "Todos",
                activo = filtroEstado == null,
                onClick = { onFiltroEstadoChange(null) }
            )
        }

        EstadoEspacio.values().forEach { estado ->
            item {
                EstadoChipActivo(
                    label = estado.name.lowercase().replaceFirstChar { it.uppercase() },
                    activo = filtroEstado == estado,
                    onClick = { onFiltroEstadoChange(estado) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GridEspaciosSection(
    tipo: String,
    total: Int,
    lista: List<EspacioParqueo>,
    modoAdmin: Boolean,
    onEspacioSelected: (EspacioParqueo) -> Unit
) {
    val espaciosMap = remember(lista) {
        lista.associateBy { it.indice }
    }

    Text(
        text = "Tipo: ${tipo.uppercase()} (${lista.size} / $total)",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (numero in 1..total) {
            val espacio = espaciosMap[numero]

            if (espacio != null) {
                EspacioSlotBoton(
                    espacio = espacio,
                    onClick = { onEspacioSelected(espacio) },
                    forzarClickable = modoAdmin
                )
            } else {
                EmptyEspacioSlot(
                    tipo = tipo,
                    numero = numero,
                    onClick = {
                        onEspacioSelected(
                            EspacioParqueo(
                                tipoVehiculo = tipo,
                                indice = numero,
                                estado = EstadoEspacio.DISPONIBLE
                            )
                        )
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun EmptyEspacioSlot(
    tipo: String,
    numero: Int,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconoVehiculo(tipo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "%02d".format(numero),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EspacioDetalleBottomSheet(
    parqueoId: String,
    espacio: EspacioParqueo,
    modoAdmin: Boolean,
    detalleOcupacion: DetalleOcupacion?,
    cargandoDetalle: Boolean,
    errorDetalle: String?,
    viewModel: AdminEspaciosViewModel,
    context: Context,
    onEspacioClick: (tipo: String, indice: Int) -> Unit,
    onCerrar: () -> Unit,
    onFinalizar: (DetalleOcupacion) -> Unit
) {
    val esDisponible = espacio.estado == EstadoEspacio.DISPONIBLE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        when {
            !modoAdmin || esDisponible -> {
                if (modoAdmin && esDisponible) {
                    AdminDisponibleSheetContent(
                        parqueoId = parqueoId,
                        espacio = espacio,
                        viewModel = viewModel,
                        context = context,
                        onCerrar = onCerrar
                    )
                } else {
                    ClienteReservaSheetContent(
                        espacio = espacio,
                        onConfirmar = {
                            onEspacioClick(espacio.tipoVehiculo, espacio.indice)
                            onCerrar()
                        },
                        onCerrar = onCerrar
                    )
                }
            }

            else -> {
                AdminOcupadoSheetContent(
                    parqueoId = parqueoId,
                    espacio = espacio,
                    detalleOcupacion = detalleOcupacion,
                    cargandoDetalle = cargandoDetalle,
                    errorDetalle = errorDetalle,
                    viewModel = viewModel,
                    context = context,
                    onCerrar = onCerrar,
                    onFinalizar = onFinalizar
                )
            }
        }
    }
}

@Composable
private fun AdminDisponibleSheetContent(
    parqueoId: String,
    espacio: EspacioParqueo,
    viewModel: AdminEspaciosViewModel,
    context: Context,
    onCerrar: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var placa by remember { mutableStateOf("") }
    var comentario by remember { mutableStateOf("") }

    Text(
        text = "Asignar / ocupación rápida",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = placa,
        onValueChange = { placa = it.uppercase() },
        label = { Text("Placa (opcional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = comentario,
        onValueChange = { comentario = it },
        label = { Text("Comentario (opcional)") },
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = "El cobro se calculará automáticamente al finalizar, según el tiempo real de ocupación y las tarifas configuradas por bloques de 30 minutos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(14.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                scope.launch {
                    try {
                        viewModel.crearReservaManual(
                            parqueoId = parqueoId,
                            tipoVehiculo = espacio.tipoVehiculo,
                            indice = espacio.indice,
                            placa = placa.ifBlank { null },
                            comentario = comentario.ifBlank { null }
                        )
                        Toast.makeText(context, "Espacio ocupado", Toast.LENGTH_SHORT).show()
                        onCerrar()
                    } catch (ex: Exception) {
                        Toast.makeText(
                            context,
                            "Error: ${ex.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            Text("Confirmar y ocupar")
        }

        TextButton(
            onClick = onCerrar,
            modifier = Modifier.weight(1f)
        ) {
            Text("Cancelar")
        }
    }
}

@Composable
private fun ClienteReservaSheetContent(
    espacio: EspacioParqueo,
    onConfirmar: () -> Unit,
    onCerrar: () -> Unit
) {
    Text(
        text = "Crear reserva",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text("Tipo de vehículo: ${espacio.tipoVehiculo.replaceFirstChar { it.uppercase() }}")
    Text("Espacio #: ${espacio.indice}")

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = onConfirmar,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Confirmar reserva")
    }

    Spacer(modifier = Modifier.height(8.dp))

    TextButton(
        onClick = onCerrar,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Cancelar")
    }
}

@Composable
private fun AdminOcupadoSheetContent(
    parqueoId: String,
    espacio: EspacioParqueo,
    detalleOcupacion: DetalleOcupacion?,
    cargandoDetalle: Boolean,
    errorDetalle: String?,
    viewModel: AdminEspaciosViewModel,
    context: Context,
    onCerrar: () -> Unit,
    onFinalizar: (DetalleOcupacion) -> Unit
) {
    val scope = rememberCoroutineScope()

    Text(
        text = "Detalle del ocupante",
        style = MaterialTheme.typography.titleLarge
    )

    Spacer(modifier = Modifier.height(12.dp))

    Text("Espacio: ${espacio.tipoVehiculo} #${espacio.indice}")

    Spacer(modifier = Modifier.height(12.dp))

    when {
        cargandoDetalle -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cargando ocupante…")
            }
        }

        errorDetalle != null -> {
            Text(
                text = "Error: $errorDetalle",
                color = MaterialTheme.colorScheme.error
            )
        }

        detalleOcupacion == null -> {
            Text("No se encontró una reserva activa o pendiente para este espacio")

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            viewModel.liberarEspacioDirecto(
                                parqueoId = parqueoId,
                                tipoVehiculo = espacio.tipoVehiculo,
                                indice = espacio.indice
                            )
                            Toast.makeText(context, "Espacio liberado", Toast.LENGTH_SHORT).show()
                            onCerrar()
                        } catch (ex: Exception) {
                            Toast.makeText(
                                context,
                                "Error al liberar: ${ex.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Liberar espacio")
            }
        }

        else -> {
            val det = detalleOcupacion
            val cliente = det.cliente
            val vehiculo = det.vehiculo
            val reserva = det.reserva
            val esManual = reserva.getString("origen") == "ADMIN"
            val formato = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }

            if (esManual) {
                val inicioReserva = reserva.getTimestamp("horaLlegada")?.toDate()
                    ?: reserva.getTimestamp("fechaInicio")?.toDate()
                inicioReserva?.let { ContadorTiempo(it) }

                Text("Ocupación manual", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Placa: ${reserva.getString("placa") ?: "—"}")

                reserva.getString("comentario")?.takeIf { it.isNotBlank() }?.let {
                    Text("Comentario: $it")
                }

                Text("Inicio: ${inicioReserva?.let { formato.format(it) } ?: "—"}")
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text("Cliente", fontWeight = FontWeight.SemiBold)
            Text("Nombre: ${cliente?.getString("name") ?: "—"}")
            Text("Email: ${cliente?.getString("email") ?: "—"}")
            val tel = cliente?.getString("phone") ?: "—"
            Text("Teléfono: $tel")

            Spacer(modifier = Modifier.height(4.dp))

            val telefonoVerificado = cliente?.getBoolean("telefonoVerificado") == true
            Text(
                text = if (telefonoVerificado) "Teléfono verificado" else "Teléfono no verificado",
                color = if (telefonoVerificado) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Vehículo", fontWeight = FontWeight.SemiBold)
            Text("Placa: ${vehiculo?.getString("placa") ?: "—"}")
            Text("Tipo: ${vehiculo?.getString("tipo") ?: "—"}")
            Text("Color: ${vehiculo?.getString("color") ?: "—"}")
            Text("Uso: ${vehiculo?.getString("uso") ?: "—"}")

            Spacer(modifier = Modifier.height(12.dp))

            Text("Reserva", fontWeight = FontWeight.SemiBold)
            val inicio = reserva.getTimestamp("horaLlegada")?.toDate()
                ?: reserva.getTimestamp("fechaInicio")?.toDate()
            val fin = reserva.getTimestamp("fechaFinal")?.toDate()
            Text("Estado: ${reserva.getString("estado")}")
            Text("Inicio: ${inicio?.let { formato.format(it) } ?: "—"}")
            Text("Fin: ${fin?.let { formato.format(it) } ?: "—"}")
            val costoEstimado = reserva.getDouble("costoEstimado")
            Text(
                text = if (costoEstimado != null && costoEstimado > 0.0) {
                    "Costo estimado: Bs %.2f".format(costoEstimado)
                } else {
                    "Costo: se calcula al finalizar"
                }
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (tel != "—") {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Llamar")
                    }
                }

                Button(
                    onClick = { onFinalizar(det) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Finalizar")
                }
            }
        }
    }
}
@Composable
private fun PagoDialog(
    context: Context,
    parqueoId: String,
    espacio: EspacioParqueo,
    detalle: DetalleOcupacion,
    viewModel: AdminEspaciosViewModel,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var tarifaHora by remember { mutableStateOf(0.0) }
    var tarifaMedia by remember { mutableStateOf(0.0) }
    var costoCalculado by remember { mutableStateOf(0.0) }
    var minutosCalculados by remember { mutableStateOf(0) }

    LaunchedEffect(parqueoId, espacio.tipoVehiculo, detalle.reservaId) {
        cargando = true
        error = null
        try {
            val tarifas = viewModel.obtenerTarifas(parqueoId)
            tarifaHora = tarifas[espacio.tipoVehiculo]?.get("hora") ?: 0.0
            tarifaMedia = tarifas[espacio.tipoVehiculo]?.get("mediaHora") ?: 0.0

            minutosCalculados = CalculadoraCobroReserva.calcularMinutosDesdeReserva(detalle.reserva)

            costoCalculado = CalculadoraCobroReserva.calcularCostoFinal(
                reserva = detalle.reserva,
                tarifaHora = tarifaHora,
                tarifaMediaHora = tarifaMedia
            )
        } catch (e: Exception) {
            error = e.message ?: "No se pudo calcular el monto"
        } finally {
            cargando = false
        }
    }
    fun procesarPago(metodoPago: String, mensajeExito: String) {
        scope.launch {
            guardando = true
            try {
                val clienteId = detalle.cliente?.id

                viewModel.finalizarReservaYLiberarEspacio(
                    reservaId = detalle.reservaId,
                    parqueoId = parqueoId,
                    tipoVehiculo = espacio.tipoVehiculo,
                    indice = espacio.indice,
                    minutosUsados = minutosCalculados,
                    costoFinal = costoCalculado
                )

                viewModel.registrarPago(
                    reservaId = detalle.reservaId,
                    parqueoId = parqueoId,
                    clienteId = clienteId,
                    monto = costoCalculado,
                    metodo = metodoPago,
                    minutosUsados = minutosCalculados
                )

                Toast.makeText(
                    context,
                    mensajeExito,
                    Toast.LENGTH_SHORT
                ).show()

                onDone()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                guardando = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!guardando) onDismiss()
        },
        title = {
            Text("Finalizar ocupación")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Resumen del cobro",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                when {
                    cargando -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Calculando monto...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    error != null -> {
                        Text(
                            text = "Error: $error",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Tiempo usado: $minutosCalculados min",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Tarifa 30 min: Bs ${"%.2f".format(tarifaMedia)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = "Tarifa 1 hora: Bs ${"%.2f".format(tarifaHora)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )

                                Text(
                                    text = "Monto final: Bs ${"%.2f".format(costoCalculado)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "Selecciona cómo realizó el pago el cliente.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (!cargando && error == null) {
                Row {
                    TextButton(
                        enabled = !guardando,
                        onClick = {
                            procesarPago(
                                metodoPago = "fisico",
                                mensajeExito = "Pago en efectivo registrado"
                            )
                        }
                    ) {
                        Text(if (guardando) "Guardando..." else "Efectivo")
                    }

                    TextButton(
                        enabled = !guardando,
                        onClick = {
                            procesarPago(
                                metodoPago = "qr",
                                mensajeExito = "Pago QR registrado"
                            )
                        }
                    ) {
                        Text(if (guardando) "Guardando..." else "QR")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !guardando
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ContadorTiempo(fechaInicio: Date) {
    var tiempo by remember { mutableStateOf("") }

    LaunchedEffect(fechaInicio) {
        while (true) {
            val diff = System.currentTimeMillis() - fechaInicio.time
            val minutosTotales = diff / (1000 * 60)
            val horas = minutosTotales / 60
            val minutos = minutosTotales % 60

            tiempo = if (horas > 0) {
                "${horas}h ${minutos}m"
            } else {
                "${minutos} min"
            }

            delay(1000)
        }
    }

    Text(
        text = "Tiempo transcurrido: $tiempo",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun EspacioSlotBoton(
    espacio: EspacioParqueo,
    onClick: () -> Unit,
    forzarClickable: Boolean = false
) {
    val colorFondo = when (espacio.estado) {
        EstadoEspacio.DISPONIBLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        EstadoEspacio.RESERVADO -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        EstadoEspacio.OCUPADO -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    }

    val borderColor = when (espacio.estado) {
        EstadoEspacio.DISPONIBLE -> MaterialTheme.colorScheme.primary
        EstadoEspacio.RESERVADO -> MaterialTheme.colorScheme.secondary
        EstadoEspacio.OCUPADO -> MaterialTheme.colorScheme.error
    }

    val clickable = forzarClickable || espacio.estado == EstadoEspacio.DISPONIBLE

    Surface(
        modifier = Modifier
            .size(64.dp)
            .animateContentSize()
            .clickable(enabled = clickable, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = colorFondo,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconoVehiculo(espacio.tipoVehiculo),
                contentDescription = null,
                tint = borderColor
            )
            Text(
                text = "%02d".format(espacio.indice),
                fontSize = 12.sp,
                color = borderColor
            )
        }
    }
}

@Composable
fun FilterChipActivo(
    label: String,
    activo: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (activo) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            1.dp,
            if (activo) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun EstadoChipActivo(
    label: String,
    activo: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (activo) {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = BorderStroke(
            1.dp,
            if (activo) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun getIconoVehiculo(tipo: String): ImageVector {
    return when (tipo.lowercase()) {
        "carro", "auto" -> Icons.Default.DirectionsCar
        "moto" -> Icons.Default.TwoWheeler
        "camion" -> Icons.Default.LocalShipping
        else -> Icons.Default.DirectionsCar
    }
}

data class DetalleOcupacion(
    val reservaId: String,
    val cliente: DocumentSnapshot?,   // puede ser null
    val vehiculo: DocumentSnapshot?,  // puede ser null
    val reserva: DocumentSnapshot
)
