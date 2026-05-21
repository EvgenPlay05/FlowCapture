package com.example.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import kotlinx.coroutines.*

class FlowCaptureTileService : TileService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var statusJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTile()

        statusJob?.cancel()
        statusJob = serviceScope.launch {
            RecordingService.isRecording.collect { _ ->
                updateTile()
            }
        }
    }

    override fun onStopListening() {
        statusJob?.cancel()
        super.onStopListening()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val recording = RecordingService.isRecording.value
        val paused = RecordingService.isPaused.value

        if (recording) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = if (paused) "Recording Paused" else "Recording..."
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "FlowCapture Screen"
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val recording = RecordingService.isRecording.value

        if (recording) {
            // If already recording, stop it directly and update
            val intent = Intent(this, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            startService(intent)
        } else {
            // Need Activity to request screen recording authorization intent
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("START_RECORDING_DIRECT", true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ safe pending intent launch
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    launchIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(launchIntent)
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
