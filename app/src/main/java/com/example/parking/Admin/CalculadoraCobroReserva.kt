package com.example.parking.Admin
import com.google.firebase.firestore.DocumentSnapshot
object CalculadoraCobroReserva {

    fun calcularMinutosDesdeReserva(reserva: DocumentSnapshot): Int {
        val inicio = reserva.getTimestamp("horaLlegada")?.toDate()
            ?: reserva.getTimestamp("fechaInicio")?.toDate()
            ?: return 0

        return ((System.currentTimeMillis() - inicio.time) / 60000)
            .toInt()
            .coerceAtLeast(0)
    }

    fun calcularCostoPorBloques(
        minutos: Long,
        tarifaMediaHora: Double,
        tarifaHora: Double
    ): Double {
        if (minutos <= 0) return 0.0

        val bloques = ((minutos + 29) / 30).toInt()
        val horasCompletas = bloques / 2
        val mediaHoraExtra = bloques % 2

        return (horasCompletas * tarifaHora) + (mediaHoraExtra * tarifaMediaHora)
    }

    fun calcularCostoFinal(
        reserva: DocumentSnapshot,
        tarifaHora: Double,
        tarifaMediaHora: Double
    ): Double {
        val minutosUsados = calcularMinutosDesdeReserva(reserva).toLong()

        val modoLibre = reserva.getBoolean("modoLibre") ?: true
        val duracionEstimada = reserva.getLong("duracionEstimadaMin") ?: 0L
        val toleranciaMin = reserva.getLong("toleranciaMin") ?: 10L
        val costoEstimado = reserva.getDouble("costoEstimado") ?: 0.0

        return if (modoLibre) {
            calcularCostoPorBloques(
                minutos = minutosUsados,
                tarifaMediaHora = tarifaMediaHora,
                tarifaHora = tarifaHora
            )
        } else {
            when {
                minutosUsados <= duracionEstimada -> costoEstimado
                minutosUsados <= duracionEstimada + toleranciaMin -> costoEstimado
                else -> {
                    val exceso = minutosUsados - (duracionEstimada + toleranciaMin)
                    costoEstimado + calcularCostoPorBloques(
                        minutos = exceso,
                        tarifaMediaHora = tarifaMediaHora,
                        tarifaHora = tarifaHora
                    )
                }
            }
        }
    }
}