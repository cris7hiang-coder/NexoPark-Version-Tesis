package com.example.parking.Client.ViewParking.CarReserv

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

suspend fun usuarioPuedeReservar(uid: String): Boolean {
    val db = FirebaseFirestore.getInstance()

    val userDoc = db.collection("users").document(uid).get().await()
    val telefonoVerificado = userDoc.getBoolean("telefonoVerificado") == true

    val vehiculosSnapshot = db.collection("users").document(uid)
        .collection("vehiculos")
        .get().await()
    val tieneVehiculo = !vehiculosSnapshot.isEmpty

    return telefonoVerificado && tieneVehiculo
}
