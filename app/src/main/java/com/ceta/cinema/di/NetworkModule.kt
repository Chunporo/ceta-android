package com.ceta.cinema.di

import com.ceta.cinema.BuildConfig
import com.ceta.cinema.core.SessionEvents
import com.ceta.cinema.data.local.secure.KeystoreTokenStore
import com.ceta.cinema.data.local.secure.TokenStore
import com.ceta.cinema.data.remote.NetworkFactory
import com.ceta.cinema.data.remote.api.CetaApi
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TokenModule {
    @Binds @Singleton abstract fun tokenStore(impl: KeystoreTokenStore): TokenStore
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun gson(): Gson = com.ceta.cinema.data.remote.ApiGson.create()

    @Provides @Singleton
    fun api(tokens: TokenStore, events: SessionEvents, gson: Gson): CetaApi =
        NetworkFactory.create(BuildConfig.API_BASE_URL, tokens, events, gson, debug = BuildConfig.DEBUG)
}
