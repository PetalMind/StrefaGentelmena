package com.strefagentelmena.navigation

import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.effectiveCustomerId

/**
 * Jednorazowe intencje nawigacji z dashboardu (osobne ViewModele na ekranach).
 */
object NavPendingActions {

    data class ScheduleNewVisitPrefill(
        val customerId: Int,
        val fallbackCustomer: Customer,
        /** Ustawiane przy „kolejnej wizycie” po trwającej — data, start od końca wizyty, pracownik. */
        val followUpFromAppointment: Appointment? = null,
    )

    @Volatile
    private var pendingCustomerEditId: Int? = null

    @Volatile
    private var pendingSchedulePrefill: ScheduleNewVisitPrefill? = null

    fun requestOpenCustomerEditor(customerId: Int) {
        if (customerId > 0) {
            synchronized(this) { pendingCustomerEditId = customerId }
        }
    }

    fun peekCustomerEditId(): Int? = synchronized(this) { pendingCustomerEditId }

    fun consumeCustomerEditId() {
        synchronized(this) { pendingCustomerEditId = null }
    }

    fun requestScheduleNewVisit(customerId: Int, fallbackCustomer: Customer) {
        synchronized(this) {
            pendingSchedulePrefill = ScheduleNewVisitPrefill(customerId, fallbackCustomer, null)
        }
    }

    /** Nowa wizyta dla tego samego klienta: ta sama data co bieżąca, start od [appointment.endTime], ten sam pracownik. */
    fun requestScheduleFollowUpVisit(appointment: Appointment) {
        synchronized(this) {
            pendingSchedulePrefill = ScheduleNewVisitPrefill(
                appointment.effectiveCustomerId(),
                appointment.customer,
                appointment,
            )
        }
    }

    fun takeSchedulePrefillWhenReady(customers: List<Customer>): ScheduleNewVisitPrefill? =
        synchronized(this) {
            val p = pendingSchedulePrefill ?: return null
            if (p.customerId > 0 && customers.isEmpty()) return null
            pendingSchedulePrefill = null
            p
        }
}
