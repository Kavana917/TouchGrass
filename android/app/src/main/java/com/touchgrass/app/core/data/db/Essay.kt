package com.touchgrass.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A submitted essay.
 *
 * PRIVACY: this text never leaves the device. Not synced, not analysed, not
 * read, not sent to any API — see app_plan.md §6.3 and tech_stack.md §6.
 * "Your essays never leave this phone" is the sentence that earns the trust
 * an app holding usage access needs, so it has to stay literally true.
 *
 * [cadenceSuspicious] records that the typing rhythm looked automated. It is
 * deliberately a flag and not a rejection — see TypingGuard for why.
 */
@Entity(tableName = "essays")
data class Essay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val body: String,
    val wordCount: Int,
    val requiredWords: Int,
    val writtenAt: Long = System.currentTimeMillis(),
    /** Seconds from first keystroke to submission. */
    val durationSeconds: Int = 0,
    val cadenceSuspicious: Boolean = false
)
