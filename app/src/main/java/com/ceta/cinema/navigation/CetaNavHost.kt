package com.ceta.cinema.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ceta.cinema.MainViewModel
import com.ceta.cinema.ui.screen.auth.ForgotPasswordScreen
import com.ceta.cinema.ui.screen.auth.LoginScreen
import com.ceta.cinema.ui.screen.auth.RegisterScreen
import com.ceta.cinema.ui.screen.home.HomeScreen

private val AUTH_ROUTES = setOf(Routes.LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD)

@Composable
fun CetaNavHost(main: MainViewModel) {
    val signedIn by main.signedIn.collectAsState()
    val known = signedIn ?: run {
        Box(Modifier.fillMaxSize()) // stored tokens are still being read
        return
    }
    val start = rememberSaveable { if (known) Routes.HOME else Routes.LOGIN }
    val nav = rememberNavController()

    // Sign-in state is the truth: signing in, signing out, or a dead session all move the user here.
    LaunchedEffect(known) {
        val current = nav.currentDestination?.route ?: return@LaunchedEffect
        if (known && current in AUTH_ROUTES) {
            nav.navigate(Routes.HOME) { popUpTo(0) }
        } else if (!known && current !in AUTH_ROUTES) {
            nav.navigate(Routes.LOGIN) { popUpTo(0) }
        }
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(
                linkError = main.linkError.collectAsState().value,
                onRegister = { nav.navigate(Routes.REGISTER) },
                onForgotPassword = { nav.navigate(Routes.FORGOT_PASSWORD) },
            )
        }
        composable(Routes.REGISTER) { RegisterScreen(onBackToLogin = { nav.popBackStack() }) }
        composable(Routes.FORGOT_PASSWORD) { ForgotPasswordScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.HOME) { HomeScreen(onSignOut = main::signOut) }
    }
}
