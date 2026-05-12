package com.example.parking.Admin.form.Edit

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.parking.Admin.form.compressToWebP
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun EditarGaleriaParqueo(
    parqueo: DocumentSnapshot,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = parqueo.id

    var imagenUrls by remember { mutableStateOf(parqueo.get("imagenes") as? List<String> ?: emptyList()) }
    var portadaUrl by remember { mutableStateOf(parqueo.getString("imagenUrl") ?: "") }
    var isUploading by remember { mutableStateOf(false) }

    var nuevaPortadaSeleccionada by remember { mutableStateOf<String?>(null) }
    var showChangePortadaDialog by remember { mutableStateOf(false) }

    var imagenAEliminar by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.size + imagenUrls.size > 5) {
            Toast.makeText(context, "Máximo 5 imágenes", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isUploading = true
                val nuevasUrls = mutableListOf<String>()
                uris.forEachIndexed { index, uri ->
                    val data = compressToWebP(context, uri)
                    val ref = FirebaseStorage.getInstance()
                        .reference
                        .child("parqueos/$uid/edit_${System.currentTimeMillis()}_$index.webp")
                    ref.putBytes(data!!).await()
                    nuevasUrls.add(ref.downloadUrl.await().toString())
                }
                imagenUrls = imagenUrls + nuevasUrls
                if (portadaUrl.isEmpty() && nuevasUrls.isNotEmpty()) {
                    portadaUrl = nuevasUrls.first()
                }
                isUploading = false
            }
        }
    }

    Column(Modifier.padding(16.dp)) {
        Text(
            "Editar Imágenes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        // Mensaje de advertencia
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "⚠️ Las imágenes mostradas aquí serán visibles para los clientes en la ficha del parqueo. " +
                        "Verifica que representen bien las condiciones actuales.",
                color = Color(0xFFF57C00),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // === Card estilo QR para agregar nuevas imágenes ===
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(enabled = imagenUrls.size < 5 && !isUploading) { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imagenUrls.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Toca para agregar hasta 5 imágenes",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Previsualiza portada si existe, si no la última
                    val preview: String? = if (portadaUrl.isNotEmpty()) portadaUrl else imagenUrls.lastOrNull()
                    if (preview != null) {
                        AsyncImage(
                            model = preview,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Contador
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(bottomStart = 8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${imagenUrls.size}/5", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Estado límite
                if (imagenUrls.size >= 5) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Límite 5/5", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(imagenUrls) { index, url ->
                Box(
                    Modifier
                        .size(120.dp)
                        .border(
                            2.dp,
                            if (url == portadaUrl) MaterialTheme.colorScheme.primary else Color.Gray,
                            RoundedCornerShape(8.dp)
                        )
                ) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                if (url != portadaUrl) {
                                    nuevaPortadaSeleccionada = url
                                    showChangePortadaDialog = true
                                }
                            }
                    )

                    if (url == portadaUrl) {
                        Box(
                            Modifier
                                .align(Alignment.TopStart)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Portada", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { imagenAEliminar = url; showDeleteDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isUploading = true
                scope.launch {
                    try {
                        FirebaseFirestore.getInstance().collection("parqueos").document(uid)
                            .update(
                                mapOf(
                                    "imagenUrl" to portadaUrl,
                                    "imagenes" to imagenUrls
                                )
                            ).await()
                        Toast.makeText(context, "Galería actualizada", Toast.LENGTH_SHORT).show()
                        onBack()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error al actualizar imágenes", Toast.LENGTH_SHORT).show()
                    } finally {
                        isUploading = false
                    }
                }
            },
            enabled = !isUploading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar galería")
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Cancelar")
        }
    }

    // Dialogs...
    if (showChangePortadaDialog && nuevaPortadaSeleccionada != null) {
        AlertDialog(
            onDismissRequest = { showChangePortadaDialog = false },
            title = { Text("Cambiar portada") },
            text = { Text("¿Deseas marcar esta imagen como portada del parqueo?") },
            confirmButton = {
                TextButton(onClick = {
                    portadaUrl = nuevaPortadaSeleccionada!!
                    showChangePortadaDialog = false
                }) { Text("Sí") }
            },
            dismissButton = {
                TextButton(onClick = { showChangePortadaDialog = false }) { Text("No") }
            }
        )
    }

    if (showDeleteDialog && imagenAEliminar != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar imagen") },
            text = { Text("¿Deseas eliminar esta imagen?") },
            confirmButton = {
                TextButton(onClick = {
                    imagenUrls = imagenUrls.filter { it != imagenAEliminar }
                    if (portadaUrl == imagenAEliminar) portadaUrl = imagenUrls.firstOrNull() ?: ""
                    showDeleteDialog = false
                }) { Text("Sí") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("No") }
            }
        )
    }
}

