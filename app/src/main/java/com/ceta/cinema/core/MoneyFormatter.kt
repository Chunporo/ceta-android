package com.ceta.cinema.core

import kotlin.math.abs

/** The one place money is formatted: whole VND with dots, like the design (85.000đ). Money is always Long. */
object MoneyFormatter {
    fun format(amount: Long): String {
        val digits = abs(amount).toString()
        val grouped = digits.reversed().chunked(3).joinToString(".").reversed()
        return (if (amount < 0) "-" else "") + grouped + "đ"
    }
}
