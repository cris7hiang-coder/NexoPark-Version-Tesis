package com.example.parking.Client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.parking.Client.ViewParking.BottomSheetParqueo
import com.example.parking.Client.ViewParking.MapaInteractivo
import com.example.parking.Client.ViewParking.ParqueoModel
import com.example.parking.Client.ViewParking.ParqueoSharedViewModel
import com.google.maps.android.compose.CameraPositionState
@Composable
fun MapaClienteScreen(
    parqueos: List<ParqueoModel>,
    navController: NavHostController,
    sharedViewModel: ParqueoSharedViewModel,
    cameraPositionState: CameraPositionState
) {
    var parqueoSeleccionado by remember { mutableStateOf<ParqueoModel?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    fun abrirDetalleParqueo(parqueo: ParqueoModel) {
        parqueoSeleccionado = parqueo
        sharedViewModel.onParqueoSeleccionado(parqueo)
        showBottomSheet = true
    }

    fun cerrarBottomSheet() {
        showBottomSheet = false
        parqueoSeleccionado = null
        sharedViewModel.limpiarParqueo()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        MapaInteractivo(
            parqueos = parqueos,
            cameraPositionState = cameraPositionState,
            onParqueoSeleccionado = { parqueo ->
                abrirDetalleParqueo(parqueo)
            }
        )

        AnimatedVisibility(
            visible = showBottomSheet && parqueoSeleccionado != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            parqueoSeleccionado?.let { parqueo ->
                BottomSheetParqueo(
                    parqueoId = parqueo.id,
                    onDismiss = { cerrarBottomSheet() },
                    navController = navController,
                    sharedViewModel = sharedViewModel
                )
            }
        }
    }
}