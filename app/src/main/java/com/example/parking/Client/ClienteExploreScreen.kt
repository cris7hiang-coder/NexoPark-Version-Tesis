package com.example.parking.Client

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.parking.Client.ViewParking.ParqueoSharedViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import com.example.parking.Perfil.PerfilDropdown
import com.example.parking.ui.theme.FondoCard
import com.google.firebase.auth.FirebaseAuth






    /*fun ClienteExploreScreen(
    navController: NavHostController,
    sharedViewModel: ParqueoSharedViewModel
) {
    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    val db = FirebaseFirestore.getInstance()

    var userName by remember { mutableStateOf("Usuario") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var filtros by remember { mutableStateOf(FiltrosCliente()) }
    var mostrarFiltro by remember { mutableStateOf(false) }

    // 👉 Cargar datos de usuario
    LaunchedEffect(uid) {
        uid?.let {
            val snapshot = db.collection("users").document(it).get().await()
            userName = snapshot.getString("name") ?: "Usuario"
            photoUrl = snapshot.getString("photoUrl") ?: auth.currentUser?.photoUrl?.toString()
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // 🔹 Encabezado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Hola, $userName",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "¿Dónde quieres parquear hoy?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                PerfilDropdown(photoUrl = photoUrl, navController = navController)
            }

            Spacer(Modifier.height(20.dp))

            // 🔹 Chip de filtros
            FilterChip(
                selected = filtros.tiposVehiculo.isNotEmpty() || filtros.zonaSeleccionada != null || filtros.precioMaximo != null || filtros.soloDisponibles,
                onClick = { mostrarFiltro = true },
                label = {
                    val totalFiltros = filtros.tiposVehiculo.size +
                            (if (filtros.zonaSeleccionada != null) 1 else 0) +
                            (if (filtros.precioMaximo != null) 1 else 0) +
                            (if (filtros.soloDisponibles) 1 else 0)

                    Text(
                        if (totalFiltros == 0) "Filtrar" else "Filtrado ($totalFiltros)",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (totalFiltros == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AzulPetroleoSuave,
                    selectedLabelColor = MaterialTheme.colorScheme.secondary,
                    containerColor = FondoCard
                )
            )

            Spacer(Modifier.height(20.dp))

            // 🔹 Aquí podrías añadir el mapa o lista de parqueos
            Text(
                "🔍 Resultados de parqueos irían aquí...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        // 🔹 Modal de filtros
        if (mostrarFiltro) {
            ModalBottomSheet(
                onDismissRequest = { mostrarFiltro = false },
                containerColor = FondoCard,
                sheetState = bottomSheetState,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                FiltroParqueoModal(
                    filtros = filtros,
                    zonas = listOf("Zona A", "Zona B", "Zona C"),
                    onAplicar = {
                        filtros = it
                        mostrarFiltro = false
                    },
                    onCancelar = { mostrarFiltro = false }
                )
            }
        }
    }
}*/