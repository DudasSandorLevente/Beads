package com.sld.beads

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sld.beads.data.ImageDao
import com.sld.beads.data.ImageRecord
import com.sld.beads.data.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ImagePickerViewModel(
    private val repository: ImageRepository, imageDao: ImageDao
) : ViewModel() {
    val latestImage: StateFlow<ImageRecord?> = imageDao.observeAll()
        .map(List<ImageRecord>::firstOrNull)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun importImage(uri: Uri) {
        if (_isImporting.value) return
        viewModelScope.launch {
            _isImporting.value = true
            _errorMessage.value = null
            repository.importImage(uri)
                .onFailure { _errorMessage.value = it.message ?: "Could not save the selected image" }
            _isImporting.value = false
        }
    }
}

class ImagePickerViewModelFactory(
    private val repository: ImageRepository, private val imageDao: ImageDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ImagePickerViewModel::class.java))
        return ImagePickerViewModel(repository, imageDao) as T
    }
}
