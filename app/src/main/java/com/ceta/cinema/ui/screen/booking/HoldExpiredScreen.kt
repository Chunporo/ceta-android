package com.ceta.cinema.ui.screen.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.ui.component.OutlineButton
import com.ceta.cinema.ui.component.Poster
import com.ceta.cinema.ui.component.PrimaryButton
import com.ceta.cinema.ui.component.RingIcon
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

/**
 * Design item 11: the seat hold ran out on the seats, snacks or payment screen. The seats went back to the cinema and nothing was
 * charged; the customer picks the same show again or another one.
 */
@Composable
fun HoldExpiredScreen(onReselect: (Long) -> Unit, onOtherShow: (String?) -> Unit, vm: HoldExpiredViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    val c = Ceta.colors
    val show = ui.showtime
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        RingIcon(Icons.Rounded.TimerOff)
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.hold_expired_title), color = c.tx, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            if (ui.seats.isEmpty()) stringResource(R.string.hold_expired_body_noseats) else stringResource(R.string.hold_expired_body, ui.seats.joinToString(", ")),
            color = c.mu, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(24.dp))
        if (show != null) Row(Modifier.fillMaxWidth().cardSurface(RoundedCornerShape(24.dp)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Poster(show.posterUrl, show.movieTitle.orEmpty(), Modifier.size(width = 56.dp, height = 84.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(show.movieTitle.orEmpty(), color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                Text(
                    listOfNotNull(show.cinemaName, Formatters.date(Formatters.localDate(show.start)), Formatters.time(show.start), show.format.label).joinToString(" · "),
                    color = c.mu, fontSize = 13.sp,
                )
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ui.seats.forEach {
                        Text(it, color = c.mu, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.LineThrough,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(c.sf2).padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton(stringResource(R.string.choose_seats_again), { onReselect(ui.showtimeId) }, pill = true)
        Spacer(Modifier.height(10.dp))
        OutlineButton(stringResource(R.string.hold_expired_other_show), { onOtherShow(show?.movieId) }, pill = true)
        Spacer(Modifier.height(20.dp))
    }
}
