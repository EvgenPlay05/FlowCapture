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

    // Update check states
    val isCheckingUpdate = MutableStateFlow(false)
    val updateStatus = MutableStateFlow<UpdateResult?>(null)

    sealed class UpdateResult {
        object UpToDate : UpdateResult()
        data class NewUpdateAvailable(val version: String, val releaseUrl: String) : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }

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

    fun checkForUpdates() {
        viewModelScope.launch {
            isCheckingUpdate.value = true
            updateStatus.value = null
            try {
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val url = java.net.URL("https://api.github.com/repos/EvgenPlay05/FlowCapture/releases/latest")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                    connection.connectTimeout = 4000
                    connection.readTimeout = 4000
                    
                    if (connection.responseCode == 200) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        val tagNameStart = responseText.indexOf("\"tag_name\":")
                        val tagValue = if (tagNameStart != -1) {
                            val start = responseText.indexOf("\"", tagNameStart + 11) + 1
                            val end = responseText.indexOf("\"", start)
                            responseText.substring(start, end)
                        } else ""

                        val htmlUrlStart = responseText.indexOf("\"html_url\":")
                        val releaseUrl = if (htmlUrlStart != -1) {
                            val start = responseText.indexOf("\"", htmlUrlStart + 11) + 1
                            val end = responseText.indexOf("\"", start)
                            responseText.substring(start, end)
                        } else "https://github.com/EvgenPlay05/FlowCapture/releases"

                        if (tagValue.isNotEmpty()) {
                            val currentVersionCode = 101 // matching 1.0.1
                            val parsedVersionCode = parseVersionCode(tagValue)
                            if (parsedVersionCode > currentVersionCode) {
                                UpdateResult.NewUpdateAvailable(tagValue, releaseUrl)
                            } else {
                                UpdateResult.UpToDate
                            }
                        } else {
                            UpdateResult.UpToDate
                        }
                    } else if (connection.responseCode == 404) {
                        // No releases yet, meaning we are up to date
                        UpdateResult.UpToDate
                    } else {
                        val messageStr = "HTTP ${connection.responseCode}: ${connection.responseMessage ?: "Rate Limit or Server Issue"}"
                        UpdateResult.Error(messageStr)
                    }
                }
                updateStatus.value = result
            } catch (e: Exception) {
                updateStatus.value = UpdateResult.Error("Connection error: ${e.localizedMessage ?: "Unknown network issue"}")
            } finally {
                isCheckingUpdate.value = false
            }
        }
    }

    private fun parseVersionCode(tag: String): Int {
        val sanitized = tag.replace("[^0-9.]".toRegex(), "")
        val parts = sanitized.split(".")
        return try {
            val major = parts.getOrNull(0)?.toInt() ?: 1
            val minor = parts.getOrNull(1)?.toInt() ?: 0
            val patch = parts.getOrNull(2)?.toInt() ?: 0
            major * 100 + minor * 10 + patch
        } catch (e: Exception) {
            101
        }
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
