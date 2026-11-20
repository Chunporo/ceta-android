package com.ceta.cinema.domain

import com.ceta.cinema.data.remote.dto.ExternalRatingDto
import com.ceta.cinema.data.remote.dto.MovieDto
import com.ceta.cinema.data.remote.mapper.toDomain
import com.ceta.cinema.domain.model.ExternalRating
import com.ceta.cinema.domain.model.ExternalRating.Source
import org.junit.Assert.assertEquals
import org.junit.Test

class ExternalRatingTest {
    @Test fun eachSiteIsWrittenTheWayItScoresFilms() {
        assertEquals("7.4", ExternalRating(Source.IMDB, 7.4, 10, 60587).display)
        assertEquals("87%", ExternalRating(Source.ROTTEN_TOMATOES, 87.0, 100, null).display)
        assertEquals("71", ExternalRating(Source.METACRITIC, 71.0, 100, null).display)
    }

    @Test fun anUnknownSiteFromANewerServerIsIgnored() {
        val movie = MovieDto(id = "m", title = "T", externalRatings = listOf(ExternalRatingDto("IMDB", 8.0, 10, 100), ExternalRatingDto("LETTERBOXD", 4.0, 5, null))).toDomain()
        assertEquals(listOf(Source.IMDB), movie.externalRatings.map { it.source })
    }
}
