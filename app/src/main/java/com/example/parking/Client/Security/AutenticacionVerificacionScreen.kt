package com.example.parking.Client.Security

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parking.ui.theme.MinimalTopBarCompact

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutenticacionVerificacionScreen(navController: NavController) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            MinimalTopBarCompact(onBack = { navController.popBackStack() })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(
                text = "Metodos de autencación",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(24.dp))

            // Bloque de verificación de teléfono
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingItem(
                        icon = Icons.Default.Phone,
                        label = "Verificar número de teléfono",
                        onClick = { navController.navigate("verificacion_telefono") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bloque exclusivo OCR
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingItem(
                        icon = Icons.Default.AccountBox,
                        label = " Verificar identidad (OCR) — Próximamente",
                        onClick = {
                            Toast.makeText(context, "Función en desarrollo", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
