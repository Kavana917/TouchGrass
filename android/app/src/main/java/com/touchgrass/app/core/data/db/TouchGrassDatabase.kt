package com.touchgrass.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's single local database. Everything lives here and never leaves
 * the device — see app_plan.md §6.3.
 *
 * When you add an entity or change one, add it to [entities], bump
 * [version], AND write a migration in [Migrations]. The destructive
 * fallback was removed at version 4 — essays cannot be regenerated, so
 * dropping tables is no longer an acceptable failure mode.
 *
 * Coming later: Book/Page/Stroke (Phase 8), DigestCache (Phase 10).
 */
@Database(
    entities = [UsageDay::class, Essay::class, PassGrant::class],
    version = 4,
    exportSchema = false
)
abstract class TouchGrassDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun essayDao(): EssayDao
    abstract fun passDao(): PassDao
}
