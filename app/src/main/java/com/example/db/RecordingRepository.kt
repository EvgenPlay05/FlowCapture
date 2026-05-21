package com.example.db

import kotlinx.coroutines.flow.Flow

class RecordingRepository(private val recordingDao: RecordingDao) {
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    suspend fun insert(recording: RecordingEntity): Long {
        return recordingDao.insertRecording(recording)
    }

    suspend fun update(recording: RecordingEntity) {
        recordingDao.updateRecording(recording)
    }

    suspend fun delete(recording: RecordingEntity) {
        recordingDao.deleteRecording(recording)
    }

    suspend fun getById(id: Int): RecordingEntity? {
        return recordingDao.getRecordingById(id)
    }
}
