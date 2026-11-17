package com.ceta.cinema.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import com.ceta.cinema.R
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ErrorMapper
import com.ceta.cinema.ui.theme.Spacing

@Composable
private fun AuthColumn(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.m),
        verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically),
    ) { content() }
}

@Composable
private fun Field(
    value: String, onChange: (String) -> Unit, label: Int, error: Int?,
    keyboard: KeyboardType = KeyboardType.Text, secret: Boolean = false,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(stringResource(label)) },
        isError = error != null,
        supportingText = error?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (secret) KeyboardType.Password else keyboard),
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorText(error: ApiError?) {
    if (error != null) Text(stringResource(ErrorMapper.messageFor(error)), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun Busy(loading: Boolean, label: Int, onClick: () -> Unit, outlined: Boolean = false) {
    val content: @Composable () -> Unit = {
        if (loading) CircularProgressIndicator(Modifier.padding(Spacing.xs)) else Text(stringResource(label))
    }
    if (outlined) OutlinedButton(onClick, enabled = !loading, modifier = Modifier.fillMaxWidth()) { content() }
    else Button(onClick, enabled = !loading, modifier = Modifier.fillMaxWidth()) { content() }
}

@Composable
fun LoginScreen(
    linkError: ApiError?,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onGuest: () -> Unit,
    vm: LoginViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    AuthColumn {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.titleLarge)
        Field(s.email, vm::onEmail, R.string.field_email, s.emailError, KeyboardType.Email)
        Field(s.password, vm::onPassword, R.string.field_password, s.passwordError, secret = true)
        ErrorText(s.error ?: linkError)
        s.linkSentTo?.let { Text(stringResource(R.string.login_link_sent, it)) }
        Busy(s.loading, R.string.login_action, vm::signIn)
        Busy(s.loading, R.string.login_google, { vm.signInWithGoogle(context, webClientId) }, outlined = true)
        Busy(s.loading, R.string.login_email_link, vm::sendLink, outlined = true)
        TextButton(onForgotPassword) { Text(stringResource(R.string.login_forgot)) }
        TextButton(onRegister) { Text(stringResource(R.string.login_register)) }
        TextButton(onGuest) { Text(stringResource(R.string.ds_guest)) }
    }
}

@Composable
fun RegisterScreen(onBackToLogin: () -> Unit, vm: RegisterViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    AuthColumn {
        Text(stringResource(R.string.register_title), style = MaterialTheme.typography.titleLarge)
        if (s.verificationSentTo != null) {
            Text(stringResource(R.string.register_verify_sent, s.verificationSentTo!!))
            Button(onBackToLogin, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.register_to_login)) }
            return@AuthColumn
        }
        Field(s.username, vm::onUsername, R.string.field_username, s.usernameError)
        Field(s.email, vm::onEmail, R.string.field_email, s.emailError, KeyboardType.Email)
        Field(s.password, vm::onPassword, R.string.field_password, s.passwordError, secret = true)
        Field(s.confirm, vm::onConfirm, R.string.field_confirm, s.confirmError, secret = true)
        ErrorText(s.error)
        Busy(s.loading, R.string.register_action, vm::submit)
        TextButton(onBackToLogin) { Text(stringResource(R.string.register_back)) }
    }
}

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, vm: ForgotPasswordViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    AuthColumn {
        Text(stringResource(R.string.forgot_title), style = MaterialTheme.typography.titleLarge)
        if (s.done) {
            Text(stringResource(R.string.forgot_done)) // same text whether or not the address exists
        } else {
            Field(s.email, vm::onEmail, R.string.field_email, s.emailError, KeyboardType.Email)
            ErrorText(s.error)
            Busy(s.loading, R.string.forgot_action, vm::submit)
        }
        TextButton(onBack) { Text(stringResource(R.string.register_back)) }
    }
}
