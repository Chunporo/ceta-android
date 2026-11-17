package com.ceta.cinema.di

import com.ceta.cinema.data.remote.firebase.FirebaseAuthSource
import com.ceta.cinema.data.remote.firebase.FirebaseAuthSourceImpl
import com.ceta.cinema.data.remote.firebase.PendingEmailStore
import com.ceta.cinema.data.remote.firebase.PrefsPendingEmailStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds @Singleton abstract fun firebaseAuth(impl: FirebaseAuthSourceImpl): FirebaseAuthSource
    @Binds @Singleton abstract fun pendingEmail(impl: PrefsPendingEmailStore): PendingEmailStore
}
