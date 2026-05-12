package com.example.parking.Client.ViewParking.CarReserv
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parking.Admin.EspacioParqueo
import com.example.parking.Admin.EstadoEspacio
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeleccionarEspacioScreen(
    parqueoId: String,
    tipoVehiculo: String,
    navController: NavController
) {
    val context = LocalContext.current

    var espacios by remember { mutableStateOf<List<EspacioParqueo>>(emptyList()) }
    var capacidad by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var cargando by remember { mutableStateOf(true) }

    // Cargar datos reales
    LaunchedEffect(Unit) {
        cargando = true
        try {
            val db = FirebaseFirestore.getInstance()

            // Capacidad del parqueo
            val parqueoSnap = db.collection("parqueos").document(parqueoId).get().await()
            val capacidadRaw = parqueoSnap.get("capacidades")
                ?: parqueoSnap.get("capacidad")

            val capMap = capacidadRaw as? Map<*, *> ?: emptyMap<Any, Any>()

            capacidad = capMap.mapNotNull { (key, value) ->
                val tipo = key?.toString()
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    ?: return@mapNotNull null

                val cantidad = when (value) {
                    is Long -> value.toInt()
                    is Int -> value
                    is Double -> value.toInt()
                    is Number -> value.toInt()
                    else -> 0
                }

                tipo to cantidad
            }.toMap()

            if (!capacidad.containsKey(tipoVehiculo)) {
                Toast.makeText(context, "Este parqueo no tiene espacios para $tipoVehiculo", Toast.LENGTH_LONG).show()
                navController.popBackStack()
                return@LaunchedEffect
            }

            // Espacios existentes
            val snapshot = db.collection("parqueos")
                .document(parqueoId)
                .collection("espacios")
                .whereEqualTo("tipoVehiculo", tipoVehiculo)
                .get().await()


            espacios = snapshot.documents.mapNotNull { doc ->
                val tipo = doc.getString("tipoVehiculo") ?: return@mapNotNull null
                val indice = (doc.getLong("indice") ?: return@mapNotNull null).toInt()
                val estado = when (doc.getString("estado")) {
                    "OCUPADO" -> EstadoEspacio.OCUPADO
                    "RESERVADO" -> EstadoEspacio.RESERVADO
                    else -> EstadoEspacio.DISPONIBLE
                }
                EspacioParqueo(tipo, indice, estado)
            }

        } catch (e: Exception) {
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        } finally {
            cargando = false
        }
    }

    Scaffold(
        topBar = {
            MinimalTopBarCompact(onBack = { navController.popBackStack() })
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                text = "Elegir espacio",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))

            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                EspacioParqueoClienteScreen(
                    tipoVehiculo = tipoVehiculo,
                    capacidad = capacidad,
                    espacios = espacios,
                        onEspacioClick = { tipo, indice ->
                            val espacio = espacios.find {
                                it.tipoVehiculo == tipo && it.indice == indice
                            } ?: EspacioParqueo(
                                tipoVehiculo = tipo,
                                indice = indice,
                                estado = EstadoEspacio.DISPONIBLE
                            )

                            if (espacio.estado != EstadoEspacio.DISPONIBLE) {
                                Toast.makeText(
                                    context,
                                    "Este espacio no está disponible",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@EspacioParqueoClienteScreen
                            }

                            navController.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("espacioSeleccionado", espacio)

                            navController.popBackStack()

                    }
                )
            }
        }
    }
}
