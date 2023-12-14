package com.strefagentelmena.models

import com.strefagentelmena.models.AppoimentsModel.Appointment

class CustomerIdGenerator {
    private val availableIds = (1..15000).toMutableSet()

    /**
     * Generate Id.
     * Użycie:
     * val id1 = idGenerator.generateId()
     * val customer1 = Customer(id1, "Jan", "Kowalski", "2023-10-01", "123456789")
     *
     * @return [Int]
     */
    fun generateId(): Int {
        return if (availableIds.isNotEmpty()) {
            val id = availableIds.random()
            availableIds.remove(id)
            id
        } else {
            0 // Brak dostępnych ID
        }
    }
}

class Customer(
    var id: Int = 0,
    var firstName: String = "",
    var lastName: String = "",
    var appointment: Appointment? = null,
    var lastAppointmentDate: String = "",
    var phoneNumber: String = "",
    var agreedToBeNotify: Boolean = false,
    var noted: String = "",
) {
    val fullName: String
        get() = "$firstName $lastName"

    fun copy(
        id: Int = this.id,
        firstName: String = this.firstName,
        lastName: String = this.lastName,
        appointment: Appointment = this.appointment ?: Appointment(),
        lastAppointmentDate: String = this.lastAppointmentDate,
        phoneNumber: String = this.phoneNumber,
        agreedToBeNotify: Boolean = this.agreedToBeNotify,
        noted: String = this.noted
    ): Customer {
        return Customer(
            id,
            firstName,
            lastName,
            appointment,
            lastAppointmentDate,
            phoneNumber,
            agreedToBeNotify,
            noted
        )
    }
}
