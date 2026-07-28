package com.touchgrass.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's single local database. Everything lives here and never leaves
 * the device — see app_plan.md §6.3.
 *
 * When you add an entity, add it to [entities] AND bump [version].
 *
 * ⚠️ FROM VERSION 3 ONWARD, WRITE A REAL MIGRATION.
 * The destructive fallback in DataModule was acceptable while the only data
 * was recomputable usage totals. This version introduces essays — someone's
 * hand-typed words, which cannot be regenerated and which they were promised
 * would be kept. Dropping that table on a schema change would be a genuine
 * betrayal, not an inconvenience.
 *
 * Coming later: Book/Page/Stroke (Phase 8), DigestCache (Phase 10).
 */
@Database(
    entities = [UsageDay::class, Essay::class, PassGrant::class],
    version = 3,
    exportSchema = false
)
abstract class TouchGrassDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun essayDao(): EssayDao
    abstract fun passDao(): PassDao
}
