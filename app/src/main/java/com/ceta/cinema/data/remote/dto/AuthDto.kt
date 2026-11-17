package com.ceta.cinema.data.remote.dto

data class FirebaseLoginRequest(val idToken: String)
data class RefreshRequest(val refreshToken: String)

data class UserDto(val id: Long?, val username: String?, val email: String?, val roles: List<String> = emptyList())

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String? = null,
    val expiresInSeconds: Long = 0,
    val user: UserDto? = null,
)
