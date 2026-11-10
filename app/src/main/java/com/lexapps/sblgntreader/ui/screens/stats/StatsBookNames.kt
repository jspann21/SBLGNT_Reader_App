package com.lexapps.sblgntreader.ui.screens.stats

import java.util.Locale

private val bookAbbreviationMap = mapOf(
    "matthew" to "Matt",
    "mark" to "Mark",
    "luke" to "Luke",
    "john" to "John",
    "acts" to "Acts",
    "romans" to "Rom",
    "1corinthians" to "1 Cor",
    "2corinthians" to "2 Cor",
    "galatians" to "Gal",
    "ephesians" to "Eph",
    "philippians" to "Phil",
    "colossians" to "Col",
    "1thessalonians" to "1 Thess",
    "2thessalonians" to "2 Thess",
    "1timothy" to "1 Tim",
    "2timothy" to "2 Tim",
    "titus" to "Titus",
    "philemon" to "Phlm",
    "hebrews" to "Heb",
    "james" to "Jas",
    "1peter" to "1 Pet",
    "2peter" to "2 Pet",
    "1john" to "1 John",
    "2john" to "2 John",
    "3john" to "3 John",
    "jude" to "Jude",
    "revelation" to "Rev"
)

internal fun normalizeBookKey(name: String): String =
    name.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")

internal fun bookAbbreviation(name: String): String {
    val normalized = normalizeBookKey(name)
    bookAbbreviationMap[normalized]?.let { return it }
    bookAbbreviationMap.entries.firstOrNull { (key, _) ->
        normalized.startsWith(key) || key.startsWith(normalized)
    }?.let { return it.value }
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return name
    val parts = trimmed.split(" ")
    val prefix = parts.first()
    val suffix = parts.drop(1).firstOrNull()
    return when {
        suffix != null -> "${prefix.take(1)} ${suffix.take(3)}"
        trimmed.length > 4 -> trimmed.take(4)
        else -> trimmed
    }
}
