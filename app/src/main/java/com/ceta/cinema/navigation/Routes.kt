package com.ceta.cinema.navigation

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot-password"
    const val HOME = "home"
    const val MOVIE_LIST = "movies?q={q}"
    const val MOVIE_DETAIL = "movie/{movieId}"
    const val SHOWTIME = "showtime/{movieId}"
    const val SEAT_MAP = "seats/{showtimeId}"
    const val CHECKOUT = "checkout/{bookingId}"
    const val PAYMENT_RESULT = "payment/result/{bookingId}"
    const val REFUND_NOTICE = "payment/refund/{bookingId}"
    const val TICKETS = "tickets"
    const val TICKET_DETAIL = "tickets/{ticketId}"
    const val BOOKINGS = "bookings"
    const val BOOKING_DETAIL = "bookings/{bookingId}"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
}
