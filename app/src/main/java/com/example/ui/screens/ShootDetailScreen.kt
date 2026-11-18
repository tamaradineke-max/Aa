package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AdGenerationState
import com.example.model.PoseSlotState
import com.example.model.PoseType
import com.example.model.StudioShoot
import com.example.ui.components.DutchAdCard
import com.example.ui.components.FullScreenImageDialog
import com.example.ui.components.OriginalPhotosRow
import com.example.ui.components.PoseImageCard

@Composable
fun ShootDetailScreen(
    shoot: StudioShoot?,
    onBack: () -> Unit,
    onSaveToGallery: (filePath: String, displayName: String) -> Unit,
    onShareImage: (filePath: String, title: String) -> Unit,
    onCopyText: (label: String, text: String) -> Unit,
    onShareAdText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inspectedImage by remember { mutableStateOf<Pair<String, String>?>(null) }

    inspectedImage?.let { (path, title) ->
        FullScreenImageDialog(
            imagePath = path,
            title = title,
            onDismiss = { inspectedImage = null }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("shoot_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = shoot?.ad?.title?.ifBlank { "Studio Shoot" } ?: "Studio Shoot",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (shoot == null) {
                item {
                    Text(
                        text = "Loading shoot details...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                item {
                    PoseImageCard(
                        poseType = PoseType.STANDING_FRONT,
                        slotState = shoot.frontPhotoPath?.let { PoseSlotState.Ready(it) }
                            ?: PoseSlotState.Error("Not generated"),
                        onInspectImage = { p, t -> inspectedImage = p to t },
                        onSaveToGallery = onSaveToGallery,
                        onShareImage = onShareImage,
                        onRegenerate = {},
                        canRegenerate = false
                    )
                }
                item {
                    PoseImageCard(
                        poseType = PoseType.STANDING_BACK,
                        slotState = shoot.backPhotoPath?.let { PoseSlotState.Ready(it) }
                            ?: PoseSlotState.Error("Not generated"),
                        onInspectImage = { p, t -> inspectedImage = p to t },
                        onSaveToGallery = onSaveToGallery,
                        onShareImage = onShareImage,
                        onRegenerate = {},
                        canRegenerate = false
                    )
                }
                item {
                    PoseImageCard(
                        poseType = PoseType.SEATED_LIFESTYLE,
                        slotState = shoot.seatedPhotoPath?.let { PoseSlotState.Ready(it) }
                            ?: PoseSlotState.Error("Not generated"),
                        onInspectImage = { p, t -> inspectedImage = p to t },
                        onSaveToGallery = onSaveToGallery,
                        onShareImage = onShareImage,
                        onRegenerate = {},
                        canRegenerate = false
                    )
                }
                item {
                    OriginalPhotosRow(
                        photoPaths = shoot.originalPhotoPaths,
                        onInspectImage = { p, t -> inspectedImage = p to t },
                        onSaveToGallery = onSaveToGallery
                    )
                }
                item {
                    DutchAdCard(
                        adState = shoot.ad?.let { AdGenerationState.Ready(it) }
                            ?: AdGenerationState.Error("No Dutch listing recorded."),
                        onCopyText = onCopyText,
                        onShareAdText = onShareAdText,
                        onRetryAd = {},
                        canRetry = false
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
