package com.ceta.cinema.ui.screen.movie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.model.Review
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MovieDetailUi(
    val movie: Movie,
    val reviews: List<Review> = emptyList(),
    val myReview: Review? = null,
    val reminderOn: Boolean = false,
    val busy: Boolean = false,
    /** A refusal to show once, as a snackbar (for example REVIEW_NOT_ALLOWED). */
    val problem: ApiError? = null,
    /** True when the action needs an account and the customer is a guest. */
    val needsLogin: Boolean = false,
    /** False for a guest: writing a review or a reminder needs an account. */
    val signedIn: Boolean = false,
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val catalog: CatalogRepository,
    private val account: AccountRepository,
    private val session: SessionRepository,
) : ViewModel() {
    private val movieId: String = checkNotNull(savedState["movieId"])
    private val _state = MutableStateFlow<UiState<MovieDetailUi>>(UiState.Loading)
    val state: StateFlow<UiState<MovieDetailUi>> = _state

    init { load() }

    private fun signedIn() = session.signedIn.value == true

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            coroutineScope {
                val movie = async { catalog.movie(movieId) }
                val reviews = async { account.reviews(movieId) }
                // Own review and reminder only exist for a signed-in customer.
                val mine = async { if (signedIn()) account.myReview(movieId) else null }
                val reminders = async { if (signedIn()) account.reminderMovieIds() else null }
                when (val m = movie.await()) {
                    is ApiResult.Err -> _state.value = UiState.Error(m.error)
                    is ApiResult.Ok -> _state.value = UiState.Content(MovieDetailUi(
                        movie = m.data,
                        reviews = (reviews.await() as? ApiResult.Ok)?.data.orEmpty(),
                        myReview = (mine.await() as? ApiResult.Ok)?.data,
                        reminderOn = ((reminders.await() as? ApiResult.Ok)?.data ?: emptySet()).contains(movieId),
                        signedIn = signedIn(),
                    ))
                }
            }
        }
    }

    /** The review form is only for signed-in customers; a guest is asked to sign in instead. */
    fun requestReview(): Boolean {
        if (signedIn()) return true
        update { it.copy(needsLogin = true) }
        return false
    }

    fun consumeProblem() = update { it.copy(problem = null, needsLogin = false) }

    fun toggleReminder() {
        val ui = current() ?: return
        if (ui.busy) return
        if (!signedIn()) { update { it.copy(needsLogin = true) }; return }
        update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = if (ui.reminderOn) account.removeReminder(movieId) else account.addReminder(movieId)
            update {
                when (result) {
                    is ApiResult.Ok -> it.copy(busy = false, reminderOn = !ui.reminderOn)
                    is ApiResult.Err -> it.copy(busy = false, problem = result.error)
                }
            }
        }
    }

    fun saveReview(rating: Int, comment: String) {
        val ui = current() ?: return
        if (ui.busy) return
        if (!signedIn()) { update { it.copy(needsLogin = true) }; return }
        update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = account.saveReview(movieId, rating, comment.trim().ifEmpty { null })) {
                is ApiResult.Err -> update { it.copy(busy = false, problem = result.error) }
                is ApiResult.Ok -> {
                    catalog.forgetMovie(movieId) // the movie's average and count just changed
                    refreshAfterReview(result.data)
                }
            }
        }
    }

    fun deleteReview() {
        val ui = current() ?: return
        if (ui.busy || ui.myReview == null) return
        update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = account.deleteReview(movieId)) {
                is ApiResult.Err -> update { it.copy(busy = false, problem = result.error) }
                is ApiResult.Ok -> { catalog.forgetMovie(movieId); refreshAfterReview(null) }
            }
        }
    }

    private suspend fun refreshAfterReview(mine: Review?) {
        val movie = (catalog.movie(movieId) as? ApiResult.Ok)?.data
        val reviews = (account.reviews(movieId) as? ApiResult.Ok)?.data
        update { it.copy(busy = false, myReview = mine, movie = movie ?: it.movie, reviews = reviews ?: it.reviews) }
    }

    private fun current(): MovieDetailUi? = (_state.value as? UiState.Content)?.data

    private fun update(change: (MovieDetailUi) -> MovieDetailUi) {
        _state.update { s -> if (s is UiState.Content) UiState.Content(change(s.data)) else s }
    }
}
