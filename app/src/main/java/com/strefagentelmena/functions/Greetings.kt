package com.strefagentelmena.functions

val greetingsManager = Greetings()

class Greetings {


    /**
     * Generate greeting
     *
     * @return
     */
    private fun generateGreeting(name: String): List<String> {
        return listOf(
            "Miło Cię widzieć $name",
            "Dzień dobry, $name!",
            "Witam w królestwie elegancji",
            "$name, witaj w swoim imperium!",
            "Cześć $name, co u Ciebie?",
            "Dobrze Cię widzieć, $name!",
            "Hej $name, co u Ciebie słychać?",
            "Witaj $name, jak się masz?",
            "Dobrze Cię tu widzieć, $name!",
        )
    }

    fun randomGreeting(name: String): String {
        val greetings = generateGreeting(name)
        return greetings.random() ?: "Brak pozdrowień, coś poszło nie tak."
    }
}