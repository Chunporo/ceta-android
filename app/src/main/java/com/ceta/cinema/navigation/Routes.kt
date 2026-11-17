package com.ceta.cinema.navigation

object Routes {
    const val ONBOARD = "onboard"
    const val LOGIN = "login"
    const val EMAIL_LOGIN = "login-email"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
    /** The five-tab shell (Home, Cinemas, My tickets, Offers, Account). */
    const val MAIN = "main"
    const val SEARCH = "search"
    const val MOVIE_DETAIL = "movie/{movieId}"
    fun movieDetail(id: String) = "movie/$id"
}
