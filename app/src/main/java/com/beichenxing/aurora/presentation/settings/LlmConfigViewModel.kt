package com.beichenxing.aurora.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beichenxing.aurora.domain.model.LlmConfig
import com.beichenxing.aurora.domain.model.LlmProvider
import com.beichenxing.aurora.domain.repository.LlmConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class LlmConfigUiState(
    val showAddDialog: Boolean = false,
    val editingConfig: LlmConfig? = null,
    val error: String? = null
)

@HiltViewModel
class LlmConfigViewModel @Inject constructor(
    private val repository: LlmConfigRepository
) : ViewModel() {

    val configs: StateFlow<List<LlmConfig>> = repository
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val activeConfig: StateFlow<LlmConfig?> = repository
        .observeActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(LlmConfigUiState())
    val uiState: StateFlow<LlmConfigUiState> = _uiState.asStateFlow()

    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true, editingConfig = null) }
    }

    fun showEditDialog(config: LlmConfig) {
        _uiState.update { it.copy(showAddDialog = true, editingConfig = config) }
    }

    fun hideDialog() {
        _uiState.update { it.copy(showAddDialog = false, editingConfig = null) }
    }

    fun saveConfig(
        name: String,
        endpoint: String,
        apiKey: String,
        model: String,
        existingId: String? = null
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "配置名称不能为空") }
            return
        }
        if (endpoint.isBlank()) {
            _uiState.update { it.copy(error = "API 地址不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val config = LlmConfig(
                    id = existingId ?: UUID.randomUUID().toString(),
                    name = name.trim(),
                    provider = LlmProvider.OPENAI_COMPATIBLE,
                    endpoint = endpoint.trim(),
                    apiKey = apiKey.trim(),
                    model = model.trim(),
                    createdAt = now,
                    updatedAt = now
                )
                repository.insert(config)

                // If this is the first config, auto-activate it
                val allConfigs = repository.getAll()
                if (allConfigs.size == 1) {
                    repository.setActive(config.id)
                }

                hideDialog()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save LLM config")
                _uiState.update { it.copy(error = "保存失败: ${e.message}") }
            }
        }
    }

    fun setActive(configId: String) {
        viewModelScope.launch {
            repository.setActive(configId)
        }
    }

    fun deleteConfig(configId: String) {
        viewModelScope.launch {
            repository.deleteById(configId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}