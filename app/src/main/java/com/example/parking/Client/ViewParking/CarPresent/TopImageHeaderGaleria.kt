package com.example.parking.Client.ViewParking.CarPresent

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopImageHeaderGaleria(
    imagenes: List<String>,
    onBack: () -> Unit,
    onShare: () -> Unit = {},
    onToggleFavorito: () -> Unit = {},
    esFavorito: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    val imagenesSeguras = if (imagenes.isNotEmpty()) imagenes else listOf("")
    val pagerState = rememberPagerState(pageCount = { imagenesSeguras.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(272.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (imagenesSeguras[page].isNotBlank()) {
                AsyncImage(
                    model = imagenesSeguras[page],
                    contentDescription = "Imagen del parqueo ${page + 1}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surface.copy(alpha = 0.18f),
                            Color.Transparent,
                            colors.surface.copy(alpha = 0.72f)
                        )
                    )
                )
        )

        HeaderCircleActionButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Volver",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderCircleActionButton(
                icon = Icons.Default.Share,
                contentDescription = "Compartir",
                onClick = onShare
            )

            HeaderCircleActionButton(
                icon = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorito",
                onClick = onToggleFavorito,
                tint = if (esFavorito) colors.error else colors.primary
            )
        }

        if (imagenesSeguras.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(50),
                color = colors.surface.copy(alpha = 0.82f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(imagenesSeguras.size) { index ->
                        val isSelected = pagerState.currentPage == index

                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        colors.primary
                                    } else {
                                        colors.onSurfaceVariant.copy(alpha = 0.45f)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                shape = CircleShape
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}