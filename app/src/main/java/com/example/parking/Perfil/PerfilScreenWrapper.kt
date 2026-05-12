package com.example.parking.ui.screens.perfil

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.parking.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PerfilScreenWrapper(
    onLogout: () -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("Cargando...") }
    var email by remember { mutableStateOf("...") }
    var phone by remember { mutableStateOf("...") }
    var photoUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(auth.currentUser?.uid) {
        val user = auth.currentUser

        if (user == null) {
            name = "Invitado"
            email = "No autenticado"
            phone = "-"
            photoUrl = null
            isLoading = false
            return@LaunchedEffect
        }

        try {
            email = user.email ?: "Correo no disponible"

            val doc = db.collection("users")
                .document(user.uid)
                .get()
                .await()

            name = doc.getString("name") ?: "Usuario"
            phone = doc.getString("phone") ?: "No proporcionado"
            photoUrl = doc.getString("photoUrl") ?: user.photoUrl?.toString()
        } catch (e: Exception) {
            name = "Usuario"
            phone = "No disponible"
            photoUrl = user.photoUrl?.toString()
            Toast.makeText(context, "No se pudo cargar el perfil", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    PerfilScreen(
        name = name,
        email = email,
        phone = phone,
        photoUrl = photoUrl,
        isLoading = isLoading,
        onLogout = {
            scope.launch {
                signOutAllSuspend(context)
                Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                onLogout()
            }
        },
        onEditProfile = { navController.navigate("editar_perfil") },
        navController = navController
    )
}
suspend fun signOutAllSuspend(context: Context) {
    val auth = FirebaseAuth.getInstance()

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.WEB_CLIENT_ID))
        .requestEmail()
        .build()

    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    try {
        googleSignInClient.signOut().await()
    } catch (_: Exception) {
    }

    auth.signOut()
}
@Composable
fun PerfilScreen(
    name: String,
    email: String,
    phone: String,
    photoUrl: String?,
    isLoading: Boolean,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    modifier = Modifier.size(78.dp)
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Foto de perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.firstOrNull()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isLoading) "Cargando perfil..." else name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Teléfono: $phone",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        PerfilSectionCard(
            title = "Perfil",
            items = listOf(
                PerfilActionItem(Icons.Default.AccountBox, "Mi perfil") { onEditProfile() }
            )
        )

        PerfilSectionCard(
            title = "Seguridad",
            items = listOf(
                PerfilActionItem(Icons.Default.Lock, "Cambiar contraseña") {
                    navController.navigate("cambiar_contrasena")
                },
                PerfilActionItem(Icons.Default.Security, "Métodos de autenticación") {
                    navController.navigate("autenticacion_verificacion")
                }
            )
        )

        PerfilSectionCard(
            title = "Configuración",
            items = listOf(
                PerfilActionItem(Icons.Default.Notifications, "Notificaciones") {
                    toast(context, "Función por implementar")
                },
                PerfilActionItem(Icons.Default.Settings, "Preferencias de la app") {
                    toast(context, "Función por implementar")
                },
                PerfilActionItem(Icons.Default.Language, "Idioma y país") {
                    toast(context, "Función por implementar")
                }
            )
        )

        PerfilSectionCard(
            title = "Asistencia",
            items = listOf(
                PerfilActionItem(Icons.Default.SupportAgent, "Soporte") {
                    toast(context, "Función por implementar")
                },
                PerfilActionItem(Icons.Default.PrivacyTip, "Política y privacidad") {
                    toast(context, "Función por implementar")
                },
                PerfilActionItem(Icons.Default.Gavel, "Términos legales") {
                    toast(context, "Función por implementar")
                }
            )
        )

        PerfilSectionCard(
            title = "Cuenta",
            items = listOf(
                PerfilActionItem(Icons.Default.History, "Historial de reservas") {
                    navController.navigate("historial_reservas")
                },
                PerfilActionItem(Icons.Default.CreditCard, "Métodos de pago") {
                    toast(context, "Función por implementar")
                }
            )
        )

        PerfilSectionCard(
            title = "Sesión",
            items = listOf(
                PerfilActionItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    label = "Cerrar sesión",
                    color = MaterialTheme.colorScheme.error,
                    onClick = onLogout
                )
            )
        )
    }
}
fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
@Composable
fun PerfilItemRow(
    icon: ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(21.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        )
    }
}
@Composable
fun PerfilSectionCard(
    title: String,
    items: List<PerfilActionItem>
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    PerfilItemRow(
                        icon = item.icon,
                        label = item.label,
                        color = item.color ?: MaterialTheme.colorScheme.onSurface,
                        onClick = item.onClick
                    )

                    if (index < items.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.6.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
data class PerfilActionItem(
    val icon: ImageVector,
    val label: String,
    val color: Color? = null,
    val onClick: () -> Unit
)