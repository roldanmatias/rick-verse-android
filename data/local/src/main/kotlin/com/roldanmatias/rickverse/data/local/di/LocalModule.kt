package com.roldanmatias.rickverse.data.local.di

import android.content.Context
import androidx.room.Room
import com.roldanmatias.rickverse.core.domain.repository.CastRepository
import com.roldanmatias.rickverse.data.local.db.CastDao
import com.roldanmatias.rickverse.data.local.db.MIGRATION_1_2
import com.roldanmatias.rickverse.data.local.db.RickVerseDatabase
import com.roldanmatias.rickverse.data.local.repository.OfflineFirstCastRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LocalProvidesModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): RickVerseDatabase =
        Room
            .databaseBuilder(context, RickVerseDatabase::class.java, "rickverse.db")
            // Real migrations, no destructive fallback: the cache survives app updates.
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun castDao(database: RickVerseDatabase): CastDao = database.castDao()
}

/** The unqualified `CastRepository` features inject is the offline-first decorator around `@Remote`. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class LocalBindingsModule {
    @Binds
    @Singleton
    abstract fun castRepository(impl: OfflineFirstCastRepository): CastRepository
}
