package com.example.player

data class TrackOption(
    val id: String,
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String? = null,
    val mimeType: String? = null,
    val isSelected: Boolean = false
)

enum class AspectRatioMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    CROP("Crop"),
    RATIO_16_9("16:9"),
    RATIO_4_3("4:3")
}

enum class OrientationMode(val label: String) {
    SENSOR("Auto"),
    LANDSCAPE("Landscape"),
    PORTRAIT("Portrait")
}
