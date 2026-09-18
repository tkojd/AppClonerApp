package com.appcloner.di

import android.content.Context
import androidx.room.Room
import com.appcloner.data.db.CloneDao
import com.appcloner.data.db.CloneDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room [CloneDatabase] and its [CloneDao].
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideCloneDatabase(@ApplicationContext context: Context): CloneDatabase {
        return Room.databaseBuilder(
            context,
            CloneDatabase::class.java,
            "clone_db"
        ).build()
    }

    @Provides
    fun provideCloneDao(database: CloneDatabase): CloneDao = database.cloneDao()
}
