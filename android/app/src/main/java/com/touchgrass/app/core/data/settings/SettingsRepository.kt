package com.touchgrass.app.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "touchgrass_settings")

/**
 * How the daily allowance is divided (app_plan.md §2.6).
 *
 * [SHARED] is the default on purpose: with one pool, hopping Instagram →
 * TikTok → Reddit gives you the same total time, not three times as much.
 * That property is the whole reason the shared budget exists.
 *
 * [PER_APP] is the advanced option for people who genuinely want different
 * limits per app — 20 minutes of Instagram but an hour of YouTube. It gives
 * up the anti-hopping guarantee in exchange for precision, which is a real
 * trade and should be a deliberate choice.
 */
enum class BudgetMode {
    SHARED,
    PER_APP;

    companion object {
        fun fromName(name: String?): BudgetMode =
            entries.firstOrNull { it.name == name } ?: SHARED
    }
}

/**
 * User preferences.
 *
 * Rule of thumb: DataStore for *preferences* (a budget, a toggle, a chosen
 * hour); Room for *records* (usage days, essays, drawings). See
 * tech_stack.md §7.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val WATCHED_PACKAGES = stringSetPreferencesKey("watched_packages")
        val DAILY_BUDGET_MINUTES = intPreferencesKey("daily_budget_minutes")
        val RESET_HOUR = intPreferencesKey("reset_hour")
        val MONITOR_ENABLED = booleanPreferencesKey("monitor_enabled")
        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val ESSAY_WORDS = intPreferencesKey("essay_words")
        val PASS_MINUTES = intPreferencesKey("pass_minutes")
        val USES_DICTATION = booleanPreferencesKey("uses_dictation")
        val BUDGET_MODE = stringPreferencesKey("budget_mode")
        val PER_APP_BUDGETS = stringPreferencesKey("per_app_budgets")
        val PENDING_BUDGET = intPreferencesKey("pending_budget")
        val PENDING_BUDGET_FROM_DAY = stringPreferencesKey("pending_budget_from_day")
        val PANIC_MONTH = stringPreferencesKey("panic_month")
        val PANIC_USED = intPreferencesKey("panic_used")
        val LAST_SEEN_AT = longPreferencesKey("last_seen_at")
        val LAST_MONITOR_HEARTBEAT = longPreferencesKey("last_monitor_heartbeat")
        val NOTIFICATION_GRACE_ENABLED = booleanPreferencesKey("notification_grace_enabled")
    }

    object Defaults {
        /**
         * 30 minutes, from app_plan.md §2.4 — but flagged there as an
         * unvalidated guess (§6.6, risk 7). Revisit in Phase 5 once the app
         * is dogfoodable: too generous and the wall never appears, too tight
         * and it gets uninstalled on day one.
         */
        const val DAILY_BUDGET_MINUTES = 30

        /**
         * 4am, not midnight. Deliberate — someone scrolling at 11:58pm
         * should not be handed a fresh budget two minutes later.
         */
        const val RESET_HOUR = 4

        /** Words required per essay (app_plan.md §2.4). Floor of 50. */
        const val ESSAY_WORDS = 150

        /**
         * Minutes bought by one essay.
         *
         * Deliberately LESS than the free daily budget — a pass should feel
         * like a top-up, not a reset. Otherwise the essay becomes a way to
         * start the day over.
         */
        const val PASS_MINUTES = 15

        /**
         * Panic unlocks per month (app_plan.md §2.6).
         *
         * Instant, no essay, no questions asked. A wellbeing app that traps
         * someone in a real emergency is a bad app, and the friction has to
         * have a documented way out that costs nothing in the moment.
         */
        const val PANIC_UNLOCKS_PER_MONTH = 3

        /** Minutes a panic unlock grants. */
        const val PANIC_MINUTES = 15
    }

    /** Packages whose foreground time counts against the budget. */
    val watchedPackages: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.WATCHED_PACKAGES] ?: emptySet() }

    val dailyBudgetMinutes: Flow<Int> =
        context.dataStore.data.map { it[Keys.DAILY_BUDGET_MINUTES] ?: Defaults.DAILY_BUDGET_MINUTES }

    val resetHour: Flow<Int> =
        context.dataStore.data.map { it[Keys.RESET_HOUR] ?: Defaults.RESET_HOUR }

    val monitorEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.MONITOR_ENABLED] ?: false }

    val setupComplete: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.SETUP_COMPLETE] ?: false }

    val essayWords: Flow<Int> =
        context.dataStore.data.map { it[Keys.ESSAY_WORDS] ?: Defaults.ESSAY_WORDS }

    val passMinutes: Flow<Int> =
        context.dataStore.data.map { it[Keys.PASS_MINUTES] ?: Defaults.PASS_MINUTES }

    /**
     * Accessibility opt-out (app_plan.md §2.5).
     *
     * Blocking dictation harms people who rely on it. Rather than locking
     * them out to catch a cheater who has easier options anyway, users who
     * dictate get a longer word requirement instead.
     */
    val usesDictation: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.USES_DICTATION] ?: false }

    val budgetMode: Flow<BudgetMode> =
        context.dataStore.data.map { BudgetMode.fromName(it[Keys.BUDGET_MODE]) }

    /**
     * Per-app limits, package → minutes. Only consulted in [BudgetMode.PER_APP].
     *
     * Stored as JSON in one preference rather than a key per package: the set
     * of watched apps changes, and DataStore has no way to enumerate or
     * remove keys by prefix.
     */
    val perAppBudgets: Flow<Map<String, Int>> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.PER_APP_BUDGETS].toMinutesMap()
        }

    suspend fun setWatchedPackages(packages: Set<String>) {
        context.dataStore.edit { it[Keys.WATCHED_PACKAGES] = packages }
    }

    suspend fun toggleWatchedPackage(pkg: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.WATCHED_PACKAGES] ?: emptySet()
            prefs[Keys.WATCHED_PACKAGES] =
                if (pkg in current) current - pkg else current + pkg
        }
    }

    /** A raise waiting for the next reset, or null. */
    val pendingBudget: Flow<Pair<Int, String>?> =
        context.dataStore.data.map { prefs ->
            val minutes = prefs[Keys.PENDING_BUDGET]
            val fromDay = prefs[Keys.PENDING_BUDGET_FROM_DAY]
            if (minutes != null && fromDay != null) minutes to fromDay else null
        }

    /**
     * Change the daily budget, with an asymmetry that matters
     * (app_plan.md §2.6):
     *
     *  - LOWERING applies immediately. Deciding you want less is a decision
     *    the app should never stand in the way of.
     *  - RAISING waits until the next reset. Otherwise the budget itself is
     *    the escape hatch — hit the wall, bump the number, carry on, and the
     *    whole mechanism is decorative.
     *
     * @param todayKey the current budget day, so we know which day the raise
     *   should start from.
     */
    suspend fun setDailyBudgetMinutes(minutes: Int, todayKey: String? = null) {
        val clamped = minutes.coerceIn(1, 24 * 60)
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.DAILY_BUDGET_MINUTES] ?: Defaults.DAILY_BUDGET_MINUTES
            if (clamped <= current || todayKey == null) {
                prefs[Keys.DAILY_BUDGET_MINUTES] = clamped
                prefs.remove(Keys.PENDING_BUDGET)
                prefs.remove(Keys.PENDING_BUDGET_FROM_DAY)
            } else {
                prefs[Keys.PENDING_BUDGET] = clamped
                prefs[Keys.PENDING_BUDGET_FROM_DAY] = todayKey
            }
        }
    }

    /**
     * Applies a pending raise once the budget day has actually turned over.
     * Called from the monitor's poll loop.
     */
    suspend fun promotePendingBudget(todayKey: String) {
        context.dataStore.edit { prefs ->
            val pending = prefs[Keys.PENDING_BUDGET] ?: return@edit
            val fromDay = prefs[Keys.PENDING_BUDGET_FROM_DAY] ?: return@edit
            if (todayKey > fromDay) {
                prefs[Keys.DAILY_BUDGET_MINUTES] = pending
                prefs.remove(Keys.PENDING_BUDGET)
                prefs.remove(Keys.PENDING_BUDGET_FROM_DAY)
            }
        }
    }

    suspend fun setResetHour(hour: Int) {
        context.dataStore.edit { it[Keys.RESET_HOUR] = hour.coerceIn(0, 23) }
    }

    suspend fun setMonitorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONITOR_ENABLED] = enabled }
    }

    suspend fun setSetupComplete(value: Boolean) {
        context.dataStore.edit { it[Keys.SETUP_COMPLETE] = value }
    }

    suspend fun setEssayWords(words: Int) {
        context.dataStore.edit { it[Keys.ESSAY_WORDS] = words.coerceIn(50, 1000) }
    }

    suspend fun setPassMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.PASS_MINUTES] = minutes.coerceIn(1, 120) }
    }

    suspend fun setUsesDictation(value: Boolean) {
        context.dataStore.edit { it[Keys.USES_DICTATION] = value }
    }

    suspend fun setBudgetMode(mode: BudgetMode) {
        context.dataStore.edit { it[Keys.BUDGET_MODE] = mode.name }
    }

    suspend fun setPerAppBudget(packageName: String, minutes: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.PER_APP_BUDGETS].toMinutesMap().toMutableMap()
            current[packageName] = minutes.coerceIn(1, 24 * 60)
            prefs[Keys.PER_APP_BUDGETS] = current.toJson()
        }
    }

    /**
     * Panic unlocks left this month.
     *
     * Shown in settings without judgement — no "you've used 2 of 3 already".
     * The count exists to stop it becoming a routine bypass, not to make
     * anyone feel watched for using one.
     */
    val panicUnlocksLeft: Flow<Int> =
        context.dataStore.data.map { prefs ->
            val storedMonth = prefs[Keys.PANIC_MONTH]
            val used = prefs[Keys.PANIC_USED] ?: 0
            if (storedMonth != currentMonthKey()) {
                Defaults.PANIC_UNLOCKS_PER_MONTH
            } else {
                (Defaults.PANIC_UNLOCKS_PER_MONTH - used).coerceAtLeast(0)
            }
        }

    /** Returns true if an unlock was available and has now been consumed. */
    suspend fun consumePanicUnlock(): Boolean {
        var granted = false
        context.dataStore.edit { prefs ->
            val month = currentMonthKey()
            val used = if (prefs[Keys.PANIC_MONTH] == month) {
                prefs[Keys.PANIC_USED] ?: 0
            } else {
                0
            }
            if (used < Defaults.PANIC_UNLOCKS_PER_MONTH) {
                prefs[Keys.PANIC_MONTH] = month
                prefs[Keys.PANIC_USED] = used + 1
                granted = true
            }
        }
        return granted
    }

    /**
     * When the app was last opened. Used to notice monitoring gaps —
     * see MonitorHealth.
     */
    val lastSeenAt: Flow<Long> =
        context.dataStore.data.map { it[Keys.LAST_SEEN_AT] ?: 0L }

    suspend fun markSeen() {
        context.dataStore.edit { it[Keys.LAST_SEEN_AT] = System.currentTimeMillis() }
    }

    /** Updated on every successful monitor poll — used for gap detection. */
    val lastMonitorHeartbeatAt: Flow<Long> =
        context.dataStore.data.map { it[Keys.LAST_MONITOR_HEARTBEAT] ?: 0L }

    suspend fun recordMonitorHeartbeat() {
        context.dataStore.edit {
            it[Keys.LAST_MONITOR_HEARTBEAT] = System.currentTimeMillis()
        }
    }

    /**
     * When enabled, resuming a watched app with a spent budget gets 60 seconds
     * before the wall appears (app_plan.md §2.6) — enough to read a DM.
     */
    val notificationGraceEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFICATION_GRACE_ENABLED] ?: true }

    suspend fun setNotificationGraceEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_GRACE_ENABLED] = value }
    }

    suspend fun clearPerAppBudget(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.PER_APP_BUDGETS].toMinutesMap().toMutableMap()
            current.remove(packageName)
            prefs[Keys.PER_APP_BUDGETS] = current.toJson()
        }
    }
}

/** `2026-07`, for scoping the monthly panic-unlock allowance. */
private fun currentMonthKey(): String {
    val calendar = java.util.Calendar.getInstance()
    return "%04d-%02d".format(
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH) + 1
    )
}

private fun String?.toMinutesMap(): Map<String, Int> {
    if (this.isNullOrBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(this)
        json.keys().asSequence().associateWith { json.optInt(it, 0) }
            .filterValues { it > 0 }
    }.getOrDefault(emptyMap())
}

private fun Map<String, Int>.toJson(): String {
    val json = JSONObject()
    forEach { (key, value) -> json.put(key, value) }
    return json.toString()
}
