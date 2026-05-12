package com.example.parking.Admin.form.Edit

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.parking.Admin.form.CapacidadesYTarifasDinamico
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
@Composable
fun EditarTiposYTarifas(
    parqueo: DocumentSnapshot,
    onBack: () -> Unit

) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = parqueo.id

    val tiposVehiculo = parqueo.get("tiposVehiculo") as? List<String> ?: emptyList()
    val capacidadesRaw = parqueo.get("capacidad") as? Map<String, Any> ?: emptyMap()
    val tarifasRaw = parqueo.get("tarifas") as? Map<String, Map<String, Any>> ?: emptyMap()

    val capacidadesIniciales = capacidadesRaw.mapNotNull { (tipo, valor) ->
        val cantidad = (valor as? Number)?.toInt()
        if (cantidad != null) tipo to cantidad else null
    }.toMap()

    val tarifasIniciales = tarifasRaw.mapValues { (_, innerMap) ->
        innerMap.mapNotNull { (clave, valor) ->
            val k = clave as? String
            val v = when (valor) {
                is Number -> valor.toDouble()
                is String -> valor.toDoubleOrNull()
                else -> null
            }
            if (k != null && v != null) k to v else null
        }.toMap()
    }

    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var tarifasPendientes by remember { mutableStateOf<Map<String, Map<String, Double>>>(emptyMap()) }

    if (mostrarConfirmacion) {
        // 🔹 Empty State estilo advertencia
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "¿Confirmar cambio de tarifas?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Este cambio será guardado inmediatamente en la base de datos.",
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar")
                }
                Button(
                    onClick = {
                        db.collection("parqueos").document(uid)
                            .update("tarifas", tarifasPendientes)
                            .addOnSuccessListener {
                                Toast.makeText(context, "Tarifas actualizadas correctamente", Toast.LENGTH_SHORT).show()
                                mostrarConfirmacion = false
                                onBack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al actualizar tarifas", Toast.LENGTH_SHORT).show()
                            }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Confirmar")
                }
            }
        }
    } else {
        CapacidadesYTarifasDinamico(
            tiposSeleccionados = tiposVehiculo,
            capacidadesIniciales = capacidadesIniciales,
            tarifasIniciales = tarifasIniciales,
            onBack = onBack,
            deshabilitarCapacidad = true // ❌ No se modifica capacidad
        ) { _, nuevasTarifas ->
            tarifasPendientes = nuevasTarifas
            mostrarConfirmacion = true
        }
    }
}
