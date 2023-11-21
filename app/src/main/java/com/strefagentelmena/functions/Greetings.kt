package com.strefagentelmena.functions
val greetingsManager = Greetings()
class Greetings {
    private fun generateGreeting(name: String = "Kinga"): List<String> {
        return listOf(
            "Miło Cię widzieć $name",
            "Dzień dobry, $name!",
            "Witam w królestwie elegancji",
            "$name, witaj w swoim imperium!",
        )
    }

    fun randomGreeting(): String {
        return generateGreeting().randomOrNull() ?: "Brak pozdrowień, coś poszło nie tak."
    }
}