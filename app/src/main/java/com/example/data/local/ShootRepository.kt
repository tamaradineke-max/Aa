package com.example.data.local

import com.example.model.DutchVintedAd
import com.example.model.GarmentUserNotes
import com.example.model.StudioShoot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class ShootRepository(
    private val shootDao: ShootDao,
    private val mediaStorageHelper: MediaStorageHelper
) {
    val allShoots: Flow<List<StudioShoot>> = shootDao.getAllShoots().map { list ->
        list.map { it.toDomain() }
    }

    fun getShootById(id: String): Flow<StudioShoot?> = shootDao.getShootById(id).map { it?.toDomain() }

    suspend fun saveShoot(shoot: StudioShoot) {
        shootDao.insertShoot(shoot.toEntity())
    }

    suspend fun deleteShoot(id: String) {
        shootDao.deleteShootById(id)
        mediaStorageHelper.deleteShootDirectory(id)
    }

    private fun ShootEntity.toDomain(): StudioShoot {
        val paths = mutableListOf<String>()
        try {
            val array = JSONArray(originalPhotoPathsJson)
            for (i in 0 until array.length()) {
                paths.add(array.getString(i))
            }
        } catch (_: Exception) {
        }
        val ad = if (adTitle.isNotBlank() || adFullCopyText.isNotBlank()) {
            DutchVintedAd(
                title = adTitle,
                description = adDescription,
                brand = adBrand,
                size = adSize,
                colour = adColour,
                material = adMaterial,
                styleAndFit = adStyleFit,
                searchTerms = adSearchTerms,
                suggestedAskingPrice = adAskingPrice,
                expectedSellingPrice = adExpectedPrice,
                quickSalePrice = adQuickSalePrice,
                readyToCopyListing = adFullCopyText
            )
        } else {
            null
        }
        return StudioShoot(
            id = id,
            createdAt = createdAt,
            originalPhotoPaths = paths,
            frontPhotoPath = frontPhotoPath,
            backPhotoPath = backPhotoPath,
            seatedPhotoPath = seatedPhotoPath,
            userNotes = GarmentUserNotes(
                brand = userNotesBrand,
                sizeFromLabel = userNotesSize,
                condition = userNotesCondition,
                customRoomNote = customRoomNote
            ),
            ad = ad
        )
    }

    private fun StudioShoot.toEntity(): ShootEntity {
        val jsonArray = JSONArray()
        originalPhotoPaths.forEach { jsonArray.put(it) }
        return ShootEntity(
            id = id,
            createdAt = createdAt,
            originalPhotoPathsJson = jsonArray.toString(),
            frontPhotoPath = frontPhotoPath,
            backPhotoPath = backPhotoPath,
            seatedPhotoPath = seatedPhotoPath,
            userNotesBrand = userNotes.brand,
            userNotesSize = userNotes.sizeFromLabel,
            userNotesCondition = userNotes.condition,
            customRoomNote = userNotes.customRoomNote,
            adTitle = ad?.title.orEmpty(),
            adDescription = ad?.description.orEmpty(),
            adBrand = ad?.brand.orEmpty(),
            adSize = ad?.size.orEmpty(),
            adColour = ad?.colour.orEmpty(),
            adMaterial = ad?.material.orEmpty(),
            adStyleFit = ad?.styleAndFit.orEmpty(),
            adSearchTerms = ad?.searchTerms.orEmpty(),
            adAskingPrice = ad?.suggestedAskingPrice.orEmpty(),
            adExpectedPrice = ad?.expectedSellingPrice.orEmpty(),
            adQuickSalePrice = ad?.quickSalePrice.orEmpty(),
            adFullCopyText = ad?.readyToCopyListing.orEmpty()
        )
    }
}
