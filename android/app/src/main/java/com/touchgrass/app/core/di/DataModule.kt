package com.touchgrass.app.core.di

import android.content.Context
import androidx.room.Room
import com.touchgrass.app.core.data.db.EssayDao
import com.touchgrass.app.core.data.db.Migrations
import com.touchgrass.app.core.data.db.PassDao
import com.touchgrass.app.core.data.db.TouchGrassDatabase
import com.touchgrass.app.core.data.db.UsageDao
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt how to build the things it can't construct on its own.
 *
 * [SingletonComponent] means these live for the whole app lifetime —
 * one database, one settings store, shared by every screen and the
 * monitor service.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TouchGrassDatabase =
        Room.databaseBuilder(
            context,
            TouchGrassDatabase::class.java,
            "touchgrass.db"
        )
            // No destructive fallback. Essays are hand-typed and cannot be
            // regenerated, so a schema change must never silently drop them
            // — if a migration is missing, crashing loudly in development is
            // the correct outcome. See Migrations.
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides
    fun provideUsageDao(database: TouchGrassDatabase): UsageDao =
        database.usageDao()

    @Provides
    fun provideEssayDao(database: TouchGrassDatabase): EssayDao =
        database.essayDao()

    @Provides
    fun providePassDao(database: TouchGrassDatabase): PassDao =
        database.passDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)
}
