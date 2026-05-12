package com.example.parking.SupAdmin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
@Composable
fun SuperAdminHomeScreen(viewModel: SuperAdminViewModel = viewModel()) {
    val context = LocalContext.current
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Panel del Superadministrador", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        error?.let {
            Text("Error: $it", color = Color.Red)
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn {
                items(users) { user ->
                    val email = user.getString("email") ?: "Sin email"
                    val role = user.getString("role") ?: "Desconocido"
                    val uid = user.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Email: $email")
                            Text("Rol actual: $role")

                            if (role == "cliente" || role == "admin") {
                                val newRole = if (role == "cliente") "admin" else "cliente"
                                val buttonText = if (role == "cliente") "Convertir en Admin" else "Degradar a Cliente"

                                Button(
                                    onClick = {
                                        viewModel.cambiarRol(
                                            uid,
                                            newRole,
                                            onSuccess = {
                                                Toast.makeText(context, "Rol actualizado a $newRole", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = {
                                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(buttonText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
