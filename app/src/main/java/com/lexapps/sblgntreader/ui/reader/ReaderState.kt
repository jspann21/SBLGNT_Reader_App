package com.lexapps.sblgntreader.ui.reader

import androidx.compose.runtime.Immutable

@Immutable
data class ReaderPrefs(
    val fontSize: Float,
    val isParagraphMode: Boolean,
    val hideVerseNumbers: Boolean,
    val isTextJustified: Boolean,
    val useGreekBookNames: Boolean,
    val unboldProperNouns: Boolean,
    val lineSpacing: Int
)


