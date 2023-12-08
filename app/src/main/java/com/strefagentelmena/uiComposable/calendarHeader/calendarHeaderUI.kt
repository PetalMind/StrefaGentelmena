package com.strefagentelmena.uiComposable.calendarHeader

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strefagentelmena.functions.calendarUiFunctions.CalendarDataSource
import com.strefagentelmena.models.calendarUiModel.CalendarUiModel
import com.strefagentelmena.uiComposable.colorsUI
import com.strefagentelmena.viewModel.ScheduleModelView
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val callendarHeaderUI = CalendarHeaderUI()

class CalendarHeaderUI {
    @SuppressLint("NewApi")
    @Composable
    fun ContentItem(date: CalendarUiModel.Date, onClickListener: (CalendarUiModel.Date) -> Unit) {
        Card(
            border = BorderStroke(width = 1.dp, color = colorsUI.cardGrey),
            elevation = CardDefaults.cardElevation(defaultElevation = if (date.isSelected) 8.dp else 4.dp),
            modifier = Modifier
                .clickable {
                    onClickListener(date)
                }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            colors = CardDefaults.cardColors(
                // background colors of the selected date
                // and the non-selected date are different
                containerColor = if (date.isSelected) {
                    colorsUI.cream
                } else {
                    colorsUI.cardGrey
                },
            ),
        ) {
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .height(58.dp)
                    .padding(4.dp)
            ) {
                Text(
                    text = date.day, // day "Mon", "Tue"
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontWeight = if (date.isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = date.date.dayOfMonth.toString(), // date "15", "16"
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontWeight = if (date.isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = if (date.isSelected) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    @Composable
    fun Content(
        data: CalendarUiModel, onDateClickListener: (CalendarUiModel.Date) -> Unit,
    ) {
        LazyRow {
            // pass the visibleDates to the UI
            items(items = data.visibleDates) { date ->
                ContentItem(date, onDateClickListener)
            }
        }
    }

    @SuppressLint("NewApi")
    @Composable
    fun CalendarApp(modifier: Modifier = Modifier, viewModel: ScheduleModelView) {
        val dataSource = CalendarDataSource()
        // we use `mutableStateOf` and `remember` inside composable function to schedules recomposition
        var calendarUiModel by remember { mutableStateOf(dataSource.getData(lastSelectedDate = dataSource.today)) }
        val selectedDate by viewModel.currentSelectedAppoinmentsDate.observeAsState()

        LaunchedEffect(selectedDate) {
            if (selectedDate != null) {
                val dates = LocalDate.parse(selectedDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                val date = CalendarUiModel.Date(
                    isSelected = false,
                    isToday = dates.isEqual(dataSource.today),
                    date = dates
                )

                val finalStartDate = date.date.minusDays(0)
                val newData = dataSource.getData(
                    startDate = finalStartDate,
                    lastSelectedDate = calendarUiModel.selectedDate.date
                )

                if (!newData.visibleDates.contains(date)) {
                    // Jeżeli wybrana data nie znajduje się w widocznych datach, zmień widoczne daty
                    calendarUiModel = newData
                } else {
                    // W przeciwnym razie, zaznacz tylko wybraną datę
                    val updatedVisibleDates = newData.visibleDates.map {
                        it.copy(
                            isSelected = it.date.atStartOfDay() == date.date.atStartOfDay()
                        )
                    }
                    calendarUiModel = newData.copy(
                        selectedDate = date,
                        visibleDates = updatedVisibleDates
                    )

                }
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
        ) {
            Header(
                data = calendarUiModel,
                onPrevClickListener = { startDate ->
                    // refresh the CalendarUiModel with new data
                    // by get data with new Start Date (which is the startDate-1 from the visibleDates)
                    val finalStartDate = startDate.minusDays(1)

                    calendarUiModel = dataSource.getData(
                        startDate = finalStartDate,
                        lastSelectedDate = calendarUiModel.selectedDate.date
                    )
                },
                onNextClickListener = { endDate ->
                    // refresh the CalendarUiModel with new data
                    // by get data with new Start Date (which is the endDate+2 from the visibleDates)
                    val finalStartDate = endDate.plusDays(2)

                    calendarUiModel = dataSource.getData(
                        startDate = finalStartDate,
                        lastSelectedDate = calendarUiModel.selectedDate.date
                    )
                }
            )

            Content(data = calendarUiModel, onDateClickListener = { date ->
                // refresh the CalendarUiModel with new data
                // by changing only the `selectedDate` with the date selected by User
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

                val parsedDate = LocalDate.parse(date.date.toString())
                val formattedDate = parsedDate.format(formatter)

                viewModel.setNewAppoimentsDate(formattedDate)

                calendarUiModel = calendarUiModel.copy(
                    selectedDate = date,
                    visibleDates = calendarUiModel.visibleDates.map {
                        it.copy(
                            isSelected = it.date.isEqual(date.date)
                        )
                    }
                )
            })
        }
    }

    @SuppressLint("NewApi")
    @Composable
    fun Header(
        data: CalendarUiModel,
        onPrevClickListener: (LocalDate) -> Unit,
        onNextClickListener: (LocalDate) -> Unit,
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            AnimatedContent(
                targetState = data,
                label = "",
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                transitionSpec = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(300, easing = LinearEasing)
                    ).togetherWith(
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Up,
                            animationSpec = tween(300, easing = LinearEasing)
                        )
                    )
                }
            ) {
                Text(
                    // show "Dzisiaj" if user selects today's date
                    // else, show the full format of the date
                    text = if (it.selectedDate.isToday) {
                        "Dzisiaj"
                    } else {
                        it.selectedDate.date.format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                        )
                    },
                    color = Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                )
            }

            IconButton(onClick = {
                onPrevClickListener(data.startDate.date)
            }) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            IconButton(onClick = {
                onNextClickListener(data.endDate.date)
            }) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "Next"
                )
            }
        }
    }
}