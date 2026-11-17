package com.ceta.cinema.data.local.secure

interface TokenStore {
    suspend fun saveTokens(access: String, refresh: String)
    suspend fun access(): String?
    suspend fun refresh(): String?
    suspend fun clear()
}
