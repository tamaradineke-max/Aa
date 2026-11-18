package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.VintedAiService
import com.example.data.local.MediaStorageHelper
import com.example.data.local.PreferencesRepository
import com.example.data.local.ShootRepository
import com.example.data.local.StudioDatabase
import com.example.model.AdGenerationState
import com.example.model.DutchVintedAd
import com.example.model.GarmentUserNotes
import com.example.model.PoseSlotState
import com.example.model.PoseType
import com.example.model.StudioShoot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "VintedStudioViewModel"

data class ActiveShootState(
    val shootId: String? = null,
    val selectedGarmentUris: List<Uri> = emptyList(),
    val savedOriginalPaths: List<String> = emptyList(),
    val userNotes: GarmentUserNotes = GarmentUserNotes(),
    val isRunningPipeline: Boolean = false,
    val statusBanner: String? = null,
    val frontPose: PoseSlotState = PoseSlotState.Idle,
    val backPose: PoseSlotState = PoseSlotState.Idle,
    val seatedPose: PoseSlotState = PoseSlotState.Idle,
    val adState: AdGenerationState = AdGenerationState.Idle
) {
    val hasAnyResults: Boolean
        get() = frontPose !is PoseSlotState.Idle ||
            backPose !is PoseSlotState.Idle ||
            seatedPose !is PoseSlotState.Idle ||
            adState !is AdGenerationState.Idle
}

class VintedStudioViewModel(application: Application) : AndroidViewModel(application) {

    val mediaStorageHelper = MediaStorageHelper(application)
    private val preferencesRepository = PreferencesRepository(application)
    private val shootRepository = ShootRepository(
        shootDao = StudioDatabase.getInstance(application).shootDao(),
        mediaStorageHelper = mediaStorageHelper
    )
    private val aiService = VintedAiService()

    private val _activeState = MutableStateFlow(ActiveShootState())
    val activeState: StateFlow<ActiveShootState> = _activeState.asStateFlow()

