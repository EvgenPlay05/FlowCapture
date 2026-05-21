package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.db.RecordingDatabase
import com.example.db.RecordingEntity
import com.example.db.RecordingRepository
import com.example.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RecordingRepository
    val settingsManager: SettingsManager

    init {
        val recordingDao = RecordingDatabase.getDatabase(application).recordingDao()
        repository = RecordingRepository(recordingDao)
        settingsManager = SettingsManager(application)
    }

    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val currentProfileName = MutableStateFlow(settingsManager.profile)

    fun selectProfile(profileName: String) {
        settingsManager.applyProfile(profileName)
        currentProfileName.value = profileName
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(recording)
        }
    }

    fun renameRecording(recording: RecordingEntity, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = recording.copy(title = newTitle)
            repository.update(updated)
        }
    }

    fun insertMockDataForTesting() {
        viewModelScope.launch(Dispatchers.IO) {
            val mockRecordings = listOf(
                RecordingEntity(
                    title = "PUBG Mobile Match #12",
                    filePath = "/mock/path/vid1.mp4",
                    uriString = "android.resource://com.example/raw/mock_vid",
                    timestamp = System.currentTimeMillis() - 3600000 * 2,
                    durationMs = 422000,
                    sizeBytes = 148 * 1024 * 1024,
                    profileName = SettingsManager.PROFILE_GAMING,
                    resolution = "1080p",
                    fps = 60,
                    audioOption = SettingsManager.AUDIO_COMBINED
                ),
                RecordingEntity(
                    title = "Database Systems Lecture - Sync Systems",
                    filePath = "/mock/path/vid2.mp4",
                    uriString = "android.resource://com.example/raw/mock_vid",
                    timestamp = System.currentTimeMillis() - 3600000 * 24,
                    durationMs = 3652000,
                    sizeBytes = 460 * 1024 * 1024,
                    profileName = SettingsManager.PROFILE_LECTURE,
                    resolution = "1080p",
                    fps = 30,
                    audioOption = SettingsManager.AUDIO_MIC
                )
            )
            mockRecordings.forEach { repository.insert(it) }
        }
    }
}
