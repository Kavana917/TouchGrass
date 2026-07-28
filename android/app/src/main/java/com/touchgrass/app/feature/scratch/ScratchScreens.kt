package com.touchgrass.app.feature.scratch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Phase 0 proof screen. Shows values from Room and DataStore, and navigates
 * to a second screen. Replaced entirely in Phase 1 by the component gallery.
 */
@Composable
fun ScratchHomeScreen(
    onNavigateToSecond: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScratchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("TouchGrass", style = MaterialTheme.typography.headlineMedium)
        Text("Phase 0 — plumbing check", style = MaterialTheme.typography.bodyMedium)

        Text("Room notes stored: ${state.noteCount}")
        Text("DataStore flag: ${state.setupComplete}")

        Button(onClick = { viewModel.addNote() }) { Text("Add a note (Room)") }
        OutlinedButton(onClick = { viewModel.clearNotes() }) { Text("Clear notes") }
        Button(onClick = { viewModel.toggleSetupComplete() }) { Text("Toggle flag (DataStore)") }

        Text(
            "Kill the app and reopen — both values should survive.",
            style = MaterialTheme.typography.bodySmall
        )

        Button(onClick = onNavigateToSecond) { Text("Go to second screen →") }
    }
}

@Composable
fun ScratchSecondScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Second screen", style = MaterialTheme.typography.headlineMedium)
        Text("Navigation works.", style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onNavigateBack) { Text("← Back") }
    }
}
