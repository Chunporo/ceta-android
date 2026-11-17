package com.ceta.cinema.core

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {
    @Test fun zero() = assertEquals("0đ", MoneyFormatter.format(0))
    @Test fun thousands() = assertEquals("1.000đ", MoneyFormatter.format(1_000))
    @Test fun underAThousandHasNoSeparator() = assertEquals("999đ", MoneyFormatter.format(999))
    @Test fun millions() = assertEquals("1.250.000đ", MoneyFormatter.format(1_250_000))
    @Test fun designPrices() {
        assertEquals("85.000đ", MoneyFormatter.format(85_000))
        assertEquals("220.000đ", MoneyFormatter.format(220_000))
    }
    @Test fun negative() = assertEquals("-20.000đ", MoneyFormatter.format(-20_000))
}
