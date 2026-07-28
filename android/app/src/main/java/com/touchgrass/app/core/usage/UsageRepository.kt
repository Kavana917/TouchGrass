package com.touchgrass.app.core.usage

import android.content.Context
import com.touchgrass.app.core.data.db.PassDao
import com.touchgrass.app.core.data.db.UsageDao
import com.touchgrass.app.core.data.db.UsageDay
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * The single source of truth for "how much time is left today".
 *
 * Combines what the OS reports (via [UsageStatsProvider]), what the user
 * chose (via [SettingsRepository]) and what we persisted (via [UsageDao])
 * into one observable [BudgetState].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class UsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageDao: UsageDao,
    private val passDao: PassDao,
    private val provider: UsageStatsProvider,
    private val settings: SettingsRepository
) {

    /**
     * Live budget state for the UI.
     *
     * Note this reads the *persisted* total, so it stays correct even when
     * the monitor service isn't running — it just stops updating.
     */
    val budgetState: Flow<BudgetState> =
        combine(
            settings.resetHour,
            settings.dailyBudgetMinutes,
            settings.watchedPackages,
            settings.monitorEnabled
        ) { resetHour, budget, watched, monitorEnabled ->
            Quad(resetHour, budget, watched, monitorEnabled)
        }.flatMapLatest { (resetHour, budget, watched, monitorEnabled) ->
            val dayKey = UsageStatsProvider.budgetDayKey(resetHour)
            combine(
                usageDao.observeDay(dayKey),
                passDao.observeMinutesGranted(dayKey)
            ) { day, bonusMinutes ->
                BudgetState(
                    dayKey = dayKey,
                    budgetMinutes = budget,
                    bonusMinutes = bonusMinutes,
                    usedMinutes = day?.minutesUsed ?: 0,
                    perApp = day?.perAppJson.toPerAppMap(),
                    permissionGranted = UsagePermission.isGranted(context),
                    monitorRunning = monitorEnabled,
                    watchedAppInForeground = false
                )
            }
        }

    val watchedPackages: Flow<Set<String>> = settings.watchedPackages

    /** What's on screen right now, for the debug readout. */
    fun currentForegroundPackage(): String? = provider.currentForegroundPackage()

    /** Minutes earned by essays on the given budget day. */
    suspend fun bonusMinutesFor(resetHour: Int): Int =
        passDao.observeMinutesGranted(UsageStatsProvider.budgetDayKey(resetHour)).first()

    /**
     * Recomputes today's usage from the OS event log and persists it.
     * Called by the monitor service on every poll.
     *
     * Returns the freshly computed minutes used, so the caller can decide
     * how urgently to poll next without a round trip through the database.
     */
    suspend fun refreshUsage(resetHour: Int, watched: Set<String>): Int {
        val dayStart = UsageStatsProvider.budgetDayStart(resetHour)
        val dayKey = UsageStatsProvider.budgetDayKey(resetHour)

        val perAppMillis = provider.foregroundMillisSince(watched, dayStart)
        val perAppMinutes = perAppMillis.mapValues { (_, millis) ->
            (millis / 60_000.0).roundToInt()
        }
        val totalMinutes = (perAppMillis.values.sum() / 60_000.0).roundToInt()

        usageDao.upsert(
            UsageDay(
                date = dayKey,
                minutesUsed = totalMinutes,
                perAppJson = perAppMinutes.toJson()
            )
        )
        return totalMinutes
    }

    /**
     * Debug-only: zeroes today's stored total.
     *
     * Note this is cosmetic — the next poll recomputes from the OS event log
     * and the real number comes straight back. That's the self-correcting
     * design working as intended, and it's also why "just clear the data"
     * isn't a way to cheat the budget.
     */
    suspend fun resetToday() {
        val resetHour = settings.resetHour.first()
        usageDao.upsert(
            UsageDay(
                date = UsageStatsProvider.budgetDayKey(resetHour),
                minutesUsed = 0,
                perAppJson = "{}"
            )
        )
    }
}

/** Small helper so `combine` can carry four values without nesting Pairs. */
private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

private fun String?.toPerAppMap(): Map<String, Int> {
    if (this.isNullOrBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        json.keys().asSequence().associateWith { key -> json.optInt(key, 0) }
    }.getOrDefault(emptyMap())
}

private fun Map<String, Int>.toJson(): String {
    val json = JSONObject()
    forEach { (key, value) -> json.put(key, value) }
    return json.toString()
}
