package com.beichenxing.aurora.presentation.character

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.beichenxing.aurora.presentation.common.component.AuroraButton
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import java.io.File

// Stagger slot: each field has a normalized start threshold [0, 1)
// The reveal transition runs over 500ms; each field fades in
// when the global progress crosses its start threshold.
private data class FieldSlot(
    val label    : String,
    val value    : String,
    val onChange : (String) -> Unit,
    val minLines : Int     = 1,
    val maxLines : Int     = 6,
    val start    : Float   // stagger start threshold
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    onNavigateBack : () -> Unit,
    viewModel      : CharacterEditViewModel = hiltViewModel()
) {
    val uiState       by viewModel.uiState.collectAsState()
    val snackbarState  = remember { SnackbarHostState() }
    val scrollState    = rememberScrollState()

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.updateAvatarFromUri(it) } }

    // Navigate back after successful save
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onNavigateBack()
    }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    // ── Reveal transition ─────────────────────────────────────────────────
    // Drives a 0→1 progress used to stagger each field's entrance.
    val revealTransition = updateTransition(
        targetState = !uiState.isLoading,
        label       = "edit_reveal"
    )
    val revealProgress by revealTransition.animateFloat(
        transitionSpec = { tween(durationMillis = 520, easing = FastOutSlowInEasing) },
        label          = "reveal_progress"
    ) { visible -> if (visible) 1f else 0f }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = uiState.draft?.name?.ifBlank { "编辑角色" } ?: "编辑角色",
                        fontWeight = FontWeight.SemiBold,
                        color      = AuroraColors.TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint               = AuroraColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AuroraColors.Transparent
                )
            )
        },
        snackbarHost    = { SnackbarHost(snackbarState) },
        containerColor  = AuroraColors.Transparent
    ) { paddingValues ->

        AnimatedContent(
            targetState  = uiState.isLoading,
            transitionSpec = {
                fadeIn(tween(250)) togetherWith fadeOut(tween(150))
            },
            label        = "edit_content",
            modifier     = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { loading ->
            if (loading) {
                // Loading state
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AuroraColors.Mint)
                }
            } else {
                val draft = uiState.draft ?: return@AnimatedContent

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Avatar ────────────────────────────────────────────
                    StaggerItem(progress = revealProgress, start = 0.00f) {
                        AvatarPicker(
                            avatarPath = draft.avatarPath,
                            onClick    = { avatarPicker.launch("image/*") }
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ── Fields ────────────────────────────────────────────
                    val fields = listOf(
                        FieldSlot("名字",       draft.name,                    viewModel::updateName,                    1, 1,  0.08f),
                        FieldSlot("描述",       draft.description,             viewModel::updateDescription,             3, 8,  0.14f),
                        FieldSlot("性格",       draft.personality,             viewModel::updatePersonality,             2, 6,  0.20f),
                        FieldSlot("场景",       draft.scenario,                viewModel::updateScenario,                2, 6,  0.26f),
                        FieldSlot("开场白",     draft.firstMessage,            viewModel::updateFirstMessage,            3, 8,  0.32f),
                        FieldSlot("示例对话",   draft.exampleDialogue,         viewModel::updateExampleDialogue,         3, 8,  0.38f),
                        FieldSlot("系统提示词", draft.systemPrompt,            viewModel::updateSystemPrompt,            3, 8,  0.44f),
                        FieldSlot("历史后指令", draft.postHistoryInstructions, viewModel::updatePostHistoryInstructions, 2, 6,  0.50f),
                        FieldSlot("作者备注",   draft.creatorNotes,            viewModel::updateCreatorNotes,            2, 6,  0.56f)
                    )

                    fields.forEach { field ->
                        StaggerItem(progress = revealProgress, start = field.start) {
                            EditField(
                                label    = field.label,
                                value    = field.value,
                                onChange = field.onChange,
                                minLines = field.minLines,
                                maxLines = field.maxLines
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Save button ───────────────────────────────────────
                    StaggerItem(progress = revealProgress, start = 0.65f) {
                        AuroraButton(
                            onClick  = { viewModel.save() },
                            enabled  = !uiState.isSaving && uiState.isDirty,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier  = Modifier.size(20.dp),
                                    color     = AuroraColors.BgPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text       = "保存",
                                    fontWeight = FontWeight.SemiBold,
                                    style      = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─── Stagger animation wrapper ────────────────────────────────────────────────

/**
 * Wraps content in a stagger entrance animation.
 *
 * [progress]  Global reveal progress [0, 1] driven by updateTransition.
 * [start]     Normalized threshold at which this item begins to appear.
 *
 * Before [start]: invisible, shifted down 18dp.
 * After [start]:  fades in and translates up to natural position.
 */
@Composable
private fun StaggerItem(
    progress : Float,
    start    : Float,
    content  : @Composable () -> Unit
) {
    // Local progress: 0 when global < start, 1 when global >= start + window
    val window    = (1f - start).coerceAtLeast(0.01f)
    val local     = ((progress - start) / window).coerceIn(0f, 1f)
    // Apply ease-out curve to local progress
    val eased     = FastOutSlowInEasing.transform(local)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha        = eased
                translationY = (1f - eased) * 18.dp.toPx()
            }
    ) {
        content()
    }
}

// ─── Avatar picker ────────────────────────────────────────────────────────────

@Composable
private fun AvatarPicker(
    avatarPath : String?,
    onClick    : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AuroraColors.BgSoft)
            .border(1.dp, AuroraColors.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (avatarPath != null) {
            // ContentScale.Fit: show full image without cropping,
            // letterbox if aspect ratio differs from container
            AsyncImage(
                model              = File(avatarPath),
                contentDescription = "角色封面",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Person,
                    contentDescription = null,
                    tint               = AuroraColors.TextMuted,
                    modifier           = Modifier.size(56.dp)
                )
                Text(
                    text  = "点击选择封面图",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuroraColors.TextMuted
                )
            }
        }

        // Camera badge — bottom-right corner
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(AuroraColors.Mint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.CameraAlt,
                contentDescription = "更换封面图",
                tint               = AuroraColors.BgPrimary,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Edit field ───────────────────────────────────────────────────────────────

@Composable
private fun EditField(
    label    : String,
    value    : String,
    onChange : (String) -> Unit,
    minLines : Int = 1,
    maxLines : Int = 6
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onChange,
        label         = { Text(label) },
        modifier      = Modifier.fillMaxWidth(),
        minLines      = minLines,
        maxLines      = maxLines,
        shape         = RoundedCornerShape(12.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = AuroraColors.Mint,
            unfocusedBorderColor = AuroraColors.Border,
            focusedLabelColor    = AuroraColors.Mint,
            unfocusedLabelColor  = AuroraColors.TextMuted,
            focusedTextColor     = AuroraColors.TextPrimary,
            unfocusedTextColor   = AuroraColors.TextPrimary,
            cursorColor          = AuroraColors.Mint
        )
    )
}