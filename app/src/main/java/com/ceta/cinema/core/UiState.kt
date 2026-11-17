package com.ceta.cinema.core

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Empty(val hint: String? = null) : UiState<Nothing>
    data class Error(val error: ApiError) : UiState<Nothing>
}
