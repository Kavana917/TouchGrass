package com.touchgrass.app.feature.onboarding

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.permissions.AppPermissions
import com.touchgrass.app.core.permissions.PermissionId
import com.touchgrass.app.core.permissions.PermissionInfo
import com.touchgrass.app.core.usage.UsageMonitorService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * One decision per pane (app_plan.md §6.2). Requesting five permissions on
 * one screen reads as malware; this app's set already looks alarming enough
 * without help.
 */
enum class OnboardingPane {
    IDEA,
    PICK_APPS,
    SET_BUDGET,
    PERM_USAGE,
    PERM_OVERLAY,
    PERM_NOTIFICATIONS,
    PERM_BATTERY,
    PRIVACY,
    DONE;

    companion object {
        val COUNT = entries.size
    }
}

data class OnboardingApp(
    val packageName: String,
    val label: String,
    val selected: Boolean
)

data class OnboardingState(
    val pane: OnboardingPane = OnboardingPane.IDEA,
    val apps: List<OnboardingApp> = emptyList(),
    val loadingApps: Boolean = true,
    val budgetMinutes: Int = SettingsRepository.Defaults.DAILY_BUDGET_MINUTES,
    val permissions: List<PermissionInfo> = emptyList()
) {
    val selectedCount: Int get() = apps.count { it.selected }

    /** Can't police apps you haven't named. */
    val canLeaveAppPicker: Boolean get() = selectedCount > 0

    val requiredPermissionsGranted: Boolean
        get() = permissions.filter { it.required }.all { it.granted }

    fun permissionFor(pane: OnboardingPane) = when (pane) {
        OnboardingPane.PERM_USAGE -> PermissionId.USAGE_ACCESS
        OnboardingPane.PERM_OVERLAY -> PermissionId.DRAW_OVER_APPS
        OnboardingPane.PERM_NOTIFICATIONS -> PermissionId.NOTIFICATIONS
        OnboardingPane.PERM_BATTERY -> PermissionId.BATTERY_UNRESTRICTED
        else -> null
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        loadApps()
        refreshPermissions()
    }

    fun refreshPermissions() {
        _state.update { it.copy(permissions = AppPermissions.all(context)) }
    }

    private fun loadApps() = viewModelScope.launch {
        val installed = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    it.packageName != context.packageName &&
                        pm.getLaunchIntentForPackage(it.packageName) != null
                }
                .map { info ->
                    OnboardingApp(
                        packageName = info.packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        selected = false
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
        // Pre-tick anything already watched, so re-running setup isn't
        // destructive.
        val alreadyWatched = settings.watchedPackages.first()
        _state.update { current ->
            current.copy(
                apps = installed.map { it.copy(selected = it.packageName in alreadyWatched) },
                loadingApps = false
            )
        }
    }

    fun toggleApp(packageName: String) {
        _state.update { current ->
            current.copy(
                apps = current.apps.map {
                    if (it.packageName == packageName) it.copy(selected = !it.selected) else it
                }
            )
        }
    }

    fun setBudget(minutes: Int) {
        _state.update { it.copy(budgetMinutes = minutes.coerceIn(1, 24 * 60)) }
    }

    fun adjustBudget(delta: Int) {
        setBudget(_state.value.budgetMinutes + delta)
    }

    fun next() {
        val current = _state.value
        val nextPane = when (current.pane) {
            OnboardingPane.IDEA -> OnboardingPane.PICK_APPS
            OnboardingPane.PICK_APPS -> OnboardingPane.SET_BUDGET
            OnboardingPane.SET_BUDGET -> OnboardingPane.PERM_USAGE
            OnboardingPane.PERM_USAGE -> OnboardingPane.PERM_OVERLAY
            OnboardingPane.PERM_OVERLAY -> OnboardingPane.PERM_NOTIFICATIONS
            OnboardingPane.PERM_NOTIFICATIONS -> OnboardingPane.PERM_BATTERY
            OnboardingPane.PERM_BATTERY -> OnboardingPane.PRIVACY
            OnboardingPane.PRIVACY -> OnboardingPane.DONE
            OnboardingPane.DONE -> OnboardingPane.DONE
        }

        // Persist as we go, so abandoning setup halfway doesn't lose the
        // choices already made.
        when (current.pane) {
            OnboardingPane.PICK_APPS -> persistApps()
            OnboardingPane.SET_BUDGET -> persistBudget()
            else -> Unit
        }

        _state.update { it.copy(pane = nextPane) }
        if (nextPane.name.startsWith("PERM_")) refreshPermissions()
    }

    fun back() {
        _state.update { current ->
            current.copy(
                pane = when (current.pane) {
                    OnboardingPane.IDEA -> OnboardingPane.IDEA
                    OnboardingPane.PICK_APPS -> OnboardingPane.IDEA
                    OnboardingPane.SET_BUDGET -> OnboardingPane.PICK_APPS
                    OnboardingPane.PERM_USAGE -> OnboardingPane.SET_BUDGET
                    OnboardingPane.PERM_OVERLAY -> OnboardingPane.PERM_USAGE
                    OnboardingPane.PERM_NOTIFICATIONS -> OnboardingPane.PERM_OVERLAY
                    OnboardingPane.PERM_BATTERY -> OnboardingPane.PERM_NOTIFICATIONS
                    OnboardingPane.PRIVACY -> OnboardingPane.PERM_BATTERY
                    OnboardingPane.DONE -> OnboardingPane.PRIVACY
                }
            )
        }
    }

    private fun persistApps() = viewModelScope.launch {
        settings.setWatchedPackages(
            _state.value.apps.filter { it.selected }.map { it.packageName }.toSet()
        )
    }

    private fun persistBudget() = viewModelScope.launch {
        // No todayKey: during setup a "raise" isn't an escape hatch, it's
        // the initial choice, so it applies immediately.
        settings.setDailyBudgetMinutes(_state.value.budgetMinutes)
    }

    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        persistApps()
        persistBudget()
        settings.setSetupComplete(true)
        UsageMonitorService.start(context)
        onDone()
    }
}