    val savedShoots: StateFlow<List<StudioShoot>> = shootRepository.allShoots
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val customModelRefPath: StateFlow<String?> = preferencesRepository.customModelRefPath
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val customBedroomRefPath: StateFlow<String?> = preferencesRepository.customBedroomRefPath
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun addGarmentUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _activeState.update { state ->
            val combined = (state.selectedGarmentUris + uris).distinct().take(5)
            state.copy(selectedGarmentUris = combined)
        }
    }

    fun removeGarmentUri(uri: Uri) {
        _activeState.update { state ->
            state.copy(selectedGarmentUris = state.selectedGarmentUris.filterNot { it == uri })
        }
    }

    fun updateUserNotes(
        brand: String = _activeState.value.userNotes.brand,
        sizeFromLabel: String = _activeState.value.userNotes.sizeFromLabel,
        condition: String = _activeState.value.userNotes.condition,
        customRoomNote: String = _activeState.value.userNotes.customRoomNote
    ) {
        _activeState.update { state ->
            state.copy(
                userNotes = GarmentUserNotes(
                    brand = brand,
                    sizeFromLabel = sizeFromLabel,
                    condition = condition,
                    customRoomNote = customRoomNote
                )
            )
        }
    }

    fun resetActiveStudio() {
        if (_activeState.value.isRunningPipeline) return
        _activeState.value = ActiveShootState()
    }

    fun startFullStudioShoot() {
        val current = _activeState.value
        if (current.isRunningPipeline || current.selectedGarmentUris.isEmpty()) return

        viewModelScope.launch {
            val shootId = UUID.randomUUID().toString()
            _activeState.update {
                it.copy(
                    shootId = shootId,
                    isRunningPipeline = true,
                    statusBanner = "Preparing studio references and garment photos...",
                    frontPose = PoseSlotState.Queued,
                    backPose = PoseSlotState.Queued,
                    seatedPose = PoseSlotState.Queued,
                    adState = AdGenerationState.Generating
                )
            }

            try {
                // Save original garment photos locally so genuine product details are always preserved
                val originalPaths = mutableListOf<String>()
                val garmentBitmaps = mutableListOf<Bitmap>()
                current.selectedGarmentUris.forEachIndexed { idx, uri ->
                    val savedPath = mediaStorageHelper.saveGarmentOriginalFromUri(uri, shootId, idx)
                    if (savedPath != null) {
                        originalPaths.add(savedPath)
                        mediaStorageHelper.decodeScaledBitmapFromFile(savedPath, maxDimension = 1024)
                            ?.let { garmentBitmaps.add(it) }
                    }
                }

                if (garmentBitmaps.isEmpty()) {
                    _activeState.update {
                        it.copy(
                            isRunningPipeline = false,
                            statusBanner = null,
                            frontPose = PoseSlotState.Error("Could not read selected garment images."),
                            backPose = PoseSlotState.Idle,
                            seatedPose = PoseSlotState.Idle,
                            adState = AdGenerationState.Error("Could not read selected garment images.")
                        )
                    }
                    return@launch
                }

                _activeState.update { it.copy(savedOriginalPaths = originalPaths) }

                // STEP 1: Dutch Vinted Advertisement (works on free Spark plan via gemini-3.8-flash)
                _activeState.update {
                    it.copy(statusBanner = "Step 1/4: Writing Dutch Vinted advertisement...")
                }
                val adResult = generateAdInternal(garmentBitmaps, _activeState.value.userNotes)
                _activeState.update { it.copy(adState = adResult) }

                val (modelRefBitmap, bedroomRefBitmap) = loadActiveReferenceBitmaps()
                val customRoom = _activeState.value.userNotes.customRoomNote.trim()

                // STEP 2: Standing Front View (generated independently)
                _activeState.update {
                    it.copy(
                        statusBanner = "Step 2/4: Photographing Standing Front View...",
                        frontPose = PoseSlotState.Generating
                    )
                }
                val frontResult = generateSinglePoseInternal(
                    shootId = shootId,
                    poseType = PoseType.STANDING_FRONT,
                    modelRefBitmap = modelRefBitmap,
                    bedroomRefBitmap = bedroomRefBitmap,
                    garmentBitmaps = garmentBitmaps,
                    customRoom = customRoom,
                    firstPoseBitmap = null
                )
                _activeState.update { it.copy(frontPose = frontResult.slotState) }

                val firstPoseBitmap = (frontResult.slotState as? PoseSlotState.Ready)?.let { ready ->
                    mediaStorageHelper.decodeScaledBitmapFromFile(ready.filePath, maxDimension = 1024)
                }

                // STEP 3: Standing Back View (generated independently)
                _activeState.update {
                    it.copy(
                        statusBanner = "Step 3/4: Photographing Standing Back View...",
                        backPose = PoseSlotState.Generating
                    )
                }
                val backResult = generateSinglePoseInternal(
                    shootId = shootId,
                    poseType = PoseType.STANDING_BACK,
                    modelRefBitmap = modelRefBitmap,
                    bedroomRefBitmap = bedroomRefBitmap,
                    garmentBitmaps = garmentBitmaps,
                    customRoom = customRoom,
                    firstPoseBitmap = firstPoseBitmap
                )
                _activeState.update { it.copy(backPose = backResult.slotState) }

                // STEP 4: Seated Lifestyle View (generated independently)
                _activeState.update {
                    it.copy(
                        statusBanner = "Step 4/4: Photographing Seated Lifestyle View...",
                        seatedPose = PoseSlotState.Generating
                    )
                }
                val seatedResult = generateSinglePoseInternal(
                    shootId = shootId,
                    poseType = PoseType.SEATED_LIFESTYLE,
                    modelRefBitmap = modelRefBitmap,
                    bedroomRefBitmap = bedroomRefBitmap,
                    garmentBitmaps = garmentBitmaps,
                    customRoom = customRoom,
                    firstPoseBitmap = firstPoseBitmap
                )
                _activeState.update {
                    it.copy(
                        seatedPose = seatedResult.slotState,
                        isRunningPipeline = false,
                        statusBanner = null
                    )
                }

                persistActiveShootIfAny()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Studio shoot pipeline failed", e)
                _activeState.update {
                    it.copy(
                        isRunningPipeline = false,
                        statusBanner = null,
                        adState = if (it.adState is AdGenerationState.Generating) {
                            AdGenerationState.Error(e.localizedMessage ?: "Studio shoot failed.")
                        } else {
                            it.adState
                        }
                    )
                }
            }
        }
    }

    fun startAdOnlyShoot() {
        val current = _activeState.value
        if (current.isRunningPipeline || current.selectedGarmentUris.isEmpty()) return

        viewModelScope.launch {
            val shootId = UUID.randomUUID().toString()
            _activeState.update {
                it.copy(
                    shootId = shootId,
                    isRunningPipeline = true,
                    statusBanner = "Writing Dutch Vinted advertisement (Free tier)...",
                    frontPose = PoseSlotState.Idle,
                    backPose = PoseSlotState.Idle,
                    seatedPose = PoseSlotState.Idle,
                    adState = AdGenerationState.Generating
                )
            }

            try {
                val originalPaths = mutableListOf<String>()
                val garmentBitmaps = mutableListOf<Bitmap>()
                current.selectedGarmentUris.forEachIndexed { idx, uri ->
                    val savedPath = mediaStorageHelper.saveGarmentOriginalFromUri(uri, shootId, idx)
                    if (savedPath != null) {
                        originalPaths.add(savedPath)
                        mediaStorageHelper.decodeScaledBitmapFromFile(savedPath, maxDimension = 1024)
                            ?.let { garmentBitmaps.add(it) }
                    }
                }

                if (garmentBitmaps.isEmpty()) {
                    _activeState.update {
                        it.copy(
                            isRunningPipeline = false,
                            statusBanner = null,
                            adState = AdGenerationState.Error("Could not read selected garment images.")
                        )
                    }
                    return@launch
                }

                _activeState.update { it.copy(savedOriginalPaths = originalPaths) }
                val adResult = generateAdInternal(garmentBitmaps, _activeState.value.userNotes)
                _activeState.update {
                    it.copy(
                        adState = adResult,
                        isRunningPipeline = false,
                        statusBanner = null
                    )
                }
                persistActiveShootIfAny()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Ad-only shoot failed", e)
                _activeState.update {
                    it.copy(
                        isRunningPipeline = false,
                        statusBanner = null,
                        adState = AdGenerationState.Error(e.localizedMessage ?: "Failed to write Dutch Vinted ad.")
                    )
                }
            }
        }
    }

    fun regenerateSinglePose(poseType: PoseType) {
        val current = _activeState.value
        val shootId = current.shootId ?: return
        if (current.isRunningPipeline || current.savedOriginalPaths.isEmpty()) return

        viewModelScope.launch {
            updatePoseSlot(poseType, PoseSlotState.Generating)
            try {
                val garmentBitmaps = current.savedOriginalPaths.mapNotNull {
                    mediaStorageHelper.decodeScaledBitmapFromFile(it, maxDimension = 1024)
                }
                val (modelRefBitmap, bedroomRefBitmap) = loadActiveReferenceBitmaps()
                val firstPoseBitmap = if (poseType != PoseType.STANDING_FRONT) {
                    (current.frontPose as? PoseSlotState.Ready)?.let {
                        mediaStorageHelper.decodeScaledBitmapFromFile(it.filePath, maxDimension = 1024)
                    }
                } else {
                    null
                }
                val result = generateSinglePoseInternal(
                    shootId = shootId,
                    poseType = poseType,
                    modelRefBitmap = modelRefBitmap,
                    bedroomRefBitmap = bedroomRefBitmap,
                    garmentBitmaps = garmentBitmaps,
                    customRoom = current.userNotes.customRoomNote.trim(),
                    firstPoseBitmap = firstPoseBitmap
                )
                updatePoseSlot(poseType, result.slotState)
                persistActiveShootIfAny()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Regenerate pose failed for $poseType", e)
                updatePoseSlot(
                    poseType,
                    PoseSlotState.Error(e.localizedMessage ?: "Failed to regenerate photo.")
                )
            }
        }
    }

    fun regenerateDutchAd() {
        val current = _activeState.value
        if (current.isRunningPipeline || current.savedOriginalPaths.isEmpty()) return

        viewModelScope.launch {
            _activeState.update { it.copy(adState = AdGenerationState.Generating) }
            try {
                val garmentBitmaps = current.savedOriginalPaths.mapNotNull {
                    mediaStorageHelper.decodeScaledBitmapFromFile(it, maxDimension = 1024)
                }
                val adResult = generateAdInternal(garmentBitmaps, current.userNotes)
                _activeState.update { it.copy(adState = adResult) }
                persistActiveShootIfAny()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Regenerate Dutch ad failed", e)
                _activeState.update {
                    it.copy(adState = AdGenerationState.Error(e.localizedMessage ?: "Failed to write ad."))
                }
            }
        }
    }

    fun updateCustomModelReference(uri: Uri) {
        viewModelScope.launch {
            val savedPath = mediaStorageHelper.saveCustomReferenceFromUri(
                uri = uri,
                fileName = "custom_model_ref_${System.currentTimeMillis()}.jpg"
            )
            if (savedPath != null) {
                preferencesRepository.setCustomModelRefPath(savedPath)
            }
        }
    }

    fun resetModelReferenceToDefault() {
        viewModelScope.launch {
            preferencesRepository.setCustomModelRefPath(null)
        }
    }

    fun updateCustomBedroomReference(uri: Uri) {
        viewModelScope.launch {
            val savedPath = mediaStorageHelper.saveCustomReferenceFromUri(
                uri = uri,
                fileName = "custom_bedroom_ref_${System.currentTimeMillis()}.jpg"
            )
            if (savedPath != null) {
                preferencesRepository.setCustomBedroomRefPath(savedPath)
            }
        }
    }

    fun resetBedroomReferenceToDefault() {
        viewModelScope.launch {
            preferencesRepository.setCustomBedroomRefPath(null)
        }
    }

    fun observeShootById(shootId: String): StateFlow<StudioShoot?> {
        return shootRepository.getShootById(shootId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )
    }

    fun deleteShoot(shootId: String) {
        viewModelScope.launch {
            shootRepository.deleteShoot(shootId)
        }
    }

    private fun updatePoseSlot(poseType: PoseType, state: PoseSlotState) {
        _activeState.update {
            when (poseType) {
                PoseType.STANDING_FRONT -> it.copy(frontPose = state)
                PoseType.STANDING_BACK -> it.copy(backPose = state)
                PoseType.SEATED_LIFESTYLE -> it.copy(seatedPose = state)
            }
        }
    }

    private suspend fun loadActiveReferenceBitmaps(): Pair<Bitmap, Bitmap> {
        val modelPath = preferencesRepository.customModelRefPath.first()
        val bedroomPath = preferencesRepository.customBedroomRefPath.first()

        val modelBitmap = if (!modelPath.isNullOrBlank()) {
            mediaStorageHelper.decodeScaledBitmapFromFile(modelPath, maxDimension = 1024)
                ?: mediaStorageHelper.loadDefaultModelReferenceBitmap(maxDimension = 1024)
        } else {
            mediaStorageHelper.loadDefaultModelReferenceBitmap(maxDimension = 1024)
        }

        val bedroomBitmap = if (!bedroomPath.isNullOrBlank()) {
            mediaStorageHelper.decodeScaledBitmapFromFile(bedroomPath, maxDimension = 1024)
                ?: mediaStorageHelper.loadDefaultBedroomReferenceBitmap(maxDimension = 1024)
        } else {
            mediaStorageHelper.loadDefaultBedroomReferenceBitmap(maxDimension = 1024)
        }

        return modelBitmap to bedroomBitmap
    }

    private data class PoseGenOutcome(val slotState: PoseSlotState)

    private suspend fun generateSinglePoseInternal(
        shootId: String,
        poseType: PoseType,
        modelRefBitmap: Bitmap,
        bedroomRefBitmap: Bitmap,
        garmentBitmaps: List<Bitmap>,
        customRoom: String,
        firstPoseBitmap: Bitmap?
    ): PoseGenOutcome {
        return try {
            val generatedBitmap = aiService.generatePosePhoto(
                poseType = poseType,
                modelRefBitmap = modelRefBitmap,
                bedroomRefBitmap = bedroomRefBitmap,
                garmentBitmaps = garmentBitmaps,
                customRoomNote = customRoom,
                consistencyFirstPoseBitmap = firstPoseBitmap
            )
            val fileName = when (poseType) {
                PoseType.STANDING_FRONT -> "pose_1_front_${System.currentTimeMillis()}.png"
                PoseType.STANDING_BACK -> "pose_2_back_${System.currentTimeMillis()}.png"
                PoseType.SEATED_LIFESTYLE -> "pose_3_seated_${System.currentTimeMillis()}.png"
            }
            val savedPath = mediaStorageHelper.saveGeneratedPoseBitmap(generatedBitmap, shootId, fileName)
            PoseGenOutcome(PoseSlotState.Ready(savedPath))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timed out generating $poseType", e)
            PoseGenOutcome(PoseSlotState.Error("Timed out after 60 seconds. Tap Retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating $poseType", e)
            val raw = e.localizedMessage ?: "Failed to generate ${poseType.title}."
            val isBillingIssue = raw.contains("billing", ignoreCase = true) ||
                raw.contains("blaze", ignoreCase = true) ||
                raw.contains("upgrade", ignoreCase = true)
            val displayMessage = if (isBillingIssue) {
                "$raw (AI image generation requires Firebase Blaze plan; Dutch Vinted Ad generation works on the free Spark plan.)"
            } else {
                raw
            }
            PoseGenOutcome(PoseSlotState.Error(displayMessage))
        }
    }

    private suspend fun generateAdInternal(
        garmentBitmaps: List<Bitmap>,
        userNotes: GarmentUserNotes
    ): AdGenerationState {
        return try {
            val ad: DutchVintedAd = aiService.generateDutchVintedAd(garmentBitmaps, userNotes)
            AdGenerationState.Ready(ad)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timed out generating Dutch Vinted ad", e)
            AdGenerationState.Error("Vinted advertisement timed out after 60 seconds. Tap Retry.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating Dutch Vinted ad", e)
            AdGenerationState.Error(e.localizedMessage ?: "Failed to write Dutch Vinted advertisement.")
        }
    }

    private suspend fun persistActiveShootIfAny() {
        val s = _activeState.value
        val shootId = s.shootId ?: return
        val frontPath = (s.frontPose as? PoseSlotState.Ready)?.filePath
        val backPath = (s.backPose as? PoseSlotState.Ready)?.filePath
        val seatedPath = (s.seatedPose as? PoseSlotState.Ready)?.filePath
        val ad = (s.adState as? AdGenerationState.Ready)?.ad
        if (frontPath == null && backPath == null && seatedPath == null && ad == null) return

        shootRepository.saveShoot(
            StudioShoot(
                id = shootId,
                createdAt = System.currentTimeMillis(),
                originalPhotoPaths = s.savedOriginalPaths,
                frontPhotoPath = frontPath,
                backPhotoPath = backPath,
                seatedPhotoPath = seatedPath,
                userNotes = s.userNotes,
                ad = ad
            )
        )
    }
}
