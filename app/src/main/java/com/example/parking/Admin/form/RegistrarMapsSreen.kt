package com.example.parking.Admin.form

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.parking.Login.PrimaryButton
import com.example.parking.ui.theme.TextoSecundario
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.IOException
import java.util.Locale

@Composable
fun RegistrarMapsScreen(
    initialLocation: LatLng? = null,
    onLocationSelected: (LatLng, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    val geocoder = remember {
        Geocoder(context, Locale.getDefault())
    }

    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedLocation by remember { mutableStateOf(initialLocation) }

    var direccion by remember {
        mutableStateOf("Ubicación no seleccionada")
    }

    var zona by remember {
        mutableStateOf("Selecciona un punto en el mapa")
    }

    var isLoadingDireccion by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (hasPermission) {
            obtenerUbicacionActual(
                context = context,
                fusedLocationClient = fusedLocationClient
            ) { location ->
                currentLocation = location
                selectedLocation = location

                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(location, 16f)
                )

                actualizarDireccionZona(
                    geocoder = geocoder,
                    latLng = location
                ) { dir, zon ->
                    direccion = dir
                    zona = zon
                }
            }
        } else {
            Toast.makeText(
                context,
                "Se necesita permiso de ubicación para continuar",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fine != PackageManager.PERMISSION_GRANTED &&
            coarse != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            hasPermission = true

            obtenerUbicacionActual(
                context = context,
                fusedLocationClient = fusedLocationClient
            ) { location ->
                currentLocation = location
                selectedLocation = location

                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(location, 16f)
                )

                actualizarDireccionZona(
                    geocoder = geocoder,
                    latLng = location
                ) { dir, zon ->
                    direccion = dir
                    zona = zon
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasPermission
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            onMapClick = { latLng ->
                selectedLocation = latLng
                isLoadingDireccion = true

                actualizarDireccionZona(
                    geocoder = geocoder,
                    latLng = latLng
                ) { dir, zon ->
                    direccion = dir
                    zona = zon
                    isLoadingDireccion = false
                }
            }
        ) {
            selectedLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "Ubicación seleccionada"
                )
            }
        }

        MapTopBar(
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 16.dp,
                    top = 18.dp
                )
        )

        LocationCard(
            direccion = direccion,
            zona = zona,
            isLoading = isLoadingDireccion,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(
                    top = 72.dp,
                    start = 16.dp,
                    end = 16.dp
                )
        )

        currentLocation?.let { userLocation ->
            FloatingActionButton(
                onClick = {
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(userLocation, 16f)
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 16.dp,
                        bottom = 120.dp
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Mi ubicación"
                )
            }
        }

        BottomButtons(
            onConfirm = {
                selectedLocation?.let {
                    onLocationSelected(
                        it,
                        direccion,
                        zona
                    )
                }
            },
            onCancel = onBack,
            enabledConfirm = selectedLocation != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}
@Composable
private fun MapTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 2.dp
    ) {
        IconButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
@Composable
private fun LocationCard(
    direccion: String,
    zona: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Ubicación del parqueo",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Selecciona en el mapa el punto exacto de acceso.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Actualizando dirección...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = direccion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = zona,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
private fun BottomButtons(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    enabledConfirm: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PrimaryButton(
                text = "Confirmar ubicación",
                icon = Icons.Default.Check,
                onClick = onConfirm,
                enabled = enabledConfirm,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = "Volver",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

fun actualizarDireccionZona(
    geocoder: Geocoder,
    latLng: LatLng,
    onResult: (String, String) -> Unit
) {
    try {
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val dir = addresses[0].getAddressLine(0) ?: "Dirección no disponible"
            val zon = addresses[0].subLocality ?: addresses[0].locality ?: "Zona desconocida"
            onResult(dir, zon)
        } else {
            onResult("Dirección no encontrada", "Zona desconocida")
        }
    } catch (e: IOException) {
        onResult("Error obteniendo dirección", "Zona desconocida")
    }
}

fun obtenerUbicacionActual(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onLocationFound: (LatLng) -> Unit
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationFound(LatLng(location.latitude, location.longitude))
                } else {
                    Toast.makeText(context, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al obtener ubicación: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    } catch (e: SecurityException) {
        Toast.makeText(context, "Permiso de ubicación no concedido", Toast.LENGTH_SHORT).show()
    }
}
