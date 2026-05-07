package com.mian.accountrecord.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Preferences module for Hilt DI.
 *
 * AppPreferences already has @Inject constructor and @Singleton annotations,
 * so Hilt can provide it automatically without explicit @Provides or @Binds.
 * This module is kept as a placeholder for future preferences-related bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule
