package com.strefagentelmena.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.navigation.NavController
import com.strefagentelmena.functions.appFunctions
import com.strefagentelmena.models.AnalyticsRange
import com.strefagentelmena.models.AtRiskCustomerInsight
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.LoyalCustomerInsight
import com.strefagentelmena.models.SalonKpiSummary
import com.strefagentelmena.models.computeSalonAnalytics
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.uiComposable.RetentionRingCard
import com.strefagentelmena.uiComposable.ScheduleFillSparklineHeatmapCard
import com.strefagentelmena.uiComposable.StrefaDialogButton
import com.strefagentelmena.uiComposable.StrefaDialogButtonRow
import com.strefagentelmena.uiComposable.StrefaDialogButtonStyle
import com.strefagentelmena.uiComposable.StrefaDialogFloatingBar
import com.strefagentelmena.uiComposable.StrefaDialogFieldGroup
import com.strefagentelmena.uiComposable.StrefaModalBodyText
import com.strefagentelmena.uiComposable.StrefaModalPanel
import com.strefagentelmena.uiComposable.StrefaModalTitleText
import com.strefagentelmena.ui.theme.SalonBg3
import com.strefagentelmena.ui.theme.SalonBorder
import com.strefagentelmena.ui.theme.SalonGold
import com.strefagentelmena.ui.theme.SalonGoldBorder
import com.strefagentelmena.ui.theme.SalonGreen
import com.strefagentelmena.ui.theme.SalonGreenBorder
import com.strefagentelmena.ui.theme.SalonMuted
import com.strefagentelmena.ui.theme.SalonMuted2
import com.strefagentelmena.ui.theme.SalonRed
import com.strefagentelmena.ui.theme.SalonRedBorder
import com.strefagentelmena.ui.theme.SalonText
import com.strefagentelmena.ui.theme.SalonWrapBg
import com.strefagentelmena.uiComposable.headersUI
import com.strefagentelmena.viewModel.MainScreenModelView

val statisticsScreen = StatisticsScreen()

class StatisticsScreen {
    @Composable
    fun StatisticsView(
        navController: NavController,
        viewModel: MainScreenModelView,
    ) {
        val customers by viewModel.customersLists.observeAsState(emptyList())
        val appointments by viewModel.appointmentsForAnalytics.observeAsState(emptyList())
        val employees by viewModel.employees.observeAsState(emptyList())
        val context = LocalContext.current
        val today = LocalDate.now()
        var customFrom by rememberSaveable { mutableStateOf(today.minusDays(29)) }
        var customTo by rememberSaveable { mutableStateOf(today) }
        var selectedRange by rememberSaveable { mutableStateOf(AnalyticsRange.Month) }
        var selectedEmployee by remember { mutableStateOf<Employee?>(null) }

        val analytics = remember(customers, appointments, selectedRange, customFrom, customTo, selectedEmployee) {
            computeSalonAnalytics(
                customers = customers,
                appointments = appointments,
                range = selectedRange,
                customFrom = customFrom,
                customTo = customTo,
                employeeId = selectedEmployee?.id,
            )
        }
        var loyalExpanded by rememberSaveable { mutableStateOf(false) }
        var atRiskExpanded by rememberSaveable { mutableStateOf(false) }
        var selectedCustomer by remember { mutableStateOf<Customer?>(null) }

        LaunchedEffect(Unit) {
            viewModel.checkAppointments()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            headersUI.AppBarWithBackArrow(
                title = "Statystyki i analizy",
                onBackPressed = { navController.popBackStack() },
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = SalonWrapBg,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (employees.isNotEmpty()) {
                        WorkerSelector(
                            employees = employees,
                            selected = selectedEmployee,
                            onSelected = { selectedEmployee = it },
                        )
                    }

                    RangeSelector(
                        selected = selectedRange,
                        onSelected = { selectedRange = it },
                    )
                    if (selectedRange == AnalyticsRange.Custom) {
                        CustomRangePickerRow(
                            from = customFrom,
                            to = customTo,
                            onPickFrom = {
                                showDatePickerDialog(context, customFrom) { picked ->
                                    customFrom = picked
                                    if (customTo.isBefore(customFrom)) customTo = customFrom
                                }
                            },
                            onPickTo = {
                                showDatePickerDialog(context, customTo) { picked ->
                                    customTo = picked
                                    if (customTo.isBefore(customFrom)) customFrom = customTo
                                }
                            },
                        )
                    }

                    KpiRow(
                        kpi = analytics.kpi,
                        range = selectedRange,
                    )

                    val retentionSubtitle = when {
                        analytics.kpi.customersWithVisits <= 0 -> "Brak klientów z wizytami"
                        else ->
                            "${analytics.kpi.returningCustomers} z ${analytics.kpi.customersWithVisits} klientów"
                    }
                    RetentionRingCard(
                        percent = analytics.retentionPercent,
                        subtitle = retentionSubtitle,
                    )

                    ScheduleFillSparklineHeatmapCard(
                        range = selectedRange,
                        referenceDate = if (selectedRange == AnalyticsRange.Custom) customTo else today,
                        sparklineDailyCounts = analytics.sparklineDailyCounts,
                        heatmapYearMonth = analytics.heatmapYearMonth,
                        heatmapDayCounts = analytics.heatmapDayCounts,
                    )

                    InsightSectionHeader(
                        title = "Najbardziej lojalni",
                        showAction = analytics.topLoyal.size > 3,
                        actionExpanded = loyalExpanded,
                        onActionClick = { loyalExpanded = !loyalExpanded },
                    )
                    if (analytics.topLoyal.isEmpty()) {
                        EmptyHint("Jeszcze nikt nie ma ≥2 wizyt w danych — po drugiej wizycie pojawi się tutaj.")
                    } else {
                        val rows =
                            if (loyalExpanded) analytics.topLoyal else analytics.topLoyal.take(3)
                        LoyalCustomersListCard(
                            rows = rows,
                            onCustomerClick = { selectedCustomer = it },
                        )
                    }

                    InsightSectionHeader(
                        title = "Nie wrócili od dawna",
                        showAction = analytics.atRisk.size > 3,
                        actionExpanded = atRiskExpanded,
                        onActionClick = { atRiskExpanded = !atRiskExpanded },
                    )
                    if (analytics.atRisk.isEmpty()) {
                        EmptyHint("Brak klientów spełniających kryterium — super!")
                    } else {
                        val rows =
                            if (atRiskExpanded) analytics.atRisk else analytics.atRisk.take(3)
                        AtRiskCustomersListCard(
                            rows = rows,
                            onCustomerClick = { selectedCustomer = it },
                        )
                    }

                    Spacer(modifier = Modifier.padding(bottom = 24.dp))
                }
            }
        }

