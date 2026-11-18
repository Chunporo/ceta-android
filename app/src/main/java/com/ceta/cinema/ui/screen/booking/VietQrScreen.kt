package com.ceta.cinema.ui.screen.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.SubcomposeAsyncImage
import com.ceta.cinema.R
import com.ceta.cinema.core.MoneyFormatter
import com.ceta.cinema.ui.component.CetaTopBar
import com.ceta.cinema.ui.component.OutlineButton
import com.ceta.cinema.ui.theme.Ceta

/** The VietQR step: the QR to scan with any banking app, the amount and the note to keep, then wait until the bank feed confirms it. */
@Composable
fun VietQrScreen(onBack: () -> Unit, onPaid: (Long) -> Unit, vm: VietQrViewModel = hiltViewModel()) {
    val ui by vm.state.collectAsState()
    val c = Ceta.colors
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(ui.paid) { if (ui.paid) onPaid(vm.bookingIdValue) }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.vq_title), onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // The QR stays on a white tile in dark mode too: banking apps scan dark-on-light.
            Box(Modifier.size(280.dp).clip(RoundedCornerShape(22.dp)).background(Color.White).padding(10.dp), contentAlignment = Alignment.Center) {
                SubcomposeAsyncImage(model = ui.imageUrl, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize(),
                    loading = { CircularProgressIndicator(color = c.ac) },
                    error = { Text(stringResource(R.string.vq_image_failed), color = Color.Black, textAlign = TextAlign.Center) })
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.sf).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.vq_amount), color = c.mu, fontSize = 13.sp)
                    Text(MoneyFormatter.format(ui.amount), color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.vq_note), color = c.mu, fontSize = 13.sp)
                        Text(ui.note, color = c.tx, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                    }
                    OutlineButton(stringResource(R.string.vq_copy), { clipboard.setText(AnnotatedString(ui.note)) }, Modifier.width(140.dp))
                }
            }
            Text(stringResource(R.string.vq_hint), color = c.mu, fontSize = 12.sp, textAlign = TextAlign.Center)
            if (ui.expired) Text(stringResource(R.string.vq_expired), color = c.ac, fontWeight = FontWeight.Bold)
            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = c.ac, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.vq_waiting), color = c.tx, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
