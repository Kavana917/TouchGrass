package com.touchgrass.app.core.usage

import android.content.Context
import com.touchgrass.app.core.data.db.PassDao
import com.touchgrass.app.core.data.db.UsageDao
import com.touchgrass.app.core.data.db.UsageDay
import com.touchgrass.app.core.data.settings.BudgetMode
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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

    private data class Config(
        val resetHour: Int,
        val budget: Int,
        val watched: Set<String>,
        val monitorEnabled: Boolean,
        val mode: BudgetMode,
        val perAppBudgets: Map<String, Int>
    )

    private val config: Flow<Config> = combine(
        settings.resetHour,
        settings.dailyBudgetMinutes,
        settings.watchedPackages,
        settings.monitorEnabled,
        combine(settings.budgetMode, settings.perAppBudgets) { mode, limits -> mode to limits }
    ) { resetHour, budget, watched, monitorEnabled, (mode, limits) ->
        Config(resetHour, budget, watched, monitorEnabled, mode, limits)
    }

    /**
     * Live budget state for the UI.
     *
     * Reads the *persisted* usage total, so it stays correct even when the
     * monitor service isn't running — it just stops updating.
     */
    val budgetState: Flow<BudgetState> =
        config.flatMapLatest { cfg ->
            val dayKey = UsageStatsProvider.budgetDayKey(cfg.resetHour)
            combine(
                usageDao.observeDay(dayKey),
                passDao.observeSharedMinutesGranted(dayKey),
                passDao.observePerAppGrants(dayKey)
            ) { day, sharedBonus, perAppGrants ->
                val usedPerApp = day?.perAppJson.toPerAppMap()
                val grantsByPackage = perAppGrants.associate { it.pkg to it.minutes }

                BudgetState(
                    dayKey = dayKey,
                    mode = cfg.mode,
                    budgetMinutes = cfg.budget,
                    bonusMinutes = sharedBonus,
                    usedMinutes = day?.minutesUsed ?: 0,
                    perApp = usedPerApp,
                    appBudgets = cfg.watched.map { pkg ->
                        AppBudget(
                            packageName = pkg,
                            // Falls back to the shared daily budget for any
                            // app with no explicit limit set yet, so turning
                            // per-app mode on never leaves an app at zero.
                            budgetMinutes = cfg.perAppBudgets[pkg] ?: cfg.budget,
                            bonusMinutes = grantsByPackage[pkg] ?: 0,
                            usedMinutes = usedPerApp[pkg] ?: 0
                        )
                    }.sortedBy { it.packageName },
                    permissionGranted = UsagePermission.isGranted(context),
                    monitorRunning = cfg.monitorEnabled
                )
            }
        }

    val watchedPackages: Flow<Set<String>> = settings.watchedPackages

    /** What's on screen right now, for the debug readout. */
    fun currentForegroundPackage(): String? = provider.currentForegroundPackage()

    /**
     * Recomputes today's usage from the OS event log and persists it.
     * Called by the monitor service on every poll.
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

    /** A snapshot of budget state, for the service's polling loop. */
    suspend fun snapshot(): BudgetState = budgetState.first()

    /**
     * Debug-only: zeroes today's stored total.
     *
     * Cosmetic — the next poll recomputes from the OS event log and the real
     * number comes straight back. That's the self-correcting design working
     * as intended, and it's also why "just clear the data" isn't a way to
     * cheat the budget.
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
