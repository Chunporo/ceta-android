package com.ceta.cinema.ui.screen.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ceta.cinema.R
import com.ceta.cinema.ui.component.CetaTopBar
import com.ceta.cinema.ui.theme.Ceta

/** Placeholder until the checkout (snacks, voucher, points, VNPay) lands in the next slice. */
@Composable
fun CheckoutScreen(bookingId: Long, onBack: () -> Unit) {
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.checkout_title), onBack)
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.booking_placeholder, bookingId), color = c.tx, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
