package com.strefagentelmena.uiComposable.calendarHeader

import android.annotation.SuppressLint
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strefagentelmena.functions.calendarUiFunctions.CalendarDataSource
import com.strefagentelmena.models.calendarUiModel.CalendarUiModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

val callendarHeaderUI = CalendarHeaderUI()

class CalendarHeaderUI {
    @SuppressLint("NewApi")
    @Composable
    fun ContentItem(date: CalendarUiModel.Date, onClickListener: (CalendarUiModel.Date) -> Unit) {
        Card(
            modifier = Modifier
                .clickable {
                    onClickListener(date)
                }
                .padding(vertical = 4.dp, horizontal = 4.dp),
            colors = CardDefaults.cardColors(
                // background colors of the selected date
                // and the non-selected date are different
                containerColor = if (date.isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
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
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = date.date.dayOfMonth.toString(), // date "15", "16"
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.bodyMedium,
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
    fun CalendarApp(modifier: Modifier = Modifier) {
        val dataSource = CalendarDataSource()
        // we use `mutableStateOf` and `remember` inside composable function to schedules recomposition
        var calendarUiModel by remember { mutableStateOf(dataSource.getData(lastSelectedDate = dataSource.today)) }

        Column(modifier = modifier.fillMaxSize()) {
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
        Row {
            Text(
                // show "Today" if user selects today's date
                // else, show the full format of the date
                text = if (data.selectedDate.isToday) {
                    "Dzisiaj"
                } else {
                    data.selectedDate.date.format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            )

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