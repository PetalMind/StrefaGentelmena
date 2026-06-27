package com.strefagentelmena.functions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

val smsManager = SMSManager()

/** Okno wysyłki: [start, end) w jednym dniu lub przez północ gdy end < start. */
internal fun isNotificationSendWindow(nowLocal: LocalTime, start: LocalTime, end: LocalTime): Boolean {
    return if (!end.isBefore(start)) {
        !nowLocal.isBefore(start) && nowLocal.isBefore(end)
    } else {
        !nowLocal.isBefore(start) || nowLocal.isBefore(end)
    }
}

enum class SmsSendFailureReason {
    OUTSIDE_REMINDER_DATE,
    OUTSIDE_SEND_WINDOW,
    INVALID_PHONE,
    EMPTY_MESSAGE,
    MESSAGE_TOO_LONG,
    MISSING_SEND_SMS_PERMISSION,
    SMS_NOT_AVAILABLE,
    API_ERROR,
}

data class SmsSendResult(
    val sent: Boolean,
    val failureReason: SmsSendFailureReason? = null,
)

class SMSManager {

    @Volatile
    private var appContext: Context? = null

    fun attachApplicationContext(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Wysyłka przypomnienia SMS, jeśli wizyta jest dziś lub jutro (kalendarzowo)
     * i bieżąca godzina mieści się w ustawionym oknie (także przez północ).
     *
     * @return true tylko po pomyślnym wywołaniu wysyłki do operatora.
     */
    /**
     * @param phoneNumberOverride numer rodzica przy wizycie dziecka; null = [Appointment.customer.phoneNumber].
     */
    fun sendNotification(
        appointment: Appointment,
        profile: ProfilePreferences,
        phoneNumberOverride: String? = null,
        ignoreReminderSchedule: Boolean = false,
    ): Boolean = sendNotificationDetailed(
        appointment = appointment,
        profile = profile,
        phoneNumberOverride = phoneNumberOverride,
        ignoreReminderSchedule = ignoreReminderSchedule,
    ).sent

    fun sendNotificationDetailed(
        appointment: Appointment,
        profile: ProfilePreferences,
        phoneNumberOverride: String? = null,
        ignoreReminderSchedule: Boolean = false,
    ): SmsSendResult {
        if (!ignoreReminderSchedule) {
            val currentTime = LocalDateTime.now()
            val appointmentDateTime = LocalDateTime.parse(
                "${appointment.date} ${appointment.startTime}",
                DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
            )

            val startTime = LocalTime.parse(profile.notificationSendStartTime, DateTimeFormatter.ofPattern("HH:mm"))
            val endTime = LocalTime.parse(profile.notificationSendEndTime, DateTimeFormatter.ofPattern("HH:mm"))

            val daysBetween = ChronoUnit.DAYS.between(currentTime.toLocalDate(), appointmentDateTime.toLocalDate())
            if (daysBetween != 0L && daysBetween != 1L) {
                return SmsSendResult(false, SmsSendFailureReason.OUTSIDE_REMINDER_DATE)
            }

            if (!isNotificationSendWindow(currentTime.toLocalTime(), startTime, endTime)) {
                return SmsSendResult(false, SmsSendFailureReason.OUTSIDE_SEND_WINDOW)
            }
        }

        val phone = phoneNumberOverride?.takeIf { it.isNotBlank() } ?: appointment.customer.phoneNumber
        return sendSMS(phone, appointmentConfirmationMessage(appointment))
    }

    private fun appointmentConfirmationMessage(appointment: Appointment): String =
        "Dzień dobry! Potwierdzamy Twoją wizytę w Strefie Gentlemana Kinga Kloss w dniu ${appointment.date} o godzinie ${appointment.startTime}. W razie zmian prosimy o telefon na numer 724 506 728."

    private fun resolveSmsManager(): SmsManager {
        val ctx = appContext
        return if (ctx != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    /**
     * @return false przy odrzuceniu numeru/treści lub błędzie API.
     */
    private fun sendSMS(phoneNumber: String?, message: String): SmsSendResult {
        val e164 = normalizePolishPhoneToE164(phoneNumber)
        if (e164 == null) {
            Log.w(TAG, "Nieprawidłowy numer telefonu: ${phoneNumber?.take(32)}")
            return SmsSendResult(false, SmsSendFailureReason.INVALID_PHONE)
        }

        if (message.isBlank()) {
            Log.w(TAG, "Pusta treść SMS.")
            return SmsSendResult(false, SmsSendFailureReason.EMPTY_MESSAGE)
        }

        if (message.length > MAX_SMS_BODY_LENGTH) {
            Log.w(TAG, "Treść SMS przekracza limit (${message.length} > $MAX_SMS_BODY_LENGTH).")
            return SmsSendResult(false, SmsSendFailureReason.MESSAGE_TOO_LONG)
        }

        val ctx = appContext
        if (ctx != null && !ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            Log.w(TAG, "Urządzenie nie obsługuje wysyłania SMS.")
            return SmsSendResult(false, SmsSendFailureReason.SMS_NOT_AVAILABLE)
        }

        if (ctx != null && ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Brak uprawnienia SEND_SMS.")
            return SmsSendResult(false, SmsSendFailureReason.MISSING_SEND_SMS_PERMISSION)
        }

        return try {
            val mgr = resolveSmsManager()
            val parts = mgr.divideMessage(message)
            mgr.sendMultipartTextMessage(e164, null, parts, null, null)
            Log.d(TAG, "Wysłano SMS na numer $e164")
            SmsSendResult(true)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wysyłania SMS", e)
            SmsSendResult(false, SmsSendFailureReason.API_ERROR)
        }
    }

    private companion object {
        private const val TAG = "SMSManager"
        /** Limit bezpieczeństwa; długie treści dzielone są na segmenty GSM. */
        private const val MAX_SMS_BODY_LENGTH = 4096
    }
}
