package com.example.parking.Client.Favorit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parking.Client.ViewParking.ParqueoPreviewCard
import com.example.parking.Client.ViewParking.eliminarDeFavoritos
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritosScreen(
    navController: NavController,
    uid: String
) {
    val db = FirebaseFirestore.getInstance()

    var favoritos by remember { mutableStateOf<List<FavoritoModel>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    DisposableEffect(uid) {
        val listener = db.collection("users")
            .document(uid)
            .collection("favoritos")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->

                if (err != null) {
                    errorMsg = err.message
                    cargando = false
                    return@addSnapshotListener
                }

                favoritos = snap?.documents?.map { doc ->
                    FavoritoModel(
                        id = doc.getString("id") ?: doc.id,
                        nombre = doc.getString("nombre") ?: "",
                        direccion = doc.getString("direccion") ?: "",
                        imagenUrl = doc.getString("imagenUrl") ?: "",
                        latitud = doc.getDouble("latitud") ?: 0.0,
                        longitud = doc.getDouble("longitud") ?: 0.0,
                        timestamp = doc.getTimestamp("timestamp")
                    )
                } ?: emptyList()

                cargando = false
            }

        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            MinimalTopBarCompact(
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Mis favoritos",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        cargando -> "Cargando tus parqueos guardados..."
                        favoritos.isNotEmpty() -> "${favoritos.size} parqueos guardados"
                        else -> "Tus parqueos guardados aparecerán aquí"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                cargando -> {
                    FavoritosLoadingState()
                }

                errorMsg != null -> {
                    FavoritosErrorState(
                        mensaje = errorMsg.orEmpty()
                    )
                }

                favoritos.isEmpty() -> {
                    FavoritosEmptyState(
                        onExplorar = {
                            navController.popBackStack()
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 4.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = favoritos,
                            key = { it.id }
                        ) { favorito ->
                            ParqueoPreviewCard(
                                nombre = favorito.nombre,
                                tarifas = emptyMap(),
                                imagenUrl = favorito.imagenUrl,
                                disponiblesPorTipo = emptyMap(),
                                isFavorito = true,
                                onToggleFavorito = { eliminarDeFavoritos(uid, favorito.id) },
                                onClick = {
                                    navController.navigate("detalle_parqueo/${favorito.id}")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                        }
                    }
                }
            }
        }

@Composable
private fun FavoritosLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cargando favoritos...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun FavoritosEmptyState(
    onExplorar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        horizontal = 24.dp,
                        vertical = 32.dp
                    )
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier.size(74.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Aún no tienes favoritos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Guarda tus parqueos preferidos para acceder más rápido.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onExplorar,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Explorar parqueos")
                }
            }
        }
    }
}
@Composable
private fun FavoritosErrorState(
    mensaje: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(34.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Error cargando favoritos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = mensaje,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
data class FavoritoModel(
    val id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val imagenUrl: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val timestamp: Timestamp? = null
)