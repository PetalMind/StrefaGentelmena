package com.strefagentelmena.models

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

class
Customer(
    var id: Int? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var appointment: Appointment? = null,
    var lastAppointmentDate: String? = null,
    var phoneNumber: String? = null,
    var agreedToBeNotify: Boolean = false,
) {
    val fullName: String
        get() = "$firstName $lastName"

    fun copy(
        id: Int? = this.id,
        firstName: String? = this.firstName,
        lastName: String? = this.lastName,
        appointment: Appointment? = this.appointment,
        lastAppointmentDate: String? = this.lastAppointmentDate,
        phoneNumber: String? = this.phoneNumber,
        agreedToBeNotify: Boolean = this.agreedToBeNotify
    ): Customer {
        return Customer(
            id,
            firstName,
            lastName,
            appointment,
            lastAppointmentDate,
            phoneNumber,
            agreedToBeNotify
        )
    }
}
