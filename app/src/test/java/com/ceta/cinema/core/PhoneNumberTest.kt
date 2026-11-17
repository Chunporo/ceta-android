package com.ceta.cinema.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberTest {
    @Test fun theDesignsExampleIsAccepted() = assertEquals("+84912345678", PhoneNumber.normalize("912 345 678"))

    @Test fun aLeadingZeroIsDropped() = assertEquals("+84912345678", PhoneNumber.normalize("0912345678"))

    @Test fun theCountryCodeInAnyFormIsUnderstood() {
        assertEquals("+84912345678", PhoneNumber.normalize("+84 912 345 678"))
        assertEquals("+84912345678", PhoneNumber.normalize("+84912345678"))
        assertEquals("+84912345678", PhoneNumber.normalize("84912345678"))
        assertEquals("+84912345678", PhoneNumber.normalize("0912-345-678"))
    }

    @Test fun tooShortOrTooLongIsRefused() {
        assertNull(PhoneNumber.normalize("12345"))
        assertNull(PhoneNumber.normalize("09123456789"))
        assertNull(PhoneNumber.normalize(""))
    }

    @Test fun aMobileNumberNeverStartsWithZeroOrOne() {
        assertNull(PhoneNumber.normalize("112345678"))
        assertNull(PhoneNumber.normalize("00912345678"))
    }

    @Test fun anotherCountryCodeIsRefused() = assertNull(PhoneNumber.normalize("+1 415 555 2671"))

    @Test fun lettersAreIgnoredButCannotMakeANumberValid() = assertNull(PhoneNumber.normalize("abc"))

    @Test fun displayGroupsTheDigits() = assertEquals("+84 912 345 678", PhoneNumber.display("+84912345678"))
}
