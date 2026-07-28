package com.touchgrass.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Throwaway entity used only to prove the Room pipeline works in Phase 0.
 *
 * Delete this (and [ScratchDao]) in Phase 2, when the real entities
 * — UsageDay, PassLedger, Essay — replace it.
 */
@Entity(tableName = "scratch_notes")
data class ScratchNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)
