package com.strefagentelmena.models

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.appoimentsModel.effectiveEmployeeId
import com.strefagentelmena.models.settngsModel.Employee

/** Pracownik osadzony w wizycie — jawne pola, bez rozszerzania grafu obiektów przy zapisie. */
fun Employee.toEmbeddedInAppointment(): Employee = Employee(
    id = id,
    name = name,
    surname = surname,
    workStartTime = workStartTime,
    workEndTime = workEndTime,
)

/**
 * Odczyt pola `notes` z Firebase (lista map / string / już sparsowana lista w pamięci).
 */
fun parseNotesFromFirebaseRaw(raw: Any?): ArrayList<CustomerNote>? {
    when (raw) {
        null -> return null
        is String -> {
            val t = raw.trim()
            return if (t.isEmpty()) null else arrayListOf(CustomerNote(t, 0L))
        }
        is List<*> -> {
            val out = ArrayList<CustomerNote>()
            for (e in raw) {
                when (e) {
                    is CustomerNote -> if (e.text.isNotBlank()) out.add(e)
                    is Map<*, *> -> {
                        val text = (e["text"] as? String)?.trim().orEmpty()
                        val ms = (e["addedAtMillis"] as? Number)?.toLong() ?: 0L
                        if (text.isNotBlank()) out.add(CustomerNote(text, ms))
                    }
                    is String -> if (e.isNotBlank()) out.add(CustomerNote(e.trim(), 0L))
                }
            }
            return out.takeIf { it.isNotEmpty() }
        }
        else -> return null
    }
}

/** Klient osadzony w wizycie — bez zagnieżdżeń, żeby uniknąć rekurencji w JSON Firebase. */
fun Customer.toEmbeddedInAppointment(): Customer = Customer(
    id = id,
    firstName = firstName,
    lastName = lastName,
    phoneNumber = phoneNumber,
    email = email,
    notes = null,
    noted = "",
    parentCustomerId = parentCustomerId,
    birthDate = birthDate,
)

private val visitDateForSort = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun Customer.lastVisitSortKey(): String =
    lastVisit?.date?.takeIf { it.isNotBlank() } ?: appointment?.date.orEmpty()

/** Dzień epoki dla sortowania chronologicznego; null gdy brak daty lub nieparsowalny format. */
fun Customer.lastVisitEpochDayOrNull(): Long? {
    val key = lastVisitSortKey()
    if (key.isBlank()) return null
    return runCatching { LocalDate.parse(key, visitDateForSort).toEpochDay() }.getOrNull()
}

/**
 * Po odczycie z Firebase: uzupełnia [lastVisit] ze starego [appointment], przycina zagnieżdżonego klienta w [appointment].
 */
fun Customer.normalizedAfterFirebaseLoad(): Customer {
    val visit = lastVisit ?: appointment?.let { VisitSummary.fromAppointment(it) }
    val slimAppointment = appointment?.copy(customer = toEmbeddedInAppointment())
    val fromNotesField = parseNotesFromFirebaseRaw(notes)
    val noteList: ArrayList<CustomerNote>? = when {
        !fromNotesField.isNullOrEmpty() -> fromNotesField
        noted.isNotBlank() -> arrayListOf(CustomerNote(noted.trim(), 0L))
        else -> null
    }
    return copy(lastVisit = visit, appointment = slimAppointment, notes = noteList)
}

fun Customer.withSyncedLastVisit(appointment: Appointment): Customer = copy(
    lastVisit = VisitSummary.fromAppointment(appointment),
    appointment = appointment.copy(customer = toEmbeddedInAppointment()),
)

fun Appointment.effectiveCustomerId(): Int = when {
    customerId != 0 -> customerId
    else -> customer.id
}

/** Po odczycie z Firebase: spójne [customerId] i „płaski” klient w wizycie. */
fun Appointment.normalizedAfterFirebaseLoad(): Appointment {
    val cid = effectiveCustomerId()
    val embedded = customer.toEmbeddedInAppointment()
    val eid = effectiveEmployeeId() ?: 0
    val slimEmployee = employee.toEmbeddedInAppointment()
    val sms = embedded.parentCustomerId.takeIf { it > 0 } ?: smsContactCustomerId.takeIf { it > 0 } ?: 0
    return copy(
        customerId = cid,
        customer = embedded,
        employeeId = eid,
        employee = slimEmployee,
        smsContactCustomerId = sms,
    )
}

/** Przed zapisem węzła Appointments — jawne ID + brak głębokiego zagnieżdżania klienta. */
fun Appointment.normalizedForFirebaseWrite(): Appointment {
    val eid = employee.id?.takeIf { it != 0 } ?: employeeId.takeIf { it != 0 } ?: 0
    return copy(
        customerId = customer.id,
        customer = customer.toEmbeddedInAppointment(),
        employeeId = eid,
        employee = employee.toEmbeddedInAppointment(),
        smsContactCustomerId = customer.parentCustomerId.takeIf { it > 0 } ?: 0,
    )
}

/**
 * Przed [setValue] na węźle `Customers` — bez rekurencji w [appointment], [lastVisit] uzupełnione ze starego snapshotu.
 */
fun Customer.trimmedForCustomerDocument(): Customer {
    val visit = lastVisit ?: appointment?.let { VisitSummary.fromAppointment(it) }
    val ap = appointment?.copy(customer = toEmbeddedInAppointment())
    val notesList = parseNotesFromFirebaseRaw(notes).orEmpty()
    val sorted = notesList
        .filter { it.text.isNotBlank() }
        .sortedByDescending { it.addedAtMillis }
    val listOut = if (sorted.isEmpty()) null else ArrayList(sorted)
    val latestNoted = sorted.firstOrNull()?.text?.trim().orEmpty().ifEmpty { noted.trim() }
    return copy(lastVisit = visit, appointment = ap, notes = listOut, noted = latestNoted)
}
