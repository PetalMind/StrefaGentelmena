package com.strefagentelmena.uiComposable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.ui.theme.SalonBg3
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonGoldBorder
import com.strefagentelmena.ui.theme.SalonGoldDim
import com.strefagentelmena.ui.theme.SalonGreen
import com.strefagentelmena.ui.theme.SalonGreenBorder
import com.strefagentelmena.ui.theme.SalonGreenDim
import com.strefagentelmena.ui.theme.SalonIconBoxBorder
import com.strefagentelmena.ui.theme.SalonMuted
import com.strefagentelmena.ui.theme.SalonMuted2
import com.strefagentelmena.ui.theme.SalonRed
import com.strefagentelmena.ui.theme.SalonRedBorder
import com.strefagentelmena.ui.theme.SalonRedConflictText
import com.strefagentelmena.ui.theme.SalonRedDim
import com.strefagentelmena.ui.theme.SalonText
import com.strefagentelmena.ui.theme.SalonBg2
import com.strefagentelmena.ui.theme.SalonDialogScrim

private val SheetTopShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
private val ModalShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(10.dp)
private val OptionShape = RoundedCornerShape(11.dp)
private val DialogButtonShape = RoundedCornerShape(12.dp)
private val FloatingBarShape = RoundedCornerShape(22.dp)

/** Lewitujący pasek akcji u dołu dialogu (zaokrąglony, z cieniem). */
@Composable
fun StrefaDialogFloatingBar(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = FloatingBarShape,
        color = SalonBg2,
        shadowElevation = 8.dp,
        tonalElevation = 2.dp,
        border = BorderStroke(0.5.dp, SalonIconBoxBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// --- Overlay / panele ---

@Composable
fun StrefaDialogScrim(
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SalonDialogScrim)
            .then(
                if (onDismissRequest != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        content()
    }
}

@Composable
fun StrefaDialogScrimCentered(
    modifier: Modifier = Modifier,
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SalonDialogScrim)
            .then(
                if (onDismissRequest != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.padding(horizontal = 20.dp)) {
            content()
        }
    }
}

/** Tło bottom sheetu: uchwyt, padding, górne zaokrąglenie jak w mockupie. */
@Composable
fun StrefaBottomSheetPanel(
    modifier: Modifier = Modifier,
    showHandle: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SheetTopShape)
            .background(SalonBg2, SheetTopShape)
            .border(0.5.dp, SalonIconBoxBorder, SheetTopShape)
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
    ) {
        if (showHandle) {
            StrefaSheetDragHandle()
            Spacer(modifier = Modifier.height(18.dp))
        }
        content()
    }
}

@Composable
fun StrefaSheetDragHandle(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF333333)),
        )
    }
}

@Composable
fun StrefaSheetTitleText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge.copy(
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 24.sp,
        ),
        color = SalonGold,
    )
}

@Composable
fun StrefaSheetSubtitleText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 18.dp),
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.W300,
            lineHeight = 14.sp,
        ),
        color = SalonMuted2,
    )
}

/** Wyśrodkowany modal z obramowaniem. */
@Composable
fun StrefaModalPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(ModalShape)
            .background(SalonBg2, ModalShape)
            .border(0.5.dp, SalonIconBoxBorder, ModalShape)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        content = content,
    )
}

@Composable
fun StrefaModalIconFrame(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

enum class StrefaModalIconVariant {
    Danger,
    Warning,
}

@Composable
fun StrefaModalIconFrame(
    variant: StrefaModalIconVariant,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    val (bg, border) = when (variant) {
        StrefaModalIconVariant.Danger -> SalonRedDim to SalonRedBorder
        StrefaModalIconVariant.Warning -> SalonGoldDim to SalonGoldBorder
    }
    StrefaModalIconFrame(
        modifier = modifier.padding(bottom = 14.dp),
        backgroundColor = bg,
        borderColor = border,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconTint)
    }
}

@Composable
fun StrefaModalTitleText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.5.sp,
        ),
        color = SalonText,
        textAlign = textAlign ?: TextAlign.Start,
    )
}

@Composable
fun StrefaModalBodyText(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 20.dp),
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.W300,
            lineHeight = 19.2.sp,
        ),
        color = SalonMuted2,
        textAlign = textAlign ?: TextAlign.Start,
    )
}

// --- Przyciski ---

enum class StrefaDialogButtonStyle {
    Ghost,
    Gold,
    Red,
    Green,
}

