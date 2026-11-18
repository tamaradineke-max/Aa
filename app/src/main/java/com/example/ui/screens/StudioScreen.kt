package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.PoseType
import com.example.ui.ActiveShootState
import com.example.ui.components.DutchAdCard
import com.example.ui.components.FullScreenImageDialog
import com.example.ui.components.OriginalPhotosRow
import com.example.ui.components.PoseImageCard
import com.example.ui.components.StudioSectionHeader
import java.io.File

@Composable
fun StudioScreen(
    activeState: ActiveShootState,
    customModelRefPath: String?,
    customBedroomRefPath: String?,
    onAddGarmentUris: (List<Uri>) -> Unit,
    onRemoveGarmentUri: (Uri) -> Unit,
    onUpdateNotes: (brand: String, size: String, condition: String, customRoom: String) -> Unit,
    onStartShoot: () -> Unit,
    onStartAdOnly: () -> Unit,
    onResetStudio: () -> Unit,
    onRegeneratePose: (PoseType) -> Unit,
    onRegenerateAd: () -> Unit,
    onOpenReferencesTab: () -> Unit,
    onSaveToGallery: (filePath: String, displayName: String) -> Unit,
    onShareImage: (filePath: String, title: String) -> Unit,
    onCopyText: (label: String, text: String) -> Unit,
    onShareAdText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val garmentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5),
        onResult = { uris -> onAddGarmentUris(uris) }
    )

    var inspectedImage by remember { mutableStateOf<Pair<String, String>?>(null) }

    inspectedImage?.let { (path, title) ->
        FullScreenImageDialog(
            imagePath = path,
            title = title,
            onDismiss = { inspectedImage = null }
        )
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(0.44f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        StudioInputPane(
                            activeState = activeState,
                            customModelRefPath = customModelRefPath,
                            customBedroomRefPath = customBedroomRefPath,
                            onPickGarmentPhotos = {
                                garmentPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveGarmentUri = onRemoveGarmentUri,
                            onUpdateNotes = onUpdateNotes,
                            onStartShoot = onStartShoot,
                            onStartAdOnly = onStartAdOnly,
                            onResetStudio = onResetStudio,
                            onOpenReferencesTab = onOpenReferencesTab
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(0.56f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    item {
                        StudioOutputPane(
                            activeState = activeState,
                            onInspectImage = { path, title -> inspectedImage = path to title },
                            onSaveToGallery = onSaveToGallery,
                            onShareImage = onShareImage,
                            onRegeneratePose = onRegeneratePose,
                            onRegenerateAd = onRegenerateAd,
                            onCopyText = onCopyText,
                            onShareAdText = onShareAdText
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 640.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        StudioInputPane(
                            activeState = activeState,
                            customModelRefPath = customModelRefPath,
                            customBedroomRefPath = customBedroomRefPath,
                            onPickGarmentPhotos = {
                                garmentPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onRemoveGarmentUri = onRemoveGarmentUri,
                            onUpdateNotes = onUpdateNotes,
                            onStartShoot = onStartShoot,
                            onStartAdOnly = onStartAdOnly,
                            onResetStudio = onResetStudio,
                            onOpenReferencesTab = onOpenReferencesTab
                        )
                    }

                    if (activeState.hasAnyResults || activeState.isRunningPipeline) {
                        item {
                            StudioOutputPane(
                                activeState = activeState,
                                onInspectImage = { path, title -> inspectedImage = path to title },
                                onSaveToGallery = onSaveToGallery,
                                onShareImage = onShareImage,
                                onRegeneratePose = onRegeneratePose,
                                onRegenerateAd = onRegenerateAd,
                                onCopyText = onCopyText,
                                onShareAdText = onShareAdText
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StudioInputPane(
    activeState: ActiveShootState,
    customModelRefPath: String?,
    customBedroomRefPath: String?,
    onPickGarmentPhotos: () -> Unit,
    onRemoveGarmentUri: (Uri) -> Unit,
    onUpdateNotes: (brand: String, size: String, condition: String, customRoom: String) -> Unit,
    onStartShoot: () -> Unit,
    onStartAdOnly: () -> Unit,
    onResetStudio: () -> Unit,
    onOpenReferencesTab: () -> Unit
) {
    var notesExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioSectionHeader(
            eyebrow = "AI Fashion Photography Studio",
            title = "New Vinted Shoot",
            subtitle = "Upload your garment photos to generate 3 consistent 2:3 studio poses and a ready-to-paste Dutch Vinted listing."
        )

        // Active Studio Reference Strip
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenReferencesTab() }
                    .padding(12.dp)
                    .testTag("studio_references_strip"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReferenceThumb(
                    customPath = customModelRefPath,
                    defaultResId = R.drawable.default_model_bedroom_ref,
                    contentDescription = stringResource(R.string.cd_model_reference)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ReferenceThumb(
                    customPath = customBedroomRefPath,
                    defaultResId = R.drawable.default_bedroom_ref,
                    contentDescription = stringResource(R.string.cd_bedroom_reference)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Locked Studio References",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append(if (customModelRefPath == null) "Default Brunette Model" else "Custom Model")
                            append(" · ")
                            append(if (customBedroomRefPath == null) "Dutch Bedroom" else "Custom Room")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Edit references",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Garment Upload Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Original Garment Photos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Add front, back & label tag photos (up to 5)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${activeState.selectedGarmentUris.size}/5",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeState.selectedGarmentUris.isEmpty()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !activeState.isRunningPipeline) { onPickGarmentPhotos() }
                            .testTag("empty_garment_upload_box")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 28.dp, horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checkroom,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to select garment photos",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Include fabric texture, sleeves, back & care/size label if available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(activeState.selectedGarmentUris) { uri ->
                            Box(
                                modifier = Modifier
                                    .size(width = 100.dp, height = 132.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = stringResource(R.string.cd_garment_photo),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (!activeState.isRunningPipeline) {
                                    IconButton(
                                        onClick = { onRemoveGarmentUri(uri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(32.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FilledTonalButton(
                    onClick = onPickGarmentPhotos,
                    enabled = !activeState.isRunningPipeline && activeState.selectedGarmentUris.size < 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("add_garment_photos_button")
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (activeState.selectedGarmentUris.isEmpty()) "Select Garment Photos"
                        else "Add More Garment Photos"
                    )
                }
            }
        }

        // Optional Verified Details & Room Override Card
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { notesExpanded = !notesExpanded }
                        .padding(vertical = 4.dp)
                        .testTag("toggle_verified_notes_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Optional Label & Room Notes",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Verified brand, size, condition, or custom room request",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (notesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (notesExpanded) "Collapse notes" else "Expand notes"
                    )
                }

                AnimatedVisibility(visible = notesExpanded) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        OutlinedTextField(
                            value = activeState.userNotes.brand,
                            onValueChange = {
                                onUpdateNotes(
                                    it,
                                    activeState.userNotes.sizeFromLabel,
                                    activeState.userNotes.condition,
                                    activeState.userNotes.customRoomNote
                                )
                            },
                            label = { Text("Verified Brand (optional)") },
                            placeholder = { Text("e.g. Zara, Mango — leave blank if unknown") },
                            singleLine = true,
                            enabled = !activeState.isRunningPipeline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("brand_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = activeState.userNotes.sizeFromLabel,
                                onValueChange = {
                                    onUpdateNotes(
                                        activeState.userNotes.brand,
                                        it,
                                        activeState.userNotes.condition,
                                        activeState.userNotes.customRoomNote
                                    )
                                },
                                label = { Text("Label Size") },
                                placeholder = { Text("e.g. S / 36") },
                                singleLine = true,
                                enabled = !activeState.isRunningPipeline,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("size_input")
                            )
                            OutlinedTextField(
                                value = activeState.userNotes.condition,
                                onValueChange = {
                                    onUpdateNotes(
                                        activeState.userNotes.brand,
                                        activeState.userNotes.sizeFromLabel,
                                        it,
                                        activeState.userNotes.customRoomNote
                                    )
                                },
                                label = { Text("Condition") },
                                placeholder = { Text("e.g. Zeer goed") },
                                singleLine = true,
                                enabled = !activeState.isRunningPipeline,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("condition_input")
                            )
                        }
                        OutlinedTextField(
                            value = activeState.userNotes.customRoomNote,
                            onValueChange = {
                                onUpdateNotes(
                                    activeState.userNotes.brand,
                                    activeState.userNotes.sizeFromLabel,
                                    activeState.userNotes.condition,
                                    it
                                )
                            },
                            label = { Text("Custom Room Request (optional)") },
                            placeholder = { Text("Leave blank to keep the exact Dutch bedroom") },
                            enabled = !activeState.isRunningPipeline,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_room_input")
                        )
                    }
                }
            }
        }

        // Primary Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStartShoot,
                enabled = activeState.selectedGarmentUris.isNotEmpty() && !activeState.isRunningPipeline,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("generate_shoot_button"),
                contentPadding = PaddingValues(vertical = 15.dp, horizontal = 16.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (activeState.isRunningPipeline) "Photoshoot Running..." else "Generate 3 Photos + Vinted Ad",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (activeState.hasAnyResults && !activeState.isRunningPipeline) {
                OutlinedButton(
                    onClick = onResetStudio,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("reset_studio_button"),
                    contentPadding = PaddingValues(vertical = 15.dp, horizontal = 14.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Start new garment shoot")
                }
            }
        }

        OutlinedButton(
            onClick = onStartAdOnly,
            enabled = activeState.selectedGarmentUris.isNotEmpty() && !activeState.isRunningPipeline,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("generate_ad_only_free_button"),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "Write Dutch Vinted Ad Only (Free Tier)",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (activeState.isRunningPipeline) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                activeState.statusBanner?.let { banner ->
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioOutputPane(
    activeState: ActiveShootState,
    onInspectImage: (String, String) -> Unit,
    onSaveToGallery: (String, String) -> Unit,
    onShareImage: (String, String) -> Unit,
    onRegeneratePose: (PoseType) -> Unit,
    onRegenerateAd: () -> Unit,
    onCopyText: (String, String) -> Unit,
    onShareAdText: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!activeState.hasAnyResults && !activeState.isRunningPipeline) {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Studio Output Ready",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Three independent 2:3 fashion photos (Front, Back, Seated) and your Dutch Vinted ad will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        val hasPoseActivity = activeState.frontPose !is com.example.model.PoseSlotState.Idle ||
            activeState.backPose !is com.example.model.PoseSlotState.Idle ||
            activeState.seatedPose !is com.example.model.PoseSlotState.Idle

        if (hasPoseActivity) {
            StudioSectionHeader(
                eyebrow = "Deliverables",
                title = "Three Separate Fashion Photos",
                subtitle = "Each pose is generated individually using the exact model, Dutch bedroom, and uploaded garment."
            )

            PoseImageCard(
                poseType = PoseType.STANDING_FRONT,
                slotState = activeState.frontPose,
                onInspectImage = onInspectImage,
                onSaveToGallery = onSaveToGallery,
                onShareImage = onShareImage,
                onRegenerate = { onRegeneratePose(PoseType.STANDING_FRONT) },
                canRegenerate = !activeState.isRunningPipeline
            )

            PoseImageCard(
                poseType = PoseType.STANDING_BACK,
                slotState = activeState.backPose,
                onInspectImage = onInspectImage,
                onSaveToGallery = onSaveToGallery,
                onShareImage = onShareImage,
                onRegenerate = { onRegeneratePose(PoseType.STANDING_BACK) },
                canRegenerate = !activeState.isRunningPipeline
            )

            PoseImageCard(
                poseType = PoseType.SEATED_LIFESTYLE,
                slotState = activeState.seatedPose,
                onInspectImage = onInspectImage,
                onSaveToGallery = onSaveToGallery,
                onShareImage = onShareImage,
                onRegenerate = { onRegeneratePose(PoseType.SEATED_LIFESTYLE) },
                canRegenerate = !activeState.isRunningPipeline
            )
        }

        OriginalPhotosRow(
            photoPaths = activeState.savedOriginalPaths,
            onInspectImage = onInspectImage,
            onSaveToGallery = onSaveToGallery
        )

        DutchAdCard(
            adState = activeState.adState,
            onCopyText = onCopyText,
            onShareAdText = onShareAdText,
            onRetryAd = onRegenerateAd,
            canRetry = !activeState.isRunningPipeline
        )
    }
}

@Composable
private fun ReferenceThumb(
    customPath: String?,
    defaultResId: Int,
    contentDescription: String
) {
    if (customPath != null && File(customPath).exists()) {
        AsyncImage(
            model = File(customPath),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.small)
        )
    } else {
        Image(
            painter = painterResource(id = defaultResId),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(MaterialTheme.shapes.small)
        )
    }
}
