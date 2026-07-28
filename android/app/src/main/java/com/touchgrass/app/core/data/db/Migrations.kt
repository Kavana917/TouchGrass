package com.touchgrass.app.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations.
 *
 * WHY THESE EXIST NOW: up to version 3 the database held only recomputable
 * usage totals, so a destructive fallback cost nothing — the next poll
 * rebuilt everything from the OS event log.
 *
 * Version 3 introduced essays. Those are hand-typed words that cannot be
 * regenerated and that the app explicitly promises to keep. Dropping them
 * on a schema change would be a genuine betrayal, not an inconvenience —
 * so from here every version bump gets a real migration and the destructive
 * fallback is gone.
 */
object Migrations {

    /** Adds PassGrant.packageName for per-app budget mode. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Nullable with no default: existing grants predate per-app mode
            // and correctly belong to the shared pool.
            db.execSQL("ALTER TABLE pass_grants ADD COLUMN packageName TEXT")
        }
    }

    val ALL = arrayOf(MIGRATION_3_4)
}
