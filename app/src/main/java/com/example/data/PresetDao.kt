package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM lut_presets ORDER BY timestamp DESC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: PresetEntity)

    @Query("DELETE FROM lut_presets WHERE id = :id")
    suspend fun deletePresetById(id: Int)

    @Query("SELECT * FROM lut_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: Int): PresetEntity?
}
