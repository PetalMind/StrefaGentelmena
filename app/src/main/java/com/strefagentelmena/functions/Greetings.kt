package com.strefagentelmena.functions
val greetingsManager = Greetings()
class Greetings {
    var name: String = ""
        set(value) {
            field = value
            generateAndPrintGreetings()
        }

    /**
     * Generate greeting
     *
     * @return
     */
    private fun generateGreeting(): List<String> {
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


    private fun generateAndPrintGreetings() {
        val greetings = generateGreeting()
        greetings.forEach { println(it) }
    }

    fun randomGreeting(): String {
        val greetings = generateGreeting()
        return greetings.random() ?: "Brak pozdrowień, coś poszło nie tak."
    }
}