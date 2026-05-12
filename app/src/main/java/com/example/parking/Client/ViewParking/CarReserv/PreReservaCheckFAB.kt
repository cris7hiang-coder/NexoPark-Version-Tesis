package com.example.parking.Client.ViewParking.CarReserv

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parking.ui.theme.AzulPetroleoFuerte
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.animation.core.animateDp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement

@Composable
fun PreReservaCheckFAB(
    parqueoId: String,
    navController: NavController,
    onReservar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val haptic = LocalHapticFeedback.current

    var cargando by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (cargando)
                    MaterialTheme.colorScheme.surfaceVariant
                else
                    MaterialTheme.colorScheme.primary,
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = !cargando,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (uid == null) {
                                Toast.makeText(
                                    context,
                                    "Debes iniciar sesión",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@clickable
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                            cargando = true

                            scope.launch {
                                try {
                                    val puedeReservar = usuarioPuedeReservar(uid)

                                    if (puedeReservar) {
                                        onReservar()
                                    } else {
                                        val userDoc = FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(uid)
                                            .get()
                                            .await()

                                        val telefonoVerificado =
                                            userDoc.getBoolean("telefonoVerificado") == true

                                        if (!telefonoVerificado) {
                                            Toast.makeText(
                                                context,
                                                "Verifica tu teléfono",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.navigate("autenticacion_verificacion")
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Registra un vehículo",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            navController.navigate("mi_vehiculo")
                                        }
                                    }

                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } finally {
                                    cargando = false
                                }
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {

                    if (cargando) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "Verificando...",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {

                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "Reservar espacio",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}