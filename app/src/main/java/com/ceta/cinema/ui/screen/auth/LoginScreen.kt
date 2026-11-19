package com.ceta.cinema.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.PhoneNumber
import com.ceta.cinema.ui.component.CetaLogo
import com.ceta.cinema.ui.component.CetaTopBar
import com.ceta.cinema.ui.component.OutlineButton
import com.ceta.cinema.ui.component.PrimaryButton
import com.ceta.cinema.ui.findActivity
import com.ceta.cinema.ui.theme.Ceta
import com.ceta.cinema.ui.theme.cardSurface

/** The design's login: phone number then SMS code, or Google. Email is one tap away; guests can browse. */
@Composable
fun LoginScreen(linkError: ApiError?, onGuest: () -> Unit, onUseEmail: () -> Unit, vm: PhoneLoginViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    if (s.step == LoginStep.OTP) {
        OtpStep(s, vm)
        return
    }
    val c = Ceta.colors
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onGuest) { Text(stringResource(R.string.ds_skip), color = c.mu) }
        }
        Spacer(Modifier.height(12.dp))
        CetaLogo(56.dp)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.ds_login), style = MaterialTheme.typography.headlineMedium, color = c.tx)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ds_login_sub), style = MaterialTheme.typography.bodyLarge, color = c.mu)
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.ds_phone), style = MaterialTheme.typography.labelMedium, color = c.mu)
        Spacer(Modifier.height(8.dp))
        PhoneField(s.phone, vm::onPhone, hasError = s.phoneError != null)
        s.phoneError?.let { Text(stringResource(it), color = c.ac, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        (s.error ?: linkError)?.let { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(stringResource(R.string.ds_cont), onClick = { context.findActivity()?.let(vm::sendCode) }, loading = s.loading)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = c.ln2)
            Text(stringResource(R.string.ds_or), color = c.mu, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 12.dp))
            HorizontalDivider(Modifier.weight(1f), color = c.ln2)
        }
        Spacer(Modifier.height(20.dp))
        OutlineButton(stringResource(R.string.ds_google), onClick = { vm.signInWithGoogle(context, webClientId) }, enabled = !s.loading)
        TextButton(onUseEmail, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.login_use_email), color = c.ac) }
        Spacer(Modifier.weight(1f, fill = false).height(24.dp))
        Text(stringResource(R.string.ds_terms), style = MaterialTheme.typography.bodySmall, color = c.mu, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PhoneField(value: String, onChange: (String) -> Unit, hasError: Boolean) {
    val c = Ceta.colors
    Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxHeight().cardSurface(RoundedCornerShape(16.dp)).border(1.dp, c.ln2, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text("+84", color = c.tx, style = MaterialTheme.typography.bodyLarge)
        }
        Box(
            Modifier.weight(1f).fillMaxHeight().cardSurface(RoundedCornerShape(16.dp))
                .border(1.5.dp, if (hasError) c.ac else c.ln2, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.tx), cursorBrush = SolidColor(c.ac),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(stringResource(R.string.ds_phone_ph), color = c.mu, style = MaterialTheme.typography.bodyLarge)
                    inner()
                },
            )
        }
    }
}

@Composable
private fun OtpStep(s: PhoneLoginUiState, vm: PhoneLoginViewModel) {
    val c = Ceta.colors
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding().verticalScroll(rememberScrollState())) {
        CetaTopBar("", onBack = vm::back)
        Column(Modifier.padding(horizontal = 24.dp)) {
            Text(stringResource(R.string.ds_otp_title), style = MaterialTheme.typography.headlineMedium, color = c.tx)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.otp_sent_to, s.sentTo?.let(PhoneNumber::display).orEmpty()), style = MaterialTheme.typography.bodyLarge, color = c.mu)
            Spacer(Modifier.height(28.dp))
            OtpBoxes(s.code, vm::onCode, hasError = s.codeError != null || s.error != null)
            s.codeError?.let { Text(stringResource(it), color = c.ac, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
            s.error?.let { Text(stringResource(ErrorMapper.messageFor(it)), color = c.ac, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(24.dp))
            PrimaryButton(stringResource(R.string.ds_verify), onClick = vm::verify, loading = s.loading)
            Spacer(Modifier.height(12.dp))
            if (s.resendSeconds > 0) {
                Text(stringResource(R.string.otp_resend_in, "%d:%02d".format(s.resendSeconds / 60, s.resendSeconds % 60)), color = c.mu,
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                TextButton({ context.findActivity()?.let(vm::sendCode) }, Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.otp_resend), color = c.ac)
                }
            }
        }
    }
}

@Composable
private fun OtpBoxes(code: String, onChange: (String) -> Unit, hasError: Boolean) {
    val c = Ceta.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = code, onValueChange = onChange, singleLine = true, cursorBrush = SolidColor(Color.Transparent),
        textStyle = MaterialTheme.typography.titleLarge.copy(color = Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(PhoneLoginViewModel.CODE_LENGTH) { i ->
                    val border = if (i == code.length || hasError) c.ac else c.ln2
                    Box(
                        Modifier.weight(1f).height(60.dp).cardSurface(RoundedCornerShape(16.dp)).border(1.5.dp, border, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(code.getOrNull(i)?.toString().orEmpty(), style = MaterialTheme.typography.titleLarge, color = c.tx) }
                }
            }
        },
    )
}
