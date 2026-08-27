package com.sld.beads

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sld.beads.data.BeadGridSize
import com.sld.beads.data.BeadColumnWindow
import com.sld.beads.data.ConfigurationField
import com.sld.beads.data.ConfigurationValidationException
import com.sld.beads.data.ImageDao
import com.sld.beads.data.ImageRecord
import com.sld.beads.data.ImageRepository
import com.sld.beads.data.WorkingArea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

data class ConfigurationFormState(
    val values: Map<ConfigurationField, String> = ConfigurationField.entries.associateWith { "" },
    val errors: Map<ConfigurationField, String> = emptyMap(),
    val isSaving: Boolean = false,
    val wasSaved: Boolean = false
)

sealed interface WorkingColumnState {
    data object Idle : WorkingColumnState
    data object Loading : WorkingColumnState
    data class Content(val window: BeadColumnWindow) : WorkingColumnState
    data class Error(val message: String) : WorkingColumnState
}

class ImagePickerViewModel(
    private val repository: ImageRepository,
    imageDao: ImageDao
) : ViewModel() {
    private val selectedImageId = MutableStateFlow<String?>(null)

    val latestImage: StateFlow<ImageRecord?> = combine(
        imageDao.observeAll(),
        selectedImageId
    ) { images, selectedId ->
        images.firstOrNull { it.id == selectedId }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val configuredImages: StateFlow<List<ImageRecord>> = imageDao.observeConfigured()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _deletingImageIds = MutableStateFlow<Set<String>>(emptySet())
    val deletingImageIds: StateFlow<Set<String>> = _deletingImageIds

    private val _deletionError = MutableStateFlow<String?>(null)
    val deletionError: StateFlow<String?> = _deletionError

    private val _workingColumnState = MutableStateFlow<WorkingColumnState>(WorkingColumnState.Idle)
    val workingColumnState: StateFlow<WorkingColumnState> = _workingColumnState

    private val _isChangingWorkingColumn = MutableStateFlow(false)
    val isChangingWorkingColumn: StateFlow<Boolean> = _isChangingWorkingColumn

    private val _savedImageIds = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val savedImageIds: SharedFlow<String> = _savedImageIds

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _formState = MutableStateFlow(ConfigurationFormState())
    val formState: StateFlow<ConfigurationFormState> = _formState

    private var loadedImageId: String? = null
    private var isCreateActive = false
    private var workingColumnJob: Job? = null
    private var workingColumnCapacity = 1
    private var workingColumnRequestId = 0

    init {
        viewModelScope.launch {
            latestImage.collect { image ->
                if (image != null && image.id != loadedImageId) {
                    loadedImageId = image.id
                    _formState.value = ConfigurationFormState(
                        values = mapOf(
                            ConfigurationField.WORKING_AREA_X to image.workingAreaX.asInput(),
                            ConfigurationField.WORKING_AREA_Y to image.workingAreaY.asInput(),
                            ConfigurationField.WORKING_AREA_WIDTH to image.workingAreaWidth.asInput(),
                            ConfigurationField.WORKING_AREA_HEIGHT to image.workingAreaHeight.asInput(),
                            ConfigurationField.BEAD_WIDTH to image.beadWidth.asInput(),
                            ConfigurationField.BEAD_HEIGHT to image.beadHeight.asInput()
                        )
                    )
                }
            }
        }
    }

    fun startCreate() {
        if (isCreateActive) return
        isCreateActive = true
        selectedImageId.value = null
        loadedImageId = null
        _formState.value = ConfigurationFormState()
        _errorMessage.value = null
    }

    fun importImage(uri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            _errorMessage.value = null
            selectedImageId.value?.let { repository.discardIncompleteImage(it) }
            selectedImageId.value = null
            loadedImageId = null
            _formState.value = ConfigurationFormState()
            repository.importImage(uri)
                .onSuccess { image ->
                    if (isCreateActive) {
                        selectedImageId.value = image.id
                    } else {
                        repository.discardIncompleteImage(image.id)
                    }
                }
                .onFailure { _errorMessage.value = it.message ?: "Could not save the selected image" }
            _isImporting.value = false
        }
    }

    fun updateConfigurationField(field: ConfigurationField, value: String) {
        if (value.isNotEmpty() && !value.all(Char::isDigit)) return
        _formState.value = _formState.value.copy(
            values = _formState.value.values + (field to value),
            errors = _formState.value.errors - field,
            wasSaved = false
        )
    }

    fun saveConfiguration() {
        val image = latestImage.value ?: return
        if (_formState.value.isSaving) return

        val parsedValues = mutableMapOf<ConfigurationField, Int>()
        val parsingErrors = mutableMapOf<ConfigurationField, String>()
        ConfigurationField.entries.forEach { field ->
            val rawValue = _formState.value.values[field].orEmpty()
            val value = rawValue.toIntOrNull()
            if (rawValue.isBlank()) parsingErrors[field] = "Required"
            else if (value == null) parsingErrors[field] = "Enter a valid whole number"
            else parsedValues[field] = value
        }
        if (parsingErrors.isNotEmpty()) {
            _formState.value = _formState.value.copy(errors = parsingErrors, wasSaved = false)
            return
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isSaving = true, errors = emptyMap(), wasSaved = false)
            _errorMessage.value = null
            repository.saveConfiguration(
                imageId = image.id,
                workingArea = WorkingArea(
                    x = parsedValues.getValue(ConfigurationField.WORKING_AREA_X),
                    y = parsedValues.getValue(ConfigurationField.WORKING_AREA_Y),
                    width = parsedValues.getValue(ConfigurationField.WORKING_AREA_WIDTH),
                    height = parsedValues.getValue(ConfigurationField.WORKING_AREA_HEIGHT)
                ),
                beadGridSize = BeadGridSize(
                    width = parsedValues.getValue(ConfigurationField.BEAD_WIDTH),
                    height = parsedValues.getValue(ConfigurationField.BEAD_HEIGHT)
                )
            ).onSuccess {
                _formState.value = _formState.value.copy(isSaving = false, wasSaved = true)
                isCreateActive = false
                _savedImageIds.tryEmit(image.id)
            }.onFailure { error ->
                val fieldErrors = (error as? ConfigurationValidationException)?.errors.orEmpty()
                _formState.value = _formState.value.copy(isSaving = false, errors = fieldErrors)
                if (fieldErrors.isEmpty()) {
                    _errorMessage.value = error.message ?: "Could not save the configuration"
                }
            }
        }
    }

    fun cancelCreate() {
        isCreateActive = false
        val imageId = selectedImageId.value ?: return
        viewModelScope.launch {
            repository.discardIncompleteImage(imageId)
            if (selectedImageId.value == imageId) {
                selectedImageId.value = null
                loadedImageId = null
            }
        }
    }

    fun deleteImage(imageId: String) {
        if (imageId in _deletingImageIds.value) return
        viewModelScope.launch {
            _deletingImageIds.value += imageId
            _deletionError.value = null
            repository.deleteImage(imageId)
                .onFailure { error ->
                    _deletionError.value = error.message ?: "Could not delete the project"
                }
            _deletingImageIds.value -= imageId
        }
    }

    fun loadWorkingColumn(imageId: String) {
        workingColumnJob?.cancel()
        workingColumnRequestId++
        _isChangingWorkingColumn.value = false
        workingColumnCapacity = 1
        _workingColumnState.value = WorkingColumnState.Loading
        workingColumnJob = viewModelScope.launch {
            repository.loadWorkingColumnColors(imageId, workingColumnCapacity)
                .onSuccess { window ->
                    _workingColumnState.value = WorkingColumnState.Content(window)
                }
                .onFailure { error ->
                    _workingColumnState.value = WorkingColumnState.Error(
                        error.message ?: "Could not calculate bead colors"
                    )
                }
        }
    }

    fun changeWorkingColumn(imageId: String, targetColumn: Int) {
        if (_isChangingWorkingColumn.value) return
        val content = _workingColumnState.value as? WorkingColumnState.Content ?: return
        if (targetColumn !in 0 until content.window.totalColumns) return
        if (targetColumn == content.window.currentColumn) return

        workingColumnJob?.cancel()
        val requestId = ++workingColumnRequestId
        _isChangingWorkingColumn.value = true
        workingColumnJob = viewModelScope.launch {
            try {
                repository.setCurrentColumn(imageId, targetColumn)
                    .onFailure { error ->
                        _workingColumnState.value = WorkingColumnState.Error(
                            error.message ?: "Could not change the current column"
                        )
                        return@launch
                    }
                repository.loadWorkingColumnColors(imageId, workingColumnCapacity)
                    .onSuccess { window ->
                        _workingColumnState.value = WorkingColumnState.Content(window)
                    }
                    .onFailure { error ->
                        _workingColumnState.value = WorkingColumnState.Error(
                            error.message ?: "Could not calculate bead colors"
                        )
                    }
            } finally {
                if (requestId == workingColumnRequestId) {
                    _isChangingWorkingColumn.value = false
                }
            }
        }
    }

    fun updateWorkingColumnCapacity(imageId: String, capacity: Int) {
        val normalizedCapacity = capacity.coerceAtLeast(1)
        if (normalizedCapacity == workingColumnCapacity) return
        val content = _workingColumnState.value as? WorkingColumnState.Content ?: return

        workingColumnCapacity = normalizedCapacity
        workingColumnJob?.cancel()
        val requestId = ++workingColumnRequestId
        _isChangingWorkingColumn.value = true
        workingColumnJob = viewModelScope.launch {
            try {
                repository.loadWorkingColumnColors(imageId, workingColumnCapacity)
                    .onSuccess { window ->
                        _workingColumnState.value = WorkingColumnState.Content(window)
                    }
                    .onFailure { error ->
                        _workingColumnState.value = WorkingColumnState.Error(
                            error.message ?: "Could not calculate neighboring columns"
                        )
                    }
            } finally {
                if (requestId == workingColumnRequestId) {
                    _isChangingWorkingColumn.value = false
                }
            }
        }
    }

    private fun Int?.asInput(): String = this?.toString().orEmpty()
}

class ImagePickerViewModelFactory(
    private val repository: ImageRepository,
    private val imageDao: ImageDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ImagePickerViewModel::class.java))
        return ImagePickerViewModel(repository, imageDao) as T
    }
}
