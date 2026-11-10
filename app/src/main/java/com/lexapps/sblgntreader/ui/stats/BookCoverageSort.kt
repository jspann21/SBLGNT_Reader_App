package com.lexapps.sblgntreader.ui.stats

enum class BookCoverageSort(val storageValue: Int) {
    CANONICAL(0),
    MOST_READABLE(1);

    companion object {
        fun fromStorage(value: Int): BookCoverageSort =
            entries.firstOrNull { it.storageValue == value } ?: CANONICAL
    }
}
