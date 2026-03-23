package com.strefagentelmena.uiComposable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.ui.theme.StrefaGentelmenaTheme

/**
 * Kompaktowy wskaźnik ładowania w stylu marki (inspiracja: `strefa_loading_animation.html`):
 * nożyczki, subtelna poświata, opcjonalna linia tekstu z motywu, kropki i cienki pasek —
 * bez ramki „urządzenia” z mockupu, dopasowany do [MaterialTheme].
 */
@Composable
fun StrefaLoadingContent(
    modifier: Modifier = Modifier,
    /** Główna linia (np. nazwa salonu); `null` — bez tekstu, tylko animacja. */
    title: String? = "Strefa Gentlemana",
    /** Drugi wiersz (np. podpis); zwykle `null` w widoku aplikacji. */
    subtitle: String? = null,
    scissorsSize: Dp = 64.dp,
    /** Cienki pasek pod kropkami (nieskończona pętla, bez znaczenia % realnego postępu). */
    showIndeterminateBar: Boolean = true,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "strefa_loading")

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    val bladeA by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blade_a",
    )
    val bladeB by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blade_b",
    )

    val dotCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dots",
    )

    val progressT by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progress",
    )
    val progressWidthFraction = (progressT / 0.8f).coerceIn(0f, 1f)
    val progressBarAlpha =
        if (progressT <= 0.8f) 1f else (1f - (progressT - 0.8f) / 0.2f).coerceIn(0f, 1f)

    val glowSide = scissorsSize * 2.2f

    Column(
        modifier = modifier
            .widthIn(max = 320.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(
            modifier = Modifier.size(glowSide),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(glowSide * 0.85f)
                    .scale(glowScale)
                    .alpha(glowAlpha * 0.4f)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
            StrefaLoadingScissors(
                bladeADegrees = bladeA,
                bladeBDegrees = bladeB,
                accent = primary,
                modifier = Modifier.size(scissorsSize),
            )
        }

        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = primary,
                textAlign = TextAlign.Center,
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = onSurfaceVariant,
                letterSpacing = 0.12.sp,
                textAlign = TextAlign.Center,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) { i ->
                val phase = (dotCycle + i * (0.2f / 1.4f)) % 1f
                val (dotAlpha, dotScale) = strefaLoadingDotPhase(phase)
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .scale(dotScale)
                        .alpha(dotAlpha)
                        .background(primary, CircleShape),
                )
            }
        }

        if (showIndeterminateBar) {
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .alpha(progressBarAlpha),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(trackColor, RoundedCornerShape(1.dp)),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressWidthFraction)
                        .height(2.dp)
                        .background(primary, RoundedCornerShape(1.dp)),
                )
            }
        }
    }
}

private fun strefaLoadingDotPhase(phase: Float): Pair<Float, Float> {
    val p = phase % 1f
    fun mix(a: Float, b: Float, t: Float) = a + (b - a) * t
    val alpha = when {
        p < 0.32f -> mix(0.15f, 1f, p / 0.32f)
        p < 0.72f -> mix(1f, 0.15f, (p - 0.32f) / 0.4f)
        else -> 0.15f
    }
    val scale = when {
        p < 0.32f -> mix(0.9f, 1.2f, p / 0.32f)
        p < 0.72f -> mix(1.2f, 0.9f, (p - 0.32f) / 0.4f)
        else -> 0.9f
    }
    return alpha to scale
}

@Composable
private fun StrefaLoadingScissors(
    bladeADegrees: Float,
    bladeBDegrees: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val strokeThin = 1.6.dp
    val strokeBlade = 2.dp
    Canvas(modifier = modifier.fillMaxSize()) {
        val s = size.width / 72f
        val pivot = Offset(36f * s, 36f * s)

        rotate(bladeADegrees, pivot) {
            drawLine(
                color = accent,
                start = Offset(36f * s, 36f * s),
                end = Offset(66f * s, 14f * s),
                strokeWidth = strokeBlade.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = accent,
                radius = 7f * s,
                center = Offset(20f * s, 26f * s),
                style = Stroke(width = strokeThin.toPx()),
            )
            drawLine(
                color = accent,
                start = Offset(36f * s, 36f * s),
                end = Offset(26.5f * s, 30f * s),
                strokeWidth = strokeBlade.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = 3f * s,
                center = Offset(20f * s, 26f * s),
            )
        }

        rotate(bladeBDegrees, pivot) {
            drawLine(
                color = accent,
                start = Offset(36f * s, 36f * s),
                end = Offset(66f * s, 58f * s),
                strokeWidth = strokeBlade.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = accent,
                radius = 7f * s,
                center = Offset(20f * s, 46f * s),
                style = Stroke(width = strokeThin.toPx()),
            )
            drawLine(
                color = accent,
                start = Offset(36f * s, 36f * s),
                end = Offset(26.5f * s, 42f * s),
                strokeWidth = strokeBlade.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = 3f * s,
                center = Offset(20f * s, 46f * s),
            )
        }

        drawCircle(
            color = accent.copy(alpha = 0.9f),
            radius = 2.5f * s,
            center = pivot,
        )
    }
}

/**
 * Pełny ekran ładowania: treść marki + komunikat i opcjonalnie „Ponów próbę”.
 */
@Composable
fun StrefaLoading(
    modifier: Modifier = Modifier,
    title: String? = "Strefa Gentlemana",
    subtitle: String? = null,
    message: String? = "Ładowanie... sprawdź połączenie z internetem",
    onRetry: (() -> Unit)? = null,
    scissorsSize: Dp = 64.dp,
    showIndeterminateBar: Boolean = true,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        ) {
            StrefaLoadingContent(
                title = title,
                subtitle = subtitle,
                scissorsSize = scissorsSize,
                showIndeterminateBar = showIndeterminateBar,
            )
            if (message != null) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
            }
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Text(
                        "Ponów próbę",
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StrefaLoadingContentPreview() {
    StrefaGentelmenaTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            StrefaLoadingContent()
        }
    }
}
