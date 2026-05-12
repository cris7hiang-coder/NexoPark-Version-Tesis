package com.example.parking.components.nav

import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val route: String,
    val matchRoutes: Set<String> = setOf(route),
    val badgeCount: Int = 0
)