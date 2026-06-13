package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PresetEntity
import com.example.data.PresetRepository
import com.example.model.GradingParams
import com.example.util.ImageProcessor
import com.example.util.LutExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PresetRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PresetRepository(db.presetDao())
    }

    val savedPresets: StateFlow<List<PresetEntity>> = repository.allPresets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _gradingParams = MutableStateFlow(GradingParams())
    val gradingParams: StateFlow<GradingParams> = _gradingParams.asStateFlow()

    private val _sourceType = MutableStateFlow("NONE") // "IMAGE", "VIDEO", "NONE"
    val sourceType: StateFlow<String> = _sourceType.asStateFlow()

    private val _sourceUri = MutableStateFlow<Uri?>(null)
    val sourceUri: StateFlow<Uri?> = _sourceUri.asStateFlow()

    private val _originalPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val originalPreviewBitmap: StateFlow<Bitmap?> = _originalPreviewBitmap.asStateFlow()

    private val _processedPreviewBitmap = MutableStateFlow<Bitmap?>(null)
    val processedPreviewBitmap: StateFlow<Bitmap?> = _processedPreviewBitmap.asStateFlow()

    private val _videoFrames = MutableStateFlow<List<Bitmap>>(emptyList())
    val videoFrames: StateFlow<List<Bitmap>> = _videoFrames.asStateFlow()

    private val _selectedFrameIndex = MutableStateFlow(0)
    val selectedFrameIndex: StateFlow<Int> = _selectedFrameIndex.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _splitCompareRatio = MutableStateFlow(1.0f) // 1.0 means full edited, 0.5 means split
    val splitCompareRatio: StateFlow<Float> = _splitCompareRatio.asStateFlow()

    private val _isCompareMode = MutableStateFlow(false)
    val isCompareMode: StateFlow<Boolean> = _isCompareMode.asStateFlow()

    private var processingJob: Job? = null

    fun setCompareMode(enabled: Boolean) {
        _isCompareMode.value = enabled
        if (!enabled) {
            _splitCompareRatio.value = 1.0f
        } else {
            _splitCompareRatio.value = 0.5f
        }
    }

    fun setSplitRatio(ratio: Float) {
        _splitCompareRatio.value = ratio
    }

    fun updateParams(update: (GradingParams) -> GradingParams) {
        _gradingParams.value = update(_gradingParams.value)
        triggerPreviewProcessing()
    }

    fun resetParams() {
        _gradingParams.value = GradingParams()
        triggerPreviewProcessing()
    }

    fun loadImage(uri: Uri) {
        _isProcessing.value = true
        _sourceType.value = "IMAGE"
        _sourceUri.value = uri
        _videoFrames.value = emptyList()
        _selectedFrameIndex.value = 0

        viewModelScope.launch {
            val downsampled = ImageProcessor.downsampleUri(getApplication(), uri, 800)
            if (downsampled != null) {
                _originalPreviewBitmap.value = downsampled
                _processedPreviewBitmap.value = downsampled
                triggerPreviewProcessing()
            } else {
                _toastMessage.value = "Failed to load image"
            }
            _isProcessing.value = false
        }
    }

    fun loadVideo(uri: Uri) {
        _isProcessing.value = true
        _sourceType.value = "VIDEO"
        _sourceUri.value = uri
        _selectedFrameIndex.value = 0

        viewModelScope.launch {
            val frames = ImageProcessor.extractFramesFromVideo(getApplication(), uri, 4)
            _videoFrames.value = frames
            if (frames.isNotEmpty()) {
                _originalPreviewBitmap.value = frames[0]
                _processedPreviewBitmap.value = frames[0]
                triggerPreviewProcessing()
            } else {
                _toastMessage.value = "Failed to extract frames from video"
            }
            _isProcessing.value = false
        }
    }

    fun selectVideoFrame(index: Int) {
        val frames = _videoFrames.value
        if (index in frames.indices) {
            _selectedFrameIndex.value = index
            _originalPreviewBitmap.value = frames[index]
            triggerPreviewProcessing()
        }
    }

    private fun triggerPreviewProcessing() {
        val original = _originalPreviewBitmap.value ?: return
        val params = _gradingParams.value

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            val processed = ImageProcessor.processBitmap(original, params)
            _processedPreviewBitmap.value = processed
        }
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            val preset = PresetEntity.fromParams(name, _gradingParams.value)
            repository.insert(preset)
            _toastMessage.value = "Preset '$name' Saved!"
        }
    }

    fun deletePreset(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
            _toastMessage.value = "Preset Deleted"
        }
    }

    fun loadPreset(params: GradingParams) {
        _gradingParams.value = params
        triggerPreviewProcessing()
        _toastMessage.value = "Preset Loaded"
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun exportLut(filename: String, size: Int): Uri? {
        val resultUri = LutExporter.exportCubeToDownloads(
            getApplication(),
            filename,
            size,
            _gradingParams.value
        )
        if (resultUri != null) {
            _toastMessage.value = "LUT exported to Downloads: $filename.cube"
        } else {
            _toastMessage.value = "Failed to export LUT"
        }
        return resultUri
    }

    fun exportOriginalGradedImage(filename: String, onSuccess: (Uri) -> Unit) {
        val uri = _sourceUri.value ?: return
        if (_sourceType.value != "IMAGE") return

        _isProcessing.value = true
        viewModelScope.launch {
            val original = ImageProcessor.loadOriginalUri(getApplication(), uri)
            if (original != null) {
                val graded = ImageProcessor.processBitmap(original, _gradingParams.value)
                val savedUri = LutExporter.saveImageToGallery(getApplication(), graded, filename)
                if (savedUri != null) {
                    _toastMessage.value = "High quality image saved to Gallery!"
                    onSuccess(savedUri)
                } else {
                    _toastMessage.value = "Failed to save image"
                }
            } else {
                _toastMessage.value = "Failed to load original image"
            }
            _isProcessing.value = false
        }
    }
}
