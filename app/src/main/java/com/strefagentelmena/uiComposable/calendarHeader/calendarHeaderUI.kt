package com.strefagentelmena.uiComposable.calendarHeader

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    @Composable
    fun ContentItem(
        date: CalendarUiModel.Date,
        onClickListener: (CalendarUiModel.Date) -> Unit
    ) {
        val containerColor = if (date.isSelected) colorsUI.cream else colorsUI.cardGrey
        val fontWeight = if (date.isSelected) FontWeight.Bold else FontWeight.Normal
        val textStyle =
            if (date.isSelected) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.bodyLarge

        Card(
            border = BorderStroke(1.dp, colorsUI.cardGrey),
            elevation = CardDefaults.cardElevation(defaultElevation = if (date.isSelected) 8.dp else 4.dp),
            modifier = Modifier
                .clickable { onClickListener(date) }
                .padding(4.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor)
        ) {
            Column(
                modifier = Modifier
                    .size(width = 48.dp, height = 58.dp)
                    .padding(4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.day, // "Mon", "Tue"
                    fontWeight = fontWeight,
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = date.date.dayOfMonth.toString(), // "15", "16"
                    fontWeight = fontWeight,
                    style = textStyle
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
    fun CalendarHeader(modifier: Modifier = Modifier, viewModel: ScheduleModelView) {
        val dataSource = CalendarDataSource()
        var calendarUiModel by remember { mutableStateOf(dataSource.getData(lastSelectedDate = dataSource.today)) }
        val selectedDate by viewModel.selectedAppointmentDate.observeAsState()

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
                    calendarUiModel = newData
                } else {
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
                    val finalStartDate = startDate.minusDays(1)
                    calendarUiModel = dataSource.getData(
                        startDate = finalStartDate,
                        lastSelectedDate = calendarUiModel.selectedDate.date
                    )
                },
                onNextClickListener = { endDate ->
                    val finalStartDate = endDate.plusDays(2)
                    calendarUiModel = dataSource.getData(
                        startDate = finalStartDate,
                        lastSelectedDate = calendarUiModel.selectedDate.date
                    )
                }
            )

            Content(data = calendarUiModel, onDateClickListener = { date ->
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val parsedDate = LocalDate.parse(date.date.toString(), formatter)

                viewModel.setNewAppoimentsDate(parsedDate)

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
                        "Dzisiaj, " + it.selectedDate.date.format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                        )
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