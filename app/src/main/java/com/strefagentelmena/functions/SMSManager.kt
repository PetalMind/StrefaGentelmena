package com.strefagentelmena.functions

import android.content.Context
import android.telephony.SmsManager
import com.strefagentelmena.models.Appointment
import com.strefagentelmena.viewModel.DashboardModelView
import com.strefagentelmena.viewModel.ScheduleModelView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

val smsManager = SMSManager()

class SMSManager {
    /**
     * Send Notification.
     *
     * @param appointment
     * @param viewModel
     * @param context
     */
    fun sendNotification(
        appointment: Appointment,
    ) {
        if (appointment.notificationSent) return

        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val currentDate = Calendar.getInstance()
        val currentHour = currentDate.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentDate.get(Calendar.MINUTE)

        // Konwersja bieżącej godziny i minuty na minuty od północy
        val currentTimeInMinutes = currentHour * 60 + currentMinute

        val appointmentDateTimeStr = "${appointment.date} ${appointment.startTime}"
        val appointmentDateTime = format.parse(appointmentDateTimeStr) ?: return@sendNotification

        // Dodane w celu uniknięcia potencjalnych problemów z wartością null

        val appointmentDateCal = Calendar.getInstance()
        appointmentDateCal.time = appointmentDateTime

        // Resetowanie godzin, minut i sekund na obu kalendarzach, aby porównać tylko daty
        currentDate.set(Calendar.HOUR_OF_DAY, 0)
        currentDate.set(Calendar.MINUTE, 0)
        currentDate.set(Calendar.SECOND, 0)
        currentDate.set(Calendar.MILLISECOND, 0)

        appointmentDateCal.set(Calendar.HOUR_OF_DAY, 0)
        appointmentDateCal.set(Calendar.MINUTE, 0)
        appointmentDateCal.set(Calendar.SECOND, 0)
        appointmentDateCal.set(Calendar.MILLISECOND, 0)

        // Obliczenie różnicy w dniach
        val diff = appointmentDateCal.timeInMillis - currentDate.timeInMillis
        val daysDifference = TimeUnit.MILLISECONDS.toDays(diff)

        // Definiowanie granic czasowych dla wysyłania SMS
        val earliestTimeInMinutes = 7 * 60 + 30  // 7:30
        val latestTimeInMinutes = 23 * 60 + 40    // 22:00

        if (daysDifference == 1L && currentTimeInMinutes in earliestTimeInMinutes..latestTimeInMinutes) {
            sendSMS(
                appointment.customer.phoneNumber,
                "Przypominamy o wizycie w dniu ${appointment.date} o godzinie ${appointment.startTime} w Strefie Gentlemana Kinga Kloss, adres: Łaska 4, Zduńska Wola."
            )
        }
    }

    /**
     * Send Sms.
     *
     * @param phoneNumber
     * @param message
     */
    private fun sendSMS(phoneNumber: String?, message: String) {
        // Zabezpieczenie przed pustym lub niewłaściwym numerem telefonu
        if (phoneNumber.isNullOrEmpty() || !phoneNumber.matches(Regex("[0-9]+"))) {
            println("Nieprawidłowy numer telefonu.")
            return
        }

        // Zabezpieczenie przed zbyt długą wiadomością
        if (message.length > 160) {
            println("Wiadomość jest zbyt długa.")
            return
        }

        // Zabezpieczenie przed pustą wiadomością
        if (message.isEmpty()) {
            println("Wiadomość jest pusta.")
            return
        }

        val smsManager = SmsManager.getDefault()
        val fullPhoneNumber = "+48$phoneNumber"
        val parts = smsManager.divideMessage(message)

        try {
            smsManager.sendMultipartTextMessage(fullPhoneNumber, null, parts, null, null)
            println("Wysłano SMS na numer $fullPhoneNumber: $message")
        } catch (e: Exception) {
            println("Wystąpił błąd podczas wysyłania SMS-a: ${e.message}")
        }
    }
}
