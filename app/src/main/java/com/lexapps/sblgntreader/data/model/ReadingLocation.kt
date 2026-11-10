package com.lexapps.sblgntreader.data.model

data class ReadingLocation(val bookId: Int, val chapter: Int) {
    val key: String get() = "$bookId:$chapter"
}

class ChapterNavigation(private val books: List<Book>) {
    val locations: List<ReadingLocation> = books.flatMap { book ->
        (1..book.chapterCount).map { ReadingLocation(book.id, it) }
    }

    fun indexOf(location: ReadingLocation): Int = locations.indexOf(location)
    fun at(index: Int): ReadingLocation? = locations.getOrNull(index)
    fun validOrFirst(location: ReadingLocation): ReadingLocation? = location.takeIf { it in locations } ?: locations.firstOrNull()
    fun adjacent(location: ReadingLocation): List<ReadingLocation> {
        val index = indexOf(location)
        return if (index < 0) emptyList() else listOfNotNull(at(index - 1), at(index + 1))
    }
}
