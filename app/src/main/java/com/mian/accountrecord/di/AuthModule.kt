package com.mian.accountrecord.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Auth module for Hilt DI.
 *
 * AuthPreferences already has @Singleton @Inject constructor(),
 * so Hilt can provide it automatically without explicit @Provides.
 *
 * UserDao is provided by DatabaseModule.
 * AuthRepository binding is in RepositoryModule.
 *
 * This module is kept for future auth-related bindings
 * (e.g., OAuth SDK wrappers, token managers).
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule
