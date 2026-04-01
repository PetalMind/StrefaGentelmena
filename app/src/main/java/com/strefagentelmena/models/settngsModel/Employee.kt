package com.strefagentelmena.models.settngsModel


class Employee(
    var id: Int? = null,
    var name: String = "",
    var surname: String = "",
    /** Godzina rozpoczęcia pracy (format `HH:mm`), np. dla walidacji wizyt. */
    var workStartTime: String = DEFAULT_WORK_START,
    /** Godzina zakończenia pracy (format `HH:mm`). */
    var workEndTime: String = DEFAULT_WORK_END,
    /** Początek urlopu (`dd.MM.yyyy`); puste = brak urlopu. */
    var vacationFrom: String = "",
    /** Koniec urlopu (`dd.MM.yyyy`); puste przy ustawionym [vacationFrom] = jeden dzień. */
    var vacationTo: String = "",
    /**
     * Początek niedostępności w dniu urlopu (`HH:mm`). Oba pola puste = cały dzień wolny w każdym dniu zakresu.
     * Gdy oba ustawione — ten sam przedział **każdego dnia** od [vacationFrom] do [vacationTo].
     */
    var vacationTimeFrom: String = "",
    var vacationTimeTo: String = "",
) {
    /** Pełna etykieta do list wyboru i zapisu (np. w Firestore `displayName`). */
    val displayName: String
        get() = "${name.trim()} ${surname.trim()}".trim()

    fun copy(
        id: Int = this.id ?: 1,
        name: String = this.name,
        surname: String = this.surname,
        workStartTime: String = this.workStartTime,
        workEndTime: String = this.workEndTime,
        vacationFrom: String = this.vacationFrom,
        vacationTo: String = this.vacationTo,
        vacationTimeFrom: String = this.vacationTimeFrom,
        vacationTimeTo: String = this.vacationTimeTo,
    ): Employee {
        return Employee(
            id, name, surname, workStartTime, workEndTime,
            vacationFrom, vacationTo, vacationTimeFrom, vacationTimeTo,
        )
    }

    companion object {
        const val DEFAULT_WORK_START = "06:00"
        const val DEFAULT_WORK_END = "18:00"
    }
}
