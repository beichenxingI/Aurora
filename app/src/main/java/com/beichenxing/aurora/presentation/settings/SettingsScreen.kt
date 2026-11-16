package com.beichenxing.aurora.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.beichenxing.aurora.domain.model.LlmConfig
import com.beichenxing.aurora.presentation.common.theme.AuroraColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: LlmConfigViewModel = hiltViewModel()
) {
    val configs by viewModel.configs.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier) {
        Scaffold(
        containerColor = AuroraColors.Transparent,
        topBar = {
            TopAppBar(title = { Text("设置") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LLM 配置",
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = viewModel::showAddDialog) {
                        Icon(Icons.Default.Add, contentDescription = "添加配置")
                    }
                }
            }

            if (configs.isEmpty()) {
                item {
                    Text(
                        text = "还没有 LLM 配置，点击右上角 + 添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(configs, key = { it.id }) { config ->
                LlmConfigItem(
                    config = config,
                    isActive = config.id == activeConfig?.id,
                    onActivate = { viewModel.setActive(config.id) },
                    onEdit = { viewModel.showEditDialog(config) },
                    onDelete = { viewModel.deleteConfig(config.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    }

    // Add/Edit dialog
    if (uiState.showAddDialog) {
        LlmConfigDialog(
            existingConfig = uiState.editingConfig,
            error = uiState.error,
            onSave = { name, endpoint, apiKey, model ->
                viewModel.saveConfig(
                    name = name,
                    endpoint = endpoint,
                    apiKey = apiKey,
                    model = model,
                    existingId = uiState.editingConfig?.id
                )
            },
            onDismiss = viewModel::hideDialog,
            onClearError = viewModel::clearError
        )
    }
}

@Composable
private fun LlmConfigItem(
    config: LlmConfig,
    isActive: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "当前使用",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = config.endpoint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (config.model.isNotBlank()) {
                    Text(
                        text = config.model,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!isActive) {
                TextButton(onClick = onActivate) {
                    Text("启用")
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LlmConfigDialog(
    existingConfig: LlmConfig?,
    error: String?,
    onSave: (name: String, endpoint: String, apiKey: String, model: String) -> Unit,
    onDismiss: () -> Unit,
    onClearError: () -> Unit
) {
    var name by remember(existingConfig) {
        mutableStateOf(existingConfig?.name ?: "")
    }
    var endpoint by remember(existingConfig) {
        mutableStateOf(existingConfig?.endpoint ?: "https://api.openai.com/v1")
    }
    var apiKey by remember(existingConfig) {
        mutableStateOf(existingConfig?.apiKey ?: "")
    }
    var model by remember(existingConfig) {
        mutableStateOf(existingConfig?.model ?: "")
    }
    var showApiKey by remember { mutableStateOf(false) }

    // Clear error when user starts typing
    LaunchedEffect(name, endpoint, apiKey, model) {
        if (error != null) onClearError()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existingConfig != null) "编辑 LLM 配置" else "添加 LLM 配置")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称") },
                    placeholder = { Text("例如：本地 Ollama") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("API 地址") },
                    placeholder = { Text("https://api.openai.com/v1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...（本地模型可留空）") },
                    singleLine = true,
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { showApiKey = !showApiKey }) {
                            Text(if (showApiKey) "隐藏" else "显示")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型名称") },
                    placeholder = { Text("例如：gpt-4o") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Error message
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, endpoint, apiKey, model) }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}