package com.strefagentelmena.functions

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

val appFunctions = AppFunctions()

class AppFunctions {
    @SuppressLint("QueryPermissionsNeeded")
    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

//    fun getCurrentWeekDays(dateString: String): List<Int> {
//        val simpleDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
//        val date =
//            simpleDateFormat.parse(dateString) ?: simpleDateFormat.parse("01.01.2000")
//
//        val calendar = Calendar.getInstance()
//        calendar.time = date
//        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
//        val daysList = mutableListOf<Int>()
//
//        // Ustawianie kalendarza na poniedziałek tego tygodnia
//        calendar.add(Calendar.DAY_OF_MONTH, Calendar.MONDAY - currentDayOfWeek)
//
//        // Pobieranie dni dla tego tygodnia
//        for (i in 0..6) {
//            daysList.add(calendar.get(Calendar.DAY_OF_MONTH))
//            calendar.add(Calendar.DAY_OF_MONTH, 1)
//        }
//
//        return daysList
//    }

    fun isEndOfMonth(day: Int, month: Int, year: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, day)

        val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return day == lastDayOfMonth
    }

    fun isFirstDayOfNewMonth(day: Int, month: Int, year: Int): Boolean {
        val currentDate = LocalDate.of(year, month, day)
        val nextDay = currentDate.plusDays(1)

        return currentDate.month != nextDay.month
    }


    @SuppressLint("SimpleDateFormat")
    fun getCurrentFormattedDate(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy")
        return dateFormat.format(calendar.time)
    }

    fun getCurrentWeekDays(formattedDate: String): List<Pair<Int, Boolean>> {
        // Rozdzielenie daty na części
        val dateParts = formattedDate.split(".")
        val day = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1 // Miesiące w kalendarzu zaczynają się od 0
        val year = dateParts[2].toInt()

        // Utworzenie instancji kalendarza i ustawienie na podaną datę
        val calendar = Calendar.getInstance().apply {
            set(year, month, day)
        }

        val daysList = mutableListOf<Pair<Int, Boolean>>()

        // Ustawienie kalendarza na początek tygodnia (poniedziałek)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val difference = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -difference)

        // Sprawdzenie, czy data należy do aktualnego miesiąca
        fun isCurrentMonth(): Boolean {
            return calendar.get(Calendar.MONTH) == month
        }

        // Pobieranie dni dla tego tygodnia
        repeat(7) {
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val isFirstDayOfMonth = currentDay == 1

            if (isCurrentMonth()) {
                daysList.add(currentDay to isFirstDayOfMonth)
            }

            if (calendar.get(Calendar.DAY_OF_MONTH) == calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                // Jeśli osiągnięto ostatni dzień miesiąca, ustaw na 1. dzień następnego miesiąca
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, 1)
            } else {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        return daysList
    }



    fun getPreviousWeek(currentFormattedDate: String): String {
        val currentDate = LocalDate.parse(currentFormattedDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val previousWeekDate = currentDate.minusWeeks(1)

        // Sprawdź, czy miesiąc zmienił się po odjęciu tygodnia
        if (currentDate.month != previousWeekDate.month) {
            // Jeśli tak, zwróć ostatni dzień poprzedniego miesiąca
            val lastDayOfPreviousMonth = previousWeekDate.withDayOfMonth(previousWeekDate.month.length(previousWeekDate.isLeapYear))
            return lastDayOfPreviousMonth.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }

        return previousWeekDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }
    fun getNextWeek(currentFormattedDate: String): String {
        val currentDate = LocalDate.parse(currentFormattedDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val nextSunday = currentDate.plusDays(7 - currentDate.dayOfWeek.value.toLong())
        val nextWeekDate = nextSunday.plusWeeks(1)

        // Sprawdź, czy miesiąc zmienił się po dodaniu tygodnia
        if (currentDate.month != nextWeekDate.month) {
            // Jeśli ostatni dzień tygodnia jest ostatnim dniem miesiąca, zwróć pierwszy dzień nowego miesiąca
            if (isFirstDayOfNewMonth(nextSunday.dayOfMonth, nextSunday.monthValue, nextSunday.year)) {
                return nextWeekDate.withDayOfMonth(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }
        }

        if (currentDate.dayOfWeek == DayOfWeek.SUNDAY) {
            return currentFormattedDate
        }

        return nextWeekDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }
}
