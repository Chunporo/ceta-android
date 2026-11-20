package com.ceta.cinema.di

import com.ceta.cinema.data.repository.MockTrendingRepository
import com.ceta.cinema.data.repository.TrendingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Swap [MockTrendingRepository] for a real one when the backend has a trending endpoint. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TrendingModule {
    @Binds abstract fun trending(impl: MockTrendingRepository): TrendingRepository
}

/** Swap [MockMembershipRepository] for a real one when the backend serves the points history, expiry and member code. */
@Module
@InstallIn(SingletonComponent::class)
abstract class MembershipModule {
    @Binds abstract fun membership(impl: com.ceta.cinema.data.repository.MockMembershipRepository): com.ceta.cinema.data.repository.MembershipRepository
}
