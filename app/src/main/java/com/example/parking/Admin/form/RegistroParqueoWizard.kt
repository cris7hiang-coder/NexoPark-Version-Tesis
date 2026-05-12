package com.example.parking.Admin.form

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegistroParqueoWizard(
    onFinishRegistro: () -> Unit = {}
) {
    var pasoActual by rememberSaveable { mutableIntStateOf(1) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    var tiposSeleccionados by remember { mutableStateOf<List<String>>(emptyList()) }
    var caracteristicasSeleccionadas by remember { mutableStateOf<List<String>>(emptyList()) }
    var reglasSeleccionadas by remember { mutableStateOf<List<String>>(emptyList()) }

    if (uid == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Usuario no autenticado",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Inicia sesión para continuar con el registro del parqueo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        return
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            RegistroWizardHeader(
                pasoActual = pasoActual,
                totalPasos = 7
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                when (pasoActual) {
                    1 -> RegistrarParqueoForm(
                        uid = uid,
                        onNext = { pasoActual = 2 }
                    )

                    2 -> RegistrarMapsScreen(
                        onLocationSelected = { latLng, direccion, zona ->
                            db.collection("parqueos")
                                .document(uid)
                                .update(
                                    "latitud", latLng.latitude,
                                    "longitud", latLng.longitude,
                                    "direccion", direccion,
                                    "zona", zona
                                )
                                .addOnSuccessListener {
                                    pasoActual = 3
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al guardar ubicación",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        },
                        onBack = { pasoActual = 1 }
                    )

                    3 -> SeleccionTipoVehiculo(
                        onTiposSeleccionados = { tipos ->
                            tiposSeleccionados = tipos
                            pasoActual = 4
                        },
                        onBack = { pasoActual = 2 }
                    )

                    4 -> CapacidadesYTarifasDinamico(
                        tiposSeleccionados = tiposSeleccionados,
                        onBack = { pasoActual = 3 }
                    ) { capacidades, tarifas ->
                        db.collection("parqueos")
                            .document(uid)
                            .update(
                                "tiposVehiculo", tiposSeleccionados,
                                "capacidades", capacidades,
                                "tarifas", tarifas
                            )
                            .addOnSuccessListener {
                                pasoActual = 5
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    context,
                                    "Error al guardar datos",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }

                    5 -> RegistroCaracteristicasScreen(
                        caracteristicasIniciales = caracteristicasSeleccionadas,
                        reglasIniciales = reglasSeleccionadas,
                        onBack = { pasoActual = 4 },
                        onNext = { caracteristicas, reglas ->
                            caracteristicasSeleccionadas = caracteristicas
                            reglasSeleccionadas = reglas

                            db.collection("parqueos")
                                .document(uid)
                                .update(
                                    mapOf(
                                        "caracteristicas" to caracteristicas,
                                        "reglas" to reglas
                                    )
                                )
                                .addOnSuccessListener {
                                    pasoActual = 6
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al guardar características",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    )

                    6 -> RegistroToleranciaScreen(
                        onBack = { pasoActual = 5 },
                        onNext = { minutos ->
                            db.collection("parqueos")
                                .document(uid)
                                .update("toleranciaMin", minutos)
                                .addOnSuccessListener {
                                    pasoActual = 7
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Error al guardar tolerancia",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    )

                    7 -> SubirImagenParqueoScreen(
                        parqueoId = uid,
                        onFinish = {
                            pasoActual = 1
                            onFinishRegistro()
                        },
                        onSkip = {
                            pasoActual = 1
                            onFinishRegistro()
                        }
                    )
                }
            }
        }
    }
}
@Composable
private fun RegistroWizardHeader(
    pasoActual: Int,
    totalPasos: Int
) {
    val progreso = (pasoActual.toFloat() / totalPasos.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Configuración del parqueo",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Completa los pasos para dejar listo tu parqueo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paso $pasoActual de $totalPasos",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = tituloPasoRegistro(pasoActual),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
private fun tituloPasoRegistro(paso: Int): String {
    return when (paso) {
        1 -> "Información general"
        2 -> "Ubicación"
        3 -> "Tipos de vehículo"
        4 -> "Capacidades y tarifas"
        5 -> "Características y reglas"
        6 -> "Tolerancia"
        7 -> "Imagen del parqueo"
        else -> "Configuración"
    }
}
