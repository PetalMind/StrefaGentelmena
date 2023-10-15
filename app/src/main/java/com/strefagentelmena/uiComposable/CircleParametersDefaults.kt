package com.strefagentelmena.uiComposable

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.strefagentelmena.models.CircleParameters

object CircleParametersDefaults {

    private val defaultCircleRadius = 12.dp

    fun circleParameters(
        radius: Dp = defaultCircleRadius,
        backgroundColor: Color = Cyan,
    ) = CircleParameters(radius, backgroundColor)
}
