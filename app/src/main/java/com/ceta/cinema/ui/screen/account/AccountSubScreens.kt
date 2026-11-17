package com.ceta.cinema.ui.screen.account

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.BuildConfig
import com.ceta.cinema.R
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.core.Formatters
import com.ceta.cinema.core.UiState
import com.ceta.cinema.domain.model.Promotion
import com.ceta.cinema.domain.model.ReminderMovie
import com.ceta.cinema.ui.component.*
import com.ceta.cinema.ui.findActivity
import com.ceta.cinema.ui.theme.Ceta

/** "Huynh Quco Trung" -> "HT": first letter of the first and last word. */
fun initialsOf(name: String?): String {
    val parts = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when { parts.isEmpty() -> "C"; parts.size == 1 -> parts[0].take(1); else -> "${parts.first().first()}${parts.last().first()}" }.uppercase()
}

// ---- Personal info -------------------------------------------------------------------------------------------

@Composable
fun ProfileScreen(onBack: () -> Unit, onDeleted: () -> Unit, vm: ProfileViewModel = hiltViewModel(), deleteVm: DeleteAccountViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.ds_profile), onBack)
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> Unit
            is UiState.Content -> ProfileContent(s.data, vm, deleteVm, onDeleted)
        }
    }
}

@Composable
private fun ProfileContent(ui: ProfileUi, vm: ProfileViewModel, deleteVm: DeleteAccountViewModel, onDeleted: () -> Unit) {
    val c = Ceta.colors
    var name by rememberSaveable(ui.profile.username) { mutableStateOf(ui.profile.username) }
    val snackbar = remember { SnackbarHostState() }
    val text = when (val m = ui.message) {
        null -> null
        ProfileMessage.Saved -> stringResource(R.string.profile_saved)
        ProfileMessage.NameInvalid -> stringResource(R.string.profile_name_invalid)
        ProfileMessage.ResetSent -> stringResource(R.string.profile_reset_sent)
        is ProfileMessage.Failed -> stringResource(ErrorMapper.messageFor(m.error))
    }
    LaunchedEffect(ui.message) { if (text != null) { snackbar.showSnackbar(text); vm.consumeMessage() } }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(88.dp).clip(CircleShape).background(c.ac2), contentAlignment = Alignment.Center) {
                Text(initialsOf(name), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.profile_name_label), color = c.mu, fontSize = 12.sp)
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(50) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = c.bg, unfocusedContainerColor = c.bg, focusedBorderColor = c.ac, unfocusedBorderColor = c.ln2),
                )
                PrimaryButton(stringResource(R.string.profile_save), { vm.saveName(name) }, enabled = name.trim() != ui.profile.username && !ui.saving, loading = ui.saving)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ui.profile.email?.let { Fact(R.string.profile_email, it) }
                ui.profile.phone?.let { Fact(R.string.profile_phone, it) }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (ui.canResetPassword) {
                    Text(stringResource(R.string.profile_reset_hint), color = c.mu, fontSize = 13.sp)
                    OutlineButton(stringResource(R.string.profile_reset_password), vm::sendPasswordReset, enabled = !ui.saving)
                } else Text(stringResource(R.string.profile_signed_with_phone), color = c.mu, fontSize = 13.sp)
            }
            TextButton({ deleteVm.start() }, Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.delete_account), color = c.ac, fontWeight = FontWeight.SemiBold)
            }
        }
        DeleteAccountDialogs(ui.profile, deleteVm, onDeleted)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp))
    }
}

@Composable
private fun Fact(label: Int, value: String) {
    val c = Ceta.colors
    Column {
        Text(stringResource(label), color = c.mu, fontSize = 12.sp)
        Text(value, color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---- My vouchers -----------------------------------------------------------------------------------------------

@Composable
fun VouchersScreen(onBack: () -> Unit, onFind: () -> Unit, vm: VouchersViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.ds_my_vouchers), onBack)
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> EmptyState(stringResource(R.string.vouchers_empty), Modifier.padding(top = 48.dp), cta = stringResource(R.string.vouchers_find), onCta = onFind)
            is UiState.Content -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.data, key = { it.id }) { VoucherCard(it) { vm.remove(it.id) } }
            }
        }
    }
}

