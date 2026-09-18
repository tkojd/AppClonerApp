package com.appcloner.di

import android.content.Context
import android.content.pm.PackageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Application-wide Hilt bindings.
 *
 * The repositories and [com.appcloner.data.CloneManager] use `@Inject` constructors
 * (annotated `@Singleton`), so they do not need explicit `@Provides` methods here.
 * This module exposes framework dependencies that lack an injectable constructor,
 * such as the [PackageManager].
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePackageManager(@ApplicationContext context: Context): PackageManager {
        return context.packageManager
    }
}
