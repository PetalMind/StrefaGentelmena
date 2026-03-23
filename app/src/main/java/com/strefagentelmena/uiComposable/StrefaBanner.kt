package com.strefagentelmena.uiComposable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonMuted2
import com.strefagentelmena.ui.theme.SalonText

/**
 * Banner informacyjny zgodny z mockupem [com.strefagentelmena.dataSource.strefa_banner_component.html]:
 * ikona w zaokrąglonym kwadracie, tytuł, opis, opcjonalna akcja ze strzałką, opcjonalne zamknięcie.
 */
enum class StrefaBannerVariant {
    Warning,
    Error,
    Info,
    Success,
}

enum class StrefaBannerDensity {
    /** Pełny rozmiar (padding 13×14, promień 14, ikona 32). */
    Comfortable,

    /** Kompaktowy — jak w podglądzie harmonogramu w HTML. */
    Compact,
}

private data class StrefaBannerPalette(
    val surface: Color,
    val border: Color,
    val iconSurface: Color,
    val iconTint: Color,
    val title: Color,
    val description: Color,
    val action: Color,
    val closeTint: Color,
)

private fun paletteFor(variant: StrefaBannerVariant): StrefaBannerPalette = when (variant) {
    StrefaBannerVariant.Warning -> StrefaBannerPalette(
        surface = Color(0x14C9A84C),
        border = Color(0x40C9A84C),
        iconSurface = Color(0x24C9A84C),
        iconTint = SalonGold,
        title = Color(0xFFE2C97E),
        description = Color(0xFFA89060),
        action = SalonGold,
        closeTint = SalonGold,
    )
    StrefaBannerVariant.Error -> StrefaBannerPalette(
        surface = Color(0x1AB43232),
        border = Color(0x40B43232),
        iconSurface = Color(0x26B43232),
        iconTint = Color(0xFFE07070),
        title = Color(0xFFE07070),
        description = Color(0xFF906060),
        action = Color(0xFFE07070),
        closeTint = Color(0xFFE07070),
    )
    StrefaBannerVariant.Info -> StrefaBannerPalette(
        surface = Color.White.copy(alpha = 0.04f),
        border = Color.White.copy(alpha = 0.10f),
        iconSurface = Color.White.copy(alpha = 0.07f),
        iconTint = SalonMuted2,
        title = SalonText,
        description = SalonMuted2,
        action = SalonText,
        closeTint = SalonMuted2,
    )
    StrefaBannerVariant.Success -> StrefaBannerPalette(
        surface = Color(0x1A3C8250),
        border = Color(0x403C8250),
        iconSurface = Color(0x263C8250),
        iconTint = Color(0xFF6DBF88),
        title = Color(0xFF6DBF88),
        description = Color(0xFF508060),
        action = Color(0xFF6DBF88),
        closeTint = Color(0xFF6DBF88),
    )
}

private fun iconFor(variant: StrefaBannerVariant): ImageVector = when (variant) {
    StrefaBannerVariant.Warning -> Icons.Filled.Warning
    // W material-icons-core brak ikony „błąd w kółku”; domyślnie Info + paleta Error — można nadpisać [icon].
    StrefaBannerVariant.Error -> Icons.Filled.Info
    StrefaBannerVariant.Info -> Icons.Filled.DateRange
    StrefaBannerVariant.Success -> Icons.Filled.Check
}

