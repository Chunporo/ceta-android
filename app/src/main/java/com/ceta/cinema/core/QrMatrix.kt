package com.ceta.cinema.core

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** The modules of a QR code, to be drawn crisply on a Canvas at any size (no bitmap to scale and blur). */
object QrMatrix {
    fun of(text: String): Array<BooleanArray> {
        require(text.isNotEmpty()) { "Nothing to encode" }
        val hints = mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.CHARACTER_SET to "UTF-8")
        val bits = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 1, 1, hints)
        return Array(bits.height) { y -> BooleanArray(bits.width) { x -> bits.get(x, y) } }
    }
}