        selectedCustomer?.let { customer ->
            CustomerQuickInfoDialog(
                customer = customer,
                onDismiss = { selectedCustomer = null },
                onMissingPhone = { viewModel.newMessage("Brak numeru telefonu u tego klienta.") },
            )
        }
    }
}

@Composable
private fun WorkerSelector(
    employees: List<Employee>,
    selected: Employee?,
    onSelected: (Employee?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalonBg3, RoundedCornerShape(12.dp))
            .border(1.dp, SalonBorder, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val isAllSalon = selected == null
        TextButton(
            onClick = { onSelected(null) },
            modifier = Modifier
                .widthIn(min = 100.dp)
                .background(
                    color = if (isAllSalon) SalonGold.copy(alpha = 0.18f) else SalonBg3,
                    shape = RoundedCornerShape(10.dp),
                ),
        ) {
            Text(
                text = "Cały salon",
                color = if (isAllSalon) SalonGold else SalonMuted2,
                fontWeight = if (isAllSalon) FontWeight.SemiBold else FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }

        employees.forEach { employee ->
            val isActive = selected?.id == employee.id
            TextButton(
                onClick = { onSelected(employee) },
                modifier = Modifier
                    .widthIn(min = 100.dp)
                    .background(
                        color = if (isActive) SalonGold.copy(alpha = 0.18f) else SalonBg3,
                        shape = RoundedCornerShape(10.dp),
                    ),
            ) {
                Text(
                    text = employee.displayName.ifBlank { employee.name },
                    color = if (isActive) SalonGold else SalonMuted2,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun InsightSectionHeader(
    title: String,
    showAction: Boolean,
    actionExpanded: Boolean,
    onActionClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.7.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = SalonMuted2,
        )
        if (showAction) {
            Text(
                text = if (actionExpanded) "pokaż mniej" else "pokaż wszystkich",
                style = MaterialTheme.typography.labelSmall,
                color = SalonMuted,
                modifier = Modifier.clickable(onClick = onActionClick),
            )
        }
    }
}

@Composable
private fun KpiRow(
    kpi: SalonKpiSummary,
    range: AnalyticsRange,
) {
    val visitsLabel = when (range) {
        AnalyticsRange.Day -> "Wizyty dzisiaj"
        AnalyticsRange.Week -> "Wizyty (7 dni)"
        AnalyticsRange.Month -> "Wizyty w miesiącu"
        AnalyticsRange.Year -> "Wizyty w roku"
        AnalyticsRange.Custom -> "Wizyty (zakres)"
    }
    val clientsLabel = when (range) {
        AnalyticsRange.Day -> "Klienci dzisiaj"
        AnalyticsRange.Week -> "Klienci (7 dni)"
        AnalyticsRange.Month -> "Klienci w miesiącu"
        AnalyticsRange.Year -> "Klienci w roku"
        AnalyticsRange.Custom -> "Klienci (zakres)"
    }
    val workedHoursLabel = when (range) {
        AnalyticsRange.Day -> "Godziny dzisiaj"
        AnalyticsRange.Week -> "Godziny (7 dni)"
        AnalyticsRange.Month -> "Godziny w miesiącu"
        AnalyticsRange.Year -> "Godziny w roku"
        AnalyticsRange.Custom -> "Godziny (zakres)"
    }
    val workedHoursValue = formatWorkedHours(kpi.workedMinutes)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KpiChip(
            modifier = Modifier.weight(1f),
            value = kpi.visitsThisMonth.toString(),
            label = visitsLabel,
        )
        KpiChip(
            modifier = Modifier.weight(1f),
            value = kpi.customersWithVisits.toString(),
            label = clientsLabel,
        )
        KpiChip(
            modifier = Modifier.weight(1f),
            value = workedHoursValue,
            label = workedHoursLabel,
        )
    }
}

private fun formatWorkedHours(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val rest = safe % 60
    return if (rest == 0) "${hours}h" else "${hours}h ${rest}m"
}

@Composable
private fun CustomRangePickerRow(
    from: LocalDate,
    to: LocalDate,
    onPickFrom: () -> Unit,
    onPickTo: () -> Unit,
) {
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalonBg3, RoundedCornerShape(12.dp))
            .border(1.dp, SalonBorder, RoundedCornerShape(12.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TextButton(
            onClick = onPickFrom,
            modifier = Modifier
                .weight(1f)
                .background(SalonBg3, RoundedCornerShape(10.dp)),
        ) {
            Text(
                text = "Od: ${from.format(fmt)}",
                color = SalonMuted2,
                fontWeight = FontWeight.Medium,
            )
        }
        TextButton(
            onClick = onPickTo,
            modifier = Modifier
                .weight(1f)
                .background(SalonGold.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
        ) {
            Text(
                text = "Do: ${to.format(fmt)}",
                color = SalonGold,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun showDatePickerDialog(
    context: android.content.Context,
    initialDate: LocalDate,
    onPicked: (LocalDate) -> Unit,
) {
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            onPicked(LocalDate.of(year, month + 1, dayOfMonth))
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth,
    ).show()
}

@Composable
private fun RangeSelector(
    selected: AnalyticsRange,
    onSelected: (AnalyticsRange) -> Unit,
) {
    val entries = listOf(
        AnalyticsRange.Day to "Dzień",
        AnalyticsRange.Week to "Tydzień",
        AnalyticsRange.Month to "Miesiąc",
        AnalyticsRange.Year to "Rok",
        AnalyticsRange.Custom to "Własny",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SalonBg3, RoundedCornerShape(12.dp))
            .border(1.dp, SalonBorder, RoundedCornerShape(12.dp))
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        entries.forEach { (range, label) ->
            val isActive = selected == range
            TextButton(
                onClick = { onSelected(range) },
                modifier = Modifier
                    .widthIn(min = 86.dp)
                    .background(
                        color = if (isActive) SalonGold.copy(alpha = 0.18f) else SalonBg3,
                        shape = RoundedCornerShape(10.dp),
                    ),
            ) {
                Text(
                    text = label,
                    color = if (isActive) SalonGold else SalonMuted2,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun KpiChip(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SalonBg3, RoundedCornerShape(14.dp))
            .border(1.dp, SalonBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = SalonGold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SalonMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = SalonMuted2,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

private fun customerInitials(firstName: String, lastName: String): String {
    val a = firstName.trim().firstOrNull()?.uppercaseChar() ?: ""
    val b = lastName.trim().firstOrNull()?.uppercaseChar() ?: ""
    return "$a$b".ifBlank { "?" }
}

private fun shortCustomerDisplayName(firstName: String, lastName: String): String {
    val f = firstName.trim()
    val l = lastName.trim()
    if (f.isBlank() && l.isBlank()) return "Bez nazwy"
    if (l.isBlank()) return f
    val initial = l.first().uppercaseChar()
    return "$f $initial."
}

private fun polishVisitWord(count: Int): String = when {
    count == 1 -> "wizyta"
    count % 10 in 2..4 && count % 100 !in 12..14 -> "wizyty"
    else -> "wizyt"
}

private fun compactLoyalRhythm(rhythmLabel: String): String {
    val s = rhythmLabel.trim()
    val noSrednio = Regex("^średnio\\s+", RegexOption.IGNORE_CASE).replace(s, "")
    return noSrednio.replace("tydzień", "tyg.", ignoreCase = true)
}

private fun loyalKind(row: LoyalCustomerInsight): LoyalKind {
    val w = row.customer.avgWeeksBetweenVisits
    return if (w > 0.05 && w >= 4.0) LoyalKind.Regular else LoyalKind.Staly
}

private enum class LoyalKind {
    Staly,
    Regular,
}

@Composable
private fun LoyalCustomersListCard(
    rows: List<LoyalCustomerInsight>,
    onCustomerClick: (Customer) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SalonBg3)
            .border(1.dp, SalonBorder, RoundedCornerShape(14.dp)),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                Divider(color = SalonBorder, thickness = 1.dp)
            }
            LoyalCustomerRow(
                row = row,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                onClick = { onCustomerClick(row.customer) },
            )
        }
    }
}

@Composable
private fun LoyalCustomerRow(
    row: LoyalCustomerInsight,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val kind = loyalKind(row)
    val visitsShown =
        row.customer.visitCount.takeIf { it > 0 } ?: row.visitCount
    val sub =
        "${visitsShown} ${polishVisitWord(visitsShown)} · ${compactLoyalRhythm(row.rhythmLabel)}"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.5.dp, SalonGold, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = customerInitials(row.customer.firstName, row.customer.lastName),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SalonGold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shortCustomerDisplayName(row.customer.firstName, row.customer.lastName),
                style = MaterialTheme.typography.titleSmall,
                color = SalonText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                color = SalonMuted2,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val (badgeText, badgeColor, badgeBorder) = when (kind) {
            LoyalKind.Staly -> Triple("stały", SalonGold, SalonGoldBorder)
            LoyalKind.Regular -> Triple("regularny", SalonGreen, SalonGreenBorder)
        }
        Box(
            modifier = Modifier
                .border(1.dp, badgeBorder, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = badgeColor,
            )
        }
    }
}

@Composable
private fun AtRiskCustomersListCard(
    rows: List<AtRiskCustomerInsight>,
    onCustomerClick: (Customer) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SalonBg3)
            .border(1.dp, SalonBorder, RoundedCornerShape(14.dp)),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                Divider(color = SalonBorder, thickness = 1.dp)
            }
            AtRiskCustomerRow(
                row = row,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                onClick = { onCustomerClick(row.customer) },
            )
        }
    }
}

@Composable
private fun AtRiskCustomerRow(
    row: AtRiskCustomerInsight,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val weeks = (row.daysSinceLastVisit / 7L).coerceAtLeast(1L).toInt()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.5.dp, SalonRed, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = customerInitials(row.customer.firstName, row.customer.lastName),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SalonRed,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shortCustomerDisplayName(row.customer.firstName, row.customer.lastName),
                style = MaterialTheme.typography.titleSmall,
                color = SalonText,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "ostatnia wizyta: $weeks tyg. temu",
                style = MaterialTheme.typography.bodySmall,
                color = SalonMuted2,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box(
            modifier = Modifier
                .border(1.dp, SalonRedBorder, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = "! $weeks tyg.",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = SalonRed,
            )
        }
    }
}

@Composable
private fun CustomerQuickInfoDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onMissingPhone: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val phone = customer.phoneNumber.trim()
    val fullName = customer.fullName.trim().ifBlank { "Bez nazwy" }
    val lastVisit = customer.lastVisit?.date?.trim().orEmpty().ifBlank { "Brak danych" }
    val email = customer.email.trim().ifBlank { "Brak danych" }
    val notesPreview = customer.noted.trim().ifBlank { "Brak notatki" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                StrefaModalPanel {
                    StrefaModalTitleText("Informacje o kliencie")
                    StrefaModalBodyText(fullName)

                    StrefaDialogFieldGroup(label = "Telefon", value = phone.ifBlank { "Brak numeru" }, valueMuted = phone.isBlank())
                    StrefaDialogFieldGroup(label = "E-mail", value = email, valueMuted = email == "Brak danych")
                    StrefaDialogFieldGroup(label = "Ostatnia wizyta", value = lastVisit, valueMuted = lastVisit == "Brak danych")
                    StrefaDialogFieldGroup(label = "Notatka", value = notesPreview, valueMuted = notesPreview == "Brak notatki")
                }
                StrefaDialogFloatingBar(
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    StrefaDialogButtonRow(
                        first = { rowModifier ->
                            StrefaDialogButton(
                                text = "Anuluj",
                                onClick = onDismiss,
                                modifier = rowModifier,
                                style = StrefaDialogButtonStyle.Ghost,
                            )
                        },
                        second = { rowModifier ->
                            StrefaDialogButton(
                                text = "Zadzwoń",
                                onClick = {
                                    if (phone.isBlank()) {
                                        onMissingPhone()
                                    } else {
                                        appFunctions.dialPhoneNumber(context, phone)
                                        onDismiss()
                                    }
                                },
                                modifier = rowModifier,
                                style = StrefaDialogButtonStyle.Gold,
                            )
                        },
                    )
                }
            }
        }
    }
}
