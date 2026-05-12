package com.example.parking.Client.ViewParking
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.example.parking.R
import com.google.android.gms.maps.model.BitmapDescriptor
@Composable
fun MapaInteractivo(
    parqueos: List<ParqueoModel>,
    cameraPositionState: CameraPositionState,
    onParqueoSeleccionado: (ParqueoModel) -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val mapProperties = MapProperties(
        isMyLocationEnabled = hasLocationPermission,
        mapStyleOptions = MapStyleOptions.loadRawResourceStyle(
            context,
            R.raw.map_style_blanco_moderno
        )
    )

    val mapUiSettings = MapUiSettings(
        zoomControlsEnabled = false,
        myLocationButtonEnabled = false,
        tiltGesturesEnabled = false,
        rotationGesturesEnabled = false
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings
        ) {

            parqueos.forEach { parqueo ->

                Marker(
                    state = MarkerState(
                        position = LatLng(
                            parqueo.latitud,
                            parqueo.longitud
                        )
                    ),

                    // ✅ AQUI va el icono, NO arriba en remember
                    icon = vectorToBitmapDescriptor(
                        context = context,
                        vectorResId = R.drawable.ic_pin_marker
                    ),

                    onClick = {
                        haptic.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        onParqueoSeleccionado(parqueo)
                        true
                    }
                )
            }
        }

        FloatingActionButton(
            onClick = {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    return@FloatingActionButton
                }

                isLoading = true

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        isLoading = false

                        if (location == null) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "No se pudo obtener tu ubicación"
                                )
                            }
                            return@addOnSuccessListener
                        }

                        val latLng = LatLng(
                            location.latitude,
                            location.longitude
                        )

                        haptic.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )

                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newLatLngZoom(
                                    latLng,
                                    17f
                                ),
                                durationMs = 900
                            )
                        }
                    }

                    .addOnFailureListener {
                        isLoading = false

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Error obteniendo ubicación"
                            )
                        }
                    }
            },

            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = 96.dp
                ),

            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
fun vectorToBitmapDescriptor(
    context: Context,
    @DrawableRes vectorResId: Int,
    @ColorInt tintColor: Int? = null
): BitmapDescriptor {
    val vectorDrawable = ContextCompat.getDrawable(
        context,
        vectorResId
    ) ?: return BitmapDescriptorFactory.defaultMarker()

    val width = if (vectorDrawable.intrinsicWidth > 0)
        vectorDrawable.intrinsicWidth else 96

    val height = if (vectorDrawable.intrinsicHeight > 0)
        vectorDrawable.intrinsicHeight else 96

    val bitmap = Bitmap.createBitmap(
        width,
        height,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)

    tintColor?.let {
        vectorDrawable.setTint(it)
    }

    vectorDrawable.setBounds(
        0,
        0,
        canvas.width,
        canvas.height
    )

    vectorDrawable.draw(canvas)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}