package com.sld.beads

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sld.beads.data.BeadGridSize
import com.sld.beads.data.ConfigurationField
import com.sld.beads.data.ConfigurationValidationException
import com.sld.beads.data.ImageDao
import com.sld.beads.data.ImageRecord
import com.sld.beads.data.ImageRepository
import com.sld.beads.data.WorkingArea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfigurationFormState(
    val values: Map<ConfigurationField, String> = ConfigurationField.entries.associateWith { "" },
    val errors: Map<ConfigurationField, String> = emptyMap(),
    val isSaving: Boolean = false,
    val wasSaved: Boolean = false
)

class ImagePickerViewModel(
    private val repository: ImageRepository,
    imageDao: ImageDao
) : ViewModel() {
    private val selectedImageId = MutableStateFlow<String?>(null)

    val latestImage: StateFlow<ImageRecord?> = combine(
        imageDao.observeAll(),
        selectedImageId
    ) { images, selectedId ->
        images.firstOrNull { it.id == selectedId } ?: images.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _formState = MutableStateFlow(ConfigurationFormState())
    val formState: StateFlow<ConfigurationFormState> = _formState

    private var loadedImageId: String? = null

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

    fun importImage(uri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            _errorMessage.value = null
            repository.importImage(uri)
                .onSuccess { selectedImageId.value = it.id }
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
            }.onFailure { error ->
                val fieldErrors = (error as? ConfigurationValidationException)?.errors.orEmpty()
                _formState.value = _formState.value.copy(isSaving = false, errors = fieldErrors)
                if (fieldErrors.isEmpty()) {
                    _errorMessage.value = error.message ?: "Could not save the configuration"
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
