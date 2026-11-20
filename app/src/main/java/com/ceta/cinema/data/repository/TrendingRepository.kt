package com.ceta.cinema.data.repository

import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.domain.model.Movie
import javax.inject.Inject
import javax.inject.Singleton

enum class Trend { UP, FLAT, DOWN }

data class TrendingItem(val movie: Movie, val trend: Trend)

/** "Searched a lot this week": the five movies people look for most, each with its movement since last week. */
interface TrendingRepository {
    suspend fun top(limit: Int = 5): ApiResult<List<TrendingItem>>
}

/**
 * TODO(backend): there is no trending endpoint. Until there is one (for example GET /movies/trending with a rank and a movement),
 * this stands in with the best-rated films in cinemas, and the movements are made up so the list shows all three arrows.
 */
@Singleton
class MockTrendingRepository @Inject constructor(private val catalog: CatalogRepository) : TrendingRepository {
    override suspend fun top(limit: Int): ApiResult<List<TrendingItem>> = when (val result = catalog.movies()) {
        is ApiResult.Err -> result
        is ApiResult.Ok -> {
            val moves = listOf(Trend.UP, Trend.UP, Trend.FLAT, Trend.DOWN, Trend.UP)
            ApiResult.Ok(
                result.data.filter { it.isNowShowing }
                    .sortedWith(compareByDescending<Movie> { it.ratingCount }.thenByDescending { it.ratingAverage ?: 0.0 }.thenBy { it.title })
                    .take(limit).mapIndexed { i, m -> TrendingItem(m, moves[i % moves.size]) },
            )
        }
    }
}
