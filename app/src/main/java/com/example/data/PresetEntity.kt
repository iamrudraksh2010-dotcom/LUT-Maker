package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.GradingParams

@Entity(tableName = "lut_presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCustom: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Grading fields
    val exposure: Float,
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val vibrance: Float,
    val temperature: Float,
    val tint: Float,
    val redBalance: Float,
    val greenBalance: Float,
    val blueBalance: Float,
    val shadows: Float,
    val highlights: Float,
    val gamma: Float,
    val selectedPresetName: String,
    val presetIntensity: Float
) {
    fun toParams(): GradingParams {
        return GradingParams(
            exposure = exposure,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            vibrance = vibrance,
            temperature = temperature,
            tint = tint,
            redBalance = redBalance,
            greenBalance = greenBalance,
            blueBalance = blueBalance,
            shadows = shadows,
            highlights = highlights,
            gamma = gamma,
            selectedPreset = selectedPresetName,
            presetIntensity = presetIntensity
        )
    }

    companion object {
        fun fromParams(name: String, params: GradingParams): PresetEntity {
            return PresetEntity(
                name = name,
                exposure = params.exposure,
                brightness = params.brightness,
                contrast = params.contrast,
                saturation = params.saturation,
                vibrance = params.vibrance,
                temperature = params.temperature,
                tint = params.tint,
                redBalance = params.redBalance,
                greenBalance = params.greenBalance,
                blueBalance = params.blueBalance,
                shadows = params.shadows,
                highlights = params.highlights,
                gamma = params.gamma,
                selectedPresetName = params.selectedPreset,
                presetIntensity = params.presetIntensity
            )
        }
    }
}
