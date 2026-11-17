package com.ceta.cinema.data.repository

import com.ceta.cinema.core.*
import com.ceta.cinema.data.remote.api.CetaApi
import com.ceta.cinema.data.remote.mapper.toDomain
import com.ceta.cinema.domain.model.*
import java.time.Duration
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Movies, cinemas, showtimes, snacks and offers: cached for 6 hours (the seat map never is). */
@Singleton
class CatalogRepository @Inject constructor(private val api: CetaApi, private val offline: OfflineFlag) {
    private val ttl = Duration.ofHours(6)
    private val movieLists = TtlCache<String, List<Movie>>(ttl)
    private val movieById = TtlCache<String, Movie>(ttl)
    private val genreList = TtlCache<Unit, List<Genre>>(ttl)
    private val cinemaLists = TtlCache<String, List<Cinema>>(ttl)
    private val showtimeLists = TtlCache<String, List<Showtime>>(ttl)
    private val comboList = TtlCache<Unit, List<Combo>>(ttl)
    private val promoList = TtlCache<Unit, List<Promotion>>(ttl)

    suspend fun movies(q: String? = null, genre: String? = null, status: MovieStatus? = null): ApiResult<List<Movie>> {
        val key = "${q.orEmpty()}|${genre.orEmpty()}|${status?.name.orEmpty()}"
        return cachedCall(movieLists, key, offline) {
            api.movies(q = q?.takeIf { it.isNotBlank() }, genre = genre, status = status?.name).map { it.toDomain() }
        }
    }

    suspend fun movie(id: String): ApiResult<Movie> = cachedCall(movieById, id, offline) { api.movie(id).toDomain() }

    /** Reviews change a movie's rating, so the detail screen asks for a fresh copy after writing one. */
    fun forgetMovie(id: String) { movieById.remove(id); movieLists.clear() }

    suspend fun genres(): ApiResult<List<Genre>> = cachedCall(genreList, Unit, offline) { api.genres().map { it.toDomain() } }

    suspend fun cinemas(city: String? = null): ApiResult<List<Cinema>> =
        cachedCall(cinemaLists, city.orEmpty(), offline) { api.cinemas(city).map { it.toDomain() } }

    suspend fun showtimesForMovie(movieId: String, date: LocalDate): ApiResult<List<Showtime>> =
        cachedCall(showtimeLists, "m:$movieId:$date", offline) { api.showtimes(movieId = movieId, date = date.toString()).map { it.toDomain() } }

    suspend fun showtimesAtCinema(cinemaId: Long, date: LocalDate): ApiResult<List<Showtime>> =
        cachedCall(showtimeLists, "c:$cinemaId:$date", offline) { api.showtimes(cinemaId = cinemaId, date = date.toString()).map { it.toDomain() } }

    suspend fun showtime(id: Long): ApiResult<Showtime> = safeApiCall { api.showtime(id).toDomain() }

    suspend fun combos(): ApiResult<List<Combo>> = cachedCall(comboList, Unit, offline) { api.combos().map { it.toDomain() } }

    suspend fun offers(): ApiResult<List<Promotion>> = cachedCall(promoList, Unit, offline) { api.promotions().map { it.toDomain() } }
}
