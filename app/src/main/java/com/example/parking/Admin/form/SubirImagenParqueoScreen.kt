package com.example.parking.Admin.form

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.parking.Admin.form.RegistroParqueoWizard
import com.example.parking.Login.PrimaryButton

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
@Composable
fun SubirImagenParqueoScreen(
    parqueoId: String,
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imagenUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imagenExistentes by remember { mutableStateOf<List<String>>(emptyList()) }
    var imagenPortadaIndex by remember { mutableStateOf<Int?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val totalImagenes = imagenExistentes.size + imagenUris.size

    LaunchedEffect(parqueoId) {
        val db = FirebaseFirestore.getInstance()

        db.collection("parqueos")
            .document(parqueoId)
            .get()
            .addOnSuccessListener { doc ->
                val lista = doc.get("imagenes") as? List<String> ?: emptyList()
                imagenExistentes = lista

                val portada = doc.getString("imagenUrl")
                imagenPortadaIndex = portada
                    ?.let { lista.indexOf(it) }
                    ?.takeIf { it >= 0 }
            }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val total = imagenExistentes.size + uris.size

        if (total > 5) {
            Toast.makeText(
                context,
                "Máximo 5 imágenes",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            imagenUris = uris

            if (imagenPortadaIndex == null && total > 0) {
                imagenPortadaIndex = 0
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            contentPadding = PaddingValues(
                top = 4.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                WizardStepHeaderCompact(
                    title = "Imágenes del parqueo",
                    subtitle = "Sube fotos reales para que los clientes conozcan el espacio.",
                    stepLabel = "Paso 7 de 7"
                )
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Fotos visibles para clientes",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Agrega hasta 5 imágenes. La portada será la principal en el perfil del parqueo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        launcher.launch("image/*")
                    },
                    enabled = totalImagenes < 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = if (totalImagenes == 0) {
                            "Seleccionar imágenes"
                        } else {
                            "Agregar más imágenes"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            item {
                if (totalImagenes > 0) {
                    Text(
                        text = "$totalImagenes / 5 imágenes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                if (totalImagenes > 0) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(imagenExistentes) { index, url ->
                            ImagenItem(
                                uri = Uri.parse(url),
                                isPortada = imagenPortadaIndex == index,
                                onClick = {
                                    imagenPortadaIndex = index
                                }
                            )
                        }

                        itemsIndexed(imagenUris) { index, uri ->
                            val globalIndex = imagenExistentes.size + index

                            ImagenItem(
                                uri = uri,
                                isPortada = imagenPortadaIndex == globalIndex,
                                onClick = {
                                    imagenPortadaIndex = globalIndex
                                }
                            )
                        }
                    }
                } else {
                    EmptyImageState()
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    text = "Guardar cambios",
                    loading = isUploading,
                    icon = Icons.Default.Check,
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (imagenExistentes.isEmpty() && imagenUris.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Selecciona al menos una imagen",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@PrimaryButton
                        }

                        isUploading = true

                        scope.launch {
                            val success = subirImagenesConExistentes(
                                context = context,
                                urisNuevas = imagenUris,
                                urlsExistentes = imagenExistentes,
                                parqueoId = parqueoId,
                                portadaIndex = imagenPortadaIndex ?: 0
                            )

                            isUploading = false

                            if (success) {
                                Toast.makeText(
                                    context,
                                    "Imágenes actualizadas",
                                    Toast.LENGTH_SHORT
                                ).show()

                                onFinish()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Error al subir imágenes",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                OutlinedButton(
                    onClick = onSkip,
                    enabled = !isUploading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = "Omitir por ahora",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
@Composable
fun ImagenItem(
    uri: Uri,
    isPortada: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(
                width = if (isPortada) 132.dp else 110.dp,
                height = if (isPortada) 132.dp else 110.dp
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isPortada) 2.dp else 1.dp,
            color = if (isPortada) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        ),
        tonalElevation = if (isPortada) 4.dp else 1.dp
    ) {
        Box {
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isPortada) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.90f)
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "Portada",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun EmptyImageState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Aún no agregaste imágenes",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Las fotos ayudan a generar confianza y mejorar reservas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
suspend fun subirImagenesConExistentes(
    context: Context,
    urisNuevas: List<Uri>,
    urlsExistentes: List<String>,
    parqueoId: String,
    portadaIndex: Int
): Boolean {
    return try {
        val storageRef = FirebaseStorage.getInstance().reference
        val db = FirebaseFirestore.getInstance()

        val nuevasUrls = mutableListOf<String>().apply { addAll(urlsExistentes) }

        // Subir solo las nuevas imágenes
        urisNuevas.forEach { uri ->
            val imageData = compressToWebP(context, uri) ?: throw Exception("Error al comprimir")
            val fileName = "img_${System.currentTimeMillis()}.webp"
            val imageRef = storageRef.child("parqueos/$parqueoId/$fileName")
            imageRef.putBytes(imageData).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            nuevasUrls.add(downloadUrl)
        }

        val portadaUrl = nuevasUrls.getOrNull(portadaIndex) ?: nuevasUrls.first()

        db.collection("parqueos").document(parqueoId).update(
            "imagenUrl", portadaUrl,
            "imagenes", nuevasUrls
        ).await()

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}


fun compressToWebP(context: Context, uri: Uri): ByteArray? {
    return try {
        val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.WEBP, 70, outputStream)
        outputStream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
