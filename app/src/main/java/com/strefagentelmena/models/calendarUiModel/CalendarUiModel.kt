package com.strefagentelmena.models.calendarUiModel

import android.annotation.SuppressLint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CalendarUiModel(
    val selectedDate: Date, // the date selected by the User. by default is Today.
    val visibleDates: List<Date> // the dates shown on the screen
) {

    val startDate: Date = visibleDates.first() // the first of the visible dates
    val endDate: Date = visibleDates.last() // the last of the visible dates

    data class Date(
        val date: LocalDate,
        val isSelected: Boolean,
        val isToday: Boolean,
        val isWeekend: Boolean = false,
        val isHoliday: Boolean = false,
        val holidayName: String? = null
    ) {
        @SuppressLint("NewApi")
        val day: String =
            date.format(DateTimeFormatter.ofPattern("E")) // get the day by formatting the date

        val dayMarker: String?
            get() = when {
                isHoliday -> "SW"
                isWeekend -> "WE"
                else -> null
            }
    }
}
