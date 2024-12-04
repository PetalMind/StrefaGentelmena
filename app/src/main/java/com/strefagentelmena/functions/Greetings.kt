package com.strefagentelmena.functions


import java.time.LocalDate
import java.time.LocalTime
import java.time.Month

class Greetings {

    /**
     * Generate greeting messages for a given name.
     *
     * @param name Name to include in the greetings.
     * @return List of personalized greeting messages.
     */
    private fun generateGreetings(name: String): List<String> {
        return listOf(
            "Miło Cię widzieć, $name!",
            "Dzień dobry, $name!",
            "Witam w królestwie elegancji!",
            "$name, witaj w swoim imperium!",
            "Cześć $name, co u Ciebie?",
            "Dobrze Cię widzieć, $name!",
            "Hej $name, co u Ciebie słychać?",
            "Witaj $name, jak się masz?",
            "Dobrze Cię tu widzieć, $name!"
        )
    }

    /**
     * Generate greeting messages for morning.
     *
     * @param name Name to include in the greetings.
     * @return List of morning greeting messages.
     */
    private fun generateMorningGreetings(name: String): List<String> {
        return listOf(
            "Dzień dobry, $name! Jak się spało?",
            "$name, poranna kawa już gotowa?",
            "Witaj $name, pora na dobry dzień!"
        )
    }

    /**
     * Generate greeting messages for winter.
     *
     * @param name Name to include in the greetings.
     * @return List of winter-themed greeting messages.
     */
    private fun generateWinterGreetings(name: String): List<String> {
        return listOf(
            "Zima przyszła, $name! Czas na ciepłą herbatę.",
            "$name, przywitaj zimowe dni!",
            "Śnieg pada, $name! Jak tam ciepłe skarpetki?"
        )
    }

    /**
     * Generate greeting messages for spring.
     *
     * @param name Name to include in the greetings.
     * @return List of spring-themed greeting messages.
     */
    private fun generateSpringGreetings(name: String): List<String> {
        return listOf(
            "Wiosna w pełni, $name! Jakie plany na dzisiaj?",
            "$name, czas na świeże powietrze i kwiaty!",
            "Ciepłe dni nadchodzą, $name! Witaj w wiosennej atmosferze!"
        )
    }

    /**
     * Generate greeting messages for summer.
     *
     * @param name Name to include in the greetings.
     * @return List of summer-themed greeting messages.
     */
    private fun generateSummerGreetings(name: String): List<String> {
        return listOf(
            "Lato w pełni, $name! Czas na plażę!",
            "$name, gorące dni czekają na Ciebie!",
            "Letnia energia, $name! Wykorzystaj ten dzień!"
        )
    }

    /**
     * Generate greeting messages for autumn.
     *
     * @param name Name to include in the greetings.
     * @return List of autumn-themed greeting messages.
     */
    private fun generateAutumnGreetings(name: String): List<String> {
        return listOf(
            "Jesień już tu jest, $name! Czas na ciepłe swetry.",
            "$name, złota jesień w pełni!",
            "Liście opadają, $name! Jakie plany na chłodne dni?"
        )
    }

    /**
     * Determine the current season based on the current date.
     *
     * @return The current season.
     */
    private fun getCurrentSeason(): String {
        val month = LocalDate.now().month
        return when (month) {
            Month.DECEMBER, Month.JANUARY, Month.FEBRUARY -> "winter"
            Month.MARCH, Month.APRIL, Month.MAY -> "spring"
            Month.JUNE, Month.JULY, Month.AUGUST -> "summer"
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> "autumn"
        }
    }

    /**
     * Determine the current part of the day (morning, afternoon, evening, night).
     *
     * @return The current part of the day.
     */
    private fun getCurrentPartOfDay(): String {
        val currentTime = LocalTime.now()
        return when {
            currentTime.isBefore(LocalTime.NOON) -> "morning"
            currentTime.isBefore(LocalTime.of(18, 0)) -> "afternoon"
            currentTime.isBefore(LocalTime.MIDNIGHT) -> "evening"
            else -> "night"
        }
    }

    /**
     * Return a personalized greeting based on the current season and part of the day.
     *
     * @param name Name to personalize the greeting.
     * @return Randomized greeting message based on season and part of the day.
     */
    fun getSeasonalAndPartOfDayGreeting(name: String): String {
        val season = getCurrentSeason()
        val partOfDay = getCurrentPartOfDay()

        // Get the appropriate greetings based on the season and part of the day
        val seasonalGreetings = when (season) {
            "winter" -> generateWinterGreetings(name)
            "spring" -> generateSpringGreetings(name)
            "summer" -> generateSummerGreetings(name)
            "autumn" -> generateAutumnGreetings(name)
            else -> generateGreetings(name)
        }

        val partOfDayGreetings = when (partOfDay) {
            "morning" -> generateMorningGreetings(name)
            "afternoon" -> listOf("Dobry wieczór, $name!")
            "evening" -> listOf("Wieczór, $name! Jak się czujesz?")
            "night" -> listOf("Dobranoc, $name! Śpij spokojnie!")
            else -> generateGreetings(name)
        }

        // Combine season and part of day greetings
        val combinedGreetings = seasonalGreetings + partOfDayGreetings

        return combinedGreetings.random()
    }

    /**
     * Return a random greeting for the given name.
     *
     * @param name Name to personalize the greeting.
     * @return Randomized greeting message.
     */
    fun getRandomGreeting(name: String): String {
        return generateGreetings(name).random()
    }
}
