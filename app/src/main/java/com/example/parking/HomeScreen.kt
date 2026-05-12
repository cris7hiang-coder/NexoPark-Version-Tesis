package com.example.parking


import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    Text("¡Bienvenido a la pantalla Home!")
}
