package com.strefagentelmena.uiComposable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.ui.theme.SalonBorder
import com.strefagentelmena.ui.theme.SalonGold

private val ChartCardBg = Color(0xFF121212)
private val ChartBarFill = Color(0xFF242424)
private val ChartBarOutlineIdle = Color(0xFF333333)
private val ChartMuted = Color(0xFF666666)

private val dayLabels = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "Sb", "Nd")

/**
 * Wykres słupkowy wizyt w bieżącym tygodniu (pon–nd). Akcent złoty dla [todayDayIndex] (0 = pon).
 */
@Composable
fun WeeklyVisitsBarChart(
    counts: List<Int>,
    todayDayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val safe = List(7) { i -> counts.getOrElse(i) { 0 } }
    val maxCount = safe.maxOrNull()?.coerceAtLeast(1) ?: 1
    val allZero = safe.all { it == 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ChartCardBg, RoundedCornerShape(18.dp))
            .border(1.dp, SalonBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = "WIZYTY W TYM TYGODNIU",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.7.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = ChartMuted,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            for (i in 0 until 7) {
                val count = safe[i]
                val isToday = i == todayDayIndex.coerceIn(0, 6)
                val labelColor = if (isToday) SalonGold else ChartMuted
                val barFraction = when {
                    allZero -> 0.07f
                    count == 0 -> 0.08f
                    else -> (count.toFloat() / maxCount).coerceIn(0.14f, 1f)
                }
                val barShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = labelColor,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(0.78f)
                                .fillMaxHeight(barFraction)
                                .background(ChartBarFill, barShape)
                                .border(
                                    width = if (isToday) 1.5.dp else 0.5.dp,
                                    color = if (isToday) SalonGold else ChartBarOutlineIdle,
                                    shape = barShape,
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dayLabels[i],
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = labelColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
