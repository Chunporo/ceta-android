package com.ceta.cinema.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.ceta.cinema.R
import com.ceta.cinema.ui.theme.Ceta

/** "Ghế: F6, F7 · Bắp nước ×2" (design item 4): one muted line above the total; the snacks part only when there are snacks. */
@Composable
fun OrderSummaryLine(seatLabels: List<String>, snackCount: Int, modifier: Modifier = Modifier) {
    val parts = buildList {
        add(stringResource(R.string.order_summary_seats, seatLabels.joinToString(", ")))
        if (snackCount > 0) add(stringResource(R.string.order_summary_snacks, snackCount))
    }
    Text(parts.joinToString(" · "), color = Ceta.colors.mu, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.fillMaxWidth())
}
