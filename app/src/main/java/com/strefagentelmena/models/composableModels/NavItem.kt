package com.strefagentelmena.models.composableModels

import androidx.compose.ui.graphics.vector.ImageVector

data class NavItem(
    val icon: ImageVector,
    val label: String,
    val route: String,
)
