package com.example.parking.model

data class Dimensiones(val ancho: Double, val largo: Double)

enum class TipoVehiculoDimension(val dimensiones: Dimensiones) {
    MOTO(Dimensiones(1.50, 2.50)),
    AUTO(Dimensiones(2.50, 5.00)),
    CAMION(Dimensiones(4.30, 18.30))
}

