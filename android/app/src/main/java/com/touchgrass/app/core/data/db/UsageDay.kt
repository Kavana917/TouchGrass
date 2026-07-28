package com.touchgrass.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per "budget day".
 *
 * The key is a day STRING (`2026-07-28`) rather than a timestamp because the
 * day boundary is the user's chosen reset hour — 4am by default, not
 * midnight. Someone scrolling at 11:58pm should not get a gift at 00:00
 * (app_plan.md §2.6).
 *
 * [minutesUsed] is recomputed from UsageStatsManager on every poll rather
 * than incremented. That makes it self-correcting: if the service is killed
 * for twenty minutes, the next poll still reports the true total, because
 * the OS was keeping count the whole time.
 */
@Entity(tableName = "usage_days")
data class UsageDay(
    @PrimaryKey val date: String,
    val minutesUsed: Int,
    /** Per-package breakdown as JSON, e.g. {"com.instagram.android":12}. */
    val perAppJson: String = "{}",
    val updatedAt: Long = System.currentTimeMillis()
)
