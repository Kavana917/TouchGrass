package com.touchgrass.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.ui.navigation.TouchGrassNavHost
import com.touchgrass.app.ui.theme.TouchGrassTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The app's single Activity. Everything else is a Composable inside it —
 * see tech_stack.md §3.1 (single-Activity architecture).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        renderFrom(intent)
    }

    override fun onResume() {
        super.onResume()
        // Timestamps each visit so gaps in monitoring can be detected.
        lifecycleScope.launch { settings.markSeen() }
    }

    /**
     * The wall launches us with extras rather than through normal
     * navigation, so a fresh Intent has to re-render.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderFrom(intent)
    }

    private fun renderFrom(intent: Intent?) {
        val openEssay = intent?.getBooleanExtra(EXTRA_OPEN_ESSAY, false) ?: false
        val returnTo = intent?.getStringExtra(EXTRA_RETURN_TO_PACKAGE)

        setContent {
            TouchGrassApp(
                startOnEssay = openEssay,
                returnToPackage = returnTo,
                setupComplete = settings.setupComplete,
                onReturnToApp = { pkg -> launchPackage(pkg) }
            )
        }
    }

    /**
     * Sends the user back to the app the wall interrupted, once they've
     * paid the toll. Landing back on our own screen after writing 150 words
     * would be a small betrayal of what they just bought.
     */
    private fun launchPackage(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        }
        finish()
    }

    companion object {
        const val EXTRA_OPEN_ESSAY = "open_essay"
        const val EXTRA_RETURN_TO_PACKAGE = "return_to_package"
    }
}

@Composable
private fun TouchGrassApp(
    startOnEssay: Boolean,
    returnToPackage: String?,
    setupComplete: Flow<Boolean>,
    onReturnToApp: (String) -> Unit
) {
    // Null until DataStore answers — we must not flash the main screen at a
    // first-run user before deciding they need onboarding.
    val complete by setupComplete.collectAsStateWithLifecycle(initialValue = null)

    val systemNight = isSystemInDarkTheme()
    var isNight by remember { mutableStateOf(systemNight) }

    TouchGrassTheme(isNight = isNight) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            complete?.let { done ->
                TouchGrassNavHost(
                    isNight = isNight,
                    onToggleNight = { isNight = !isNight },
                    startOnEssay = startOnEssay,
                    setupComplete = done,
                    returnToPackage = returnToPackage,
                    onReturnToApp = onReturnToApp,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
