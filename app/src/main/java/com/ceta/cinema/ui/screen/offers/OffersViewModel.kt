package com.ceta.cinema.ui.screen.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceta.cinema.core.ApiError
import com.ceta.cinema.core.ApiResult
import com.ceta.cinema.core.OfflineFlag
import com.ceta.cinema.core.UiState
import com.ceta.cinema.data.repository.AccountRepository
import com.ceta.cinema.data.repository.CatalogRepository
import com.ceta.cinema.data.repository.SessionRepository
import com.ceta.cinema.domain.model.Promotion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OffersUi(
    val offers: List<Promotion>,
    /** Promotion ids in the customer's voucher wallet. */
    val saved: Set<Long>,
    val busy: Set<Long> = emptySet(),
    val justSaved: Boolean = false,
    val problem: ApiError? = null,
    val needsLogin: Boolean = false,
)

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val catalog: CatalogRepository,
    private val account: AccountRepository,
    private val session: SessionRepository,
    offlineFlag: OfflineFlag,
) : ViewModel() {
    val offline: StateFlow<Boolean> = offlineFlag.offline
    private val _state = MutableStateFlow<UiState<OffersUi>>(UiState.Loading)
    val state: StateFlow<UiState<OffersUi>> = _state

    init { load() }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            when (val offers = catalog.offers()) {
                is ApiResult.Err -> _state.value = UiState.Error(offers.error)
                is ApiResult.Ok -> {
                    // The wallet only exists for a signed-in customer; a guest just sees every offer as not saved.
                    val saved = if (session.signedIn.value == true) (account.walletPromotionIds() as? ApiResult.Ok)?.data.orEmpty() else emptySet()
                    _state.value = UiState.Content(OffersUi(offers.data, saved))
                }
            }
        }
    }

    fun toggleSave(promotion: Promotion) {
        val ui = current() ?: return
        if (promotion.id in ui.busy) return
        if (session.signedIn.value != true) { update { it.copy(needsLogin = true) }; return }
        val wasSaved = promotion.id in ui.saved
        // Show the change at once, put it back if the server says no.
        update { it.copy(busy = it.busy + promotion.id, saved = if (wasSaved) it.saved - promotion.id else it.saved + promotion.id, justSaved = false, problem = null) }
        viewModelScope.launch {
            val result = if (wasSaved) account.removeVoucher(promotion.id) else account.saveVoucher(promotion.id)
            update {
                when (result) {
                    is ApiResult.Ok -> it.copy(busy = it.busy - promotion.id, justSaved = !wasSaved)
                    is ApiResult.Err -> it.copy(busy = it.busy - promotion.id, problem = result.error,
                        saved = if (wasSaved) it.saved + promotion.id else it.saved - promotion.id)
                }
            }
        }
    }

    fun consumeMessage() = update { it.copy(justSaved = false, problem = null, needsLogin = false) }

    private fun current(): OffersUi? = (_state.value as? UiState.Content)?.data

    private fun update(change: (OffersUi) -> OffersUi) {
        _state.update { s -> if (s is UiState.Content) UiState.Content(change(s.data)) else s }
    }
}