@Composable
private fun VoucherCard(promo: Promotion, onRemove: () -> Unit) {
    val c = Ceta.colors
    val english = LocalConfiguration.current.locales[0].language == "en"
    val hue = (promo.code.hashCode().toUInt().toLong() % 360).toFloat()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf)) {
        Box(
            Modifier.fillMaxWidth().height(84.dp).background(Brush.linearGradient(listOf(Color.hsv(hue, 0.6f, 0.62f), Color.hsv((hue + 40) % 360, 0.65f, 0.45f)))).padding(16.dp),
            contentAlignment = Alignment.BottomStart,
        ) { Text(promo.title(english), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2) }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(promo.code, color = c.tx, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                promo.validUntil?.let { Text(stringResource(R.string.ds_until) + " " + Formatters.date(Formatters.localDate(it)), color = c.mu, fontSize = 12.sp) }
            }
            TextButton(onRemove) { Text(stringResource(R.string.voucher_unsave), color = c.mu) }
        }
    }
}

// ---- Reminders --------------------------------------------------------------------------------------------------

@Composable
fun RemindersScreen(onBack: () -> Unit, onMovie: (String) -> Unit, vm: RemindersViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.reminders_title), onBack)
        when (val s = state) {
            UiState.Loading -> LoadingBox()
            is UiState.Error -> ErrorBox(s.error, vm::load)
            is UiState.Empty -> EmptyState(stringResource(R.string.reminders_empty), Modifier.padding(top = 48.dp))
            is UiState.Content -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(s.data, key = { it.id }) { ReminderRow(it, { onMovie(it.id) }, { vm.remove(it.id) }) }
            }
        }
    }
}

@Composable
private fun ReminderRow(movie: ReminderMovie, onOpen: () -> Unit, onRemove: () -> Unit) {
    val c = Ceta.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Poster(movie.posterUrl, movie.id, Modifier.size(width = 56.dp, height = 84.dp).clip(RoundedCornerShape(12.dp)))
        Text(movie.title, color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.weight(1f))
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.sf2).clickable(onClick = onRemove), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.NotificationsOff, stringResource(R.string.reminders_remove), tint = c.mu, modifier = Modifier.size(21.dp))
        }
    }
}

// ---- Payment methods ----------------------------------------------------------------------------------------------

@Composable
fun PaymentMethodsScreen(onBack: () -> Unit) {
    val c = Ceta.colors
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.ds_pay_methods), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).border(1.5.dp, c.ac, RoundedCornerShape(22.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFD64B3A)), contentAlignment = Alignment.Center) {
                    Text("VN", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("VNPay", color = c.tx, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.pm_default), color = c.ac, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(c.acs).padding(horizontal = 7.dp, vertical = 2.dp))
                    }
                    Text(stringResource(R.string.pm_vnpay_desc), color = c.mu, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
            Text(stringResource(R.string.pm_note), color = c.mu, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(horizontal = 4.dp))
            Text(stringResource(R.string.pm_more), color = c.mu, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

// ---- Help ---------------------------------------------------------------------------------------------------------

private val FAQ = listOf(
    R.string.faq_q1 to R.string.faq_a1, R.string.faq_q2 to R.string.faq_a2, R.string.faq_q3 to R.string.faq_a3,
    R.string.faq_q4 to R.string.faq_a4, R.string.faq_q5 to R.string.faq_a5, R.string.faq_q6 to R.string.faq_a6,
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val c = Ceta.colors
    var open by rememberSaveable { mutableIntStateOf(-1) }
    Column(Modifier.fillMaxSize().background(c.bg).systemBarsPadding()) {
        CetaTopBar(stringResource(R.string.ds_help), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.help_faq), color = c.tx, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf)) {
                FAQ.forEachIndexed { i, (q, a) ->
                    if (i > 0) HorizontalDivider(color = c.ln)
                    Column(Modifier.fillMaxWidth().clickable { open = if (open == i) -1 else i }.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(q), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(if (open == i) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = c.mu)
                        }
                        if (open == i) Text(stringResource(a), color = c.mu, fontSize = 14.sp, lineHeight = 21.sp)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.sf).padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.help_about), color = c.tx, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.help_version, BuildConfig.VERSION_NAME), color = c.mu, fontSize = 13.sp)
            }
        }
    }
}

// ---- Delete account -------------------------------------------------------------------------------------------------

