package com.example.parking.Admin.form.Edit

import androidx.compose.foundation.lazy.grid.items // <- ESTE es el bueno

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.parking.Admin.form.RegistroToleranciaScreen
import com.example.parking.ui.theme.AzulPetroleo
import com.example.parking.ui.theme.EditEmptyState
import com.example.parking.ui.theme.EmptyStateView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
@Composable
fun EditarParqueoScreen(parqueoId: String, onVolver: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    var parqueo by remember { mutableStateOf<DocumentSnapshot?>(null) }
    var seccionSeleccionada by remember { mutableStateOf<String?>(null) }
    var tiempoToleranciaMin by remember { mutableStateOf(10) }

    // Prompt
    var showPrompt by remember { mutableStateOf(false) }
    var promptTitle by remember { mutableStateOf("") }
    var promptMessage by remember { mutableStateOf("") }
    var onPromptConfirm by remember { mutableStateOf<() -> Unit>({}) }

    LaunchedEffect(parqueoId) {
        db.collection("parqueos").document(parqueoId).get()
            .addOnSuccessListener { doc ->
                parqueo = doc
                // Inicializar tolerancia
                tiempoToleranciaMin = (doc.getLong("tiempoTolerancia")?.toInt() ?: 10)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar datos", Toast.LENGTH_SHORT).show()
            }
    }
    parqueo?.let {
        when (seccionSeleccionada) {
            "datos" -> EditarDatosBasicosForm(parqueo = it, onBack = { seccionSeleccionada = null })
            "ubicacion" -> EditarUbicacionParqueo(parqueo = it, onBack = { seccionSeleccionada = null })
            "tipos" -> EditarTiposYTarifas(parqueo = it, onBack = { seccionSeleccionada = null })
            "imagenes" -> EditarGaleriaParqueo(parqueo = it, onBack = { seccionSeleccionada = null })
            "caracteristicas" -> {
                val caracteristicasActuales = it.get("caracteristicas") as? List<String> ?: emptyList()
                val reglasActuales = it.get("reglas") as? List<String> ?: emptyList()
                EditarCaracteristicasYReglas(
                    caracteristicasActuales = caracteristicasActuales,
                    reglasActuales = reglasActuales,
                    onBack = { seccionSeleccionada = null },
                    onGuardar = { nuevasCaracteristicas, nuevasReglas ->
                        promptTitle = "Confirmar cambios"
                        promptMessage = "¿Deseas guardar las nuevas características y reglas?"
                        onPromptConfirm = {
                            db.collection("parqueos").document(parqueoId)
                                .update(
                                    "caracteristicas", nuevasCaracteristicas,
                                    "reglas", nuevasReglas
                                )
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Actualizado correctamente", Toast.LENGTH_SHORT).show()
                                    seccionSeleccionada = null
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                                }
                        }
                        showPrompt = true
                    }
                )
            }
            "tolerancia" -> {
                RegistroToleranciaScreen(
                    valorInicial = tiempoToleranciaMin,
                    onBack = { seccionSeleccionada = null },
                    onNext = { nuevoTiempo ->
                        promptTitle = "Confirmar cambios"
                        promptMessage = "¿Deseas guardar el nuevo tiempo de tolerancia de $nuevoTiempo minutos?"
                        onPromptConfirm = {
                            db.collection("parqueos").document(parqueoId)
                                .update("tiempoTolerancia", nuevoTiempo)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        context,
                                        "Tiempo de tolerancia actualizado a $nuevoTiempo min",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Actualizamos el estado compartido
                                    tiempoToleranciaMin = nuevoTiempo
                                    seccionSeleccionada = null
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "Error al actualizar: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        }
                        showPrompt = true
                    }
                )
            }
            else -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Panel de Edición de Parqueo",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val botones = listOf(
                        Triple("Datos Básicos", Icons.Default.Info, Color(0xFF1F3B4D)) to "datos",
                        Triple("Ubicación", Icons.Default.Place, Color(0xFF1F3B4D)) to "ubicacion",
                        Triple("Tipos y Tarifas", Icons.Default.List, Color(0xFF1F3B4D)) to "tipos",
                        Triple("Galería de Imágenes", Icons.Default.Photo, Color(0xFF1F3B4D)) to "imagenes",
                        Triple("Características y Reglas", Icons.Default.Checklist, Color(0xFF1F3B4D)) to "caracteristicas",
                        Triple("Tiempo de Tolerancia", Icons.Default.Timer, Color(0xFF1F3B4D)) to "tolerancia" // Nueva sección
                            //  Triple("QR del Parqueo", Icons.Default.QrCode, Color(0xFF1F3B4D)) to "qr",
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(botones) { (data, seccion) ->
                            ActionCardButtonMinimal(
                                icon = data.second,
                                text = data.first,
                                iconColor = data.third
                            ) {
                                promptTitle = "Confirmación"
                                promptMessage = "¿Deseas ingresar a la sección \"${data.first}\"?"
                                onPromptConfirm = { seccionSeleccionada = seccion }
                                showPrompt = true
                            }
                        }
                    }
                        Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onVolver,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Volver")
                    }
                }
            }
        }
    }

    if (showPrompt) {
        EditEmptyState(
            title = promptTitle,
            message = promptMessage,
            icon = Icons.Default.Warning,
            onActionClick = {
                showPrompt = false
                onPromptConfirm()
            },
            actionLabel = "Sí, continuar"
        )
    }
}

@Composable
fun ActionCardButtonMinimal(
    icon: ImageVector,
    text: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text,
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
