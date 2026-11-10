package com.lexapps.sblgntreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.foundation.layout.PaddingValues
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.data.model.VocabConfig
import com.lexapps.sblgntreader.ui.settings.UserVocabItem
import com.lexapps.sblgntreader.ui.settings.UserVocabSortOption
import com.lexapps.sblgntreader.ui.settings.UserVocabViewModel
import com.lexapps.sblgntreader.ui.settings.VocabSettingsViewModel
import com.lexapps.sblgntreader.ui.theme.AppFonts
import com.lexapps.sblgntreader.R
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import kotlin.math.roundToInt
import com.lexapps.sblgntreader.ui.screens.components.VocabularyTransferActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabSettingsScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    vm: VocabSettingsViewModel = hiltViewModel(),
    userVm: UserVocabViewModel = hiltViewModel()
) {
    var contentVisible by remember { mutableStateOf(false) }
    val configs by vm.configs.collectAsStateWithLifecycle()
    val configsLoading by vm.isLoading.collectAsStateWithLifecycle()
    val configsError by vm.error.collectAsStateWithLifecycle()
    val words by userVm.words.collectAsStateWithLifecycle()
    val userLoadError by userVm.loadError.collectAsStateWithLifecycle()
    val userVocabWordCount = words.size
    val sortOption by userVm.sortOption.collectAsStateWithLifecycle()
    val wordToDelete = remember { mutableStateOf<Word?>(null) }
    val haptics = LocalHapticFeedback.current
    val widthClass = windowSizeClass.widthSizeClass
    val horizontalPadding = when (widthClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 32.dp
        WindowWidthSizeClass.Expanded -> 48.dp
        else -> 16.dp
    }
    val maxContentWidth = when (widthClass) {
        WindowWidthSizeClass.Compact -> null
        WindowWidthSizeClass.Medium -> 720.dp
        WindowWidthSizeClass.Expanded -> 900.dp
        else -> null
    }

    LaunchedEffect(Unit) {
        contentVisible = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.vocab_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (wordToDelete.value != null) {
            AlertDialog(
                onDismissRequest = { wordToDelete.value = null },
                title = { Text(stringResource(R.string.dialog_delete_word_title)) },
                text = {
                    val lemma = wordToDelete.value?.lemma?.takeIf { it.isNotBlank() }
                        ?: wordToDelete.value?.text.orEmpty()
                    Text(stringResource(R.string.dialog_delete_word_message, lemma))
                },
                confirmButton = {
                    TextButton(onClick = {
                        wordToDelete.value?.let { userVm.deleteWord(it) }
                        wordToDelete.value = null
                    }) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { wordToDelete.value = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(tween(durationMillis = 200)),
            exit = fadeOut(tween(durationMillis = 150))
        ) {
            val animation = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                animation.snapTo(0f)
                animation.animateTo(1f, animationSpec = tween(durationMillis = 450))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val contentModifier = Modifier
                    .align(Alignment.TopCenter)
                    .let { base -> if (maxContentWidth != null) base.widthIn(max = maxContentWidth) else base }
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 16.dp)
                LazyColumn(
                    modifier = contentModifier,
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    if (configsLoading && configs.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    configsError?.let { error ->
                        item { VocabularyError(error, vm::retry) }
                    }
                    var systemSectionIndex = 0
                    configs.forEach { cfg ->
                        if (cfg.isUser) {
                            addUserConfigSection(
                                cfg = cfg,
                                userVocabWordCount = userVocabWordCount,
                                sortOption = sortOption,
                                words = words,
                                loadError = userLoadError,
                                vm = vm,
                                userVm = userVm,
                                haptics = haptics,
                                wordToDelete = wordToDelete,
                                animationProgress = animation.value
                            )
                        } else {
                            addSystemConfigSection(
                                cfg = cfg,
                                vm = vm,
                                haptics = haptics,
                                animationProgress = animation.value,
                                showTopDivider = systemSectionIndex > 0
                            )
                            systemSectionIndex++
                        }
                    }
                    item { VocabularyTransferActions() }
                }
            }
        }
    }
}

private fun LazyListScope.addUserConfigSection(
    cfg: VocabConfig,
    userVocabWordCount: Int,
    sortOption: UserVocabSortOption,
    words: List<UserVocabItem>,
    loadError: String?,
    vm: VocabSettingsViewModel,
    userVm: UserVocabViewModel,
    haptics: HapticFeedback,
    wordToDelete: MutableState<Word?>,
    animationProgress: Float
) {
    item {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }

    item {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = cfg.name, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = pluralStringResource(
                            id = R.plurals.user_vocab_word_count,
                            count = userVocabWordCount,
                            userVocabWordCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.user_vocab_instruction),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    val options = listOf(UserVocabSortOption.RECENT, UserVocabSortOption.ALPHABETICAL)
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = sortOption == option && animationProgress >= 0.3f,
                            onClick = { userVm.setSortOption(option) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            label = {
                                val label = when (option) {
                                    UserVocabSortOption.RECENT -> stringResource(R.string.user_vocab_sort_recent)
                                    UserVocabSortOption.ALPHABETICAL -> stringResource(R.string.user_vocab_sort_alphabetical)
                                }
                                Text(
                                    text = label,
                                    style = if (option == UserVocabSortOption.ALPHABETICAL) {
                                        MaterialTheme.typography.bodySmall.copy(fontFamily = AppFonts.sblGreek)
                                    } else {
                                        MaterialTheme.typography.bodySmall
                                    }
                                )
                            }
                        )
                    }
                }
            }
            Switch(
                modifier = Modifier.semantics { contentDescription = cfg.name },
                checked = cfg.isActive,
                onCheckedChange = {
                    vm.setActive(cfg.name, it)
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }
    }

    if (loadError != null) {
        item { VocabularyError(loadError, userVm::retry) }
    }

    items(words, key = { it.word.lemma ?: it.word.text }) { item ->
        val word = item.word
        val dimAlpha = if (cfg.isActive) lerp(0.4f, 1f, animationProgress.coerceIn(0f, 1f)) else 0.4f
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(dimAlpha)
                .padding(horizontal = 0.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.lemma ?: word.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = AppFonts.sblGreek)
                )
                val gloss = word.gloss
                if (!gloss.isNullOrBlank()) {
                    Text(
                        text = gloss,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { wordToDelete.value = word }, enabled = cfg.isActive) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.cd_delete_word)
                )
            }
        }
    }
}