@Composable
private fun DeleteAccountDialogs(profile: com.ceta.cinema.domain.model.Profile, vm: DeleteAccountViewModel, onDeleted: () -> Unit) {
    val ui by vm.state.collectAsState()
    val c = Ceta.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    // A dialog is its own window: resolve the words here, in the app's chosen language, and hand it plain text.
    val title = stringResource(R.string.delete_title)
    val warning = stringResource(R.string.delete_warning)
    val cont = stringResource(R.string.delete_continue)
    val cancel = stringResource(R.string.cancel)
    val intro = stringResource(R.string.delete_confirm_intro)
    val passwordLabel = stringResource(R.string.delete_password_label)
    val google = stringResource(R.string.delete_with_google)
    val sendCode = stringResource(R.string.delete_send_code)
    val codeLabel = stringResource(R.string.delete_code_label)
    val confirmButton = stringResource(R.string.delete_confirm_button)
    val unknown = stringResource(R.string.delete_unknown_method)
    val error = ui.error?.let { stringResource(ErrorMapper.messageFor(it)) }
    LaunchedEffect(ui.deleted) { if (ui.deleted) onDeleted() }
    var secret by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(ui.step) { if (ui.step != DeleteStep.CONFIRM) secret = "" }

    when (ui.step) {
        DeleteStep.IDLE -> Unit
        DeleteStep.WARN -> AlertDialog(
            onDismissRequest = vm::cancel, containerColor = c.sf,
            title = { Text(title, color = c.tx) },
            text = { Text(warning, color = c.mu, fontSize = 14.sp) },
            confirmButton = { TextButton(vm::proceed) { Text(cont, color = c.ac, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(vm::cancel) { Text(cancel, color = c.mu) } },
        )
        DeleteStep.CONFIRM -> AlertDialog(
            onDismissRequest = { if (!ui.working) vm.cancel() }, containerColor = c.sf,
            title = { Text(title, color = c.tx) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (ui.method == null) unknown else intro, color = c.mu, fontSize = 14.sp)
                    when (ui.method) {
                        com.ceta.cinema.data.remote.firebase.SignInMethod.PASSWORD -> OutlinedTextField(
                            value = secret, onValueChange = { secret = it.take(100) }, singleLine = true, label = { Text(passwordLabel) },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), enabled = !ui.working,
                        )
                        com.ceta.cinema.data.remote.firebase.SignInMethod.GOOGLE -> Unit
                        com.ceta.cinema.data.remote.firebase.SignInMethod.PHONE -> if (ui.codeSentFor != null) OutlinedTextField(
                            value = secret, onValueChange = { secret = it.filter(Char::isDigit).take(6) }, singleLine = true, label = { Text(codeLabel) },
                            modifier = Modifier.fillMaxWidth(), enabled = !ui.working,
                        )
                        null -> Unit
                    }
                    if (ui.working) LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.ac)
                    error?.let { Text(it, color = c.ac, fontSize = 13.sp) }
                }
            },
            confirmButton = {
                when (ui.method) {
                    com.ceta.cinema.data.remote.firebase.SignInMethod.PASSWORD ->
                        TextButton({ vm.confirmWithPassword(profile.email.orEmpty(), secret) }, enabled = secret.isNotEmpty() && !ui.working) { Text(confirmButton, color = c.ac, fontWeight = FontWeight.Bold) }
                    com.ceta.cinema.data.remote.firebase.SignInMethod.GOOGLE ->
                        TextButton({ vm.confirmWithGoogle(context, webClientId) }, enabled = !ui.working) { Text(google, color = c.ac, fontWeight = FontWeight.Bold) }
                    com.ceta.cinema.data.remote.firebase.SignInMethod.PHONE ->
                        if (ui.codeSentFor == null) TextButton({ context.findActivity()?.let(vm::sendCode) }, enabled = !ui.working) { Text(sendCode, color = c.ac, fontWeight = FontWeight.Bold) }
                        else TextButton({ vm.confirmWithCode(secret) }, enabled = secret.length >= 6 && !ui.working) { Text(confirmButton, color = c.ac, fontWeight = FontWeight.Bold) }
                    null -> Unit
                }
            },
            dismissButton = { TextButton(vm::cancel, enabled = !ui.working) { Text(cancel, color = c.mu) } },
        )
    }
}
