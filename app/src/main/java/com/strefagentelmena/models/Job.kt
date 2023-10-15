package com.strefagentelmena.models

class Job(
    val jobId: Int?,
    val customerId: Int?, // odniesienie do ID klienta
    val haircutType: HaircutType, // rodzaj strzyżenia
    val appointmentDate: String, // data i czas wizyty
    val isNotify: Boolean = false,
    val notifyDate: String? = null,
    val isCompleted: Boolean = false, // czy praca została zakończona
)
