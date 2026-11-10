package com.lexapps.sblgntreader.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
internal fun ReaderQuickstart(
    active: Boolean,
    useModalDrawer: Boolean,
    drawerOpen: Boolean,
    currentPage: Int,
    wordTapped: Boolean,
    wordLongPressed: Boolean,
    targets: QuickstartTargets,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onComplete: () -> Unit,
    onReset: () -> Unit
) {
    var step by rememberSaveable { mutableStateOf(QuickstartStep.BookMenu) }
    val initialPage = remember(active, step) { currentPage }
    LaunchedEffect(active) {
        if (!active) { step = QuickstartStep.BookMenu; onReset() }
    }
    if (!active) return
    LaunchedEffect(wordTapped, step) {
        if (step == QuickstartStep.TapWord && wordTapped) step = QuickstartStep.LongPressWord
    }
    LaunchedEffect(wordLongPressed, step) {
        if (step == QuickstartStep.LongPressWord && wordLongPressed) step = QuickstartStep.SwipeChapters
    }
    LaunchedEffect(currentPage, step) {
        if (step == QuickstartStep.SwipeChapters && currentPage != initialPage) onComplete()
    }
    LaunchedEffect(useModalDrawer, drawerOpen, step) {
        if (step == QuickstartStep.BookMenu && (!useModalDrawer || drawerOpen)) step = QuickstartStep.Settings
    }
    fun next() {
        val steps = QuickstartStep.entries
        if (step == steps.last()) onComplete() else step = steps[step.ordinal + 1]
    }
    QuickstartOverlay(
        step = step,
        targets = targets,
        onNext = ::next,
        onAdvance = ::next,
        onTapTarget = when (step) {
            QuickstartStep.BookMenu -> onOpenDrawer.takeIf { useModalDrawer }
            QuickstartStep.Settings -> onOpenSettings
            else -> null
        },
        onOpenVocabSettings = onOpenVocabulary.takeIf { step == QuickstartStep.VocabSystems }
    )
}
