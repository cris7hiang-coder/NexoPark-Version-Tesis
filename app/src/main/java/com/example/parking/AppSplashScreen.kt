package com.example.parking

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parking.ui.theme.Inter
import com.example.parking.ui.theme.Urbanist
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun SplashNexoParkScreen(onFinish: () -> Unit) {

    // ICONO
    val squareScale = remember { Animatable(0f) }
    val squareRotation = remember { Animatable(-180f) }
    val squareAlpha = remember { Animatable(0f) }
    val fadeOut = remember { Animatable(1f) }

    // TEXTO
    val textOffsetX = remember { Animatable(-60f) }
    val textAlpha = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.8f) }

    // Rebote "Park"
    val parkScale = remember { Animatable(0f) }
    val parkOffsetY = remember { Animatable(20f) }

    // Pulso icono
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )

    // Flotación
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        )
    )

    // Brillo
    val shineAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                0f at 0
                0f at 3000
                0.6f at 3200
                0f at 3500
            }
        )
    )

    LaunchedEffect(Unit) {

        // ICONO
        launch {
            squareAlpha.animateTo(1f, tween(300))
            squareRotation.animateTo(
                -10f,
                spring(dampingRatio = 0.7f, stiffness = 200f)
            )
            squareRotation.animateTo(0f, tween(400))
        }

        launch {
            delay(100)
            squareScale.animateTo(
                1f,
                spring(dampingRatio = 0.6f, stiffness = 180f)
            )
        }

        delay(600)

        // TEXTO "Nexo"
        launch {
            textAlpha.animateTo(1f, tween(500))
            textOffsetX.animateTo(
                0f,
                spring(dampingRatio = 0.8f)
            )
            textScale.animateTo(
                1f,
                spring(dampingRatio = 0.7f)
            )
        }

        delay(300)

        // PARK
        launch {
            parkScale.animateTo(
                1f,
                spring(dampingRatio = 0.5f, stiffness = 400f)
            )
            parkOffsetY.animateTo(
                0f,
                spring(dampingRatio = 0.6f)
            )
        }

        delay(1500)

        fadeOut.animateTo(0f, tween(800))
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1E2E))
            .graphicsLayer { alpha = fadeOut.value },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            // ICONO
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = squareScale.value
                    scaleY = squareScale.value
                    rotationZ = squareRotation.value
                    alpha = squareAlpha.value
                    translationY = floatOffset
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF162D3D))
                        .border(
                            1.5.dp,
                            Color(0xFF4A90E2).copy(alpha = 0.35f),
                            RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFF4A90E2).copy(alpha = shineAlpha),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Text(
                        "N",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Urbanist,
                        color = Color.White,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // TEXTO
            Row(
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationX = textOffsetX.value
                    scaleX = textScale.value
                    scaleY = textScale.value
                }
            ) {
                Text(
                    "Nexo",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = Inter,
                    color = Color.White
                )

                Spacer(Modifier.width(6.dp))

                Text(
                    "Park",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Inter,
                    color = Color(0xFF4A90E2),
                    modifier = Modifier.graphicsLayer {
                        scaleX = parkScale.value
                        scaleY = parkScale.value
                        translationY = parkOffsetY.value
                    }
                )
            }
        }
    }
}
