package com.example.parking.Admin

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import java.io.Serializable


data class EspacioParqueo(
    val tipoVehiculo: String = "",
    val indice: Int = 0,
    val estado: EstadoEspacio = EstadoEspacio.DISPONIBLE
) : Serializable

enum class EstadoEspacio {
   DISPONIBLE, RESERVADO, OCUPADO
}

