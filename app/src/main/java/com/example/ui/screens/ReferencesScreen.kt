package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.StudioSectionHeader
import java.io.File

@Composable
fun ReferencesScreen(
    customModelRefPath: String?,
    customBedroomRefPath: String?,
    onChangeModelRef: (Uri) -> Unit,
    onResetModelRef: () -> Unit,
    onChangeBedroomRef: (Uri) -> Unit,
    onResetBedroomRef: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modelPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onChangeModelRef) }
    )
    val bedroomPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(onChangeBedroomRef) }
    )

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
                StudioSectionHeader(
                    eyebrow = "Consistency Anchor",
                    title = "Model & Bedroom References",
                    subtitle = "Pre-loaded with your brunette model and Dutch-style bedroom so every garment shoot looks cohesive."
                )
            }

            item {
                ReferenceEditorCard(
                    title = "Model Identity Reference",
                    subtitle = "Maintains exact face, proportions, long brown hair, and friendly natural expression across all poses.",
                    icon = Icons.Default.Face,
                    isCustom = customModelRefPath != null,
                    customPath = customModelRefPath,
                    defaultResId = R.drawable.default_model_bedroom_ref,
                    previewAspectRatio = 3f / 4f,
                    contentDescription = stringResource(R.string.cd_model_reference),
                    changeButtonTag = "change_model_ref_button",
                    resetButtonTag = "reset_model_ref_button",
                    onPickNew = {
                        modelPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onResetDefault = onResetModelRef
                )
            }

            item {
                ReferenceEditorCard(
                    title = "Dutch Bedroom Setting Reference",
                    subtitle = "Keeps bed, bedding, window, blind, shelving, dresser, flooring, lighting, and proportions identical across all 3 photos.",
                    icon = Icons.Default.Bed,
                    isCustom = customBedroomRefPath != null,
                    customPath = customBedroomRefPath,
                    defaultResId = R.drawable.default_bedroom_ref,
                    previewAspectRatio = 3f / 2f,
                    contentDescription = stringResource(R.string.cd_bedroom_reference),
                    changeButtonTag = "change_bedroom_ref_button",
                    resetButtonTag = "reset_bedroom_ref_button",
                    onPickNew = {
                        bedroomPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onResetDefault = onResetBedroomRef
                )
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Active Studio Standards",
                            style = MaterialTheme.typography.titleSmall
                        )
                        StudioRuleLine("3 separate 2:3 portrait photos (Standing Front, Standing Back, Seated Lifestyle) — never collages.")
                        StudioRuleLine("Authoritative garment reproduction — exact colours, knit structure, collar, seams, and fit.")
                        StudioRuleLine("Original product photos preserved alongside AI presentation shots.")
                        StudioRuleLine("Dutch Vinted listing without hashtags and zero invented brands, materials, or measurements.")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ReferenceEditorCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isCustom: Boolean,
    customPath: String?,
    defaultResId: Int,
    previewAspectRatio: Float,
    contentDescription: String,
    changeButtonTag: String,
    resetButtonTag: String,
    onPickNew: () -> Unit,
    onResetDefault: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isCustom) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    }
                ) {
                    Text(
                        text = if (isCustom) "Custom" else "Studio Default",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCustom) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(previewAspectRatio)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                if (customPath != null && File(customPath).exists()) {
                    AsyncImage(
                        model = File(customPath),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Image(
                        painter = painterResource(id = defaultResId),
                        contentDescription = contentDescription,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = onPickNew,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag(changeButtonTag)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Photo")
                }

                if (isCustom) {
                    OutlinedButton(
                        onClick = onResetDefault,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag(resetButtonTag)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Default")
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioRuleLine(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
