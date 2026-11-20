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
import com.ceta.cinema.domain.model.BookingStatus
import com.ceta.cinema.ui.screen.account.HelpScreen
import com.ceta.cinema.ui.screen.account.HistoryScreen
import com.ceta.cinema.ui.screen.account.PaymentMethodsScreen
import com.ceta.cinema.ui.screen.account.ProfileScreen
import com.ceta.cinema.ui.screen.account.RemindersScreen
import com.ceta.cinema.ui.screen.account.VouchersScreen
import com.ceta.cinema.ui.screen.account.NotificationsScreen
import com.ceta.cinema.ui.screen.cinema.CinemasTab
import com.ceta.cinema.ui.screen.home.HomeTab
import com.ceta.cinema.ui.screen.offers.OffersTab
import com.ceta.cinema.ui.screen.main.AccountTab
import com.ceta.cinema.ui.screen.main.MainShell
import com.ceta.cinema.ui.screen.main.Tab
import com.ceta.cinema.ui.screen.movie.MovieDetailScreen
import com.ceta.cinema.ui.screen.onboarding.OnboardingScreen
import com.ceta.cinema.ui.screen.search.SearchScreen
import com.ceta.cinema.ui.screen.seat.SeatMapScreen
import com.ceta.cinema.ui.screen.showtime.ShowtimePickerScreen
import com.ceta.cinema.ui.screen.booking.CheckoutScreen
import com.ceta.cinema.ui.screen.booking.ComboScreen
import com.ceta.cinema.ui.screen.booking.HoldExpiredScreen
import com.ceta.cinema.ui.screen.booking.PayFailScreen
import com.ceta.cinema.ui.screen.booking.PaymentResultScreen
import com.ceta.cinema.ui.screen.booking.VietQrScreen
import com.ceta.cinema.ui.screen.ticket.TicketDetailScreen
import com.ceta.cinema.ui.screen.ticket.TicketsTab
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
            known || settings.guestMode -> Routes.MAIN
            else -> Routes.LOGIN
        }
    }
    val nav = rememberNavController()

    // Signing in leaves the auth screens; losing the session (sign-out or an expired refresh token) returns to Login.
    val previous = remember { mutableStateOf(known) }
    LaunchedEffect(known) {
        if (known) main.setGuest(false)
        val current = nav.currentDestination?.route ?: return@LaunchedEffect
        if (known && current in setOf(Routes.LOGIN, Routes.EMAIL_LOGIN, Routes.REGISTER, Routes.FORGOT_PASSWORD)) {
            nav.navigate(Routes.MAIN) { popUpTo(0) }
        } else if (!known && previous.value && current != Routes.LOGIN) {
            nav.navigate(Routes.LOGIN) { popUpTo(0) }
        }
        previous.value = known
    }

    // Back from the VNPay page (deep link): ask the server about the saved order.
    LaunchedEffect(Unit) { main.paymentReturn.collect { if (known) nav.navigate(Routes.paymentResult(it.bookingId, it.declined)) } }

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
                onGuest = { main.setGuest(true); nav.navigate(Routes.MAIN) { popUpTo(Routes.LOGIN) { inclusive = true } } },
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
            MovieDetailScreen(onBack = { nav.popBackStack() }, onBook = { nav.navigate(Routes.showtimes(it)) }, onLogin = { nav.navigate(Routes.LOGIN) })
        }
        composable(Routes.SHOWTIMES, arguments = listOf(navArgument("movieId") { type = NavType.StringType })) {
            ShowtimePickerScreen(onBack = { nav.popBackStack() }, onPick = { id ->
                // Booking needs an account; browsing does not. A guest is asked to sign in only here.
                if (known) nav.navigate(Routes.seatMap(id)) else nav.navigate(Routes.LOGIN)
            })
        }
        composable(Routes.SEAT_MAP, arguments = listOf(navArgument("showtimeId") { type = NavType.LongType })) { entry ->
            SeatMapScreen(onBack = { nav.popBackStack() }, onBooked = { nav.navigate(Routes.combo(it)) }, onHoldExpired = { seats -> showHoldExpired(nav, showtimeId = entry.arguments?.getLong("showtimeId"), seats = seats) })
        }
        composable(Routes.HOLD_EXPIRED, arguments = listOf(
            navArgument("showtimeId") { type = NavType.LongType },
            navArgument("seats") { type = NavType.StringType; defaultValue = "" },
        )) {
            HoldExpiredScreen(
                onReselect = { id -> nav.navigate(Routes.seatMap(id)) { popUpTo(Routes.HOLD_EXPIRED) { inclusive = true } } },
                onOtherShow = { movieId ->
                    if (movieId != null) nav.navigate(Routes.showtimes(movieId)) { popUpTo(Routes.HOLD_EXPIRED) { inclusive = true } }
                    else nav.popBackStack(Routes.MAIN, inclusive = false)
                },
            )
        }
        composable(Routes.TICKET_DETAIL, arguments = listOf(navArgument("bookingId") { type = NavType.LongType })) { TicketDetailScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.HISTORY) {
            HistoryScreen(onBack = { nav.popBackStack() }, onOpen = { order ->
                when (order.status) {
                    BookingStatus.PAID -> nav.navigate(Routes.ticketDetail(order.id))
                    BookingStatus.PENDING -> nav.navigate(Routes.checkout(order.id))
                    else -> Unit
                }
            })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(onBack = { nav.popBackStack() }, onDeleted = { main.setGuest(true); nav.popBackStack(Routes.MAIN, inclusive = false) })
        }
        composable(Routes.VOUCHERS) { VouchersScreen(onBack = { nav.popBackStack() }, onFind = { nav.popBackStack(); main.openTab(Tab.PROMOS) }) }
        composable(Routes.REMINDERS) { RemindersScreen(onBack = { nav.popBackStack() }, onMovie = { nav.navigate(Routes.movieDetail(it)) }) }
        composable(Routes.PAYMENT_METHODS) { PaymentMethodsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.HELP) { HelpScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { nav.popBackStack() }, onOpen = { n ->
                when {
                    n.type == "MOVIE_ON_SALE" && n.movieId != null -> nav.navigate(Routes.movieDetail(n.movieId))
                    n.bookingId != null && n.type in setOf("ORDER_PAID", "SHOWTIME_REMINDER") -> nav.navigate(Routes.ticketDetail(n.bookingId))
                    n.bookingId != null -> nav.navigate(Routes.HISTORY)
                }
            })
        }
        composable(Routes.COMBO, arguments = listOf(navArgument("bookingId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("bookingId") ?: 0L
            ComboScreen(onBack = { nav.popBackStack() }, onNext = { nav.navigate(Routes.checkout(id)) }, onChooseSeats = { nav.popBackStack(Routes.SEAT_MAP, inclusive = false) }, onHoldExpired = { showtimeId, seats -> showHoldExpired(nav, showtimeId, seats) })
        }
        composable(Routes.CHECKOUT, arguments = listOf(navArgument("bookingId") { type = NavType.LongType })) {
            CheckoutScreen(
                onBack = { nav.popBackStack() },
                onPaying = { nav.navigate(Routes.paymentResult(it)) },
                onVietQr = { id, img, note -> nav.navigate(Routes.vietQr(id, img, note)) },
                onChooseSeats = { nav.popBackStack(Routes.SEAT_MAP, inclusive = false) },
                onHoldExpired = { showtimeId, seats -> showHoldExpired(nav, showtimeId, seats) },
            )
        }
        composable(Routes.VIETQR, arguments = listOf(
            navArgument("bookingId") { type = NavType.LongType },
            navArgument("img") { type = NavType.StringType; defaultValue = "" },
            navArgument("note") { type = NavType.StringType; defaultValue = "" },
        )) {
            VietQrScreen(
                onBack = { nav.popBackStack() },
                onPaid = { id -> nav.navigate(Routes.paymentResult(id)) { popUpTo(Routes.VIETQR) { inclusive = true } } },
            )
        }
        composable(Routes.PAY_FAIL, arguments = listOf(navArgument("bookingId") { type = NavType.LongType })) {
            PayFailScreen(
                onClose = { nav.popBackStack(Routes.CHECKOUT, inclusive = false) },
                onPickOther = { nav.popBackStack(Routes.CHECKOUT, inclusive = false) },
                onSupport = { nav.navigate(Routes.HELP) },
                onCancelled = { main.openTab(Tab.HOME); nav.navigate(Routes.MAIN) { popUpTo(0) } },
                onRetried = { id -> nav.navigate(Routes.paymentResult(id)) { popUpTo(Routes.PAY_FAIL) { inclusive = true } } },
                onHoldExpired = { showtimeId, seats -> showHoldExpired(nav, showtimeId, seats) },
            )
        }
        composable(Routes.PAYMENT_RESULT, arguments = listOf(
            navArgument("bookingId") { type = NavType.LongType },
            navArgument("failed") { type = NavType.BoolType; defaultValue = false },
        )) { entry ->
            PaymentResultScreen(
                onDeclined = { nav.navigate(Routes.payFail(entry.arguments?.getLong("bookingId") ?: 0L)) { popUpTo(Routes.PAYMENT_RESULT) { inclusive = true } } },
                onViewTickets = { main.openTab(Tab.TICKETS); nav.navigate(Routes.MAIN) { popUpTo(0) } },
                onHome = { main.openTab(Tab.HOME); nav.navigate(Routes.MAIN) { popUpTo(0) } },
                onChooseSeats = { nav.popBackStack(Routes.SEAT_MAP, inclusive = false) },
            )
        }
        composable(Routes.MAIN) {
            MainShell(requested = main.requestedTab.collectAsState().value, onTabShown = main::tabShown) { tab ->
                when (tab) {
                    Tab.HOME -> HomeTab(
                        onMovie = { nav.navigate(Routes.movieDetail(it)) }, onBook = { nav.navigate(Routes.showtimes(it)) },
                        onSearch = { nav.navigate(Routes.SEARCH) }, onNotifications = { if (known) nav.navigate(Routes.NOTIFICATIONS) else nav.navigate(Routes.LOGIN) }, onOffers = { main.openTab(Tab.PROMOS) }, onCinemas = { main.openTab(Tab.CINEMAS) },
                        onResume = { nav.navigate(Routes.checkout(it)) },
                        onShow = { id -> if (known) nav.navigate(Routes.seatMap(id)) else nav.navigate(Routes.LOGIN) },
                    )
                    Tab.CINEMAS -> CinemasTab(
                        onMovie = { nav.navigate(Routes.movieDetail(it)) },
                        // Booking needs an account; browsing the cinemas does not.
                        onShow = { id -> if (known) nav.navigate(Routes.seatMap(id)) else nav.navigate(Routes.LOGIN) },
                    )
                    Tab.PROMOS -> OffersTab(onLogin = { nav.navigate(Routes.LOGIN) })
                    Tab.TICKETS -> TicketsTab(
                        onTicket = { nav.navigate(Routes.ticketDetail(it)) }, onBook = { main.openTab(Tab.HOME) },
                        onSignIn = { nav.navigate(Routes.LOGIN) },
                    )
                    Tab.ACCOUNT -> AccountTab(settings, known, main::setDark, main::setLanguage,
                        onSignIn = { nav.navigate(Routes.LOGIN) }, onSignOut = main::signOut,
                        onHistory = { nav.navigate(Routes.HISTORY) }, onProfile = { nav.navigate(Routes.PROFILE) },
                        onVouchers = { nav.navigate(Routes.VOUCHERS) }, onReminders = { nav.navigate(Routes.REMINDERS) },
                        onPayments = { nav.navigate(Routes.PAYMENT_METHODS) }, onHelp = { nav.navigate(Routes.HELP) })
                }
            }
        }
    }
}

/**
 * The seat hold ran out (design item 11): replace everything from the seat map onward with the "hold expired" page. An order
 * resumed from Home has no seat map below it, so that screen is dropped instead. Without a show id (an older server) go home.
 */
private fun showHoldExpired(nav: androidx.navigation.NavHostController, showtimeId: Long?, seats: List<String>) {
    if (showtimeId == null) { nav.popBackStack(Routes.MAIN, inclusive = false); return }
    val hasSeatMap = runCatching { nav.getBackStackEntry(Routes.SEAT_MAP) }.getOrNull() != null
    nav.navigate(Routes.holdExpired(showtimeId, seats)) {
        if (hasSeatMap) popUpTo(Routes.SEAT_MAP) { inclusive = true } else popUpTo(nav.currentDestination?.route ?: Routes.MAIN) { inclusive = true }
    }
}
