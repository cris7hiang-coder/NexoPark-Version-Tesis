package com.example.parking.Admin
import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.parking.Client.ViewParking.CarReserv.Detall.ReservaEstado
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.parking.R
import com.example.parking.ui.theme.AzulPetroleoFuerte
import com.example.parking.ui.theme.FondoCard
import com.example.parking.ui.theme.RojoCoral
import com.example.parking.ui.theme.TextoBase
import com.example.parking.ui.theme.TextoSecundario
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.storage
import kotlinx.coroutines.delay
import java.io.ByteArrayOutputStream
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EscanearQRAdminScreen(
    navController: androidx.navigation.NavHostController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val permisoCamara = rememberPermissionState(Manifest.permission.CAMERA)

    var resultado by remember { mutableStateOf<Boolean?>(null) }
    var mensaje by remember { mutableStateOf("") }
    var escaneoExitoso by remember { mutableStateOf(false) }
    var modoSalida by remember { mutableStateOf(false) }

    var mostrarDialogoPago by remember { mutableStateOf(false) }
    var showSelectorPagoDigital by remember { mutableStateOf(false) }
    var reservaSalidaId by remember { mutableStateOf<String?>(null) }
    var parqueoIdSalida by remember { mutableStateOf<String?>(null) }
    var costoSalida by remember { mutableStateOf<Double?>(null) }

    var fotoUri by remember { mutableStateOf<Uri?>(null) }

    val previewView = remember { PreviewView(context) }
    val scanner = remember { BarcodeScanning.getClient() }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
        }
    }

    val limpiarEstadoEscaneo = {
        escaneoExitoso = false
        resultado = null
        mensaje = ""
    }

    val limpiarEstadoSalida = {
        reservaSalidaId = null
        parqueoIdSalida = null
        costoSalida = null
    }

    val galeriaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                subirFotoYRegistrarPago(
                    context = context,
                    imageUri = it,
                    reservaId = reservaSalidaId,
                    parqueoId = parqueoIdSalida,
                    monto = costoSalida
                )
            }
        }
        showSelectorPagoDigital = false
    }

    val tomarFotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && fotoUri != null) {
            coroutineScope.launch {
                subirFotoYRegistrarPago(
                    context = context,
                    imageUri = fotoUri!!,
                    reservaId = reservaSalidaId,
                    parqueoId = parqueoIdSalida,
                    monto = costoSalida
                )
            }
        }
        showSelectorPagoDigital = false
    }

    LaunchedEffect(Unit) {
        permisoCamara.launchPermissionRequest()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (permisoCamara.status.isGranted) {

            AndroidView(
                factory = { ctx ->
                    previewView.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val analyzer = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        analyzer.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image

                            if (mediaImage != null && !escaneoExitoso) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val raw = barcodes.firstOrNull()?.rawValue?.trim()

                                        if (!raw.isNullOrBlank()) {
                                            escaneoExitoso = true

                                            coroutineScope.launch {
                                                if (!modoSalida) {
                                                    val valido = validarYActivarQR(
                                                        qrValue = raw,
                                                        modoAsignacion = ModoAsignacionEspacio.AUTOMATICO
                                                    )

                                                    resultado = valido
                                                    mensaje = if (valido) {
                                                        "Ingreso validado y espacio asignado correctamente"
                                                    } else {
                                                        "No se pudo validar el QR o asignar un espacio compatible"
                                                    }

                                                    if (!valido) {
                                                        escaneoExitoso = false
                                                    }
                                                } else {
                                                    val (valido, costo, ids) = registrarSalidaQR(raw)
                                                    resultado = valido

                                                    if (valido && costo != null && ids != null) {
                                                        reservaSalidaId = ids.first
                                                        parqueoIdSalida = ids.second
                                                        costoSalida = costo
                                                        mensaje = "Salida registrada. Selecciona el método de pago."
                                                        mostrarDialogoPago = true
                                                    } else {
                                                        limpiarEstadoSalida()
                                                        mensaje = "QR inválido, reserva no activa o ya finalizada"
                                                        escaneoExitoso = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("QRAdmin", "Error leyendo QR: ${e.message}", e)
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                analyzer
                            )
                        } catch (e: Exception) {
                            Log.e("QRAdmin", "Cam error: ${e.message}", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)
                            )
                        )
                    )
            )

            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.86f))
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Text(
                        text = if (modoSalida) "Modo salida" else "Modo entrada",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Control de acceso",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = if (modoSalida) {
                                "Escanea el QR del cliente para registrar la salida y el cobro."
                            } else {
                                "Escanea el QR del cliente para validar el ingreso y asignar un espacio compatible."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    modoSalida = false
                                    limpiarEstadoSalida()
                                    limpiarEstadoEscaneo()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (!modoSalida) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!modoSalida) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("Entrada")
                            }

                            Button(
                                onClick = {
                                    modoSalida = true
                                    limpiarEstadoSalida()
                                    limpiarEstadoEscaneo()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (modoSalida) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = if (modoSalida) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp,
                                    focusedElevation = 0.dp,
                                    hoveredElevation = 0.dp,
                                    disabledElevation = 0.dp
                                )
                            ) {
                                Text("Salida")
                            }
                        }
                    }
                }
            }

            resultado?.let { esValido ->
                ResultadoAnimadoOverlay(
                    exito = esValido,
                    mensaje = mensaje
                ) {
                    limpiarEstadoEscaneo()
                }
            }

            if (mostrarDialogoPago && costoSalida != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        mostrarDialogoPago = false
                        resultado = true
                        mensaje = "Salida registrada (pago físico)"
                        escaneoExitoso = false
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    SeleccionPagoBottomSheet(
                        monto = costoSalida!!,
                        onFisico = {
                            mostrarDialogoPago = false
                            resultado = true
                            mensaje = "Pago físico registrado"
                            escaneoExitoso = false
                        },
                        onDigital = {
                            mostrarDialogoPago = false
                            showSelectorPagoDigital = true
                        },
                        onNinguno = {
                            mostrarDialogoPago = false
                            resultado = true
                            mensaje = "Salida registrada con pago físico"
                            escaneoExitoso = false
                        }
                    )
                }
            }

            if (showSelectorPagoDigital) {
                AlertDialog(
                    onDismissRequest = { showSelectorPagoDigital = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(24.dp),
                    title = {
                        Text(
                            text = "Seleccionar comprobante",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    fotoUri = createImageUri(context)
                                    fotoUri?.let { uri ->
                                        tomarFotoLauncher.launch(uri)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Tomar foto")
                            }

                            Button(
                                onClick = {
                                    galeriaLauncher.launch("image/*")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp,
                                    focusedElevation = 0.dp,
                                    hoveredElevation = 0.dp,
                                    disabledElevation = 0.dp
                                )
                            ) {
                                Text("Elegir desde galería")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }

        } else {
            CameraPermissionState(
                onRequestPermission = { permisoCamara.launchPermissionRequest() }
            )
        }
    }
}

enum class ModoAsignacionEspacio {
    AUTOMATICO,
    MANUAL
}
suspend fun validarYActivarQR(
    qrValue: String,
    modoAsignacion: ModoAsignacionEspacio = ModoAsignacionEspacio.AUTOMATICO,
    espacioIdManual: String? = null
): Boolean {
    val reservaId = qrValue.trim()
    if (reservaId.isBlank()) return false

    val uidAdmin = FirebaseAuth.getInstance().currentUser?.uid ?: return false
    val db = Firebase.firestore

    return try {
        val reservaRef = db.collection("reservas").document(reservaId)
        val reservaSnap = reservaRef.get().await()
        if (!reservaSnap.exists()) return false

        val estado = reservaSnap.getString("estado")
        val qrActivo = reservaSnap.getBoolean("qrActivo") ?: false
        val fechaExpiracion = reservaSnap.getTimestamp("fechaExpiracion")?.toDate()
        val parqueoId = reservaSnap.getString("parqueoId") ?: return false
        val clienteId = reservaSnap.getString("clienteId")
        val tipoVehiculo = reservaSnap.getString("tipoVehiculo") ?: return false

        if (estado != ReservaEstado.PENDIENTE) return false
        if (!qrActivo) return false

        val ahora = Date()
        if (fechaExpiracion != null && ahora.after(fechaExpiracion)) {
            db.runTransaction { transaction ->
                transaction.update(
                    reservaRef,
                    mapOf(
                        "estado" to ReservaEstado.EXPIRADA,
                        "qrActivo" to false
                    )
                )
                false
            }.await()
            return false
        }

        val parqueoRef = db.collection("parqueos").document(parqueoId)
        val parqueoSnap = parqueoRef.get().await()
        if (parqueoSnap.getString("adminId") != uidAdmin) return false

        val espaciosRef = parqueoRef.collection("espacios")

        val espacioSeleccionadoRef = when (modoAsignacion) {
            ModoAsignacionEspacio.MANUAL -> {
                val id = espacioIdManual?.trim().orEmpty()
                if (id.isBlank()) return false
                espaciosRef.document(id)
            }

            ModoAsignacionEspacio.AUTOMATICO -> {
                val candidatos = espaciosRef
                    .whereEqualTo("estado", "DISPONIBLE")
                    .whereEqualTo("tipoVehiculo", tipoVehiculo)
                    .get()
                    .await()

                val primerDisponible = candidatos.documents
                    .sortedBy { it.getLong("indice") ?: Long.MAX_VALUE }
                    .firstOrNull()

                primerDisponible?.reference ?: return false
            }
        }

        db.runTransaction { transaction ->
            val reservaTx = transaction.get(reservaRef)
            if (!reservaTx.exists()) return@runTransaction false

            val estadoTx = reservaTx.getString("estado")
            val qrActivoTx = reservaTx.getBoolean("qrActivo") ?: false

            if (estadoTx != ReservaEstado.PENDIENTE) return@runTransaction false
            if (!qrActivoTx) return@runTransaction false

            val espacioSnap = transaction.get(espacioSeleccionadoRef)
            if (!espacioSnap.exists()) return@runTransaction false

            val estadoEspacio = espacioSnap.getString("estado")
            val tipoVehiculoEspacio = espacioSnap.getString("tipoVehiculo")

            if (estadoEspacio != "DISPONIBLE") return@runTransaction false
            if (tipoVehiculoEspacio != tipoVehiculo) return@runTransaction false

            transaction.update(
                reservaRef,
                mapOf(
                    "estado" to ReservaEstado.ACTIVA,
                    "horaLlegada" to FieldValue.serverTimestamp(),
                    "qrActivo" to true,
                    "presente" to true,
                    "espacioId" to espacioSeleccionadoRef.id
                )
            )

            transaction.update(
                espacioSeleccionadoRef,
                mapOf(
                    "estado" to "OCUPADO",
                    "cliente" to clienteId,
                    "reservaId" to reservaId
                )
            )

            true
        }.await()

    } catch (e: Exception) {
        Log.e("QRAdmin", "Error activando reserva: ${e.message}", e)
        false
    }
}
suspend fun registrarSalidaQR(
    qrValue: String
): Triple<Boolean, Double?, Pair<String, String>?> {

    val reservaId = qrValue.trim()
    if (reservaId.isBlank()) return Triple(false, null, null)

    val uidAdmin = FirebaseAuth.getInstance().currentUser?.uid
        ?: return Triple(false, null, null)

    val db = Firebase.firestore

    return try {
        var costoFinalCalculado: Double? = null
        var parqueoId: String? = null
        var clienteId: String? = null

        val exito = db.runTransaction { transaction ->

            val reservaRef = db.collection("reservas").document(reservaId)
            val reservaSnap = transaction.get(reservaRef)

            if (!reservaSnap.exists()) return@runTransaction false

            val estado = reservaSnap.getString("estado")
            val cobroProcesado = reservaSnap.getBoolean("cobroProcesado") ?: false

            if (estado != ReservaEstado.ACTIVA) return@runTransaction false
            if (cobroProcesado) return@runTransaction false

            parqueoId = reservaSnap.getString("parqueoId") ?: return@runTransaction false
            clienteId = reservaSnap.getString("clienteId")
            val espacioId = reservaSnap.getString("espacioId")

            val parqueoRef = db.collection("parqueos").document(parqueoId!!)
            val parqueoSnap = transaction.get(parqueoRef)

            if (parqueoSnap.getString("adminId") != uidAdmin) {
                return@runTransaction false
            }

            val tipoVehiculo = reservaSnap.getString("tipoVehiculo")
                ?.trim()
                ?.lowercase()
                ?: return@runTransaction false

            val tarifas = parqueoSnap.get("tarifas") as? Map<*, *> ?: return@runTransaction false
            val tarifaTipo = tarifas[tipoVehiculo] as? Map<*, *> ?: return@runTransaction false

            val tarifaHora = (tarifaTipo["hora"] as? Number)?.toDouble() ?: 0.0
            val tarifaMediaHora = (tarifaTipo["mediaHora"] as? Number)?.toDouble() ?: 0.0

            costoFinalCalculado = CalculadoraCobroReserva.calcularCostoFinal(
                reserva = reservaSnap,
                tarifaHora = tarifaHora,
                tarifaMediaHora = tarifaMediaHora
            )

            val minutosUsados = CalculadoraCobroReserva
                .calcularMinutosDesdeReserva(reservaSnap)
                .toLong()

            val ahora = Date()

            transaction.update(
                reservaRef,
                mapOf(
                    "estado" to ReservaEstado.FINALIZADA,
                    "horaSalida" to FieldValue.serverTimestamp(),
                    "costoFinal" to costoFinalCalculado,
                    "minutosUsados" to minutosUsados,
                    "cobroProcesado" to true,
                    "qrActivo" to false
                )
            )

            if (!espacioId.isNullOrBlank()) {
                val espacioRef = parqueoRef.collection("espacios").document(espacioId)
                transaction.update(
                    espacioRef,
                    mapOf(
                        "estado" to EstadoEspacio.DISPONIBLE.name,
                        "cliente" to null,
                        "placa" to null,
                        "reservaId" to null
                    )
                )
            }

            val pagoRef = parqueoRef.collection("pagos").document()
            transaction.set(
                pagoRef,
                mapOf(
                    "reservaId" to reservaId,
                    "clienteId" to clienteId,
                    "monto" to costoFinalCalculado,
                    "fecha" to FieldValue.serverTimestamp(),
                    "mes" to SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(ahora),
                    "tipoPago" to "fisico",
                    "comprobanteUrl" to null,
                    "registradoPor" to uidAdmin,
                    "fechaRegistro" to FieldValue.serverTimestamp()
                )
            )

            true
        }.await()

        Triple(
            exito,
            costoFinalCalculado,
            if (exito && parqueoId != null) Pair(reservaId, parqueoId!!) else null
        )

    } catch (e: Exception) {
        Log.e("QRAdmin", "Error salida: ${e.message}", e)
        Triple(false, null, null)
    }
}
suspend fun registrarTipoPago(
    reservaId: String,
    parqueoId: String,
    tipoPago: String,
    comprobanteUrl: String? = null,
    monto: Double? = null
) {
    val db = Firebase.firestore

    try {
        val pagosColl = db.collection("parqueos")
            .document(parqueoId)
            .collection("pagos")

        val query = pagosColl
            .whereEqualTo("reservaId", reservaId)
            .limit(1)
            .get()
            .await()

        if (!query.isEmpty) {
            val doc = query.documents.first()

            pagosColl.document(doc.id).update(
                mapOf(
                    "tipoPago" to tipoPago,
                    "comprobanteUrl" to comprobanteUrl,
                    "registradoPor" to FirebaseAuth.getInstance().currentUser?.uid,
                    "fechaRegistro" to FieldValue.serverTimestamp(),
                    "monto" to monto
                )
            ).await()

        } else {
            val pagoRef = pagosColl.document()

            pagoRef.set(
                mapOf(
                    "reservaId" to reservaId,
                    "tipoPago" to tipoPago,
                    "comprobanteUrl" to comprobanteUrl,
                    "fecha" to FieldValue.serverTimestamp(),
                    "fechaRegistro" to FieldValue.serverTimestamp(),
                    "registradoPor" to FirebaseAuth.getInstance().currentUser?.uid,
                    "monto" to monto
                )
            ).await()
        }

    } catch (e: Exception) {
        Log.e("QRAdmin", "Error registrarTipoPago: ${e.message}", e)
    }
}
@Composable
private fun CameraPermissionState(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Permiso de cámara requerido",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Activa el permiso para escanear códigos QR de ingreso y salida.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        focusedElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        disabledElevation = 0.dp
                    )
                ) {
                    Text("Solicitar permiso")
                }
            }
        }
    }
}

