package com.strefagentelmena.functions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

val appFunctions = AppFunctions()

class AppFunctions {
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

    fun getCurrentWeekDays(formattedDate: String): List<Int> {
        val dateParts = formattedDate.split(".")
        val day = dateParts[0].toInt()
        val month = dateParts[1].toInt() - 1 // miesiące w kalendarzu zaczynają się od 0
        val year = dateParts[2].toInt()

        val calendar = Calendar.getInstance().apply {
            set(year, month, day)
        }

        val daysList = mutableListOf<Int>()

        // Ustawianie kalendarza na poniedziałek tego tygodnia
        val difference = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_MONTH, -difference)

        // Pobieranie dni dla tego tygodnia
        for (i in 0..6) {
            daysList.add(calendar.get(Calendar.DAY_OF_MONTH))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return daysList
    }

}
