package com.ceta.cinema.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ceta.cinema.ui.screen.auth.LoginScreen
import com.ceta.cinema.ui.screen.home.HomeScreen

@Composable
fun CetaNavHost(startDestination: String) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(onSignedOut = {
                nav.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
            })
        }
    }
}
