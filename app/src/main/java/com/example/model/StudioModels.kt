package com.example.model

enum class PoseType(
    val stepNumber: Int,
    val title: String,
    val subtitle: String
) {
    STANDING_FRONT(
        stepNumber = 1,
        title = "Standing Front View",
        subtitle = "Full-length facing camera"
    ),
    STANDING_BACK(
        stepNumber = 2,
        title = "Standing Back View",
        subtitle = "Full back design, hair moved aside"
    ),
    SEATED_LIFESTYLE(
        stepNumber = 3,
        title = "Seated Lifestyle View",
        subtitle = "Natural pose on edge of bed"
    )
}

data class GarmentUserNotes(
    val brand: String = "",
    val sizeFromLabel: String = "",
    val condition: String = "",
    val customRoomNote: String = ""
)

data class DutchVintedAd(
    val title: String,
    val description: String,
    val brand: String,
    val size: String,
    val colour: String,
    val material: String,
    val styleAndFit: String,
    val searchTerms: String,
    val suggestedAskingPrice: String,
    val expectedSellingPrice: String,
    val quickSalePrice: String,
    val readyToCopyListing: String
)

sealed interface PoseSlotState {
    data object Idle : PoseSlotState
    data object Queued : PoseSlotState
    data object Generating : PoseSlotState
    data class Ready(val filePath: String) : PoseSlotState
    data class Error(val message: String) : PoseSlotState
}

sealed interface AdGenerationState {
    data object Idle : AdGenerationState
    data object Generating : AdGenerationState
    data class Ready(val ad: DutchVintedAd) : AdGenerationState
    data class Error(val message: String) : AdGenerationState
}

data class StudioShoot(
    val id: String,
    val createdAt: Long,
    val originalPhotoPaths: List<String>,
    val frontPhotoPath: String?,
    val backPhotoPath: String?,
    val seatedPhotoPath: String?,
    val userNotes: GarmentUserNotes,
    val ad: DutchVintedAd?
)
