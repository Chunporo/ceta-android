package com.ceta.cinema.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.local.prefs.SettingsStore
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.domain.model.Movie
import com.ceta.cinema.domain.model.MovieStatus
import com.ceta.cinema.domain.model.Promotion
import com.ceta.cinema.domain.usecase.NextShowtimes
import com.ceta.cinema.domain.usecase.NextShowtimesPicker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import com.ceta.cinema.core.CinemaZone

data class HomeData(val nowShowing: List<Movie>, val comingSoon: List<Movie>, val offers: List<Promotion>) {
    /** The big cards at the top: the first few movies on screens. */
    val featured: List<Movie> get() = nowShowing.take(HERO_COUNT)

    companion object { const val HERO_COUNT = 5 }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val account: AccountRepository,
    private val session: SessionRepository,
    settings: SettingsStore,
    offlineFlag: OfflineFlag,
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state

    private val _unread = MutableStateFlow(0L)
    val unread: StateFlow<Long> = _unread

    val displayName: StateFlow<String?> = settings.settings.map { it.displayName }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val offline: StateFlow<Boolean> = offlineFlag.offline

    /** An order created but not paid whose seats are still held: offered on Home so a killed app can carry on. */
    val pendingOrder: StateFlow<Long?> = settings.settings
        .map { s -> s.pendingBookingId?.takeIf { s.pendingHoldUntilMillis > System.currentTimeMillis() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _nextShows = MutableStateFlow<Map<String, NextShowtimes>>(emptyMap())
    /** Today's next times per carousel movie, filled as each one reaches the centre. A movie that is absent is still loading. */
    val nextShows: StateFlow<Map<String, NextShowtimes>> = _nextShows
    private val asked = HashSet<String>()

    init { load() }

    /** The carousel centred on [movie]: look up its next showtimes once (a failed lookup shows "other days" instead of nothing). */
    fun onCentered(movie: Movie) {
        if (!asked.add(movie.id)) return
        viewModelScope.launch {
            val today = LocalDate.now(clock.withZone(CinemaZone))
            val shows = (catalog.showtimesForMovie(movie.id, today) as? ApiResult.Ok)?.data.orEmpty()
            _nextShows.update { it + (movie.id to NextShowtimesPicker.pick(shows, clock.instant())) }
        }
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            coroutineScope {
                val now = async { catalog.movies(status = MovieStatus.NOW_SHOWING) }
                val soon = async { catalog.movies(status = MovieStatus.COMING_SOON) }
                val offers = async { catalog.offers() }
                val nowResult = now.await()
                val soonResult = soon.await()
                _state.value = when {
                    nowResult is ApiResult.Err -> UiState.Error(nowResult.error)
                    else -> UiState.Content(HomeData(
                        nowShowing = (nowResult as ApiResult.Ok).data,
                        comingSoon = (soonResult as? ApiResult.Ok)?.data.orEmpty(),
                        // Offers are a nice extra: if they fail the rest of the page still works.
                        offers = (offers.await() as? ApiResult.Ok)?.data.orEmpty().take(OFFERS_ON_HOME),
                    ))
                }
            }
            refreshUnread()
        }
    }

    fun refreshUnread() {
        if (session.signedIn.value != true) { _unread.value = 0; return }
        viewModelScope.launch { _unread.value = (account.unreadCount() as? ApiResult.Ok)?.data ?: 0 }
    }

    companion object { const val OFFERS_ON_HOME = 4 }
}
