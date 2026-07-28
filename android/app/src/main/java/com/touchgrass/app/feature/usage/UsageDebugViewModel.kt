package com.touchgrass.app.feature.usage

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.usage.BudgetState
import com.touchgrass.app.core.usage.UsageMonitorService
import com.touchgrass.app.core.usage.UsagePermission
import com.touchgrass.app.core.usage.UsageRepository
import com.touchgrass.app.core.usage.UsageStatsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class InstalledApp(
    val packageName: String,
    val label: String,
    val watched: Boolean
)

/**
 * Drives the Phase 2 debug screen.
 *
 * This screen is scaffolding — the real Pass status screen and the watched-app
 * picker arrive in Phase 5's onboarding. Its job right now is to let us
 * *watch the numbers move* on a real device, which is the only way to know
 * the monitor actually works.
 */
@HiltViewModel
class UsageDebugViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageRepository: UsageRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _permissionGranted = MutableStateFlow(UsagePermission.isGranted(context))
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _liveForeground = MutableStateFlow<String?>(null)
    val liveForeground: StateFlow<String?> = _liveForeground.asStateFlow()

    val budgetState: StateFlow<BudgetState> =
        combine(
            usageRepository.budgetState,
            settings.dailyBudgetMinutes
        ) { state, budget ->
            state.copy(budgetMinutes = budget)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetState()
        )

    init {
        refreshPermission()
        loadInstalledApps()
    }

    fun refreshPermission() {
        _permissionGranted.value = UsagePermission.isGranted(context)
    }

    /**
     * Lists launchable apps. Uses the manifest `<queries>` declaration rather
     * than QUERY_ALL_PACKAGES, which Play treats as sensitive.
     */
    private fun loadInstalledApps() = viewModelScope.launch {
        val apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launchable = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { info ->
                    // Skip our own app and anything with no launcher entry.
                    info.packageName != context.packageName &&
                        pm.getLaunchIntentForPackage(info.packageName) != null
                }
                .map { info: ApplicationInfo ->
                    InstalledApp(
                        packageName = info.packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        watched = false
                    )
                }
                .sortedBy { it.label.lowercase() }
            launchable
        }
        _installedApps.value = apps

        // Keep the watched flags in sync with settings.
        viewModelScope.launch {
            settings.watchedPackages.collect { watched ->
                _installedApps.value = _installedApps.value.map {
                    it.copy(watched = it.packageName in watched)
                }
            }
        }
    }

    fun toggleWatched(packageName: String) = viewModelScope.launch {
        settings.toggleWatchedPackage(packageName)
    }

    fun setBudget(minutes: Int) = viewModelScope.launch {
        settings.setDailyBudgetMinutes(minutes)
    }

    fun startMonitor() {
        UsageMonitorService.start(context)
        refreshPermission()
    }

    fun stopMonitor() {
        UsageMonitorService.stop(context)
        viewModelScope.launch { settings.setMonitorEnabled(false) }
    }

    fun resetToday() = viewModelScope.launch {
        usageRepository.resetToday()
    }

    /** Manual poll, so the screen can be checked without waiting for the service. */
    fun pollNow() = viewModelScope.launch {
        val resetHour = settings.resetHour.first()
        val watched = settings.watchedPackages.first()
        if (watched.isNotEmpty()) {
            usageRepository.refreshUsage(resetHour, watched)
        }
        _liveForeground.value = usageRepository.currentForegroundPackage()
    }

    fun currentDayKey(resetHour: Int): String = UsageStatsProvider.budgetDayKey(resetHour)
}
