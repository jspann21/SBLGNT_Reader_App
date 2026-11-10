package com.lexapps.sblgntreader.ui.screens.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.ui.reader.WordTextContent

@Composable
fun WordText(
    content: WordTextContent,
    style: TextStyle,
    onWordClick: (Word) -> Unit,
    onWordLongClick: (Word) -> Unit,
    modifier: Modifier = Modifier
) {
    var layout by remember(content) { mutableStateOf<TextLayoutResult?>(null) }
    val click by rememberUpdatedState(onWordClick)
    val longClick by rememberUpdatedState(onWordLongClick)
    val haptics = LocalHapticFeedback.current
    Text(
        text = content.text,
        style = style,
        modifier = modifier.pointerInput(content) {
            detectTapGestures(
                onTap = { position ->
                    layout?.wordAtPosition(content, position)?.let(click)
                },
                onLongPress = { position ->
                    layout?.wordAtPosition(content, position)?.let {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        longClick(it)
                    }
                }
            )
        },
        onTextLayout = { layout = it }
    )
}

private fun TextLayoutResult.wordAtPosition(content: WordTextContent, position: Offset): Word? {
    val line = getLineForVerticalPosition(position.y)
    val start = getLineStart(line)
    val end = getLineEnd(line, visibleEnd = true)
    if (start >= end || position.y < getLineTop(line) || position.y >= getLineBottom(line)) return null
    // Platform line bounds can include the first-line indent; use the visible characters instead.
    val first = getBoundingBox(start)
    val last = getBoundingBox(end - 1)
    val left = minOf(first.left, last.left)
    val right = maxOf(first.right, last.right)
    if (position.x < left || position.x >= right) return null
    return content.wordAt(getOffsetForPosition(position))
}
