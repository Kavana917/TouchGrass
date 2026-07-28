package com.touchgrass.app.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Creates a single DataStore instance tied to the app context.
// Declared at file scope because DataStore must be a singleton per file name.
private val Context.dataStore by preferencesDataStore(name = "touchgrass_settings")

/**
 * Small key–value settings, distinct from Room.
 *
 * Rule of thumb: DataStore for *preferences* (a budget, a toggle, a chosen hour);
 * Room for *records* (essays, passes, drawings). See tech_stack.md §7.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
    }

    /** Placeholder flag used in Phase 0 to prove settings survive a restart. */
    val setupComplete: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.SETUP_COMPLETE] ?: false }

    suspend fun setSetupComplete(value: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SETUP_COMPLETE] = value }
    }
}
