package com.example.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val filePath: String,
    val uriString: String,
    val timestamp: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val profileName: String,
    val resolution: String,
    val fps: Int,
    val audioOption: String
) : Serializable
