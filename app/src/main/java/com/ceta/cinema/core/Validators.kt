package com.ceta.cinema.core

import androidx.annotation.StringRes
import com.ceta.cinema.R

/** Checks run before any call, so a bad form never costs a request. Each returns an error string or null. */
object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    @StringRes fun email(value: String): Int? = if (EMAIL.matches(value.trim())) null else R.string.validation_email
    @StringRes fun password(value: String): Int? = if (value.length >= 8) null else R.string.validation_password
    @StringRes fun username(value: String): Int? = if (value.trim().length in 3..50) null else R.string.validation_username
    @StringRes fun confirm(password: String, confirm: String): Int? =
        if (password == confirm) null else R.string.validation_confirm
}
