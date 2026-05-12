package com.example.parking.Client.CarClient

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.parking.Login.PrimaryButton
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
@Composable
fun MiVehiculoScreen(navController: NavController) {

    val context = LocalContext.current
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val db = FirebaseFirestore.getInstance()

    var vehiculos by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(uid) {
        db.collection("users")
            .document(uid)
            .collection("vehiculos")
            .get()
            .addOnSuccessListener {
                vehiculos = it.documents
                loading = false
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al cargar vehículos", Toast.LENGTH_SHORT).show()
                loading = false
            }
    }

    Scaffold(
        topBar = {
            MinimalTopBarCompact(onBack = { navController.popBackStack() })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {

            Spacer(Modifier.height(16.dp))

            ScreenHeader(
                title = "Mis vehículos",
                subtitle = "Administra tus vehículos registrados"
            )

            Spacer(Modifier.height(24.dp))

            when {
                loading -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                vehiculos.isEmpty() -> {
                    EmptyVehiculosState {
                        navController.navigate("registrar_vehiculo")
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        vehiculos.forEach { vehiculo ->
                            VehiculoCardModern(vehiculo) {
                                val id = vehiculo.id
                                val data = vehiculo.data ?: emptyMap()

                                navController.currentBackStackEntry?.savedStateHandle?.set("vehiculoId", id)
                                navController.currentBackStackEntry?.savedStateHandle?.set("vehiculoDetalle", data)

                                navController.navigate("detalle_vehiculo")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
@Composable
fun EmptyVehiculosState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(42.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "No tienes vehículos registrados",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(20.dp))

        PrimaryButton(
            text = "Registrar vehículo",
            onClick = onAdd,
            modifier = Modifier.width(220.dp)
        )
    }
}
@Composable
fun VehiculoCardModern(
    vehiculo: DocumentSnapshot,
    onClick: () -> Unit
) {
    val alias = vehiculo.getString("nombreVehiculo")?.takeIf { it.isNotBlank() }
    val placa = vehiculo.getString("placa") ?: ""

    val tipoRaw = vehiculo.getString("tipo") ?: "auto"
    val vehiculoUI = mapVehiculo(tipoRaw)

    val imagenUrl = vehiculo.getString("imagenUrl")

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (!imagenUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = imagenUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = vehiculoUI.icon,
                        contentDescription = vehiculoUI.label,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {

                    Column(modifier = Modifier.padding(16.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically) {

                            Spacer(Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {

                                Text(
                                    text = alias ?: placa,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (alias != null) {
                                    Text(
                                        text = "Placa: $placa",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                            }
                        }
                    }


                }}}}}


data class VehiculoUI(
    val icon: ImageVector,
    val label: String
)

fun mapVehiculo(tipo: String): VehiculoUI {
    return when (tipo.lowercase()) {
        "auto" -> VehiculoUI(Icons.Default.DirectionsCar, "Auto")
        "moto" -> VehiculoUI(Icons.Default.TwoWheeler, "Moto")
        "camion" -> VehiculoUI(Icons.Default.LocalShipping, "Camión")
        else -> VehiculoUI(Icons.Default.HelpOutline, "Otro")
    }
}
