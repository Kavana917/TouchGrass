package com.touchgrass.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touchgrass.app.core.data.settings.SettingsRepository
import com.touchgrass.app.core.feed.Stream
import com.touchgrass.app.core.feed.StreamCategory
import com.touchgrass.app.core.feed.StreamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedState(
    val streams: List<Stream> = emptyList(),
    val favourites: Set<String> = emptySet(),
    val categoryFilter: StreamCategory? = null,
    val audioOn: Boolean = false
) {
    val visible: List<Stream>
        get() = categoryFilter?.let { filter ->
            streams.filter { it.category == filter }
        } ?: streams

    val categories: List<StreamCategory>
        get() = streams.map { it.category }.distinct().sortedBy { it.ordinal }
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val streamRepository: StreamRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _categoryFilter = MutableStateFlow<StreamCategory?>(null)

    val state: StateFlow<FeedState> = combine(
        streamRepository.streams,
        streamRepository.favourites,
        _categoryFilter,
        settings.feedAudioOn
    ) { streams, favourites, filter, audioOn ->
        FeedState(
            streams = streams,
            favourites = favourites,
            categoryFilter = filter,
            audioOn = audioOn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedState()
    )

    private val _clearMode = MutableStateFlow(false)
    val clearMode: StateFlow<Boolean> = _clearMode.asStateFlow()

    init {
        viewModelScope.launch { streamRepository.load() }
    }

    fun setCategoryFilter(category: StreamCategory?) {
        _categoryFilter.value = category
    }

    fun toggleFavourite(streamId: String) = viewModelScope.launch {
        streamRepository.toggleFavourite(streamId)
    }

    fun toggleAudio() = viewModelScope.launch {
        settings.setFeedAudioOn(!state.value.audioOn)
    }

    fun setClearMode(enabled: Boolean) {
        _clearMode.value = enabled
    }

    fun streamById(id: String): Stream? = state.value.streams.firstOrNull { it.id == id }
}
