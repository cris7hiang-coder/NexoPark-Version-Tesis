package com.example.parking.Admin

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.firebase.firestore.DocumentSnapshot
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ----------------- generarReporteCSV (guardar + compartir) -----------------
@Composable
fun ReporteReservasPdfSheet(
    file: File,
    onCompartir: () -> Unit,
    onCerrar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Text(
            text = "PDF generado",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onCompartir,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text("Compartir PDF")
        }

        OutlinedButton(
            onClick = onCerrar,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Cerrar")
        }
    }
}
fun generarReporteReservasPdfFile(
    context: Context,
    reservas: List<DocumentSnapshot>,
    tipoVehiculoPorReserva: Map<String, String>,
    tipoPagoPorReserva: Map<String, String>
): File? {
    if (reservas.isEmpty()) {
        Toast.makeText(
            context,
            "No hay reservas para exportar",
            Toast.LENGTH_SHORT
        ).show()
        return null
    }

    val pdfDocument = PdfDocument()

    val pageWidth = 595
    val pageHeight = 842
    val margin = 40f
    val bottomLimit = pageHeight - 72f

    var pageNumber = 1
    var y = 50f

    val primary = android.graphics.Color.rgb(31, 59, 77)
    val primaryDark = android.graphics.Color.rgb(22, 45, 61)
    val text = android.graphics.Color.rgb(46, 46, 46)
    val muted = android.graphics.Color.rgb(107, 114, 128)
    val line = android.graphics.Color.rgb(229, 231, 235)
    val soft = android.graphics.Color.rgb(243, 246, 248)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryDark
        textSize = 21f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = text
        textSize = 13.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = text
        textSize = 9f
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 9.5f
    }

    val metricValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryDark
        textSize = 17f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = line
        strokeWidth = 1f
    }

    fun money(value: Double): String {
        return "%.2f".format(Locale.US, value)
    }

    fun normalizarEstado(estado: String?): String {
        return estado
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?: "desconocido"
    }

    fun estadoVisual(estado: String?): String {
        return when (normalizarEstado(estado)) {
            "finalizada" -> "Finalizada"
            "activa" -> "Activa"
            "pendiente" -> "Pendiente"
            "expirada" -> "Expirada"
            "cancelada" -> "Cancelada"
            else -> "Desconocido"
        }
    }

    fun tipoPagoVisual(tipo: String?): String {
        return when (tipo?.trim()?.lowercase(Locale.ROOT)) {
            "fisico", "físico", "efectivo", "cash" -> "Físico"
            "digital", "qr", "transferencia", "transferencia_qr", "pago_qr" -> "Digital"
            null, "", "desconocido" -> "Sin registrar"
            else -> tipo.replaceFirstChar { it.uppercase() }
        }
    }

    fun tipoVehiculoVisual(tipo: String?): String {
        return when (tipo?.trim()?.lowercase(Locale.ROOT)) {
            "auto" -> "Auto"
            "moto" -> "Moto"
            "camion", "camión" -> "Camión"
            "otros", null, "" -> "Otros"
            else -> tipo.replaceFirstChar { it.uppercase() }
        }
    }

    fun fechaPrincipal(doc: DocumentSnapshot): Date? {
        return doc.getTimestamp("fechaInicio")?.toDate()
            ?: doc.getTimestamp("horaLlegada")?.toDate()
            ?: doc.getTimestamp("horaSalida")?.toDate()
            ?: doc.getTimestamp("createdAt")?.toDate()
    }

    fun fechaTexto(doc: DocumentSnapshot): String {
        return fechaPrincipal(doc)?.let {
            SimpleDateFormat("dd/MM/yyyy", Locale("es")).format(it)
        } ?: "-"
    }

    fun costoReserva(doc: DocumentSnapshot): Double {
        return doc.getDouble("costoFinal")
            ?: doc.getDouble("costoEstimado")
            ?: 0.0
    }

    fun nuevaPagina(): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(
            pageWidth,
            pageHeight,
            pageNumber
        ).create()

        return pdfDocument.startPage(pageInfo)
    }

    var page = nuevaPagina()
    var canvas = page.canvas

    fun drawFooter() {
        canvas.drawLine(
            margin,
            pageHeight - 45f,
            pageWidth - margin,
            pageHeight - 45f,
            linePaint
        )

        canvas.drawText(
            "Sistema de parqueo - Reporte administrativo",
            margin,
            pageHeight - 24f,
            labelPaint
        )

        canvas.drawText(
            "Página $pageNumber",
            pageWidth - margin - 56f,
            pageHeight - 24f,
            labelPaint
        )
    }

    fun cerrarPaginaActual() {
        drawFooter()
        pdfDocument.finishPage(page)
    }

    fun crearNuevaPagina() {
        cerrarPaginaActual()

        pageNumber++

        page = nuevaPagina()
        canvas = page.canvas
        y = 50f
    }

    fun ensureSpace(required: Float) {
        if (y + required > bottomLimit) {
            crearNuevaPagina()
        }
    }

    fun drawHeader() {
        canvas.drawText(
            "REPORTE DE RESERVAS",
            margin,
            y,
            titlePaint
        )

        y += 26f

        val generado = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale("es")
        ).format(Date())

        canvas.drawText(
            "Generado: $generado",
            margin,
            y,
            labelPaint
        )

        y += 22f

        canvas.drawLine(
            margin,
            y,
            pageWidth - margin,
            y,
            linePaint
        )

        y += 24f
    }

    fun drawMetricCard(
        x: Float,
        title: String,
        value: String
    ) {
        val cardWidth = 158f
        val cardHeight = 62f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = soft
            style = Paint.Style.FILL
        }

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = line
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        canvas.drawRoundRect(
            x,
            y,
            x + cardWidth,
            y + cardHeight,
            12f,
            12f,
            bgPaint
        )

        canvas.drawRoundRect(
            x,
            y,
            x + cardWidth,
            y + cardHeight,
            12f,
            12f,
            borderPaint
        )

        canvas.drawText(
            title,
            x + 12f,
            y + 21f,
            labelPaint
        )

        canvas.drawText(
            value,
            x + 12f,
            y + 47f,
            metricValuePaint
        )
    }

    fun drawResumen() {
        val total = reservas.size

        val finalizadas = reservas.count {
            normalizarEstado(it.getString("estado")) == "finalizada"
        }

        val canceladas = reservas.count {
            normalizarEstado(it.getString("estado")) == "cancelada"
        }

        val expiradas = reservas.count {
            normalizarEstado(it.getString("estado")) == "expirada"
        }

        val ingresosTotales = reservas
            .filter {
                normalizarEstado(it.getString("estado")) == "finalizada"
            }
            .sumOf { costoReserva(it) }

        canvas.drawText(
            "Resumen del periodo filtrado",
            margin,
            y,
            sectionPaint
        )

        y += 14f

        drawMetricCard(
            x = margin,
            title = "Reservas",
            value = total.toString()
        )

        drawMetricCard(
            x = margin + 174f,
            title = "Finalizadas",
            value = finalizadas.toString()
        )

        drawMetricCard(
            x = margin + 348f,
            title = "Ingresos",
            value = "Bs ${money(ingresosTotales)}"
        )

        y += 78f

        drawMetricCard(
            x = margin,
            title = "Canceladas",
            value = canceladas.toString()
        )

        drawMetricCard(
            x = margin + 174f,
            title = "Expiradas",
            value = expiradas.toString()
        )

        y += 82f
    }

    fun drawTableHeader() {
        val tableWidth = pageWidth - margin * 2
        val headerHeight = 30f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primary
            style = Paint.Style.FILL
        }

        canvas.drawRoundRect(
            margin,
            y,
            margin + tableWidth,
            y + headerHeight,
            10f,
            10f,
            bgPaint
        )

        canvas.drawText("Fecha", margin + 8f, y + 20f, headerPaint)
        canvas.drawText("Reserva", margin + 76f, y + 20f, headerPaint)
        canvas.drawText("Estado", margin + 202f, y + 20f, headerPaint)
        canvas.drawText("Vehículo", margin + 288f, y + 20f, headerPaint)
        canvas.drawText("Pago", margin + 370f, y + 20f, headerPaint)
        canvas.drawText("Monto", margin + 444f, y + 20f, headerPaint)

        y += headerHeight
    }

    fun drawReservaRow(
        index: Int,
        doc: DocumentSnapshot
    ) {
        val rowHeight = 29f
        val tableWidth = pageWidth - margin * 2

        ensureSpace(rowHeight + 12f)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (index % 2 == 0) {
                android.graphics.Color.WHITE
            } else {
                android.graphics.Color.rgb(250, 250, 250)
            }
            style = Paint.Style.FILL
        }

        canvas.drawRect(
            margin,
            y,
            margin + tableWidth,
            y + rowHeight,
            bgPaint
        )

        val reservaId = doc.id.take(14)
        val estado = estadoVisual(doc.getString("estado"))
        val vehiculo = tipoVehiculoVisual(tipoVehiculoPorReserva[doc.id])
        val pago = tipoPagoVisual(tipoPagoPorReserva[doc.id])
        val monto = costoReserva(doc)

        canvas.drawText(
            fechaTexto(doc),
            margin + 8f,
            y + 19f,
            bodyPaint
        )

        canvas.drawText(
            reservaId,
            margin + 76f,
            y + 19f,
            bodyPaint
        )

        canvas.drawText(
            estado.take(12),
            margin + 202f,
            y + 19f,
            bodyPaint
        )

        canvas.drawText(
            vehiculo.take(12),
            margin + 288f,
            y + 19f,
            bodyPaint
        )

        canvas.drawText(
            pago.take(12),
            margin + 370f,
            y + 19f,
            bodyPaint
        )

        canvas.drawText(
            "Bs ${money(monto)}",
            margin + 444f,
            y + 19f,
            bodyPaint
        )

        canvas.drawLine(
            margin,
            y + rowHeight,
            margin + tableWidth,
            y + rowHeight,
            linePaint
        )

        y += rowHeight
    }

    try {
        drawHeader()
        drawResumen()

        canvas.drawText(
            "Detalle de reservas",
            margin,
            y,
            sectionPaint
        )

        y += 14f

        drawTableHeader()

        reservas.forEachIndexed { index, doc ->
            if (y + 42f > bottomLimit) {
                crearNuevaPagina()
                drawHeader()

                canvas.drawText(
                    "Detalle de reservas",
                    margin,
                    y,
                    sectionPaint
                )

                y += 14f

                drawTableHeader()
            }

            drawReservaRow(
                index = index,
                doc = doc
            )
        }

        cerrarPaginaActual()

        val file = File(
            context.cacheDir,
            "reporte_reservas_${System.currentTimeMillis()}.pdf"
        )

        FileOutputStream(file).use { output ->
            pdfDocument.writeTo(output)
        }

        return file

    } catch (e: Exception) {
        Toast.makeText(
            context,
            e.message ?: "No se pudo generar el PDF",
            Toast.LENGTH_LONG
        ).show()

        return null

    } finally {
        pdfDocument.close()
    }
}
fun compartirReportePdf(
    context: Context,
    file: File
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            "Compartir reporte PDF"
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}