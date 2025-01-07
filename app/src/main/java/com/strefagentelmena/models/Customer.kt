package com.strefagentelmena.models

import com.strefagentelmena.models.appoimentsModel.Appointment

class Customer(
    var id: Int = 0,  // Teraz możesz ręcznie ustawić ID
    var firstName: String = "",
    var lastName: String = "",
    var appointment: Appointment? = null,
    var phoneNumber: String = "",
    var noted: String = "",
) {
    val fullName: String
        get() = "$firstName $lastName"

    fun copy(
        id: Int = this.id,
        firstName: String = this.firstName,
        lastName: String = this.lastName,
        appointment: Appointment? = this.appointment ?: null,
        phoneNumber: String = this.phoneNumber,
        noted: String = this.noted
    ): Customer {
        return Customer(
            id,
            firstName,
            lastName,
            appointment,
            phoneNumber,
            noted
        )
    }
}

