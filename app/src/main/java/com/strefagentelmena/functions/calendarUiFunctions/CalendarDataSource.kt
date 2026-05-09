package com.strefagentelmena.functions.calendarUiFunctions

import android.annotation.SuppressLint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.models.calendarUiModel.CalendarUiModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit
import java.net.HttpURLConnection
import java.net.URL
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarDataSource {
    private val gson = Gson()
    private val holidayApiBaseUrl = "https://date.nager.at/api/v3/PublicHolidays"
    private val loadedRemoteYears = mutableSetOf<Int>()
    private val remotePolishHolidays = mutableMapOf<LocalDate, String>()

    private val fixedPolishHolidays = mapOf(
        MonthDay.of(1, 1) to "Nowy Rok",
        MonthDay.of(1, 6) to "Swieto Trzech Kroli",
        MonthDay.of(5, 1) to "Swieto Pracy",
        MonthDay.of(5, 3) to "Swieto Konstytucji 3 Maja",
        MonthDay.of(8, 15) to "Wniebowziecie Najswietszej Maryi Panny",
        MonthDay.of(11, 1) to "Wszystkich Swietych",
        MonthDay.of(11, 11) to "Narodowe Swieto Niepodleglosci",
        MonthDay.of(12, 24) to "Wigilia Bozego Narodzenia",
        MonthDay.of(12, 25) to "Boze Narodzenie (pierwszy dzien)",
        MonthDay.of(12, 26) to "Boze Narodzenie (drugi dzien)",
    )

    private data class NagerHolidayDto(
        val date: String,
        val localName: String?,
        val name: String?
    )

    val today: LocalDate
        @SuppressLint("NewApi")
        get() {
            return LocalDate.now()
        }


    @SuppressLint("NewApi")
    fun getData(startDate: LocalDate = today, lastSelectedDate: LocalDate): CalendarUiModel {
        val firstDayOfWeek = startDate.with(DayOfWeek.MONDAY)
        val endDayOfWeek = firstDayOfWeek.plusDays(7)
        val visibleDates = getDatesBetween(firstDayOfWeek, endDayOfWeek)
        return toUiModel(visibleDates, lastSelectedDate)
    }

    @SuppressLint("NewApi")
    fun toDateUiModel(date: LocalDate, isSelectedDate: Boolean): CalendarUiModel.Date {
        val holidayName = getPolishHolidayName(date)
        return CalendarUiModel.Date(
            isSelected = isSelectedDate,
            isToday = date.isEqual(today),
            isWeekend = isWeekend(date),
            isHoliday = holidayName != null,
            holidayName = holidayName,
            date = date,
        )
    }

    @SuppressLint("NewApi")
    suspend fun ensurePolishHolidaysInRange(dates: List<LocalDate>) {
        val years = dates.map { it.year }.toSet()
        years.forEach { year ->
            ensurePolishHolidaysForYear(year)
        }
    }

    @SuppressLint("NewApi")
    private suspend fun ensurePolishHolidaysForYear(year: Int) {
        if (loadedRemoteYears.contains(year)) return
        val remoteHolidays = fetchRemotePolishHolidays(year) ?: return

        remoteHolidays.forEach { holiday ->
            val parsedDate = runCatching { LocalDate.parse(holiday.date) }.getOrNull() ?: return@forEach
            val holidayName = holiday.localName ?: holiday.name ?: return@forEach
            remotePolishHolidays[parsedDate] = holidayName
        }

        loadedRemoteYears.add(year)
    }

    @SuppressLint("NewApi")
    private fun getDatesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val numOfDays = ChronoUnit.DAYS.between(startDate, endDate)
        return Stream.iterate(startDate) { date ->
            date.plusDays(/* daysToAdd = */ 1)
        }
            .limit(numOfDays)
            .collect(Collectors.toList())
    }

    @SuppressLint("NewApi")
    private fun toUiModel(
        dateList: List<LocalDate>,
        lastSelectedDate: LocalDate
    ): CalendarUiModel {
        return CalendarUiModel(
            selectedDate = toItemUiModel(lastSelectedDate, true),
            visibleDates = dateList.map {
                toItemUiModel(it, it.isEqual(lastSelectedDate))
            },
        )
    }

    @SuppressLint("NewApi")
    private fun toItemUiModel(date: LocalDate, isSelectedDate: Boolean): CalendarUiModel.Date {
        return toDateUiModel(date, isSelectedDate)
    }

    @SuppressLint("NewApi")
    private fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }

    @SuppressLint("NewApi")
    private fun getPolishHolidayName(date: LocalDate): String? {
        remotePolishHolidays[date]?.let { return it }
        fixedPolishHolidays[MonthDay.from(date)]?.let { return it }

        val easterSunday = getEasterSunday(date.year)
        val easterMonday = easterSunday.plusDays(1)
        val pentecost = easterSunday.plusDays(49)
        val corpusChristi = easterSunday.plusDays(60)

        return when (date) {
            easterSunday -> "Wielkanoc"
            easterMonday -> "Poniedzialek Wielkanocny"
            pentecost -> "Zeslanie Ducha Swietego"
            corpusChristi -> "Boze Cialo"
            else -> null
        }
    }

    private suspend fun fetchRemotePolishHolidays(year: Int): List<NagerHolidayDto>? {
        return withContext(Dispatchers.IO) {
            val url = URL("$holidayApiBaseUrl/$year/PL")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }

            try {
                if (connection.responseCode !in 200..299) return@withContext null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val listType = object : TypeToken<List<NagerHolidayDto>>() {}.type
                gson.fromJson<List<NagerHolidayDto>>(body, listType)
            } catch (_: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }
    }

    @SuppressLint("NewApi")
    private fun getEasterSunday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day)
    }
}
