package com.strefagentelmena.models.settngsModel


class Employee(
var id: Int? = null,
var name: String = "",
    var surname: String = ""
) {
    fun copy(
        id: Int = this.id ?: 1,
        name: String = this.name,
        surname: String = this.surname
    ): Employee {
        return Employee(
            id, name, surname
        )
    }
}
