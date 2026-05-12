package com.example.parking.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MinimalTopBarCompact(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(35.dp) // Reducir la altura de la barra
            .background(backgroundColor)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(start = 8.dp)
                .align(Alignment.CenterStart) // Mantener la flecha a la izquierda
                .offset(y = 10.dp) // Ajuste para bajar la flecha un poco
                .size(40.dp) // Tamaño compacto del botón
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = iconTint,
                modifier = Modifier.size(24.dp) // Ajuste al tamaño de la flecha
            )
        }
    }
}

