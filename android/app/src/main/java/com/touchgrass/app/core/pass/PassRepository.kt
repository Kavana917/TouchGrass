package com.touchgrass.app.core.pass

import com.touchgrass.app.core.data.db.Essay
import com.touchgrass.app.core.data.db.EssayDao
import com.touchgrass.app.core.data.db.PassDao
import com.touchgrass.app.core.data.db.PassGrant
import com.touchgrass.app.core.data.settings.BudgetMode
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.usage.UsageStatsProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Issues passes and reports how many bonus minutes today has earned.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PassRepository @Inject constructor(
    private val essayDao: EssayDao,
    private val passDao: PassDao,
    private val settings: SettingsRepository
) {

    /** Minutes earned by essays on the current budget day. */
    val bonusMinutesToday: Flow<Int> =
        settings.resetHour.flatMapLatest { resetHour ->
            passDao.observeMinutesGranted(UsageStatsProvider.budgetDayKey(resetHour))
        }

    val recentEssays: Flow<List<Essay>> = essayDao.observeRecent()

    val essayCount: Flow<Int> = essayDao.observeCount()

    fun essay(id: Long): Flow<Essay?> = essayDao.observeById(id)

    /**
     * Records the essay and grants the pass, as one operation.
     *
     * Returns the minutes granted so the caller can tell the user what they
     * just bought.
     */
    suspend fun issuePassForEssay(
        word: String,
        body: String,
        wordCount: Int,
        requiredWords: Int,
        durationSeconds: Int,
        cadenceSuspicious: Boolean,
        /**
         * Which app the minutes are for, in per-app mode. Ignored in shared
         * mode, where every grant goes to the one pool.
         */
        targetPackage: String? = null
    ): Int {
        val resetHour = settings.resetHour.first()
        val dayKey = UsageStatsProvider.budgetDayKey(resetHour)
        val minutes = settings.passMinutes.first()
        val mode = settings.budgetMode.first()

        val essayId = essayDao.insert(
            Essay(
                word = word,
                body = body,
                wordCount = wordCount,
                requiredWords = requiredWords,
                durationSeconds = durationSeconds,
                cadenceSuspicious = cadenceSuspicious
            )
        )

        passDao.insert(
            PassGrant(
                dayKey = dayKey,
                minutesGranted = minutes,
                essayId = essayId,
                // Null in shared mode: one pool, no target. In per-app mode
                // an untargeted grant would top up every app at once, which
                // is exactly what per-app limits exist to prevent.
                packageName = if (mode == BudgetMode.PER_APP) targetPackage else null
            )
        )

        return minutes
    }

    val panicUnlocksLeft: Flow<Int> = settings.panicUnlocksLeft

    /**
     * The emergency exit (app_plan.md §2.6).
     *
     * Instant, no essay, no questions, no "are you sure?". Three a month.
     *
     * This exists because a wellbeing app that traps someone during a real
     * emergency is a bad app, and no amount of good intentions about
     * friction changes that. The count is there so it doesn't quietly become
     * the normal way in — not to make anyone justify using one.
     *
     * @return minutes granted, or null if none were left.
     */
    suspend fun usePanicUnlock(targetPackage: String? = null): Int? {
        if (!settings.consumePanicUnlock()) return null

        val resetHour = settings.resetHour.first()
        val dayKey = UsageStatsProvider.budgetDayKey(resetHour)
        val mode = settings.budgetMode.first()

        passDao.insert(
            PassGrant(
                dayKey = dayKey,
                minutesGranted = SettingsRepository.Defaults.PANIC_MINUTES,
                // No essay bought this one.
                essayId = null,
                packageName = if (mode == BudgetMode.PER_APP) targetPackage else null
            )
        )
        return SettingsRepository.Defaults.PANIC_MINUTES
    }

    /** How many passes have been bought today — shown without judgement. */
    suspend fun passesToday(): Int {
        val resetHour = settings.resetHour.first()
        return passDao.countForDay(UsageStatsProvider.budgetDayKey(resetHour))
    }
}
