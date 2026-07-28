package com.touchgrass.app.feature.essay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.touchgrass.app.core.data.db.Essay
import com.touchgrass.app.core.pass.PassRepository
import com.touchgrass.app.ui.components.BodyText
import com.touchgrass.app.ui.components.PixelText
import com.touchgrass.app.ui.components.RetroButton
import com.touchgrass.app.ui.components.RetroWindow
import com.touchgrass.app.ui.components.SunkenField
import com.touchgrass.app.ui.components.Wallpaper
import com.touchgrass.app.ui.theme.Dimens
import com.touchgrass.app.ui.theme.RetroTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class EssayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    passRepository: PassRepository
) : ViewModel() {

    private val essayId: Long = savedStateHandle.get<Long>("essayId") ?: 0L

    val essay: StateFlow<Essay?> = passRepository.essay(essayId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )
}

/**
 * One essay, in full.
 *
 * The body renders in sans at body size, not pixel — you're reading it, and
 * §5's dividing line applies to your own writing as much as anywhere else.
 *
 * Still no grade, no score, no assessment of any kind. This is a record that
 * the work happened, and a place to reread it if you want to.
 */
@Composable
fun EssayDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EssayDetailViewModel = hiltViewModel()
) {
    val essay by viewModel.essay.collectAsStateWithLifecycle()
    val formatter = SimpleDateFormat("EEEE d MMMM, HH:mm", Locale.getDefault())

    Wallpaper(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ItemSpacing),
            verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing)
        ) {
            val current = essay
            RetroWindow(
                title = current?.word?.replaceFirstChar { it.uppercase() } ?: "Essay",
                statusText = current?.let { "${it.wordCount} words" } ?: "—",
                statusSecondary = current?.let {
                    if (it.durationSeconds > 0) "${it.durationSeconds / 60} min" else null
                }
            ) {
                if (current == null) {
                    BodyText("Essay not found.")
                } else {
                    PixelText(
                        text = current.word.uppercase(),
                        style = RetroTheme.typography.heading
                    )
                    PixelText(
                        text = formatter.format(Date(current.writtenAt)),
                        color = RetroTheme.colors.surfaceShadow
                    )

                    SunkenField(modifier = Modifier.fillMaxWidth()) {
                        BodyText(current.body)
                    }
                }

                RetroButton(text = "Back", onClick = onBack)
            }
        }
    }
}
