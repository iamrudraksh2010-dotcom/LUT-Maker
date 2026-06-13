package com.example.model

data class GradingParams(
    val exposure: Float = 0f,       // -2f..2f
    val brightness: Float = 0f,     // -1f..1f
    val contrast: Float = 1f,       // 0.5f..2.0f
    val saturation: Float = 1f,     // 0.0f..2.0f
    val vibrance: Float = 0f,       // -1f..1f
    val temperature: Float = 0f,    // -1f..1f
    val tint: Float = 0f,           // -1f..1f
    val redBalance: Float = 1f,     // 0.5f..1.5f
    val greenBalance: Float = 1f,   // 0.5f..1.5f
    val blueBalance: Float = 1f,    // 0.5f..1.5f
    val shadows: Float = 1f,        // 0.0f..2.0f
    val highlights: Float = 1f,     // 0.0f..2.0f
    val gamma: Float = 1f,          // 0.5f..2.0f
    val selectedPreset: String = "None",
    val presetIntensity: Float = 1f
) {
    fun isDefaults(): Boolean {
        return exposure == 0f &&
                brightness == 0f &&
                contrast == 1f &&
                saturation == 1f &&
                vibrance == 0f &&
                temperature == 0f &&
                tint == 0f &&
                redBalance == 1f &&
                greenBalance == 1f &&
                blueBalance == 1f &&
                shadows == 1f &&
                highlights == 1f &&
                gamma == 1f &&
                selectedPreset == "None"
    }
}
