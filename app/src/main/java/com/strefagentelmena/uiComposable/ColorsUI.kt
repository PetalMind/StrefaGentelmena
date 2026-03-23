package com.strefagentelmena.uiComposable

import androidx.compose.ui.graphics.Color
import com.strefagentelmena.ui.theme.SalonBg
import com.strefagentelmena.ui.theme.SalonBg2
import com.strefagentelmena.ui.theme.SalonBg3
import com.strefagentelmena.ui.theme.SalonBorder
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonGoldBorder
import com.strefagentelmena.ui.theme.SalonGoldDim
import com.strefagentelmena.ui.theme.SalonMuted
import com.strefagentelmena.ui.theme.SalonMuted2
import com.strefagentelmena.ui.theme.SalonText

val colorsUI = ColorsUI()

/**
 * Kolory pomocnicze — nazwy historyczne, wartości dopasowane do mockupu HTML (złoto na grafitowym tle).
 * Preferuj [androidx.compose.material3.MaterialTheme.colorScheme] w nowych ekranach.
 */
class ColorsUI {
    val rusticBrown = SalonGold
    val buttonsGreen = SalonGold
    val headersBlue = SalonBg
    val grey = SalonBg2
    val darkGrey = SalonMuted2
    val yellow = SalonGoldDim
    val cardGrey = SalonBg2
    val mintGreen = SalonGold
    val green = SalonGold
    val teaGreen = SalonGoldDim
    val papaya = SalonBg2
    val sunset = SalonGoldBorder
    val amaranthPurple = Color(0xFFCC6666)
    val murrey = SalonMuted
    val fontGrey = SalonText
    val fireEngineRed = Color(0xFFE57373)
    val chocolateCosmos = Color(0xFF8B3A3A)
    val mustard = SalonGold
    val carmine = Color(0xFFB71C1C)
    val raisinBlack = SalonBg3
    val bittersweet = Color(0xFFFF8A65)
    val babyBlue = SalonGold.copy(alpha = 0.28f)
    val jade = SalonGold
    val cream = SalonGold
    val safaron = SalonGold
    val border = SalonBorder
}
