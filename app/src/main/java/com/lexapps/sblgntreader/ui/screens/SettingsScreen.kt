package com.lexapps.sblgntreader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lexapps.sblgntreader.ui.settings.SettingsViewModel
import com.lexapps.sblgntreader.ui.AppViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import com.lexapps.sblgntreader.R
import androidx.compose.ui.Alignment
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    vm: SettingsViewModel = hiltViewModel(),
    appVm: AppViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val quickstartResetConfirmation = stringResource(R.string.quickstart_reset_confirmation)
    val fontSizeTitle = stringResource(R.string.font_size_title)
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
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val state by vm.state.collectAsStateWithLifecycle()
        val themeIndex by appVm.themeModeIndex.collectAsStateWithLifecycle()
        val haptics = LocalHapticFeedback.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val contentModifier = Modifier
                .align(Alignment.TopCenter)
                .let { base -> if (maxContentWidth != null) base.widthIn(max = maxContentWidth) else base }
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
            LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
            // Learned Words section
            item { SectionHeader(stringResource(R.string.learned_words_header)) }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.vocab_settings_title)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.learned_words_support),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("vocabSettings") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                    SwitchListItem(
                        title = stringResource(R.string.unbold_proper_nouns),
                        checked = state.unboldProperNouns,
                        onCheckedChange = { vm.setUnboldProperNouns(it) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.learned_words_stats_button)) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.learned_words_stats_support),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("stats") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // Formatting section
            item { SectionHeader(stringResource(R.string.formatting_header)) }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(fontSizeTitle) },
                        supportingContent = {
                            var fontSize by remember(state.fontSize) { mutableFloatStateOf(state.fontSize) }
                            Column(Modifier.fillMaxWidth()) {
                                Text(
                                    stringResource(
                                        R.string.font_size_current,
                                        fontSize.roundToInt()
                                    )
                                )
                                Slider(
                                    modifier = Modifier.semantics { contentDescription = fontSizeTitle },
                                    value = fontSize,
                                    onValueChange = { fontSize = it },
                                    valueRange = 18f..34f,
                                    steps = (34 - 18) - 1,
                                    onValueChangeFinished = {
                                        vm.setFontSize(fontSize)
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.line_spacing_title)) },
                        supportingContent = {
                            val lineSpacingOptions = listOf(
                                R.string.line_spacing_compact,
                                R.string.line_spacing_cozy,
                                R.string.line_spacing_spaced
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                lineSpacingOptions.forEachIndexed { index, label ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(index = index, count = lineSpacingOptions.size),
                                        onClick = { vm.setLineSpacing(index) },
                                        selected = index == state.lineSpacing
                                    ) {
                                        Text(stringResource(label))
                                    }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider()
                    SwitchListItem(
                        title = stringResource(R.string.verse_by_verse_mode),
                        checked = !state.isParagraphMode,
                        onCheckedChange = {
                            vm.setParagraphMode(!it)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                    HorizontalDivider()
                    SwitchListItem(
                        title = stringResource(R.string.justify_text),
                        checked = state.isTextJustified,
                        onCheckedChange = {
                            vm.setTextJustification(it)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                    HorizontalDivider()
                    SwitchListItem(
                        title = stringResource(R.string.hide_verse_numbers),
                        checked = state.hideVerseNumbers,
                        onCheckedChange = {
                            vm.setHideVerseNumbers(it)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                    HorizontalDivider()
                    SwitchListItem(
                        title = stringResource(R.string.use_greek_book_names),
                        checked = state.useGreekBookNames,
                        onCheckedChange = {
                            vm.setUseGreekBookNames(it)
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
            }

            // Theme section
            item { SectionHeader(stringResource(R.string.theme_header)) }
            item {
                SettingsCard {
                    RadioListItem(
                        title = stringResource(R.string.theme_system_default),
                        selected = themeIndex == 0,
                        onClick = { vm.setThemeMode(0) }
                    )
                    HorizontalDivider()
                    RadioListItem(
                        title = stringResource(R.string.theme_light),
                        selected = themeIndex == 1,
                        onClick = { vm.setThemeMode(1) }
                    )
                    HorizontalDivider()
                    RadioListItem(
                        title = stringResource(R.string.theme_dark),
                        selected = themeIndex == 2,
                        onClick = { vm.setThemeMode(2) }
                    )
                }
            }

            // About section
            item { SectionHeader(stringResource(R.string.about_header)) }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.about_header)) },
                        supportingContent = { Text(stringResource(R.string.about_support)) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("about") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // Tutorial restart row
            item {
                fun restartTutorial() {
                    vm.restartQuickstart {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch {
                            snackbarHostState.showSnackbar(message = quickstartResetConfirmation)
                        }
                    }
                }
                ListItem(
                    headlineContent = { Text(stringResource(R.string.restart_quickstart_title)) },
                    supportingContent = {
                        Text(stringResource(R.string.restart_quickstart_support))
                    },
                    trailingContent = {
                        TextButton(onClick = { restartTutorial() }) {
                            Text(stringResource(R.string.action_restart))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { restartTutorial() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            item { Spacer(Modifier.padding(bottom = 12.dp)) }
        }
    }
}
}


@Composable
private fun SwitchListItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supportingText: String? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supportingText?.let { { Text(it) } },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        },
        modifier = Modifier
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .fillMaxWidth(),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun RadioListItem(title: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            RadioButton(selected = selected, onClick = null)
        },
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 4.dp)) {
            content()
        }
    }
}
