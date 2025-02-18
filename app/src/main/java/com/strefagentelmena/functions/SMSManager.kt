package com.strefagentelmena.functions

import android.telephony.SmsManager
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter

val smsManager = SMSManager()

class SMSManager {
    /**
     * Send Notification.
     *
     * @param appointment
     * @param viewModel
     * @param context
     */
    fun sendNotification(appointment: Appointment, profile: ProfilePreferences): Boolean {
        val currentTime = LocalDateTime.now()
        val appointmentDateTime = LocalDateTime.parse("${appointment.date} ${appointment.startTime}", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

        val startTime = LocalTime.parse(profile.notificationSendStartTime, DateTimeFormatter.ofPattern("HH:mm"))
        val endTime = LocalTime.parse(profile.notificationSendEndTime, DateTimeFormatter.ofPattern("HH:mm"))

        val daysDifference = Period.between(currentTime.toLocalDate(), appointmentDateTime.toLocalDate()).days

        val isEndTimeAfterMidnight = endTime.isBefore(startTime) // Check if end time is after midnight

        if ((daysDifference.toLong() == 1L || daysDifference.toLong() == 0L) && currentTime.toLocalTime().isAfter(startTime) &&
            (currentTime.toLocalTime().isBefore(endTime) || (isEndTimeAfterMidnight && currentTime.toLocalTime().isBefore(LocalTime.MAX)))) {
            sendSMS(
                appointment.customer.phoneNumber,
                "Czekamy na Ciebie w Strefie Gentlemana Kinga Kloss w dniu ${appointment.date} o godzinie ${appointment.startTime}. W razie zmian prosimy o telefon na numer 724 506 728 "
            )
            return true // Notification sent
        }

        return false // Notification not sent
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
