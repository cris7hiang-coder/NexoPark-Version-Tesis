package com.example.parking.Client.ViewParking.CarReserv.Detall

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.offset
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaQRReservaScreen(
    reservaId: String,
    navController: NavController
) {
    val db = Firebase.firestore

    var qrBitmap by remember(reservaId) { mutableStateOf<ImageBitmap?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var estadoQR by remember { mutableStateOf(ReservaEstado.PENDIENTE) }
    var qrActivo by remember { mutableStateOf(true) }
    var debeCerrarPantalla by remember { mutableStateOf(false) }

    DisposableEffect(reservaId) {
        val listener = db.collection("reservas")
            .document(reservaId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    cargando = false
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    cargando = false
                    debeCerrarPantalla = true
                    return@addSnapshotListener
                }

                val estado = snapshot.getString("estado").orEmpty()
                val activo = snapshot.getBoolean("qrActivo") ?: false

                estadoQR = estado
                qrActivo = activo
                cargando = false

                if (qrBitmap == null) {
                    qrBitmap = generarQR(reservaId)
                }

                val disponible = activo && (
                        estado == ReservaEstado.PENDIENTE ||
                                estado == ReservaEstado.ACTIVA
                        )

                if (!disponible) {
                    debeCerrarPantalla = true
                }
            }

        onDispose { listener.remove() }
    }

    LaunchedEffect(debeCerrarPantalla) {
        if (debeCerrarPantalla) {
            delay(1400)
            navController.popBackStack()
        }
    }

    val disponible = qrActivo && (
            estadoQR == ReservaEstado.PENDIENTE ||
                    estadoQR == ReservaEstado.ACTIVA
            )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Código QR",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    cargando -> {
                        QrLoadingCard()
                    }

                    !disponible -> {
                        QrUnavailableCard(
                            estado = estadoQR
                        )
                    }

                    qrBitmap != null -> {
                        QrReservaContentCard(
                            reservaId = reservaId,
                            estadoQR = estadoQR,
                            qrBitmap = qrBitmap!!
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun QrLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.6.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Preparando acceso QR",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Estamos validando la disponibilidad actual de tu reserva.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun QrUnavailableCard(
    estado: String
) {
    val title = when (estado) {
        ReservaEstado.EXPIRADA -> "Reserva expirada"
        ReservaEstado.FINALIZADA -> "Reserva finalizada"
        else -> "QR no disponible"
    }

    val message = when (estado) {
        ReservaEstado.EXPIRADA ->
            "El código QR ya no está disponible porque la reserva venció."

        ReservaEstado.FINALIZADA ->
            "El acceso QR fue deshabilitado porque la reserva ya concluyó."

        else ->
            "El código QR no se encuentra disponible para esta reserva."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = colorEstadoQR(estado).copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = colorEstadoQR(estado),
                    modifier = Modifier
                        .padding(12.dp)
                        .size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun QrReservaContentCard(
    reservaId: String,
    estadoQR: String,
    qrBitmap: ImageBitmap
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.82f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Acceso de reserva",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = mensajePrincipalQR(estadoQR),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            EstadoBadgeQR(estadoQR)

            Spacer(modifier = Modifier.height(18.dp))

            QrSupportCard(estadoQR = estadoQR)

            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                ),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier.padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "Código QR de reserva",
                        modifier = Modifier.size(250.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = textoEstadoQR(estadoQR),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = colorEstadoQR(estadoQR)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Código de reserva",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

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
private fun QrSupportCard(
    estadoQR: String,
    modifier: Modifier = Modifier
) {
    val accentColor = colorEstadoQR(estadoQR)

    val title = when (estadoQR) {
        ReservaEstado.PENDIENTE -> "Validación de ingreso"
        ReservaEstado.ACTIVA -> "Validación de salida"
        else -> "Validación administrativa"
    }

    val message = when (estadoQR) {
        ReservaEstado.PENDIENTE ->
            "Presenta este código al llegar para que el administrador autorice tu ingreso."

        ReservaEstado.ACTIVA ->
            "Conserva este mismo código para que el administrador valide la salida."

        else ->
            "Este código está asociado a tu reserva y solo puede ser validado por el personal autorizado."
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = accentColor.copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            accentColor.copy(alpha = 0.16f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.14f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.FactCheck,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
private fun mensajePrincipalQR(estado: String): String = when (estado) {
    ReservaEstado.PENDIENTE ->
        "Presenta este código para que el administrador valide tu ingreso."

    ReservaEstado.ACTIVA ->
        "Conserva este mismo código para la validación administrativa de salida."

    ReservaEstado.FINALIZADA ->
        "La reserva ya fue completada y el acceso QR se encuentra cerrado."

    ReservaEstado.EXPIRADA ->
        "La reserva venció y el acceso QR dejó de estar disponible."

    else ->
        "Código de acceso asociado a la reserva."
}


fun generarQR(
    contenido: String,
    tamaño: Int = 512
): ImageBitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(
        contenido,
        BarcodeFormat.QR_CODE,
        tamaño,
        tamaño,
        mapOf(EncodeHintType.MARGIN to 1)
    )

    val bitmap = Bitmap.createBitmap(tamaño, tamaño, Bitmap.Config.ARGB_8888)

    for (x in 0 until tamaño) {
        for (y in 0 until tamaño) {
            bitmap.setPixel(
                x,
                y,
                if (bitMatrix[x, y]) android.graphics.Color.BLACK
                else android.graphics.Color.WHITE
            )
        }
    }

    return bitmap.asImageBitmap()
}
@Composable
fun EstadoBadgeQR(estado: String?) {
    val bgColor = colorEstadoQR(estado).copy(alpha = 0.12f)
    val textColor = colorEstadoQR(estado)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = textoEstadoQR(estado),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = textColor
        )
    }
}
@Composable
fun colorEstadoQR(estado: String?): Color = when (estado) {
    ReservaEstado.PENDIENTE -> MaterialTheme.colorScheme.tertiary
    ReservaEstado.ACTIVA -> Color(0xFF3FA36C)
    ReservaEstado.FINALIZADA -> MaterialTheme.colorScheme.primary
    ReservaEstado.EXPIRADA -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
fun textoEstadoQR(estado: String?): String = when (estado) {
    ReservaEstado.PENDIENTE -> "Pendiente de validación"
    ReservaEstado.ACTIVA -> "Acceso validado"
    ReservaEstado.FINALIZADA -> "Reserva finalizada"
    ReservaEstado.EXPIRADA -> "Reserva expirada"
    else -> "Estado no disponible"
}
fun guardarImagenEnGaleria(context: Context, image: ImageBitmap, nombreArchivo: String) {
    val bitmap = image.asAndroidBitmap()
    val fos: OutputStream?
    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    fos = uri?.let { resolver.openOutputStream(it) }
    fos?.use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        Toast.makeText(context, "Imagen guardada en galería", Toast.LENGTH_SHORT).show()
    }
}

fun compartirQR(context: Context, image: ImageBitmap, nombreArchivo: String) {
    val bitmap = image.asAndroidBitmap()
    val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(cachePath, nombreArchivo)
    val fos = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    fos.close()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Compartir QR"))
}
