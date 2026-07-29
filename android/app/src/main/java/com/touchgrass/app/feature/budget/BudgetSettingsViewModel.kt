package com.touchgrass.app.feature.budget

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchgrass.app.core.data.settings.BudgetMode
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.usage.UsageStatsProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WatchedAppBudget(
    val packageName: String,
    val label: String,
    val minutes: Int,
    val explicit: Boolean
)

data class BudgetSettingsState(
    val mode: BudgetMode = BudgetMode.SHARED,
    val dailyBudget: Int = SettingsRepository.Defaults.DAILY_BUDGET_MINUTES,
    val pendingBudget: Int? = null,
    val passMinutes: Int = SettingsRepository.Defaults.PASS_MINUTES,
    val essayWords: Int = SettingsRepository.Defaults.ESSAY_WORDS,
    val resetHour: Int = SettingsRepository.Defaults.RESET_HOUR,
    val apps: List<WatchedAppBudget> = emptyList()
)

@HiltViewModel
class BudgetSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) : ViewModel() {

    val state: StateFlow<BudgetSettingsState> =
        combine(
            combine(
                settings.budgetMode,
                settings.dailyBudgetMinutes,
                settings.pendingBudget
            ) { mode, daily, pending -> Triple(mode, daily, pending) },
            combine(
                settings.watchedPackages,
                settings.perAppBudgets
            ) { watched, perApp -> watched to perApp },
            combine(
                settings.passMinutes,
                settings.essayWords,
                settings.resetHour
            ) { pass, words, hour -> Triple(pass, words, hour) }
        ) { budget, watchedPair, toll ->
            val (mode, daily, pending) = budget
            val (watched, perApp) = watchedPair
            val (pass, words, hour) = toll
            BudgetSettingsState(
                mode = mode,
                dailyBudget = daily,
                pendingBudget = pending?.first,
                passMinutes = pass,
                essayWords = words,
                resetHour = hour,
                apps = watched.sorted().map { pkg ->
                    WatchedAppBudget(
                        packageName = pkg,
                        label = labelFor(pkg),
                        minutes = perApp[pkg] ?: daily,
                        explicit = perApp.containsKey(pkg)
                    )
                }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetSettingsState()
        )

    private fun labelFor(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    fun setMode(mode: BudgetMode) = viewModelScope.launch {
        settings.setBudgetMode(mode)
    }

    fun setDailyBudget(minutes: Int) = viewModelScope.launch {
        val todayKey = UsageStatsProvider.budgetDayKey(settings.resetHour.first())
        settings.setDailyBudgetMinutes(minutes, todayKey)
    }

    fun adjustDailyBudget(delta: Int) = viewModelScope.launch {
        val todayKey = UsageStatsProvider.budgetDayKey(settings.resetHour.first())
        settings.setDailyBudgetMinutes(state.value.dailyBudget + delta, todayKey)
    }

    fun setAppBudget(packageName: String, minutes: Int) = viewModelScope.launch {
        settings.setPerAppBudget(packageName, minutes)
    }

    fun adjustAppBudget(packageName: String, delta: Int) = viewModelScope.launch {
        val current = state.value.apps.firstOrNull { it.packageName == packageName }?.minutes
            ?: state.value.dailyBudget
        settings.setPerAppBudget(packageName, current + delta)
    }

    fun setPassMinutes(minutes: Int) = viewModelScope.launch {
        settings.setPassMinutes(minutes)
    }

    fun adjustPassMinutes(delta: Int) = viewModelScope.launch {
        settings.setPassMinutes(state.value.passMinutes + delta)
    }

    fun adjustEssayWords(delta: Int) = viewModelScope.launch {
        settings.setEssayWords(state.value.essayWords + delta)
    }

    fun setResetHour(hour: Int) = viewModelScope.launch {
        settings.setResetHour(hour)
    }
}
