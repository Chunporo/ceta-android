package com.ceta.cinema.data.remote.mapper

import com.ceta.cinema.data.remote.dto.*
import com.ceta.cinema.domain.model.*
import java.time.Instant
import java.time.LocalDate

/** The only place that parses timestamps: if the backend ever sends epoch millis, fix it here. */
internal fun String?.toInstantOrNull(): Instant? = this?.let { runCatching { Instant.parse(it) }.getOrNull() }
internal fun String?.toLocalDateOrNull(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

fun GenreDto.toDomain() = Genre(id ?: 0, name.orEmpty(), slug.orEmpty())

fun MovieDto.toDomain() = Movie(
    id = id, title = title, posterUrl = posterUrl, ageRating = ageRating, status = MovieStatus.parse(status),
    durationMin = duration, releaseDate = releaseDate.toLocalDateOrNull(), trailerUrl = trailerUrl, director = director,
    language = language, description = description, ratingAverage = ratingAverage, ratingCount = ratingCount, fromPrice = fromPrice,
    cast = cast, genres = genres.mapNotNull { it.name }, formats = formats.map { ShowFormat.parse(it) }.toSet(),
    castMembers = if (castMembers.isNotEmpty()) castMembers.map { CastMember(it.name, it.role?.takeIf { r -> r.isNotBlank() }, it.photoUrl?.takeIf { u -> u.isNotBlank() }) }
    else cast.map { CastMember(it, null, null) },
)

fun CinemaDto.toDomain() = Cinema(id, name, city, address, latitude, longitude, imax)

fun ShowtimeDto.toDomain() = Showtime(
    id = id, movieId = movie?.id, movieTitle = movie?.title, posterUrl = movie?.posterUrl,
    cinemaId = cinema?.id, cinemaName = cinema?.name, roomName = room?.name,
    start = startTime.toInstantOrNull() ?: Instant.EPOCH, end = endTime.toInstantOrNull(), basePrice = basePrice ?: 0L,
    format = ShowFormat.parse(format), languageMode = LanguageMode.parse(languageMode), cancelled = status == "CANCELLED",
    availableSeats = availableSeats, totalSeats = totalSeats,
)

fun SeatDto.toDomain() = Seat(
    id = seatId, label = label, row = rowNumber ?: 0, column = columnNumber ?: 0,
    kind = SeatKind.parse(type),
    status = if (active) SeatStatus.parse(status) else SeatStatus.INACTIVE,
    price = price ?: 0L,
)

fun ComboDto.toDomain() = Combo(id, name, nameEn, description, descriptionEn, imageUrl, price)

fun PromotionDto.toDomain() = Promotion(
    id = id, code = code, title = title, titleEn = titleEn, description = description, descriptionEn = descriptionEn,
    imageUrl = imageUrl, validUntil = validUntil.toInstantOrNull(),
    kind = runCatching { PromotionKind.valueOf(kind) }.getOrDefault(PromotionKind.COUNTER_PERK), // unknown = not applicable online
)

fun ReviewDto.toDomain() = Review(id, rating, comment, author ?: "***", mine, createdAt.toInstantOrNull())

fun LoyaltyDto.toDomain() = Loyalty(
    balance, lifetimeEarned, tier, nextTier, pointsToNextTier, redeemBlockPoints, redeemBlockVnd,
    stats.ticketsWatched, stats.vouchers, stats.reviews,
)

fun BookingSeatDto.toDomain() = BookingSeat(seatId, label, price)
fun ComboLineDto.toDomain() = ComboLine(comboId, name, quantity, unitPrice)

fun CreateBookingDto.toDomain() = CreatedBooking(bookingId, holdExpiresAt.toInstantOrNull(), totalAmount, seats.map { it.toDomain() })

fun BookingDto.toDomain() = Booking(
    id = id, status = BookingStatus.parse(status), totalAmount = totalAmount,
    holdExpiresAt = holdExpiresAt.toInstantOrNull(), secondsRemaining = secondsRemaining,
    seats = seats.map { it.toDomain() }, tickets = tickets.map { BookingTicketRef(it.ticketId, it.seatId, it.label, it.code) },
    movieTitle = movieTitle, roomName = roomName, startTime = startTime.toInstantOrNull(), refundRequired = refundRequired,
    comboAmount = comboAmount, combos = combos.map { it.toDomain() }, discountAmount = discountAmount, voucherCode = voucherCode,
    pointsAmount = pointsAmount, pointsUsed = pointsUsed,
)

fun BookingExtrasDto.toDomain() = Extras(totalAmount, comboAmount, discountAmount, voucherCode, combos.map { it.toDomain() }, pointsAmount, pointsUsed)

fun BookingHistoryDto.toDomain() = BookingSummary(
    id, BookingStatus.parse(status), totalAmount, movieTitle, moviePosterUrl, roomName, seatNames,
    startTime.toInstantOrNull(), createdAt.toInstantOrNull(),
)

fun TicketDto.toDomain() = Ticket(
    id = ticketId, bookingId = bookingId, code = code, seatLabel = seatLabel, price = price, qrPayload = qrPayload, used = used,
    showtimeId = showtime.showtimeId, movieTitle = showtime.movieTitle, posterUrl = showtime.posterUrl,
    cinemaName = showtime.cinemaName, cinemaAddress = showtime.cinemaAddress, roomName = showtime.roomName,
    format = ShowFormat.parse(showtime.format), start = showtime.startTime.toInstantOrNull() ?: Instant.EPOCH,
    end = showtime.endTime.toInstantOrNull(),
)

fun NotificationDto.toDomain() = AppNotification(id, type, title, body, bookingId, movieId, createdAt.toInstantOrNull(), read)

private const val PHONE_EMAIL_DOMAIN = "@phone.ceta.invalid"

/** The backend gives a phone sign-in an internal address "p<digits>@phone.ceta.invalid"; show the number, never that address. */
fun UserDto.toProfile(): Profile {
    val mail = email.orEmpty()
    val phone = if (mail.endsWith(PHONE_EMAIL_DOMAIN)) "+" + mail.removeSuffix(PHONE_EMAIL_DOMAIN).removePrefix("p").filter { it.isDigit() } else null
    return Profile(username = username.orEmpty(), email = mail.takeIf { it.isNotBlank() && phone == null }, phone = phone)
}