/**
 * Helper para subir foto a Firebase Storage y luego llamar a registrarTipoPago
 */
suspend fun subirFotoYRegistrarPago(
    context: Context,
    imageUri: Uri,
    reservaId: String?,
    parqueoId: String?,
    monto: Double?
) {
    if (reservaId == null || parqueoId == null) return

    try {
        val storageRef = Firebase.storage.reference
        val fileName = "pagos/$parqueoId/${System.currentTimeMillis()}.webp"
        val ref = storageRef.child(fileName)

        val bytes = compressToWebP(context, imageUri)
        if (bytes == null) {
            Log.e("QRAdmin", "Error al comprimir imagen")
            return
        }

        ref.putBytes(bytes).await()
        val url = ref.downloadUrl.await().toString()

        registrarTipoPago(
            reservaId = reservaId,
            parqueoId = parqueoId,
            tipoPago = "digital",
            comprobanteUrl = url,
            monto = monto
        )

    } catch (e: Exception) {
        Log.e("QRAdmin", "Error subirFotoYRegistrarPago: ${e.message}", e)
    }
}
fun createImageUri(context: Context): Uri? {
    return try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "pagocomprobante_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }
        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    } catch (e: Exception) {
        Log.e("QRAdmin", "createImageUri error: ${e.message}")
        null
    }
}
fun compressToWebP(context: Context, uri: Uri): ByteArray? = try {
    val bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

    val maxSize = 2000
    val scaled = if (bitmap.width > maxSize || bitmap.height > maxSize) {
        val ratio = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * ratio).toInt(),
            (bitmap.height * ratio).toInt(),
            true
        )
    } else bitmap

    ByteArrayOutputStream().apply {
        val format =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                Bitmap.CompressFormat.WEBP_LOSSY
            else
                @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP

        scaled.compress(format, 70, this)
    }.toByteArray()

} catch (e: Exception) {
    Log.e("QRAdmin", "compressToWebP error: ${e.message}")
    null
}
@Composable
fun SeleccionPagoBottomSheet(
    monto: Double,
    onFisico: () -> Unit,
    onDigital: () -> Unit,
    onNinguno: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .navigationBarsPadding()
    ) {
        Text(
            text = "Registrar pago",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Monto a cobrar: Bs ${"%.2f".format(monto)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFisico,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                focusedElevation = 0.dp,
                hoveredElevation = 0.dp,
                disabledElevation = 0.dp
            )
        ) {
            Text("Pago físico")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDigital,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Pago digital")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onNinguno,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Continuar sin comprobante digital",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
fun ResultadoAnimadoOverlay(
    exito: Boolean,
    mensaje: String,
    autoCerrar: Boolean = exito,
    onCerrar: () -> Unit
) {
    val composition by rememberLottieComposition(
        if (exito)
            LottieCompositionSpec.RawRes(R.raw.success)
        else
            LottieCompositionSpec.RawRes(R.raw.error)
    )

    LaunchedEffect(exito) {
        if (autoCerrar) {
            delay(1500)
            onCerrar()
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(220)) +
                scaleIn(
                    initialScale = 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
        exit = fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.96f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp)
                ) {
                    LottieAnimation(
                        composition = composition,
                        iterations = 1,
                        modifier = Modifier.size(132.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = mensaje,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (!autoCerrar) {
                        Spacer(modifier = Modifier.height(22.dp))

                        Button(
                            onClick = onCerrar,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (exito) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp,
                                focusedElevation = 0.dp,
                                hoveredElevation = 0.dp,
                                disabledElevation = 0.dp
                            )
                        ) {
                            Text("Aceptar")
                        }
                    }
                }
            }
        }
    }
}