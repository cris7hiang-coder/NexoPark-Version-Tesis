package com.example.parking.Perfil

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.parking.Login.AppTextField
import com.example.parking.Login.PrimaryButton
import com.example.parking.ui.theme.MinimalTopBarCompact
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Perfil(navController: NavController) {

    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var emergencyNumber by remember { mutableStateOf("") }
    var infoAdicional by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf<String?>(null) }

    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val currentUser = user ?: return@rememberLauncherForActivityResult

        uri?.let {
            isUploading = true

            val ref = storage.reference.child("profile_images/${currentUser.uid}.jpg")

            ref.putFile(it)
                .continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { url ->

                    photoUrl = url.toString()

                    db.collection("users")
                        .document(currentUser.uid)
                        .update("photoUrl", photoUrl)

                    currentUser.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setPhotoUri(url)
                            .build()
                    )

                    isUploading = false
                }
                .addOnFailureListener {
                    isUploading = false
                    Toast.makeText(context, "Error al subir imagen", Toast.LENGTH_SHORT).show()
                }
        }
    }

    LaunchedEffect(user?.uid) {
        user?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    name = doc.getString("name") ?: ""
                    phone = doc.getString("phone") ?: ""
                    emergencyNumber = doc.getString("emergencyNumber") ?: ""
                    infoAdicional = doc.getString("infoAdicional") ?: ""
                    photoUrl = doc.getString("photoUrl") ?: user.photoUrl?.toString()
                }
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
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {

            Spacer(Modifier.height(20.dp))

            PerfilHeader()

            Spacer(Modifier.height(28.dp))

            // 🔷 Avatar
            ProfileAvatar(
                photoUrl = photoUrl,
                isUploading = isUploading,
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(28.dp))

            // 🔷 Card principal
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    SectionTitle("Información personal")

                    PerfilCampo(
                        icon = Icons.Default.Person,
                        label = "Nombre",
                        value = name,
                        enabled = false
                    )

                    PerfilCampo(
                        icon = Icons.Default.Phone,
                        label = "Teléfono",
                        value = phone,
                        enabled = false
                    )

                    Spacer(Modifier.height(8.dp))

                    SectionTitle("Contacto de emergencia")

                    PerfilCampo(
                        icon = Icons.Default.Warning,
                        label = "Número de emergencia",
                        value = emergencyNumber,
                        onValueChange = { emergencyNumber = it }
                    )

                    PerfilCampo(
                        icon = Icons.Default.Info,
                        label = "Información adicional",
                        value = infoAdicional,
                        onValueChange = { infoAdicional = it },
                        singleLine = false,
                        maxLines = 4,
                        minHeight = 100.dp
                    )

                    Spacer(Modifier.height(12.dp))

                    PrimaryButton(
                        text = "Guardar cambios",
                        onClick = {
                            user?.uid?.let { uid ->
                                db.collection("users").document(uid).update(
                                    mapOf(
                                        "phone" to phone,
                                        "emergencyNumber" to emergencyNumber,
                                        "infoAdicional" to infoAdicional,
                                        "photoUrl" to photoUrl
                                    )
                                ).addOnSuccessListener {
                                    Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
@Composable
fun ProfileAvatar(
    photoUrl: String?,
    isUploading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(110.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() },
        contentAlignment = Alignment.BottomEnd
    ) {
        if (!photoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            )
        }

        if (isUploading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                strokeWidth = 2.dp
            )
        }

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}
@Composable
fun PerfilCampo(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: ((String) -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    minHeight: Dp = 56.dp
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange?.invoke(it) },
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,

            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),

            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),

            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),

            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),

            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
@Composable
fun PerfilHeader() {
    Column {
        Text(
            text = "Mi perfil",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Administra tu información personal y datos de contacto.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
    }
}
@Composable
fun SectionTitle(title: String) {
    Text( text = title,
        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(vertical = 8.dp)
    )
}