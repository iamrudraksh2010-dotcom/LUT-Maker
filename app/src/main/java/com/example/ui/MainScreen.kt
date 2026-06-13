package com.example.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.GradingParams
import com.example.viewmodel.LutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LutViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Observe State flows
    val originalBitmap by viewModel.originalPreviewBitmap.collectAsState()
    val processedBitmap by viewModel.processedPreviewBitmap.collectAsState()
    val sourceType by viewModel.sourceType.collectAsState()
    val videoFrames by viewModel.videoFrames.collectAsState()
    val selectedFrameIndex by viewModel.selectedFrameIndex.collectAsState()
    val gradingParams by viewModel.gradingParams.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val isCompareMode by viewModel.isCompareMode.collectAsState()
    val splitCompareRatio by viewModel.splitCompareRatio.collectAsState()
    val savedPresets by viewModel.savedPresets.collectAsState()

    // Dialog trigger states
    var showExportDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    // Media picking launchers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadImage(uri)
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadVideo(uri)
        }
    }

    // Handles toast displays
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    ScaffoldWithEdgeToEdge(modifier = modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background) // Sophisticated Dark Background
        ) {
            // WORKSPACE HEADER
            HeaderArea(
                sourceType = sourceType,
                isCompareMode = isCompareMode,
                onToggleCompare = { viewModel.setCompareMode(!isCompareMode) },
                onReset = { viewModel.resetParams() },
                onImportImage = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onImportVideo = {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onSavePresetClick = { if (sourceType != "NONE") showSavePresetDialog = true },
                onExportClick = { if (sourceType != "NONE") showExportDialog = true },
                paramsDefault = gradingParams.isDefaults(),
                onHelpClick = { showHelpDialog = true }
            )

            // VIEWPORT AREA / PREVIEW VISUALIZER
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (sourceType == "NONE") {
                    EmptyStatePlaceholder(
                        onImportImage = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onImportVideo = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )
                } else {
                    val orig = originalBitmap
                    val proc = processedBitmap

                    if (orig != null && proc != null) {
                        SplitCompareViewer(
                            original = orig,
                            processed = proc,
                            splitRatio = splitCompareRatio,
                            isCompareMode = isCompareMode,
                            onRatioChanged = { viewModel.setSplitRatio(it) }
                        )

                        // Mode indicator label
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .border(BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.3f)), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isCompareMode) "BEFORE | AFTER (DRAG)" else "GRADING ACTIVE",
                                color = if (isCompareMode) Color(0xFF00B0FF) else Color(0xFFFF5722),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    } else {
                        CircularProgressIndicator(
                            color = Color(0xFFFF5722),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Global Processing Indicator overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = isProcessing,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFFFF5722), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Processing...", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // VIDEO TIMELINE REFERENCE FRAME SELECTOR (Only visible for videos)
            if (sourceType == "VIDEO" && videoFrames.isNotEmpty()) {
                VideoFrameTimeline(
                    frames = videoFrames,
                    selectedIndex = selectedFrameIndex,
                    onFrameSelected = { viewModel.selectVideoFrame(it) }
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // CONTROLS DRAWER / EDITING TABS
            if (sourceType != "NONE") {
                ControlsDrawer(
                    params = gradingParams,
                    onParamsChanged = { viewModel.updateParams(it) },
                    savedPresets = savedPresets,
                    onLoadPreset = { viewModel.loadPreset(it) },
                    onDeletePreset = { viewModel.deletePreset(it) },
                    onExportImage = {
                        viewModel.exportOriginalGradedImage("lut_graded_${System.currentTimeMillis()}") {
                            // Graded image saved successfully callback
                        }
                    },
                    sourceType = sourceType
                )
            } else {
                // Empty controls helper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color(0xFF13151B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Insert an Image or Video above to reveal professional grading wheels.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }

    // EXPORT LUT .CUBE DIALOG
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onConfirmExport = { name, size ->
                viewModel.exportLut(name, size)
                showExportDialog = false
            }
        )
    }

    // SAVE CUSTOM PRESET DIALOG
    if (showSavePresetDialog) {
        SavePresetDialog(
            onDismiss = { showSavePresetDialog = false },
            onSave = { name ->
                viewModel.savePreset(name)
                showSavePresetDialog = false
            }
        )
    }

    // HELP / APK DIRECT INSTALLATION GUIDE DIALOG
    if (showHelpDialog) {
        HelpDialog(
            onDismiss = { showHelpDialog = false }
        )
    }
}

@Composable
fun ScaffoldWithEdgeToEdge(
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val padding = PaddingValues(0.dp)
        content(padding)
    }
}

@Composable
fun HeaderArea(
    sourceType: String,
    isCompareMode: Boolean,
    onToggleCompare: () -> Unit,
    onReset: () -> Unit,
    onImportImage: () -> Unit,
    onImportVideo: () -> Unit,
    onSavePresetClick: () -> Unit,
    onExportClick: () -> Unit,
    paramsDefault: Boolean,
    onHelpClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // `#1A1C1E` Header Background
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Main App Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, // `#D1E4FF` Accent
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LUT MAKER",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurface // `#E1E2E4` Text
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "APK Installation Guide",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Quick Import Buttons
            Row {
                Button(
                    onClick = onImportImage,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background // `#0E1113` Inner button
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // `#2F3033` Border
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("import_image_button")
                ) {
                    Icon(Icons.Default.Photo, contentDescription = "Import Photo", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Photo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = onImportVideo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.background // `#0E1113` Inner button
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // `#2F3033` Border
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("import_video_button")
                ) {
                    Icon(Icons.Default.Movie, contentDescription = "Import Video", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tool bar for adjustments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sourceType != "NONE") {
                    // Compare Toggle
                    IconButton(
                        onClick = onToggleCompare,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isCompareMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            contentColor = if (isCompareMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isCompareMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                ),
                                CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Compare, contentDescription = "Compare Toggle", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    // Reset adjustment values Button
                    IconButton(
                        onClick = onReset,
                        enabled = !paramsDefault,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = if (!paramsDefault) MaterialTheme.colorScheme.primary else Color.DarkGray
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (!paramsDefault) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
                                ),
                                CircleShape
                            )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset values", modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Export Actions
            if (sourceType != "NONE") {
                Row {
                    Button(
                        onClick = onSavePresetClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("save_preset_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Preset", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("export_lut_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export LUT", fontSize = 11.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(
    onImportImage: () -> Unit,
    onImportVideo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Create Professional 3D LUTs",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Adjust exposure, color balance, HSL arrays, and custom curves in original quality. Export directly to .cube files.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.Center) {
            Card(
                onClick = onImportImage,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .size(width = 130.dp, height = 100.dp)
                    .testTag("empty_import_image")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Outlined.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Grade Image", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Card(
                onClick = onImportVideo,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .size(width = 130.dp, height = 100.dp)
                    .testTag("empty_import_video")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(imageVector = Icons.Outlined.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Grade Video", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VideoFrameTimeline(
    frames: List<Bitmap>,
    selectedIndex: Int,
    onFrameSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF13151B))
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("VIDEO TIMELINE REFERENCE FRAMES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            frames.forEachIndexed { index, frame ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 54.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            BorderStroke(
                                2.dp,
                                if (isSelected) Color(0xFFFF5722) else Color.Transparent
                            ),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onFrameSelected(index) }
                ) {
                    Image(
                        bitmap = frame.asImageBitmap(),
                        contentDescription = "Keyframe $index",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Stamp for Frame index
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("Frame ${index + 1}", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFF5722).copy(alpha = 0.15f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplitCompareViewer(
    original: Bitmap,
    processed: Bitmap,
    splitRatio: Float,
    isCompareMode: Boolean,
    onRatioChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val width = constraints.maxWidth.toFloat()
        val maxW = maxWidth
        val maxH = maxHeight

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 500.dp) // Maintain pleasant ratios on large foldables/tablets
                .clipToBounds()
        ) {
            // Underneath Layer: Original
            Image(
                bitmap = original.asImageBitmap(),
                contentDescription = "Original View",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            // Top Layer: Processed, manually clipped by canvas bounds
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (isCompareMode) {
                            drawIntoCanvas { canvas ->
                                canvas.save()
                                val clipW = size.width * splitRatio
                                canvas.clipRect(
                                    androidx.compose.ui.geometry.Rect(0f, 0f, clipW, size.height)
                                )
                                drawContent()
                                canvas.restore()
                            }
                        } else {
                            drawContent()
                        }
                    }
            ) {
                Image(
                    bitmap = processed.asImageBitmap(),
                    contentDescription = "Graded View",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Divider and drag handle
            if (isCompareMode) {
                val handlePositionX = maxW * splitRatio

                // Split screen marker line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .absoluteOffset(x = handlePositionX - 1.dp)
                        .background(Color.White.copy(alpha = 0.8f))
                )

                // Drag orb indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .absoluteOffset(
                            x = handlePositionX - 20.dp,
                            y = (maxH / 2) - 20.dp
                        )
                        .shadow(elevation = 6.dp, shape = CircleShape)
                        .background(Color.White, shape = CircleShape)
                        .border(BorderStroke(1.5.dp, Color(0xFF00B0FF)), CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newRatio = (splitRatio + (dragAmount.x / width)).coerceIn(0f, 1f)
                                onRatioChanged(newRatio)
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                        contentDescription = "Drag to compare slider",
                        tint = Color(0xFF13151B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ControlsDrawer(
    params: GradingParams,
    onParamsChanged: ((GradingParams) -> GradingParams) -> Unit,
    savedPresets: List<com.example.data.PresetEntity>,
    onLoadPreset: (GradingParams) -> Unit,
    onDeletePreset: (Int) -> Unit,
    onExportImage: () -> Unit,
    sourceType: String
) {
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("TONE", "COLOR", "LUT PRESETS", "USER SAVES")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
            .background(MaterialTheme.colorScheme.surface) // `#1A1C1E` Controls Drawer Base
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // `#2F3033` Border
    ) {
        // Tab Headers
        ScrollableTabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = MaterialTheme.colorScheme.primary // `#D1E4FF` Tab Indicator Accent
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = activeTab == index
                Tab(
                    selected = isSelected,
                    onClick = { activeTab = index },
                    modifier = Modifier.height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        val icon = when (index) {
                            0 -> Icons.Default.LightMode
                            1 -> Icons.Default.ColorLens
                            2 -> Icons.Default.AutoAwesome
                            else -> Icons.Default.FolderOpen
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Active control panel content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // Tone panel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LutSlider(
                            label = "Exposure",
                            value = params.exposure,
                            valueRange = -2f..2f,
                            displayValue = { String.format(java.util.Locale.US, "%.2f EV", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(exposure = v) } },
                            testTag = "slider_exposure"
                        )
                        LutSlider(
                            label = "Contrast",
                            value = params.contrast,
                            valueRange = 0.5f..2.0f,
                            displayValue = { String.format(java.util.Locale.US, "%.2fx", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(contrast = v) } },
                            testTag = "slider_contrast"
                        )
                        LutSlider(
                            label = "Brightness",
                            value = params.brightness,
                            valueRange = -0.5f..0.5f,
                            displayValue = { String.format(java.util.Locale.US, "%+.1f", it * 100) },
                            onValueChange = { v -> onParamsChanged { it.copy(brightness = v) } },
                            testTag = "slider_brightness"
                        )
                        LutSlider(
                            label = "Gamma",
                            value = params.gamma,
                            valueRange = 0.5f..2.0f,
                            displayValue = { String.format(java.util.Locale.US, "%.2f", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(gamma = v) } },
                            testTag = "slider_gamma"
                        )
                        LutSlider(
                            label = "Highlights Recovery",
                            value = params.highlights,
                            valueRange = 0.0f..2.0f,
                            displayValue = { String.format(java.util.Locale.US, "%.2fx", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(highlights = v) } },
                            testTag = "slider_highlights"
                        )
                        LutSlider(
                            label = "Shadows Lift",
                            value = params.shadows,
                            valueRange = 0.0f..2.0f,
                            displayValue = { String.format(java.util.Locale.US, "%.2fx", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(shadows = v) } },
                            testTag = "slider_shadows"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                1 -> {
                    // Color panel
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))
                        LutSlider(
                            label = "Saturation",
                            value = params.saturation,
                            valueRange = 0.0f..2.0f,
                            displayValue = { String.format(java.util.Locale.US, "%.2f", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(saturation = v) } },
                            testTag = "slider_saturation"
                        )
                        LutSlider(
                            label = "Vibrance Boost",
                            value = params.vibrance,
                            valueRange = -1.0f..1.0f,
                            displayValue = { String.format(java.util.Locale.US, "%+.0f%%", it * 100) },
                            onValueChange = { v -> onParamsChanged { it.copy(vibrance = v) } },
                            testTag = "slider_vibrance"
                        )
                        LutSlider(
                            label = "Temperature (Cool - Warm)",
                            value = params.temperature,
                            valueRange = -1.0f..1.0f,
                            displayValue = { String.format(java.util.Locale.US, "%+d K", (it * 3000).toInt()) },
                            onValueChange = { v -> onParamsChanged { it.copy(temperature = v) } },
                            testTag = "slider_temperature"
                        )
                        LutSlider(
                            label = "Tint (Green - Magenta)",
                            value = params.tint,
                            valueRange = -1.0f..1.0f,
                            displayValue = { String.format(java.util.Locale.US, "%+d Tint", (it * 100).toInt()) },
                            onValueChange = { v -> onParamsChanged { it.copy(tint = v) } },
                            testTag = "slider_tint"
                        )
                        Text(
                            text = "RGB CHANNEL MIXER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            letterSpacing = 0.5.sp
                        )
                        LutSlider(
                            label = "Red Balance",
                            value = params.redBalance,
                            valueRange = 0.5f..1.5f,
                            displayValue = { String.format(java.util.Locale.US, "%.2fs", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(redBalance = v) } },
                            testTag = "slider_red"
                        )
                        LutSlider(
                            label = "Green Balance",
                            value = params.greenBalance,
                            valueRange = 0.5f..1.5f,
                            displayValue = { String.format(java.util.Locale.US, "%.2fs", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(greenBalance = v) } },
                            testTag = "slider_green"
                        )
                        LutSlider(
                            label = "Blue Balance",
                            value = params.blueBalance,
                            valueRange = 0.5f..1.5f,
                            displayValue = { String.format(java.util.Locale.US, "%.2fs", it) },
                            onValueChange = { v -> onParamsChanged { it.copy(blueBalance = v) } },
                            testTag = "slider_blue"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                2 -> {
                    // Creative Presets
                    val presets = listOf(
                        "None", "Teal & Orange", "Vintage", "Cyberpunk",
                        "Cinematic", "Noir", "B&W", "Warm Golden",
                        "Cold Breeze", "Forest Green", "Acid Sunset"
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "CHOOSE BASIS LOOK MODEL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                        )

                        // Grid list of presets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presets.forEach { name ->
                                val isSelected = params.selectedPreset == name
                                Card(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 70.dp)
                                        .testTag("preset_$name")
                                        .clickable {
                                            onParamsChanged { it.copy(selectedPreset = name) }
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background
                                    ),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    when (name) {
                                                        "Teal & Orange" -> Color(0xFF00B0FF)
                                                        "Vintage" -> Color(0xFFD4AF37)
                                                        "Cyberpunk" -> Color(0xFFFF007F)
                                                        "Cinematic" -> Color(0xFF00FFCC)
                                                        "Noir", "B&W" -> Color.LightGray
                                                        "Warm Golden" -> Color(0xFFFFA000)
                                                        "Cold Breeze" -> Color(0xFFE0F7FA)
                                                        "Forest Green" -> Color(0xFF2E7D32)
                                                        "Acid Sunset" -> Color(0xFF8E24AA)
                                                        else -> Color.DarkGray
                                                    },
                                                    CircleShape
                                                )
                                        )
                                        Text(
                                            text = name,
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (params.selectedPreset != "None") {
                            Spacer(modifier = Modifier.height(14.dp))
                            LutSlider(
                                label = "${params.selectedPreset} Blend Intensity",
                                value = params.presetIntensity,
                                valueRange = 0f..1f,
                                displayValue = { String.format(java.util.Locale.US, "%.0f%%", it * 100) },
                                onValueChange = { v -> onParamsChanged { it.copy(presetIntensity = v) } },
                                testTag = "preset_intensity"
                            )
                        }
                    }
                }
                else -> {
                    // Saved presets list
                    if (savedPresets.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No custom presets saved yet.", color = Color.Gray, fontSize = 12.sp)
                            Text("Dial sliders and save above to keep look catalogs.", color = Color.DarkGray, fontSize = 10.sp)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "TAP TO RECALL PROFILE LOOKS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            savedPresets.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                                        .clickable { onLoadPreset(item.toParams()) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "Created: ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.US).format(java.util.Date(item.timestamp))}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Load Preset",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .padding(horizontal = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        IconButton(
                                            onClick = { onDeletePreset(item.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete preset", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SAVE HIGH-RES COMPLETED IMAGE CONTROL
        if (sourceType == "IMAGE") {
            Button(
                onClick = onExportImage,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .height(44.dp)
                    .testTag("save_image_gallery"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export High-Quality Photo to Gallery", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun LutSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: (Float) -> String,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text(displayValue(value), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Slider(
                value = value,
                valueRange = valueRange,
                onValueChange = onValueChange,
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline,
                    thumbColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .height(28.dp)
                    .testTag(testTag)
            )
        }
    }
}

@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onConfirmExport: (String, Int) -> Unit
) {
    var filename by remember { mutableStateOf("MyAwesomeLook") }
    var size by remember { mutableStateOf(32) } // default industry size
    val sizes = listOf(16, 32, 64)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Export .cube 3D LUT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Exported files are universal and can be loaded into Premiere, FCPX, DaVinci, Lightroom, or OBS Studio.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Filename Input
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    label = { Text("LUT Filename", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lut_filename_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Block Size Picker
                Text(
                    text = "SELECT 3D GRID RESOLUTION",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sizes.forEach { s ->
                        val isSel = size == s
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                .border(BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline), RoundedCornerShape(6.dp))
                                .clickable { size = s }
                        ) {
                            Text(
                                text = "${s}x${s}x${s} (${if (s == 32) "Std" else if (s == 64) "Pro" else "Lite"})",
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (filename.isNotBlank()) {
                                onConfirmExport(filename.trim(), size)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Export to Downloads", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("My Custom Look") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Save Custom Preset",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This will bake all slider settings so you can load them onto other photos or video keyframes instantly.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (presetName.isNotBlank()) {
                                onSave(presetName.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save Preset", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun HelpDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Direct Install APK Guide",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A standalone, installable Android file (debug.apk) is built in real-time each time code is compiled. This allows you to test current modifications directly on physical Android phones or other emulators!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HelpStepItem(
                        step = "1",
                        title = "Locate debug.apk",
                        description = "Once compile completes, you can find the 'debug.apk' file directly in the workspace root directory of the editor."
                    )
                    HelpStepItem(
                        step = "2",
                        title = "Download debug.apk",
                        description = "Right-click 'debug.apk' from the file tree sidebar to download it, or download the workspace as a ZIP package."
                    )
                    HelpStepItem(
                        step = "3",
                        title = "Install APK",
                        description = "Transfer the file to your Android phone, click it to install (enable 'Install Unknown Apps' if requested), and explore features!"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HelpStepItem(step: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 14.sp)
        }
    }
}
