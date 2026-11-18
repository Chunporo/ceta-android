package com.ceta.cinema.ui.screen.booking

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferNoteTest {
    @Test fun readsTheNoteTheServerPutInTheQrUrl() {
        assertEquals("CETA5A1", transferNote("https://img.vietqr.io/image/MB-0123456789-compact2.png?amount=170000&addInfo=CETA5A1&accountName=CETA+CINEMA"))
    }

    @Test fun aUrlWithoutANoteGivesAnEmptyNote() {
        assertEquals("", transferNote("https://img.vietqr.io/image/MB-1-compact2.png?amount=1"))
    }
}
