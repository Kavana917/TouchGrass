package com.touchgrass.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
        setContent {
            TouchGrassTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TouchGrassNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
