package com.lexapps.sblgntreader.ui.screens.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexapps.sblgntreader.data.model.Paragraph
import com.lexapps.sblgntreader.data.model.Verse
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.ui.theme.AppFonts
import com.lexapps.sblgntreader.ui.reader.buildParagraphText

@Composable
fun ParagraphRenderer(
    paragraphs: List<Paragraph>,
    verseMap: Map<String, Verse>,
    fontSizeSp: Float,
    showVerseNumbers: Boolean,
    onWordClick: (Word) -> Unit,
    onWordLongClick: (Word) -> Unit,
    isTextJustified: Boolean,
    activeLevels: Map<String, Int>,
    userVocabLemmas: Set<String>,
    unboldProperNouns: Boolean,
    lineSpacing: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
    listState: LazyListState = rememberLazyListState(),
) {
    val haptics = LocalHapticFeedback.current
    var notifiedEdge by remember { mutableStateOf<String?>(null) }
    var wasScrolling by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            wasScrolling = true
        } else if (wasScrolling) {
            wasScrolling = false
            val previousEdge = notifiedEdge
            val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            val atBottom = lastVisible != null && lastVisible >= (paragraphs.size - 1)
            val newEdge = when {
                atTop -> "top"
                atBottom -> "bottom"
                else -> null
            }
            if (newEdge != previousEdge) {
                notifiedEdge = newEdge
                notifiedEdge?.let {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding
    ) {
        items(paragraphs, key = { it.id }, contentType = { "paragraph" }) { p ->
            val line = remember(p.id, verseMap, activeLevels, showVerseNumbers, unboldProperNouns, fontSizeSp, userVocabLemmas) {
                buildParagraphText(p, verseMap, fontSizeSp, showVerseNumbers, unboldProperNouns, activeLevels, userVocabLemmas)
            }
            // bottom spacing to match Flutter: 8dp (adjusted)
            val lineHeightMultiplier = when (lineSpacing) {
                0 -> 1.3f
                1 -> 1.5f
                else -> 1.9f
            }
            // Separate paragraph spacing multiplier - reduced for tighter paragraph separation
            val paragraphSpacingMultiplier = when (lineSpacing) {
                0 -> 0.8f  // Compact: tighter paragraph spacing
                1 -> 1.0f  // Cozy: balanced paragraph spacing
                else -> 1.2f  // Spaced: slightly more paragraph spacing
            }
            val paragraphSpacing = (fontSizeSp * paragraphSpacingMultiplier).dp
            androidx.compose.foundation.layout.Box(Modifier.padding(bottom = paragraphSpacing)) {
                WordText(
                    content = line,
                    onWordClick = onWordClick,
                    onWordLongClick = onWordLongClick,
                    style = TextStyle(
                        fontFamily = AppFonts.sblGreek,
                        fontSize = fontSizeSp.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = if (isTextJustified) TextAlign.Justify else TextAlign.Start,
                        lineHeight = (fontSizeSp * lineHeightMultiplier).sp
                    ),
                    modifier = Modifier
                )
            }
        }
    }
}
