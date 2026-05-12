package com.example.parking.Admin.form.Edit

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun EditarDatosBasicosForm(
    parqueo: DocumentSnapshot,
    onBack: () -> Unit,

) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val uid = parqueo.id

    var nombre by remember { mutableStateOf(parqueo.getString("nombre") ?: "") }
    var descripcion by remember { mutableStateOf(parqueo.getString("descripcion") ?: "") }
    var encargado by remember { mutableStateOf(parqueo.getString("encargado") ?: "") }
    var telefono by remember { mutableStateOf(parqueo.getString("telefono") ?: "") }

    var showWarning by remember { mutableStateOf(false) }
    var showEmptyState by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título
        Text(
            text = "Editar Datos Básicos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Campos
        val textFieldColors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre del parqueo") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        OutlinedTextField(value = encargado, onValueChange = { encargado = it }, label = { Text("Encargado") }, modifier = Modifier.fillMaxWidth(), colors = textFieldColors)
        OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = textFieldColors)

        Spacer(modifier = Modifier.height(8.dp))

        // Botón Guardar
        Button(
            onClick = {
                showWarning = true
                showEmptyState = true
            },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Guardar cambios")
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Cancelar")
        }
    }

    // Mensaje de advertencia tipo EmptyStateView que desaparece automáticamente
    if (showEmptyState) {
        LaunchedEffect(Unit) {
            delay(3000)
            showEmptyState = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
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
                    Icon(Icons.Default.Warning, contentDescription = "Advertencia", tint = Color(0xFFF57C00), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Estás a punto de modificar los datos del parqueo.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57C00),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Estos cambios se reflejarán inmediatamente en la información del parqueo.",
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
    }

    // Confirmación real antes de actualizar
    if (showWarning) {
        AlertDialog(
            onDismissRequest = { showWarning = false },
            title = { Text("Confirmar cambios", fontWeight = FontWeight.Bold) },
            text = { Text("¿Estás seguro de que deseas actualizar los datos de este parqueo?") },
            confirmButton = {
                TextButton(onClick = {
                    db.collection("parqueos").document(uid).update(
                        mapOf(
                            "nombre" to nombre,
                            "descripcion" to descripcion,
                            "encargado" to encargado,
                            "telefono" to telefono
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(context, "Datos actualizados", Toast.LENGTH_SHORT).show()
                        showWarning = false
                        onBack()
                    }.addOnFailureListener {
                        Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                        showWarning = false
                    }
                }) {
                    Text("Sí, actualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWarning = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
