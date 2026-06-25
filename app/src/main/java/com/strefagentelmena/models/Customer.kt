package com.strefagentelmena.models

import com.strefagentelmena.models.appoimentsModel.Appointment
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class Customer(
    var id: Int = 0,  // Teraz możesz ręcznie ustawić ID
    var firstName: String = "",
    var lastName: String = "",
    var appointment: Appointment? = null,
    var phoneNumber: String = "",
    var email: String = "",
    var noted: String = "",
    /**
     * Historia notatek po normalizacji to [ArrayList] [CustomerNote].
     * W Firebase pole `notes` bywa czasem zapisane jako pojedynczy string — wtedy deserializacja daje [String], nie listę.
     */
    var notes: Any? = null,
    /** Ostatnia wizyta — preferowane przy sortowaniu i liście; stare dane tylko w [appointment]. */
    var lastVisit: VisitSummary? = null,
    /** Liczba wizyt w harmonogramie — denormalizacja z [Appointments], aktualizowana przy zmianach. */
    var visitCount: Int = 0,
    /** Średni odstęp między kolejnymi wizytami (tygodnie), 0 przy mniej niż dwóch wizytach. */
    var avgWeeksBetweenVisits: Double = 0.0,
    /** 0 = konto główne (rodzic / osoba bez powiązania); inaczej ID rodzica w [Customers]. */
    var parentCustomerId: Int = 0,
    /** Data urodzenia `dd.MM.yyyy`; pusta gdy nieznana. Wiek liczony w locie (aktualizuje się co rok). */
    var birthDate: String = "",
    /** Soft-delete: rekord pozostaje w Firebase, ale nie jest pokazywany w aplikacji. */
    var deleted: Boolean = false,
) {
    val fullName: String
        get() = "$firstName $lastName"

    fun copy(
        id: Int = this.id,
        firstName: String = this.firstName,
        lastName: String = this.lastName,
        appointment: Appointment? = this.appointment ?: null,
        phoneNumber: String = this.phoneNumber,
        email: String = this.email,
        noted: String = this.noted,
        notes: Any? = this.notes,
        lastVisit: VisitSummary? = this.lastVisit,
        visitCount: Int = this.visitCount,
        avgWeeksBetweenVisits: Double = this.avgWeeksBetweenVisits,
        parentCustomerId: Int = this.parentCustomerId,
        birthDate: String = this.birthDate,
        deleted: Boolean = this.deleted,
    ): Customer {
        return Customer(
            id,
            firstName,
            lastName,
            appointment,
            phoneNumber,
            email,
            noted,
            notes,
            lastVisit,
            visitCount,
            avgWeeksBetweenVisits,
            parentCustomerId,
            birthDate,
            deleted,
        )
    }
}

private val customerBirthDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun Customer.parseCustomerBirthDateOrNull(): LocalDate? =
    birthDate.trim().takeIf { it.isNotBlank() }?.let {
        runCatching { LocalDate.parse(it, customerBirthDateFormatter) }.getOrNull()
    }

/** Wiek w latach (dzisiaj), null gdy brak poprawnej [birthDate]. */
fun Customer.computedAgeYears(): Int? {
    val bd = parseCustomerBirthDateOrNull() ?: return null
    return ChronoUnit.YEARS.between(bd, LocalDate.now()).toInt().coerceAtLeast(0)
}

fun formatAgeYearsPl(years: Int): String {
    val y = years.coerceIn(0, 150)
    val n = y % 10
    val nn = y % 100
    val w = when {
        y == 1 -> "rok"
        n in 2..4 && nn !in 12..14 -> "lata"
        else -> "lat"
    }
    return "$y $w"
}

/** Krótka etykieta np. „7 lat” — do listy / profilu. */
fun Customer.ageShortLabel(): String? =
    computedAgeYears()?.let { formatAgeYearsPl(it) }

/** Klienci bez przypisanego rodzica — do wyboru „konta” przy wizycie. */
fun List<Customer>.familyRootAccounts(): List<Customer> =
    filter { it.parentCustomerId == 0 }.sortedBy { it.fullName }

fun List<Customer>.childrenOfParent(parentId: Int): List<Customer> =
    filter { it.parentCustomerId == parentId }.sortedBy { it.fullName }

/** Notatki od najnowszej (po normalizacji z Firebase). */
fun Customer.notesOrderedNewestFirst(): List<CustomerNote> {
    val raw = parseNotesFromFirebaseRaw(notes)?.filter { it.text.isNotBlank() }.orEmpty()
    if (raw.isNotEmpty()) return raw.sortedByDescending { it.addedAtMillis }
    if (noted.isNotBlank()) return listOf(CustomerNote(noted.trim(), 0L))
    return emptyList()
}
