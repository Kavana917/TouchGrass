package com.touchgrass.app.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "touchgrass_settings")

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
    }

    object Defaults {
        /**
         * 30 minutes, from app_plan.md §2.4 — but flagged there as an
         * unvalidated guess (§6.6, risk 9). Revisit in Phase 6 once the app
         * is dogfoodable: too generous and the wall never appears, too tight
         * and it gets uninstalled on day one.
         */
        const val DAILY_BUDGET_MINUTES = 30

        /**
         * 4am, not midnight. Deliberate — someone scrolling at 11:58pm
         * should not be handed a fresh budget two minutes later.
         */
        const val RESET_HOUR = 4
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

    /**
     * Lowering the budget applies immediately; raising it should wait until
     * the next reset (app_plan.md §2.6) so it can't be used as an escape
     * hatch mid-craving. That deferral lands in Phase 5 with the rest of the
     * settings UI — for now this is a straight write, used only by the debug
     * screen.
     */
    suspend fun setDailyBudgetMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DAILY_BUDGET_MINUTES] = minutes.coerceIn(1, 24 * 60) }
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
}
