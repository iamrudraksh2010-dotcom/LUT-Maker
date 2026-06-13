package com.example.data

import kotlinx.coroutines.flow.Flow

class PresetRepository(private val presetDao: PresetDao) {
    val allPresets: Flow<List<PresetEntity>> = presetDao.getAllPresets()

    suspend fun insert(preset: PresetEntity) {
        presetDao.insertPreset(preset)
    }

    suspend fun deleteById(id: Int) {
        presetDao.deletePresetById(id)
    }

    suspend fun getById(id: Int): PresetEntity? {
        return presetDao.getPresetById(id)
    }
}
