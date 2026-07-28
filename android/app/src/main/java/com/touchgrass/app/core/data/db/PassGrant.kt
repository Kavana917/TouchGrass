package com.touchgrass.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A pass earned by writing an essay: N extra minutes, on a given budget day.
 *
 * Stored as a ledger of grants rather than a mutable "minutes remaining"
 * counter, for the same reason usage is recomputed rather than accumulated
 * (see UsageStatsProvider): a ledger can be re-derived and audited, a
 * counter can only drift.
 *
 * [dayKey] scopes the grant to one budget day so passes don't roll over —
 * tomorrow starts fresh at the base budget.
 */
@Entity(tableName = "pass_grants")
data class PassGrant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Budget day this pass applies to, e.g. `2026-07-28`. */
    val dayKey: String,
    val minutesGranted: Int,
    /** The essay that bought it, or null for a panic unlock (Phase 5). */
    val essayId: Long?,
    val issuedAt: Long = System.currentTimeMillis()
)
