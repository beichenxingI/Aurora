package com.beichenxing.aurora.presentation.character

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.beichenxing.aurora.presentation.common.component.AuroraButton
import com.beichenxing.aurora.presentation.common.component.AuroraDialog
import com.beichenxing.aurora.presentation.common.component.AuroraIconTextButton
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.common.theme.AuroraMotion
import com.beichenxing.aurora.domain.model.Character
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onCharacterClick : (String) -> Unit,
    onEditCharacter  : (String) -> Unit,
    modifier         : Modifier = Modifier,
    viewModel        : CharacterListViewModel = hiltViewModel()
) {
    val characters by viewModel.characters.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.importFromUris(uris)
    }

    LaunchedEffect(uiState.importSuccess) {
        uiState.importSuccess?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(uiState.importError) {
        uiState.importError?.let { snackbarHostState.showSnackbar(it); viewModel.clearMessages() }
    }

    // Find currently selected character from live list
    val selectedCharacter = uiState.selectedCharacterId?.let { id ->
        characters.find { it.id == id }
    }

    Box(modifier = modifier) {
        Scaffold(
            containerColor = AuroraColors.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(title = { Text("角色") })
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { importLauncher.launch("*/*") },
                    modifier = Modifier.padding(bottom = 88.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "导入角色卡")
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (characters.isEmpty() && !uiState.isLoading) {
                    EmptyCharacterState(
                        onImportClick = { importLauncher.launch("*/*") },
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(characters, key = { it.id }) { character ->
                            CharacterCard(
                                character = character,
                                // Tap opens the detail sheet
                                onClick = { viewModel.selectCharacter(character.id) },
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(character.id, character.isFavorite)
                                }
                            )
                        }
                    }
                }

                // Loading overlay during import
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AuroraColors.LoadingScrim),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("正在导入...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // ─── Character Detail Dialog ───────────────────────────────────────────
        if (selectedCharacter != null) {
            CharacterDetailDialog(
                character       = selectedCharacter,
                onDismiss       = { viewModel.dismissSheet() },
                onStartChat     = {
                    viewModel.dismissSheet()
                    onCharacterClick(selectedCharacter.id)
                },
                onTogglePin     = {
                    viewModel.togglePinned(selectedCharacter.id, selectedCharacter.isPinned)
                },
                onDeleteRequest = { viewModel.requestDelete() },
                onEditRequest   = {
                    viewModel.dismissSheet()
                    onEditCharacter(selectedCharacter.id)
                }
            )
        }

        // ─── Delete Confirm Dialog ─────────────────────────────────────────────
        if (uiState.showDeleteConfirm && selectedCharacter != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("删除角色") },
                text = {
                    Text(
                        "确定要删除「${selectedCharacter.name}」吗？\n" +
                        "角色及其全部聊天记录将被一并删除，无法恢复。"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDelete() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// ─── Character Detail Dialog ──────────────────────────────────────────────────

@Composable
private fun CharacterDetailDialog(
    character       : Character,
    onDismiss       : () -> Unit,
    onStartChat     : () -> Unit,
    onTogglePin     : () -> Unit,
    onDeleteRequest : () -> Unit,
    onEditRequest   : () -> Unit
) {
    AuroraDialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

                // ── Avatar hero image ─────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                ) {
                    if (character.avatarPath != null) {
                        AsyncImage(
                            model = File(character.avatarPath),
                            contentDescription = character.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AuroraColors.BgSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = AuroraColors.TextMuted
                            )
                        }
                    }

                    // Bottom scrim over hero image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(AuroraColors.CardNameScrim)
                    )
                }

                // ── Name + subtitle ───────────────────────────────────────
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp,
                        top = 16.dp, bottom = 0.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AuroraColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitle = character.creatorNotes
                        .takeIf { it.isNotBlank() }
                        ?: character.description.take(80).takeIf { it.isNotBlank() }

                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = AuroraColors.TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Start chat button ─────────────────────────────────────
                AuroraButton(
                    onClick = onStartChat,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "开始聊天",
                        modifier = Modifier.padding(start = 8.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Divider ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(1.dp)
                        .background(AuroraColors.Border)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Action icon row ───────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Pin / Unpin
                    ActionIconButton(
                        icon = Icons.Default.PushPin,
                        label = if (character.isPinned) "取消置顶" else "置顶",
                        tint = AuroraColors.Mint,
                        onClick = onTogglePin
                    )

                    // Edit (placeholder for now)
                    ActionIconButton(
                        icon = Icons.Default.Edit,
                        label = "编辑",
                        tint = AuroraColors.Sky,
                        onClick = onEditRequest
                    )

                    // Export (placeholder for now)
                    ActionIconButton(
                        icon = Icons.Default.FileUpload,
                        label = "导出",
                        tint = AuroraColors.Lilac,
                        onClick = { /* TODO: export */ }
                    )

                    // Delete
                    ActionIconButton(
                        icon = Icons.Default.Delete,
                        label = "删除",
                        tint = AuroraColors.Danger,
                        onClick = onDeleteRequest
                    )
                }
        }
    }
}

@Composable
private fun ActionIconButton(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = AuroraMotion.ActionTintAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = AuroraColors.TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Character Card ───────────────────────────────────────────────────────────

@Composable
private fun CharacterCard(
    character: Character,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier  = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape     = RoundedCornerShape(16.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = AuroraColors.BgSurface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
        ) {
            // ── Full-bleed avatar ─────────────────────────────────────────────
            if (character.avatarPath != null) {
                AsyncImage(
                    model              = File(character.avatarPath),
                    contentDescription = character.name,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier           = Modifier
                        .fillMaxSize()
                        .background(AuroraColors.BgSoft),
                    contentAlignment   = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier    = Modifier.size(56.dp),
                        tint        = AuroraColors.TextMuted
                    )
                }
            }

            // ── Bottom scrim + name ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(AuroraColors.CardNameScrim)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Column {
                    // Pinned badge
                    if (character.isPinned) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Icon(
                                imageVector  = Icons.Default.PushPin,
                                contentDescription = null,
                                tint         = AuroraColors.Mint,
                                modifier     = Modifier.size(10.dp)
                            )
                        }
                    }
                    Text(
                        text      = character.name,
                        style     = MaterialTheme.typography.titleSmall,
                        color     = AuroraColors.TextPrimary,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Favorite button ───────────────────────────────────────────────
            IconButton(
                onClick  = onFavoriteClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AuroraColors.CardOverlay)
            ) {
                Icon(
                    imageVector = if (character.isFavorite) Icons.Default.Favorite
                                  else Icons.Default.FavoriteBorder,
                    contentDescription = if (character.isFavorite) "取消收藏" else "收藏",
                    tint        = if (character.isFavorite) AuroraColors.Danger
                                  else AuroraColors.TextSecondary,
                    modifier    = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyCharacterState(
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "还没有角色",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "点击右下角的 + 号导入角色卡\n支持 PNG、CharX、JSON 格式",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}