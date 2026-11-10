package com.lexapps.sblgntreader.ui.util

private val leadingDigitRegex = Regex("^(\\d+)(\\S.*)")

fun formatBookNameForDisplay(rawName: String): String {
    val trimmed = rawName.trim()
    val match = leadingDigitRegex.find(trimmed) ?: return trimmed
    val (digits, rest) = match.destructured
    return "$digits ${rest.trimStart()}"
}
