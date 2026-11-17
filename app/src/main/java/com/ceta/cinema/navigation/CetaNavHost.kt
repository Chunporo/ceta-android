package com.ceta.cinema.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ceta.cinema.MainViewModel
import com.ceta.cinema.data.local.prefs.AppSettings
import com.ceta.cinema.ui.screen.auth.EmailLoginScreen
import com.ceta.cinema.ui.screen.auth.ForgotPasswordScreen
import com.ceta.cinema.ui.screen.auth.LoginScreen
import com.ceta.cinema.ui.screen.auth.RegisterScreen
import com.ceta.cinema.ui.screen.home.HomeTab
import com.ceta.cinema.ui.screen.main.AccountTab
import com.ceta.cinema.ui.screen.main.MainShell
import com.ceta.cinema.ui.screen.main.Tab
import com.ceta.cinema.ui.screen.movie.MovieDetailScreen
import com.ceta.cinema.ui.screen.onboarding.OnboardingScreen
import com.ceta.cinema.ui.screen.search.SearchScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.ceta.cinema.ui.theme.Ceta

@Composable
fun CetaNavHost(main: MainViewModel, settings: AppSettings) {
    val signedIn by main.signedIn.collectAsState()
    val known = signedIn ?: run {
        Box(Modifier.fillMaxSize()) // stored tokens are still being read
        return
    }
    val start = rememberSaveable {
        when {
            !settings.onboardingSeen -> Routes.ONBOARD
            known -> Routes.MAIN
            else -> Routes.LOGIN
        }
    }
    val nav = rememberNavController()

    // Signing in leaves the auth screens; losing the session (sign-out or an expired refresh token) returns to Login.
    val previous = remember { mutableStateOf(known) }
    LaunchedEffect(known) {
        val current = nav.currentDestination?.route ?: return@LaunchedEffect
        if (known && current in setOf(Routes.LOGIN, Routes.EMAIL_LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD)) {
            nav.navigate(Routes.MAIN) { popUpTo(0) }
        } else if (!known && previous.value && current != Routes.LOGIN) {
            nav.navigate(Routes.LOGIN) { popUpTo(0) }
        }
        previous.value = known
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ONBOARD) {
            OnboardingScreen(onFinish = {
                main.finishOnboarding()
                nav.navigate(if (known) Routes.MAIN else Routes.LOGIN) { popUpTo(Routes.ONBOARD) { inclusive = true } }
            })
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                linkError = main.linkError.collectAsState().value,
                onGuest = { nav.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onUseEmail = { nav.navigate(Routes.EMAIL_LOGIN) },
            )
        }
        composable(Routes.EMAIL_LOGIN) {
            EmailLoginScreen(
                linkError = main.linkError.collectAsState().value,
                onRegister = { nav.navigate(Routes.REGISTER) },
                onForgotPassword = { nav.navigate(Routes.FORGOT_PASSWORD) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.REGISTER) { RegisterScreen(onBackToLogin = { nav.popBackStack() }) }
        composable(Routes.FORGOT_PASSWORD) { ForgotPasswordScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SEARCH) { SearchScreen(onBack = { nav.popBackStack() }, onMovie = { nav.navigate(Routes.movieDetail(it)) }) }
        composable(Routes.MOVIE_DETAIL, arguments = listOf(navArgument("movieId") { type = NavType.StringType })) {
            MovieDetailScreen(onBack = { nav.popBackStack() }, onBook = { /* showtimes arrive in the next slice */ }, onLogin = { nav.navigate(Routes.LOGIN) })
        }
        composable(Routes.MAIN) {
            MainShell { tab ->
                when (tab) {
                    Tab.HOME -> HomeTab(
                        onMovie = { nav.navigate(Routes.movieDetail(it)) }, onBook = { nav.navigate(Routes.movieDetail(it)) },
                        onSearch = { nav.navigate(Routes.SEARCH) }, onNotifications = {}, onOffers = {}, onCinemas = {},
                    )
                    Tab.ACCOUNT -> AccountTab(settings, known, main::setDark, main::setLanguage,
                        onSignIn = { nav.navigate(Routes.LOGIN) }, onSignOut = main::signOut)
                    else -> Box(Modifier.fillMaxSize().systemBarsPadding(), contentAlignment = Alignment.Center) {
                        Text(tab.name, color = Ceta.colors.mu)
                    }
                }
            }
        }
    }
}
