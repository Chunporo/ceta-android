package com.ceta.cinema.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** One injectable clock, so anything that depends on "now" can be tested with a fake one. */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {
    @Provides @Singleton fun clock(): Clock = Clock.systemUTC()
}
