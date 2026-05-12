package com.example.parking.Admin.form.Edit

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
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.parking.Admin.form.subirQRAlmacenamiento
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EditarQrScreen(
    parqueoId: String,
    qrActualUrl: String?, // QR existente desde Firestore
    onBack: () -> Unit,

) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var qrUri by remember { mutableStateOf<Uri?>(null) }
    var showPrompt by remember { mutableStateOf(false) }
    var showDeletePrompt by remember { mutableStateOf(false) }
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
            "Editar QR del parqueo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // Mensaje de advertencia sobre pago
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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
                when {
                    qrUri != null -> {
                        Image(
                            painter = rememberAsyncImagePainter(qrUri),
                            contentDescription = "QR seleccionado",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    !qrActualUrl.isNullOrEmpty() -> {
                        Image(
                            painter = rememberAsyncImagePainter(qrActualUrl),
                            contentDescription = "QR actual",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Toca para seleccionar QR", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showDeletePrompt = true },
                    enabled = (qrUri != null) || (!qrActualUrl.isNullOrEmpty()),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Eliminar QR")
                }
            Button(
                onClick = { showPrompt = true },
                enabled = qrUri != null && !isUploading,
                modifier = Modifier.weight(1f)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Subiendo...")
                } else {
                    Text("Guardar cambios")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar")
        }
    }

    // Prompt para subir QR
    if (showPrompt) {
        AlertDialog(
            onDismissRequest = { showPrompt = false },
            title = { Text("Confirmar subida", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas subir este QR para el parqueo? Esto reemplazará el QR actual.") },
            confirmButton = {
                TextButton(onClick = {
                    showPrompt = false
                    qrUri?.let {
                        scope.launch {
                            isUploading = true
                            val success = subirQRAlmacenamiento(context, it, parqueoId)
                            isUploading = false
                            if (success) onBack()
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

    // Prompt para eliminar QR
    if (showDeletePrompt) {
        AlertDialog(
            onDismissRequest = { showDeletePrompt = false },
            title = { Text("Confirmar eliminación", fontWeight = FontWeight.Bold) },
            text = { Text("¿Deseas eliminar el QR actual del parqueo? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeletePrompt = false
                    scope.launch {
                        isUploading = true
                        try {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("parqueos").document(parqueoId)
                                .update("qrUrl", null).await()
                            qrUri = null
                            Toast.makeText(context, "QR eliminado", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error al eliminar QR", Toast.LENGTH_SHORT).show()
                        }
                        isUploading = false
                    }
                }) {
                    Text("Sí, eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePrompt = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
