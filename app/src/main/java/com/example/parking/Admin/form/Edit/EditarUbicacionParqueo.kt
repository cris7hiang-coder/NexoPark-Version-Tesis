package com.example.parking.Admin.form.Edit

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.parking.Admin.form.RegistrarMapsScreen
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun EditarUbicacionParqueo(
    parqueo: DocumentSnapshot,
    onBack: () -> Unit,)
{
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = parqueo.id

    var latLng by remember {
        mutableStateOf(
            LatLng(
                parqueo.getDouble("latitud") ?: 0.0,
                parqueo.getDouble("longitud") ?: 0.0
            )
        )
    }
    var direccion by remember { mutableStateOf(parqueo.getString("direccion") ?: "") }
    var zona by remember { mutableStateOf(parqueo.getString("zona") ?: "") }

    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mostrarAdvertencia by remember { mutableStateOf(true) }

    // Ocultar la advertencia automáticamente después de 3 segundos
    LaunchedEffect(Unit) {
        delay(3000)
        mostrarAdvertencia = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Editar Ubicación", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        // Advertencia temporal
        AnimatedVisibility(visible = mostrarAdvertencia) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Advertencia",
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Estás a punto de modificar la ubicación del parqueo.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57C00),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Este cambio afectará la ubicación que ven los clientes en el mapa.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Mapa normal, interactivo
        RegistrarMapsScreen(
            initialLocation = latLng,
            onLocationSelected = { newLatLng, nuevaDireccion, nuevaZona ->
                latLng = newLatLng
                direccion = nuevaDireccion
                zona = nuevaZona
                mostrarConfirmacion = true
            },
            onBack = onBack
        )
    }

    // Prompt de confirmación al seleccionar nueva ubicación
    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("Confirmar cambio") },
            text = { Text("¿Seguro que deseas actualizar la ubicación del parqueo?") },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("parqueos").document(uid)
                        .update(
                            "latitud", latLng.latitude,
                            "longitud", latLng.longitude,
                            "direccion", direccion,
                            "zona", zona
                        )
                        .addOnSuccessListener {
                            Toast.makeText(context, "Ubicación actualizada", Toast.LENGTH_SHORT).show()
                            mostrarConfirmacion = false
                            onBack()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error al guardar ubicación", Toast.LENGTH_SHORT).show()
                        }
                }) {
                    Text("Sí, actualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
