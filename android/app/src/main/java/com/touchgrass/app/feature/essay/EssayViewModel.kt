package com.touchgrass.app.feature.essay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.essay.EssayValidator
import com.touchgrass.app.core.essay.TypingGuard
import com.touchgrass.app.core.essay.WordGenerator
import com.touchgrass.app.core.pass.PassRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EssayUiState(
    val word: String = "",
    val text: String = "",
    val requiredWords: Int = SettingsRepository.Defaults.ESSAY_WORDS,
    val wordCount: Int = 0,
    /** Set when a change was blocked — stated flatly, never accusingly. */
    val notice: String? = null,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val rerollsLeft: Int = EssayViewModel.MAX_REROLLS,
    /** Minutes granted, once the essay is accepted. */
    val grantedMinutes: Int? = null
) {
    val canSubmit: Boolean get() = wordCount >= requiredWords && !submitting
    val remainingWords: Int get() = (requiredWords - wordCount).coerceAtLeast(0)

    /**
     * Rerolling is only offered before writing starts. Swapping the prompt
     * after 80 words would either discard that work or leave an essay
     * attached to a word it isn't about.
     */
    val canReroll: Boolean get() = rerollsLeft > 0 && text.isEmpty()
}

@HiltViewModel
class EssayViewModel @Inject constructor(
    private val wordGenerator: WordGenerator,
    private val passRepository: PassRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val guard = TypingGuard()

    private val _uiState = MutableStateFlow(EssayUiState())
    val uiState: StateFlow<EssayUiState> = _uiState.asStateFlow()

    init {
        newChallenge()
    }

    fun newChallenge() = viewModelScope.launch {
        guard.reset()
        val required = requiredWordsForUser()
        val word = wordGenerator.next()
        _uiState.value = EssayUiState(
            word = word,
            requiredWords = required,
            rerollsLeft = MAX_REROLLS,
            loading = false
        )
    }

    /**
     * Swap the prompt for a different one.
     *
     * LIMITED ON PURPOSE. app_plan.md §2.4: "if you pick the topic, you'll
     * pick the same easy topic every time and have a canned paragraph
     * memorised by week two. Randomness keeps the cost from decaying."
     * Unlimited rerolls is just picking your own topic with extra steps.
     *
     * Two is enough to escape a word you genuinely have nothing for, and
     * not enough to fish for one you've already written about.
     */
    fun reroll() = viewModelScope.launch {
        val state = _uiState.value
        if (!state.canReroll) return@launch

        val word = wordGenerator.next()
        _uiState.update {
            it.copy(word = word, rerollsLeft = it.rerollsLeft - 1, notice = null)
        }
    }

    /**
     * A dictation user gets a longer requirement instead of being blocked
     * (app_plan.md §2.5). Speaking 150 words is much faster than typing
     * them, so the toll has to be longer to stay comparable.
     */
    private suspend fun requiredWordsForUser(): Int {
        val base = settings.essayWords.first()
        val dictates = settings.usesDictation.first()
        return if (dictates) (base * 1.6).toInt() else base
    }

    fun onTextChange(next: String) {
        val current = _uiState.value.text

        if (!guard.accept(current, next)) {
            _uiState.update { it.copy(notice = guard.lastRejection) }
            return
        }

        _uiState.update {
            it.copy(
                text = next,
                wordCount = EssayValidator.countWords(next),
                notice = null
            )
        }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun submit() = viewModelScope.launch {
        val state = _uiState.value
        val result = EssayValidator.validate(state.text, state.requiredWords)

        if (!result.valid) {
            _uiState.update { it.copy(notice = result.message) }
            return@launch
        }

        _uiState.update { it.copy(submitting = true) }

        val minutes = passRepository.issuePassForEssay(
            word = state.word,
            body = state.text,
            wordCount = result.wordCount,
            requiredWords = state.requiredWords,
            durationSeconds = guard.durationSeconds,
            cadenceSuspicious = guard.looksAutomated()
        )

        _uiState.update { it.copy(submitting = false, grantedMinutes = minutes) }
    }

    companion object {
        /** Change to a larger number for more forgiving rerolls. */
        const val MAX_REROLLS = 2
    }
}
