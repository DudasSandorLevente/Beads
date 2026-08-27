package com.sld.beads.data

data class WorkingArea(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class BeadGridSize(
    val width: Int,
    val height: Int
)

enum class ConfigurationField {
    WORKING_AREA_X,
    WORKING_AREA_Y,
    WORKING_AREA_WIDTH,
    WORKING_AREA_HEIGHT,
    BEAD_WIDTH,
    BEAD_HEIGHT
}

class ConfigurationValidationException(
    val errors: Map<ConfigurationField, String>
) : IllegalArgumentException("The image configuration is invalid")

fun validateConfiguration(
    workingArea: WorkingArea,
    beadGridSize: BeadGridSize,
    imageWidth: Int,
    imageHeight: Int
): Map<ConfigurationField, String> = buildMap {
    if (workingArea.x < 0) put(ConfigurationField.WORKING_AREA_X, "Must be zero or greater")
    if (workingArea.y < 0) put(ConfigurationField.WORKING_AREA_Y, "Must be zero or greater")
    if (workingArea.width <= 0) put(ConfigurationField.WORKING_AREA_WIDTH, "Must be greater than zero")
    if (workingArea.height <= 0) put(ConfigurationField.WORKING_AREA_HEIGHT, "Must be greater than zero")
    if (beadGridSize.width <= 0) put(ConfigurationField.BEAD_WIDTH, "Must be greater than zero")
    if (beadGridSize.height <= 0) put(ConfigurationField.BEAD_HEIGHT, "Must be greater than zero")

    if (workingArea.x >= 0 && workingArea.width > 0 &&
        workingArea.x.toLong() + workingArea.width > imageWidth
    ) {
        put(ConfigurationField.WORKING_AREA_WIDTH, "Rectangle exceeds image width ($imageWidth px)")
    }
    if (workingArea.y >= 0 && workingArea.height > 0 &&
        workingArea.y.toLong() + workingArea.height > imageHeight
    ) {
        put(ConfigurationField.WORKING_AREA_HEIGHT, "Rectangle exceeds image height ($imageHeight px)")
    }
}
