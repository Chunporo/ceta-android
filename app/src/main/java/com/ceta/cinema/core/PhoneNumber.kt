package com.ceta.cinema.core

/** Vietnamese mobile numbers as people type them: "912 345 678", "0912345678", "+84 912 345 678". */
object PhoneNumber {
    /** The E.164 number (+84912345678), or null when the input is not a plausible mobile number. */
    fun normalize(input: String): String? {
        var digits = input.filter { it.isDigit() }
        if (input.trim().startsWith("+")) {
            if (!digits.startsWith("84")) return null
            digits = digits.removePrefix("84")
        } else if (digits.startsWith("84") && digits.length == 11) {
            digits = digits.removePrefix("84")
        }
        digits = digits.removePrefix("0")
        // After the country code a mobile number is 9 digits and never starts with 0 or 1.
        if (digits.length != 9 || digits.first() in "01") return null
        return "+84$digits"
    }

    /** "+84912345678" -> "+84 912 345 678", for showing back to the customer. */
    fun display(e164: String): String {
        val local = e164.removePrefix("+84")
        return if (local.length == 9) "+84 ${local.substring(0, 3)} ${local.substring(3, 6)} ${local.substring(6)}" else e164
    }
}
