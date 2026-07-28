package com.touchgrass.app

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
import com.touchgrass.app.ui.navigation.TouchGrassNavHost
import com.touchgrass.app.ui.theme.TouchGrassTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The app's single Activity. Everything else is a Composable inside it —
 * see tech_stack.md §3.1 (single-Activity architecture).
 *
 * [AndroidEntryPoint] lets Hilt inject into this Activity and anything below it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { TouchGrassApp() }
    }
}

@Composable
private fun TouchGrassApp() {
    // Night state lives above the theme so the gallery can toggle it.
    // In Phase 5 this moves into DataStore as a user setting.
    val systemNight = isSystemInDarkTheme()
    var isNight by remember { mutableStateOf(systemNight) }

    TouchGrassTheme(isNight = isNight) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            TouchGrassNavHost(
                isNight = isNight,
                onToggleNight = { isNight = !isNight },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