@Composable
fun StrefaDialogButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: StrefaDialogButtonStyle = StrefaDialogButtonStyle.Ghost,
    enabled: Boolean = true,
) {
    val (bg, border, fg) = when (style) {
        StrefaDialogButtonStyle.Ghost -> Triple(SalonBg3, SalonIconBoxBorder, SalonMuted2)
        StrefaDialogButtonStyle.Gold -> Triple(SalonGold, null, Color(0xFF0E0E0E))
        StrefaDialogButtonStyle.Red -> Triple(SalonRedDim, SalonRedBorder, SalonRed)
        StrefaDialogButtonStyle.Green -> Triple(SalonGreenDim, SalonGreenBorder, SalonGreen)
    }
    val shape = DialogButtonShape
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (border != null) {
                    Modifier.border(0.5.dp, border, shape)
                } else Modifier
            )
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.24.sp,
            ),
            color = if (enabled) fg else fg.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun StrefaDialogDeleteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Usuń",
    enabled: Boolean = true,
) {
    StrefaDialogButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = StrefaDialogButtonStyle.Red,
        enabled = enabled,
    )
}

@Composable
fun StrefaDialogNotifyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Wyślij",
    enabled: Boolean = true,
) {
    StrefaDialogButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style = StrefaDialogButtonStyle.Green,
        enabled = enabled,
    )
}

@Composable
fun StrefaDialogButtonRow(
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        first(Modifier.weight(1f))
        second(Modifier.weight(1f))
    }
}

// --- Lista akcji (ikonka + tekst) ---

@Composable
fun StrefaDialogOptionList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun StrefaDialogOptionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = SalonMuted2,
    destructive: Boolean = false,
) {
    val titleColor = if (destructive) SalonRed else SalonText
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(OptionShape)
            .border(0.5.dp, SalonIconBoxBorder, OptionShape)
            .background(SalonBg3)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (destructive) SalonRed else leadingTint,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = titleColor,
        )
    }
}

@Composable
fun StrefaDialogInfoListItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(OptionShape)
            .border(0.5.dp, SalonIconBoxBorder, OptionShape)
            .background(SalonBg3)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.2.sp,
            ),
            color = SalonMuted,
        )
        Text(
            text = value.ifBlank { "—" },
            modifier = Modifier.padding(top = 3.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = SalonText,
        )
    }
}

// --- Pola formularza (tryb tylko do odczytu / slot) ---

@Composable
fun StrefaDialogFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.padding(bottom = 5.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 1.4.sp,
        ),
        color = SalonMuted,
    )
}

@Composable
fun StrefaDialogReadOnlyField(
    text: String,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .border(0.5.dp, SalonIconBoxBorder, FieldShape)
            .background(SalonBg3)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            fontWeight = if (muted) FontWeight.W300 else FontWeight.Normal,
        ),
        color = if (muted) SalonMuted else SalonText,
    )
}

@Composable
fun StrefaDialogFieldGroup(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueMuted: Boolean = false,
) {
    Column(modifier = modifier.padding(bottom = 10.dp)) {
        StrefaDialogFieldLabel(label)
        StrefaDialogReadOnlyField(text = value, muted = valueMuted)
    }
}

@Composable
fun StrefaDialogFieldRow(
    modifier: Modifier = Modifier,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        first(Modifier.weight(1f))
        second(Modifier.weight(1f))
    }
}

// --- Wybór jednej opcji (radio) ---

@Composable
fun StrefaDialogRadioOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) SalonGoldBorder else SalonIconBoxBorder
    val bg = if (selected) SalonGoldDim else SalonBg3
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(OptionShape)
            .border(0.5.dp, borderColor, OptionShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (selected) SalonGold else SalonIconBoxBorder,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(SalonGold),
                )
            }
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = SalonText,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = SalonMuted2,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

// --- Konflikt + sukces ---

@Composable
fun StrefaDialogConflictBlock(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, SalonRedBorder, RoundedCornerShape(10.dp))
            .background(SalonRedDim)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp),
            tint = SalonRed,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.W300,
                lineHeight = 16.5.sp,
            ),
            color = SalonRedConflictText,
        )
    }
}

@Composable
fun StrefaDialogSuccessRing(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Check,
    iconTint: Color = SalonGreen,
) {
    Box(
        modifier = modifier
            .padding(bottom = 16.dp)
            .size(56.dp)
            .clip(CircleShape)
            .border(1.5.dp, SalonGreenBorder, CircleShape)
            .background(SalonGreenDim),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = iconTint)
    }
}
