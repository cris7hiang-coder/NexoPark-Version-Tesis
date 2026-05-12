package com.example.parking.Admin.form

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SubirQRScreen(
    parqueoId: String,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var qrUri by remember { mutableStateOf<Uri?>(null) }
    var showPrompt by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        qrUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Subir QR del parqueo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))
        // Mensaje de advertencia sobre pago
        Card(
           // fondo claro tipo advertencia
            colors = CardDefaults.cardColors(),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "⚠️ Este QR será utilizado para pagos del parqueo. Asegúrate de que corresponde al parqueo correcto.",
                color = Color(0xFFF57C00), // naranja para destacar
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.size(200.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (qrUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(qrUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Toca para seleccionar QR", color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { showPrompt = true },
            enabled = qrUri != null && !isUploading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Subiendo...")
            } else {
                Text("Guardar QR")
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Omitir por ahora")
        }
    }

    // Prompt de confirmación
    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false },
            title = { Text("Confirmar subida", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas subir este QR para el parqueo? Esta acción actualizará el QR actual.") },
            confirmButton = {
                TextButton(onClick = {
                    showPrompt = false
                    qrUri?.let {
                        scope.launch {
                            isUploading = true
                            val success = subirQRAlmacenamiento(context, it, parqueoId)
                            isUploading = false
                            if (success) onFinish()
                        }
                    }
                }) {
                    Text("Sí, subir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrompt = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
suspend fun subirQRAlmacenamiento(
    context: Context,
    uri: Uri,
    parqueoId: String
): Boolean {
    return try {
        val storageRef = FirebaseStorage.getInstance().reference
        val qrRef = storageRef.child("parqueos/$parqueoId/qr.jpg")

        qrRef.putFile(uri).await()
        val downloadUrl = qrRef.downloadUrl.await()

        FirebaseFirestore.getInstance()
            .collection("parqueos")
            .document(parqueoId)
            .update("qrUrl", downloadUrl.toString())
            .await()

        Toast.makeText(context, "QR subido correctamente", Toast.LENGTH_SHORT).show()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al subir QR", Toast.LENGTH_SHORT).show()
        false
    }
}