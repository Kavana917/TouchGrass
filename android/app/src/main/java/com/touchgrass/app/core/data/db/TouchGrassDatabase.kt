package com.touchgrass.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's single local database. Everything lives here and never leaves
 * the device — see app_plan.md §6.3.
 *
 * When you add an entity, add it to [entities] AND bump [version],
 * or Room will crash at runtime with a schema mismatch.
 */
@Database(
    entities = [ScratchNote::class],
    version = 1,
    exportSchema = false
)
abstract class TouchGrassDatabase : RoomDatabase() {
    abstract fun scratchDao(): ScratchDao
}
