package com.touchgrass.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's single local database. Everything lives here and never leaves
 * the device — see app_plan.md §6.3.
 *
 * When you add an entity, add it to [entities] AND bump [version],
 * or Room will crash at runtime with a schema mismatch.
 *
 * Coming in later phases: Essay + PassLedger (Phase 3),
 * Book/Page/Stroke (Phase 8), DigestCache (Phase 10).
 */
@Database(
    entities = [UsageDay::class],
    version = 2,
    exportSchema = false
)
abstract class TouchGrassDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
}
