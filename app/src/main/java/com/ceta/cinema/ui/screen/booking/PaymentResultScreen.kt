package com.ceta.cinema.ui.screen.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.domain.usecase.PaymentOutcome
import com.ceta.cinema.ui.component.OutlineButton
import com.ceta.cinema.ui.component.PrimaryButton
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

/** What happened to the payment, as the server tells it. There is deliberately no back button while it is still being confirmed. */
@Composable
fun PaymentResultScreen(
    onViewTickets: () -> Unit, onHome: () -> Unit, onChooseSeats: () -> Unit,
    vm: PaymentResultViewModel = hiltViewModel(),
) {
    val ui by vm.state.collectAsState()
    val c = Ceta.colors
    val orderId = ui.booking?.id
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        when (ui.outcome) {
            PaymentOutcome.PROCESSING -> {
                CircularProgressIndicator(color = c.ac, modifier = Modifier.size(56.dp))
                Message(R.string.result_pending_title, R.string.result_pending_body)
            }
            PaymentOutcome.PAID -> {
                // The design's success: a soft ring around a solid accent disc with a white tick.
                Box(Modifier.size(112.dp).clip(CircleShape).background(c.acs), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(80.dp).clip(CircleShape).background(c.ac), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.ds_success), color = c.tx, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.ds_success_sub), color = c.mu, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 300.dp))
                val code = ui.booking?.tickets?.firstOrNull()?.code
                if (code != null) {
                    Spacer(Modifier.height(18.dp))
                    Column(Modifier.cardSurface(RoundedCornerShape(18.dp)).padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.ds_booking_code), color = c.mu, fontSize = 12.sp)
                        Text(code, color = c.tx, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp, fontFamily = FontFamily.Monospace)
                    }
                } else orderId?.let { Spacer(Modifier.height(12.dp)); Text(stringResource(R.string.result_order, it), color = c.mu, style = MaterialTheme.typography.labelMedium) }
                Spacer(Modifier.height(32.dp))
                PrimaryButton(stringResource(R.string.ds_view_ticket), onViewTickets)
                Spacer(Modifier.height(10.dp))
                OutlineButton(stringResource(R.string.ds_back_home), onHome)
            }
            PaymentOutcome.STILL_PENDING -> {
                Badge(Icons.Rounded.Payments, c.ac)
                Message(R.string.result_still_title, R.string.result_still_body)
                Spacer(Modifier.height(24.dp))
                PrimaryButton(stringResource(R.string.result_check_again), vm::checkAgain)
                Spacer(Modifier.height(10.dp))
                OutlineButton(stringResource(R.string.ds_back_home), onHome)
            }
            PaymentOutcome.EXPIRED -> {
                Badge(Icons.Rounded.ErrorOutline, c.ac)
                Message(R.string.result_expired_title, R.string.result_expired_body)
                Spacer(Modifier.height(24.dp))
                PrimaryButton(stringResource(R.string.choose_seats_again), onChooseSeats)
                Spacer(Modifier.height(10.dp))
                OutlineButton(stringResource(R.string.ds_back_home), onHome)
            }
            PaymentOutcome.CANCELLED -> {
                Badge(Icons.Rounded.ErrorOutline, c.ac)
                Message(R.string.result_cancelled_title, R.string.result_cancelled_body)
                Spacer(Modifier.height(24.dp))
                PrimaryButton(stringResource(R.string.choose_seats_again), onChooseSeats)
                Spacer(Modifier.height(10.dp))
                OutlineButton(stringResource(R.string.ds_back_home), onHome)
            }
            PaymentOutcome.REFUND -> {
                Badge(Icons.Rounded.ErrorOutline, c.ac)
                Message(R.string.result_refund_title, R.string.result_refund_body)
                orderId?.let { Text(stringResource(R.string.result_order, it), color = c.mu, style = MaterialTheme.typography.labelMedium) }
                Spacer(Modifier.height(24.dp))
                // No way back to the basket from here: those seats are gone.
                PrimaryButton(stringResource(R.string.ds_back_home), onHome)
            }
        }
    }
}

@Composable
private fun Badge(icon: ImageVector, color: Color) {
    Box(Modifier.size(84.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun Message(title: Int, body: Int) {
    val c = Ceta.colors
    Spacer(Modifier.height(20.dp))
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = c.tx, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = c.mu, textAlign = TextAlign.Center)
}