@Composable
private fun VocabularyError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

private fun LazyListScope.addSystemConfigSection(
    cfg: VocabConfig,
    vm: VocabSettingsViewModel,
    haptics: HapticFeedback,
    animationProgress: Float,
    showTopDivider: Boolean = false
) {
    item {
        Column(Modifier.fillMaxWidth()) {
            if (showTopDivider) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = if (showTopDivider) 8.dp else 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(cfg.displayName)
                Switch(
                    modifier = Modifier.semantics { contentDescription = cfg.displayName },
                    checked = cfg.isActive,
                    onCheckedChange = {
                        vm.setActive(cfg.name, it)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
            if (cfg.isActive && cfg.minLevel < cfg.maxLevel) {
                var selectedLevel by remember(cfg.name, cfg.selectedLevel) {
                    mutableFloatStateOf(cfg.selectedLevel.toFloat())
                }
                Text(stringResource(R.string.vocab_level_label, cfg.levelLabel, selectedLevel.roundToInt()))
                val sliderLabel = "${cfg.displayName} ${cfg.levelLabel}"
                Slider(
                    modifier = Modifier.semantics { contentDescription = sliderLabel },
                    value = selectedLevel,
                    valueRange = cfg.minLevel.toFloat()..cfg.maxLevel.toFloat(),
                    steps = (cfg.maxLevel - cfg.minLevel - 1).coerceAtLeast(0),
                    onValueChange = { selectedLevel = it },
                    onValueChangeFinished = {
                        vm.setLevel(cfg.name, selectedLevel.roundToInt())
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }
    }
}
