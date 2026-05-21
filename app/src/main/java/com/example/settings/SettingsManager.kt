package com.example.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flowcapture_prefs", Context.MODE_PRIVATE)

    companion object {
        // Recording Options
        const val KEY_RESOLUTION = "pref_resolution"
        const val KEY_FPS = "pref_fps"
        const val KEY_BITRATE = "pref_bitrate"
        const val KEY_CODEC = "pref_codec"
        const val KEY_AUDIO_OPTION = "pref_audio_option"
        const val KEY_AUDIO_QUALITY = "pref_audio_quality"
        const val KEY_PROFILE = "pref_profile"
        const val KEY_SAVE_LOCATION = "pref_save_location"
        const val KEY_SAVE_LOCATION_NAME = "pref_save_location_name"

        // Bubble Settings
        const val KEY_BUBBLE_ENABLED = "pref_bubble_enabled"
        const val KEY_BUBBLE_SIZE = "pref_bubble_size"
        const val KEY_BUBBLE_OPACITY = "pref_bubble_opacity"
        const val KEY_HIDE_BUBBLE_IN_RECORDING = "pref_hide_bubble_in_recording"
        const val KEY_ALLOW_DRAWING = "pref_allow_drawing"

        // Audio options
        const val AUDIO_NONE = "None"
        const val AUDIO_MIC = "Mic Only"
        const val AUDIO_SYSTEM = "System Audio"
        const val AUDIO_COMBINED = "System + Mic"

        // Profiles
        const val PROFILE_CUSTOM = "Custom"
        const val PROFILE_GAMING = "Gaming"
        const val PROFILE_LECTURE = "Lecture"
        const val PROFILE_LOW_STORAGE = "Low Storage"
        const val PROFILE_HIGH_QUALITY = "High Quality"
    }

    var resolution: String
        get() = prefs.getString(KEY_RESOLUTION, "1080p") ?: "1080p"
        set(value) = prefs.edit().putString(KEY_RESOLUTION, value).apply()

    var fps: Int
        get() = prefs.getInt(KEY_FPS, 30)
        set(value) = prefs.edit().putInt(KEY_FPS, value).apply()

    var bitrateMbps: Int
        get() = prefs.getInt(KEY_BITRATE, 8)
        set(value) = prefs.edit().putInt(KEY_BITRATE, value).apply()

    var codec: String
        get() = prefs.getString(KEY_CODEC, "H.264") ?: "H.264"
        set(value) = prefs.edit().putString(KEY_CODEC, value).apply()

    var audioOption: String
        get() = prefs.getString(KEY_AUDIO_OPTION, AUDIO_COMBINED) ?: AUDIO_COMBINED
        set(value) = prefs.edit().putString(KEY_AUDIO_OPTION, value).apply()

    var audioQualityKbps: Int
        get() = prefs.getInt(KEY_AUDIO_QUALITY, 128)
        set(value) = prefs.edit().putInt(KEY_AUDIO_QUALITY, value).apply()

    var profile: String
        get() = prefs.getString(KEY_PROFILE, PROFILE_HIGH_QUALITY) ?: PROFILE_HIGH_QUALITY
        set(value) = prefs.edit().putString(KEY_PROFILE, value).apply()

    var saveLocationPath: String
        get() = prefs.getString(KEY_SAVE_LOCATION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVE_LOCATION, value).apply()

    var saveLocationName: String
        get() = prefs.getString(KEY_SAVE_LOCATION_NAME, "Built-in (Movies/FlowCapture)") ?: "Built-in (Movies/FlowCapture)"
        set(value) = prefs.edit().putString(KEY_SAVE_LOCATION_NAME, value).apply()

    // Bubble Attributes
    var isBubbleEnabled: Boolean
        get() = prefs.getBoolean(KEY_BUBBLE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BUBBLE_ENABLED, value).apply()

    var bubbleSize: String
        get() = prefs.getString(KEY_BUBBLE_SIZE, "Medium") ?: "Medium"
        set(value) = prefs.edit().putString(KEY_BUBBLE_SIZE, value).apply()

    var bubbleOpacity: Float
        get() = prefs.getFloat(KEY_BUBBLE_OPACITY, 0.8f)
        set(value) = prefs.edit().putFloat(KEY_BUBBLE_OPACITY, value).apply()

    var isHideBubbleInRecordingEnabled: Boolean
        get() = prefs.getBoolean(KEY_HIDE_BUBBLE_IN_RECORDING, true)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_BUBBLE_IN_RECORDING, value).apply()

    var isAllowDrawingEnabled: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_DRAWING, true)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_DRAWING, value).apply()

    var language: String
        get() = prefs.getString("pref_language", "English") ?: "English"
        set(value) = prefs.edit().putString("pref_language", value).apply()

    var videoOrientation: String
        get() = prefs.getString("pref_video_orientation", "Auto") ?: "Auto"
        set(value) = prefs.edit().putString("pref_video_orientation", value).apply()

    fun applyProfile(profileName: String) {
        this.profile = profileName
        when (profileName) {
            PROFILE_GAMING -> {
                resolution = "1080p"
                fps = 60
                bitrateMbps = 12
                codec = "H.264"
                audioOption = AUDIO_COMBINED
                audioQualityKbps = 192
            }
            PROFILE_LECTURE -> {
                resolution = "1080p"
                fps = 30
                bitrateMbps = 6
                codec = "H.264"
                audioOption = AUDIO_MIC
                audioQualityKbps = 128
            }
            PROFILE_LOW_STORAGE -> {
                resolution = "720p"
                fps = 24
                bitrateMbps = 2
                codec = "H.265"
                audioOption = AUDIO_MIC
                audioQualityKbps = 96
            }
            PROFILE_HIGH_QUALITY -> {
                resolution = "1440p"
                fps = 65 // Actually 60
                fps = 60
                bitrateMbps = 16
                codec = "H.265"
                audioOption = AUDIO_COMBINED
                audioQualityKbps = 256
            }
        }
    }
}
