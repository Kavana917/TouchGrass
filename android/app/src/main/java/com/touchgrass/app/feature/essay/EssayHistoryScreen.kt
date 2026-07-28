package com.touchgrass.app.feature.essay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.core.data.db.Essay
import com.touchgrass.app.core.pass.PassRepository
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroListRow
import com.touchgrass.app.ui.components.RetroListView
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EssayHistoryViewModel @Inject constructor(
    passRepository: PassRepository
) : ViewModel() {
    val essays: StateFlow<List<Essay>> = passRepository.recentEssays.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
}

/**
 * Past essays. Read-only, and deliberately unremarkable.
 *
 * NO SCORES, NO GRADES, NO STREAKS. The anti-goals in app_plan.md §1 rule
 * out shame metrics, and a list of essays ranked by quality or a "days in a
 * row" counter would be exactly that. This is a record that the work
 * happened, nothing more.
 */
@Composable
fun EssayHistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EssayHistoryViewModel = hiltViewModel()
) {
    val essays by viewModel.essays.collectAsStateWithLifecycle()
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            RetroWindow(
                title = "Essays",
                statusText = if (essays.isEmpty()) {
                    "0 items"
                } else {
                    "${essays.size} item${if (essays.size == 1) "" else "s"}"
                }
            ) {
                if (essays.isEmpty()) {
                    BodyText("No essays yet.")
                } else {
                    RetroListView(
                        headers = listOf("Word", "Words", "When"),
                        weights = listOf(2f, 1f, 2f)
                    ) {
                        essays.forEach { essay ->
                            RetroListRow(
                                cells = listOf(
                                    essay.word,
                                    essay.wordCount.toString(),
                                    formatter.format(Date(essay.writtenAt))
                                )
                            )
                        }
                    }
                }
                RetroButton(text = "Back", onClick = onBack)
            }
        }
    }
}
