package com.touchgrass.app.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchgrass.app.core.data.settings.BudgetMode
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.pass.PassRepository
import com.touchgrass.app.core.permissions.AppPermissions
import com.touchgrass.app.core.usage.BudgetState
import com.touchgrass.app.core.usage.MonitorHealth
import com.touchgrass.app.core.usage.UsageMonitorService
import com.touchgrass.app.core.usage.UsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val budget: BudgetState = BudgetState(),
    val watchedCount: Int = 0,
    val permissionsReady: Boolean = false,
    val pendingBudget: Int? = null,
    val notificationGraceOn: Boolean = true,
    val healthIssue: MonitorHealth.Issue? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageRepository: UsageRepository,
    private val settings: SettingsRepository,
    private val passRepository: PassRepository,
    private val monitorHealth: MonitorHealth
) : ViewModel() {

    private val _healthIssue = MutableStateFlow<MonitorHealth.Issue?>(null)
    val healthIssue: StateFlow<MonitorHealth.Issue?> = _healthIssue.asStateFlow()

    val panicUnlocksLeft: StateFlow<Int> = passRepository.panicUnlocksLeft.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = 0
    )

    private val _panicGranted = MutableStateFlow<Int?>(null)
    val panicGranted: StateFlow<Int?> = _panicGranted.asStateFlow()

    val state: StateFlow<HomeUiState> = combine(
        usageRepository.budgetState,
        settings.watchedPackages,
        settings.pendingBudget,
        settings.notificationGraceEnabled,
        _healthIssue
    ) { budget, watched, pending, graceOn, health ->
        val perms = AppPermissions.all(context)
        HomeUiState(
            budget = budget,
            watchedCount = watched.size,
            permissionsReady = perms.filter { it.required }.all { it.granted },
            pendingBudget = pending?.first,
            notificationGraceOn = graceOn,
            healthIssue = health
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    init {
        ensureMonitorRunning()
        refreshHealth()
    }

    fun onResume() {
        refreshHealth()
        ensureMonitorRunning()
    }

    fun refreshHealth() = viewModelScope.launch {
        _healthIssue.value = monitorHealth.check()
    }

    fun ensureMonitorRunning() = viewModelScope.launch {
        val setupDone = settings.setupComplete.first()
        if (!setupDone) return@launch

        val perms = AppPermissions.all(context)
        if (!perms.filter { it.required }.all { it.granted }) return@launch

        UsageMonitorService.start(context)
    }

    fun restartMonitor() {
        UsageMonitorService.start(context)
        refreshHealth()
    }

    fun usePanicUnlock() = viewModelScope.launch {
        _panicGranted.value = passRepository.usePanicUnlock()
    }

    fun dismissPanicResult() {
        _panicGranted.value = null
    }

    fun setNotificationGraceEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setNotificationGraceEnabled(enabled)
    }
}
