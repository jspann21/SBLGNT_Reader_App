package com.lexapps.sblgntreader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.ui.reader.ReaderPrefs
import com.lexapps.sblgntreader.ui.reader.buildVerseText
import com.lexapps.sblgntreader.ui.screens.components.WordText
import com.lexapps.sblgntreader.ui.screens.components.ParagraphRenderer
import com.lexapps.sblgntreader.ui.theme.AppFonts
import com.lexapps.sblgntreader.data.model.ChapterData
import androidx.compose.ui.unit.Dp

@Composable
internal fun ReaderChapter(
    data: ChapterData,
    prefs: ReaderPrefs,
    activeLevels: Map<String, Int>,
    userVocabLemmas: Set<String>,
    readerMaxWidth: Dp?,
    listState: LazyListState,
    onWordTapped: () -> Unit,
    onWordLongPressed: () -> Unit,
    onSetVocabulary: (Word, Boolean) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Box(Modifier.fillMaxSize()) {
        var selectedWord by remember(data.bookId, data.chapter) { mutableStateOf<Word?>(null) }
        var longPressedWord by remember(data.bookId, data.chapter) { mutableStateOf<Word?>(null) }
        val baseContentModifier = Modifier
            .align(Alignment.TopCenter)
        val boundedContentModifier = if (readerMaxWidth != null) {
            baseContentModifier.widthIn(max = readerMaxWidth)
        } else {
            baseContentModifier
        }.fillMaxWidth()
        if (prefs.isParagraphMode) {
            val verseMap = remember(data.verses) { data.verses.associateBy { it.number } }
            ParagraphRenderer(
                paragraphs = data.paragraphs,
                verseMap = verseMap,
                fontSizeSp = prefs.fontSize,
                showVerseNumbers = !prefs.hideVerseNumbers,
                onWordClick = {
                    selectedWord = it
                    onWordTapped()
                },
                onWordLongClick = {
                    longPressedWord = it
                    onWordLongPressed()
                },
                isTextJustified = prefs.isTextJustified,
                activeLevels = activeLevels,
                userVocabLemmas = userVocabLemmas,
                unboldProperNouns = prefs.unboldProperNouns,
                lineSpacing = prefs.lineSpacing,
                modifier = boundedContentModifier,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                listState = listState
            )
        } else {
            val verseListState = listState
            val notifiedEdge = remember { mutableStateOf<String?>(null) }
            var wasScrolling by remember { mutableStateOf(false) }
            LaunchedEffect(verseListState.isScrollInProgress) {
                if (verseListState.isScrollInProgress) {
                    wasScrolling = true
                } else if (wasScrolling) {
                    wasScrolling = false
                    val atTop = verseListState.firstVisibleItemIndex == 0 && verseListState.firstVisibleItemScrollOffset == 0
                    val lastVisible = verseListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                    val atBottom = lastVisible != null && lastVisible >= (data.verses.size - 1)
                    val edge = when {
                        atTop -> "top"
                        atBottom -> "bottom"
                        else -> null
                    }
                    if (edge != null && edge != notifiedEdge.value) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        notifiedEdge.value = edge
                    } else if (edge == null) {
                        notifiedEdge.value = null
                    }
                }
            }
            LazyColumn(
                modifier = boundedContentModifier,
                state = verseListState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(
                    items = data.verses,
                    key = { it.id },
                    contentType = { "verse" }
                ) { verse ->
                    val text = remember(verse, activeLevels, prefs.hideVerseNumbers, prefs.unboldProperNouns, userVocabLemmas) {
                        buildVerseText(verse, prefs, activeLevels, userVocabLemmas)
                    }
                    val lineHeightMultiplier = when (prefs.lineSpacing) {
                        0 -> 1.3f
                        1 -> 1.5f
                        else -> 1.9f
                    }
                    WordText(
                        content = text,
                        onWordClick = { selectedWord = it; onWordTapped() },
                        onWordLongClick = { longPressedWord = it; onWordLongPressed() },
                        style = TextStyle(
                            textAlign = if (prefs.isTextJustified) TextAlign.Justify else TextAlign.Start,
                            fontFamily = AppFonts.sblGreek,
                            fontSize = prefs.fontSize.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = (prefs.fontSize * lineHeightMultiplier).sp,
                            textIndent = TextIndent(firstLine = (prefs.fontSize * 0.8f).sp)
                        ),
                        modifier = Modifier
                    )
                    Spacer(Modifier.height(15.dp))
                }
            }
        }
        if (selectedWord != null) {
            WordBottomSheet(word = selectedWord!!, onDismiss = { selectedWord = null })
        }
        if (longPressedWord != null) {
            val word = longPressedWord!!
            val lemma = word.lemma ?: word.text
            val isUserVocab = userVocabLemmas.contains(lemma)
            WordVocabPopup(
                word = word,
                isUserVocab = isUserVocab,
                onDismiss = { longPressedWord = null },
                onConfirm = {
                    onSetVocabulary(word, !isUserVocab)
                }
            )
        }
    }
}