@Composable
fun StrefaBanner(
    variant: StrefaBannerVariant,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    density: StrefaBannerDensity = StrefaBannerDensity.Comfortable,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    /** Nadpisuje domyślną ikonę dla wariantu. */
    icon: ImageVector? = null,
) {
    val palette = paletteFor(variant)
    val shapeRadius = when (density) {
        StrefaBannerDensity.Comfortable -> 14.dp
        StrefaBannerDensity.Compact -> 10.dp
    }
    val horizontalPadding = when (density) {
        StrefaBannerDensity.Comfortable -> 14.dp
        StrefaBannerDensity.Compact -> 12.dp
    }
    val verticalPadding = when (density) {
        StrefaBannerDensity.Comfortable -> 13.dp
        StrefaBannerDensity.Compact -> 10.dp
    }
    val iconBoxSize: Dp
    val iconBoxRadius: Dp
    val iconSize: Dp
    val titleSp: Float
    val descSp: Float
    val actionSp: Float
    when (density) {
        StrefaBannerDensity.Comfortable -> {
            iconBoxSize = 32.dp
            iconBoxRadius = 9.dp
            iconSize = 16.dp
            titleSp = 13f
            descSp = 11f
            actionSp = 11f
        }
        StrefaBannerDensity.Compact -> {
            iconBoxSize = 26.dp
            iconBoxRadius = 7.dp
            iconSize = 13.dp
            titleSp = 12f
            descSp = 10f
            actionSp = 10f
        }
    }

    val shape = RoundedCornerShape(shapeRadius)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface, shape)
            .border(0.5.dp, palette.border, shape)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .background(palette.iconSurface, RoundedCornerShape(iconBoxRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon ?: iconFor(variant),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = palette.iconTint,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                color = palette.title,
                fontSize = titleSp.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (titleSp * 1.2f).sp,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = palette.description,
                fontSize = descSp.sp,
                fontWeight = FontWeight.Light,
                lineHeight = (descSp * 1.5f).sp,
            )
            if (actionLabel != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(9.dp))
                Row(
                    modifier = Modifier
                        .clickable(onClick = onActionClick)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = actionLabel,
                        color = palette.action,
                        fontSize = actionSp.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size((actionSp + 1f).dp),
                        tint = palette.action,
                    )
                }
            }
        }

        if (onDismiss != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Zamknij",
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(0.4f),
                    tint = palette.closeTint,
                )
            }
        }
    }
}

data class StrefaScheduleErrorBanner(
    val variant: StrefaBannerVariant,
    val title: String,
    val description: String,
)

/** Mapuje [ScheduleModelView.appointmentError] na treść banera (formularz wizyty) — tylko błędy blokujące zapis. */
fun strefaScheduleErrorBannerOrNull(message: String): StrefaScheduleErrorBanner? {
    val m = message.trim()
    if (m.isEmpty()) return null
    if (m == "Niepoprawny format godziny") {
        return StrefaScheduleErrorBanner(
            variant = StrefaBannerVariant.Error,
            title = "Niepoprawny format godziny",
            description = m,
        )
    }
    return StrefaScheduleErrorBanner(
        variant = StrefaBannerVariant.Error,
        title = "Nie można zapisać",
        description = m,
    )
}

/** [ScheduleModelView.appointmentScheduleNotice] — kolizje godzin / poza godzinami pracy (informacja, zapis dozwolony). */
fun strefaScheduleNoticeBannerOrNull(message: String): StrefaScheduleErrorBanner? {
    val m = message.trim()
    if (m.isEmpty()) return null
    return StrefaScheduleErrorBanner(
        variant = StrefaBannerVariant.Info,
        title = "Harmonogram",
        description = m,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF080808)
@Composable
private fun StrefaBannerPreviewAll() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StrefaBanner(
            variant = StrefaBannerVariant.Warning,
            title = "Wizyty się nakładają",
            description = "09:30 Bartłomiej W. i 10:00 Marek K. kolidują o 30 minut.",
            actionLabel = "Rozwiąż konflikt",
            onActionClick = {},
            onDismiss = {},
        )
        StrefaBanner(
            variant = StrefaBannerVariant.Error,
            title = "Wizyta bez przypisanego klienta",
            description = "Wizyta o 14:00 nie ma przypisanego klienta.",
            actionLabel = "Uzupełnij dane",
            onActionClick = {},
            onDismiss = {},
        )
        StrefaBanner(
            variant = StrefaBannerVariant.Info,
            title = "Następna wizyta za 15 minut",
            description = "Piotr Nowak · Strzyżenie + broda · 09:30",
            actionLabel = "Zobacz profil",
            onActionClick = {},
            onDismiss = {},
        )
        StrefaBanner(
            variant = StrefaBannerVariant.Success,
            title = "Wizyta została zapisana",
            description = "Bartłomiej W. · 20 marca · 09:30",
            onDismiss = {},
        )
        StrefaBanner(
            variant = StrefaBannerVariant.Warning,
            title = "Wizyty się nakładają",
            description = "09:30 i 10:00 kolidują o 30 minut.",
            density = StrefaBannerDensity.Compact,
        )
    }
}
