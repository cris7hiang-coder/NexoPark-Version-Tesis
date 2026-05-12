package com.example.parking.Client.ViewParking.CarReserv

sealed class Rutas(val ruta: String) {
    object PantallaReserva : Rutas("pantalla_reserva/{parqueoId}")


}
