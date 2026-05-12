package com.example.parking.Client

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState

class ClienteMapaViewModel : ViewModel() {

    private val posicionInicial = CameraPosition.fromLatLngZoom(
        LatLng(-16.2902, -63.5887),
        6f
    )

    var cameraPositionState by mutableStateOf(
        CameraPositionState(position = posicionInicial)
    )
        private set
}
