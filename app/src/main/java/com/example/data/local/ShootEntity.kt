package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "studio_shoots")
data class ShootEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val originalPhotoPathsJson: String,
    val frontPhotoPath: String?,
    val backPhotoPath: String?,
    val seatedPhotoPath: String?,
    val userNotesBrand: String,
    val userNotesSize: String,
    val userNotesCondition: String,
    val customRoomNote: String,
    val adTitle: String,
    val adDescription: String,
    val adBrand: String,
    val adSize: String,
    val adColour: String,
    val adMaterial: String,
    val adStyleFit: String,
    val adSearchTerms: String,
    val adAskingPrice: String,
    val adExpectedPrice: String,
    val adQuickSalePrice: String,
    val adFullCopyText: String
)
