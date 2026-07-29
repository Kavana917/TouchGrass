package com.touchgrass.app.feature.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchgrass.app.core.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class WatchedAppRow(
    val packageName: String,
    val label: String,
    val watched: Boolean
)

@HiltViewModel
class WatchedAppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) : ViewModel() {

    private val installed = MutableStateFlow<List<WatchedAppRow>>(emptyList())

    val apps: StateFlow<List<WatchedAppRow>> =
        combine(installed, settings.watchedPackages) { list, watched ->
            list.map { it.copy(watched = it.packageName in watched) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val rows = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { info ->
                    info.packageName != context.packageName &&
                        pm.getLaunchIntentForPackage(info.packageName) != null
                }
                .map { info: ApplicationInfo ->
                    WatchedAppRow(
                        packageName = info.packageName,
                        label = pm.getApplicationLabel(info).toString(),
                        watched = false
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
        installed.value = rows
    }

    fun toggle(packageName: String) = viewModelScope.launch {
        settings.toggleWatchedPackage(packageName)
    }
}
