package com.strefagentelmena.models

/**
 * Jedna notatka przy kliencie — treść i znacznik czasu dodania (ms od epoki UTC).
 * [addedAtMillis] == 0 oznacza wpis z legacy pola [Customer.noted] bez znanej daty.
 */
data class CustomerNote(
    var text: String = "",
    var addedAtMillis: Long = 0L,
)
