package com.lexapps.sblgntreader.data.repo

import android.util.Log
import com.lexapps.sblgntreader.data.db.BookDao
import com.lexapps.sblgntreader.data.db.ChapterDao
import com.lexapps.sblgntreader.data.db.ParagraphDao
import com.lexapps.sblgntreader.data.db.WordDao
import com.lexapps.sblgntreader.data.model.toWord
import com.lexapps.sblgntreader.data.db.StatsDao
import com.lexapps.sblgntreader.data.db.WordTokenStat
import com.lexapps.sblgntreader.data.db.UserVocabDao
import com.lexapps.sblgntreader.data.db.UserVocabEntity
import com.lexapps.sblgntreader.data.db.VerseDao
import com.lexapps.sblgntreader.data.db.VocabMetadataDao
import com.lexapps.sblgntreader.data.model.Book
import com.lexapps.sblgntreader.data.model.BookCoverage
import com.lexapps.sblgntreader.data.model.LearnedStats
import com.lexapps.sblgntreader.data.model.PosCoverage
import com.lexapps.sblgntreader.data.model.Paragraph
import com.lexapps.sblgntreader.data.model.SourceStats
import com.lexapps.sblgntreader.data.model.SuggestedLemma
import com.lexapps.sblgntreader.data.model.Verse
import com.lexapps.sblgntreader.data.model.VocabConfig
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.data.model.VocabularyCourse
import com.lexapps.sblgntreader.data.prefs.VocabPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao
) {
    suspend fun getBooksWithChapterCounts(): List<Book> = withContext(Dispatchers.IO) {
        Log.d("BookRepository", "Fetching books...")
        val books = bookDao.getAllBooks()
        val counts = bookDao.getChapterCounts().associate { it.bookId to it.count }
        val result = books.mapNotNull { b ->
            val bookId = b.bookId ?: return@mapNotNull null
            Book(
                id = bookId,
                name = b.name,
                osisName = b.osisName,
                chapterCount = counts[bookId] ?: 0
            )
        }
        Log.d("BookRepository", "Loaded ${result.size} books; chapterCounts=${counts.size}")
        result
    }
}

@Singleton
class TextRepository @Inject constructor(
    private val wordDao: WordDao,
    private val chapterDao: ChapterDao,
    private val verseDao: VerseDao,
    private val paragraphDao: ParagraphDao,
    private val vocabMetadataDao: VocabMetadataDao,
) {
    suspend fun getChapterData(bookId: Int, chapter: Int): Pair<List<Verse>, List<Paragraph>> =
        withContext(Dispatchers.IO) {
            Log.d("TextRepository", "getChapterData bookId=$bookId chapter=$chapter")
            val chapterId = chapterDao.getChapterId(bookId, chapter)
            if (chapterId == null) {
                Log.w("TextRepository", "Chapter not found for book=$bookId, chapter=$chapter")
                return@withContext Pair(emptyList(), emptyList())
            }
            val verses = verseDao.getVersesByChapter(chapterId)
            val verseIds = verses.mapNotNull { it.verseId }
            val courses = vocabMetadataDao.getAll().associate { it.vocabName to VocabularyCourse(it.displayName, it.levelLabel) }
            val wordsByVerse = verseIds.chunked(900)
                .flatMap { wordDao.forVerses(it) }
                .map { it.toWord(courses) }
                .groupBy { it.verseId }
            val verseModels = verses.map { v ->
                val wordModels = wordsByVerse[v.verseId] ?: emptyList()
                Verse(
                    id = (v.verseId ?: 0),
                    chapterId = v.chapterId,
                    bookId = bookId,
                    chapterNumber = chapter,
                    number = v.verseNumber,
                    words = wordModels
                )
            }

            val paragraphs = paragraphDao.getParagraphsByChapter(chapterId).map { p ->
                val spans = parseVerseSpans(p.verseSpans)
                Paragraph(id = (p.paragraphId ?: 0), chapterId = p.chapterId, paragraphIndex = p.paragraphIndex, verseSpans = spans)
            }
            Log.d("TextRepository", "chapterId=$chapterId verses=${verseModels.size} paragraphs=${paragraphs.size}")
            Pair(verseModels, paragraphs)
        }

    private fun parseVerseSpans(raw: String): List<String> = try {
        val node = JSONArray(raw)
        (0 until node.length()).map { idx -> node.get(idx).toString() }
    } catch (_: org.json.JSONException) {
        listOf(raw)
    }
}

@Singleton
class VocabRepository @Inject constructor(
    private val wordDao: WordDao,
    private val vocabMetadataDao: VocabMetadataDao,
    private val userVocabDao: UserVocabDao
) {
    suspend fun getAvailableVocabConfigs(prefs: VocabPrefs): List<VocabConfig> = withContext(Dispatchers.IO) {
        val metas = vocabMetadataDao.getAll()
        metas.map { m ->
            VocabConfig(
                name = m.vocabName,
                minLevel = maxOf(1, m.minLevel),
                maxLevel = maxOf(1, m.maxLevel),
                selectedLevel = prefs.getLevel(m.vocabName).coerceIn(maxOf(1, m.minLevel), maxOf(1, m.maxLevel)),
                isActive = prefs.isActive(m.vocabName),
                displayName = m.displayName,
                levelLabel = m.levelLabel
            )
        }.sortedBy { it.name }
    }

    fun observeActiveLevels(prefs: VocabPrefs): Flow<Map<String, Int>> = flow {
        val metas = vocabMetadataDao.getAll()

        val metaFlows: List<Flow<Pair<String, Int>?>> = metas.map { m ->
            combine(prefs.activeFlow(m.vocabName), prefs.levelsFlow(m.vocabName)) { active, level ->
                if (active) m.vocabName to level.coerceIn(maxOf(1, m.minLevel), maxOf(1, m.maxLevel)) else null
            }
        }

        val userVocabFlow: Flow<Pair<String, Int>?> = prefs.activeFlow("User").map { isActive ->
            if (isActive) "User" to 1 else null
        }

        val allFlows: List<Flow<Pair<String, Int>?>> = metaFlows + listOf(userVocabFlow)

        if (allFlows.isEmpty()) {
            emit(emptyMap())
        } else {
            combine(allFlows) { pairs ->
                pairs.filterNotNull().toMap()
            }.collect {
                emit(it)
            }
        }
    }

    suspend fun addUserVocabLemma(lemma: String) = withContext(Dispatchers.IO) {
        userVocabDao.insert(UserVocabEntity(lemma = lemma, createdAt = System.currentTimeMillis()))
    }

    suspend fun removeUserVocabLemma(lemma: String) = withContext(Dispatchers.IO) {
        userVocabDao.deleteByLemma(lemma)
    }

    fun observeUserVocabEntries(): Flow<List<UserVocabEntity>> = userVocabDao.observeAll()

    @Suppress("unused") // Kept for future flows that need to query before triggering DB updates
    suspend fun isUserVocabLemma(lemma: String): Boolean = withContext(Dispatchers.IO) {
        userVocabDao.getByLemma(lemma) != null
    }

    fun observeUserVocabLemmas(): Flow<Set<String>> =
        userVocabDao.observeAll().map { entities -> entities.map { it.lemma }.toSet() }

    suspend fun getWordsByIds(ids: List<Int>): List<Word> = withContext(Dispatchers.IO) {
        val courses = vocabMetadataDao.getAll().associate { it.vocabName to VocabularyCourse(it.displayName, it.levelLabel) }
        ids.distinct().chunked(900).flatMap { wordDao.forIds(it) }.map { it.toWord(courses) }.sortedBy { it.id }
    }

    suspend fun getWordsByLemmas(lemmas: List<String>): List<Word> = withContext(Dispatchers.IO) {
        val courses = vocabMetadataDao.getAll().associate { it.vocabName to VocabularyCourse(it.displayName, it.levelLabel) }
        lemmas.distinct().chunked(900).flatMap { wordDao.forLemmas(it) }.map { it.toWord(courses) }
    }

}

@Singleton
class StatsRepository @Inject constructor(
    private val statsDao: StatsDao,
    private val vocabMetadataDao: VocabMetadataDao
) {
    private val tokenStatsMutex = Mutex()
    private var cachedTokenStats: List<WordTokenStat>? = null

    suspend fun calculateStats(
        activeLevels: Map<String, Int>,
        userLemmas: Set<String>,
        includeProperNouns: Boolean
    ): LearnedStats = withContext(Dispatchers.IO) {
        val rows = tokenStatsMutex.withLock {
            cachedTokenStats ?: run {
                val trimCharacters = buildString {
                    for (character in Char.MIN_VALUE..Char.MAX_VALUE) {
                        if (character.isWhitespace()) append(character)
                    }
                }
                statsDao.getTokenStats(trimCharacters).also { cachedTokenStats = it }
            }
        }
        val levels = activeLevels.mapKeys { it.key.lowercase() }
        val metadata = vocabMetadataDao.getAll().associateBy { it.vocabName }
        val sources = levels.filterKeys { it != "user" && it != "proper" }
            .map { (key, level) -> SourceAccumulator(key, level) } + listOfNotNull(
            if ("user" in levels) SourceAccumulator("user", null) else null,
            if (includeProperNouns) SourceAccumulator("proper", null) else null
        )
        val learnedKeys = mutableSetOf<String>()
        val books = linkedMapOf<Int, BookAccumulator>()
        val posTotals = mutableMapOf<String, Long>()
        val posLearned = mutableMapOf<String, Long>()
        val lemmaPos = mutableMapOf<String, MutableMap<String, Long>>()
        val remainingByLemma = mutableMapOf<String, Long>()
        var totalWords = 0L
        var learnedWords = 0L

        for (row in rows) {
            currentCoroutineContext().ensureActive()
            val lemma = row.lemma?.trim()?.takeIf { it.isNotEmpty() }
            val normalized = row.normalizedText?.trim()?.takeIf { it.isNotEmpty() }
                ?: row.text.trim().takeIf { it.isNotEmpty() }
            val key = lemma?.let { "lemma::$it" }
                ?: normalized?.let { "normalized::$it" }
            var learned = false
            for (source in sources) {
                val covered = when (source.key) {
                    "user" -> (row.lemma ?: row.text) in userLemmas
                    "proper" -> row.type == "proper"
                    else -> row.vocabLevels[source.key]?.let { it <= requireNotNull(source.level) } == true
                }
                if (covered) {
                    source.wordCount += row.count
                    if (key != null) source.lemmaKeys += key
                    learned = true
                }
            }
            totalWords += row.count
            val book = books.getOrPut(row.bookId) { BookAccumulator(row.bookName) }
            book.totalWords += row.count
            if (learned) {
                learnedWords += row.count
                book.learnedWords += row.count
                if (key != null) learnedKeys += key
            } else if (lemma != null) {
                remainingByLemma[lemma] = (remainingByLemma[lemma] ?: 0L) + row.count
            }

            val pos = row.pos.trim()
            if (pos.isNotEmpty()) {
                posTotals[pos] = (posTotals[pos] ?: 0L) + row.count
                if (learned) posLearned[pos] = (posLearned[pos] ?: 0L) + row.count
                if (lemma != null) {
                    val counts = lemmaPos.getOrPut(lemma) { mutableMapOf() }
                    counts[pos] = (counts[pos] ?: 0L) + row.count
                }
            }
        }

        val verbTotal = posTotals["V"] ?: 0L
        val verbLearned = posLearned["V"] ?: 0L
        val posCoverage = listOf(
            PosCoverage("V", verbTotal, verbLearned),
            PosCoverage("NON_V", posTotals.values.sum() - verbTotal, posLearned.values.sum() - verbLearned)
        ) + listOf("N", "J", "T", "P").map { pos ->
            PosCoverage(pos, posTotals[pos] ?: 0L, posLearned[pos] ?: 0L)
        }
        LearnedStats(
            totalWordCount = totalWords,
            totalLearnedWordCount = learnedWords,
            totalLearnedLemmaCount = learnedKeys.size,
            sources = sources.filter { it.key != "proper" || it.wordCount > 0L }.map {
                val course = metadata[it.key]
                SourceStats(
                    it.key,
                    if (course != null && course.minLevel == course.maxLevel) null else it.level,
                    it.lemmaKeys.size,
                    it.wordCount,
                    course?.displayName ?: it.key,
                    course?.levelLabel ?: "Chapter"
                )
            },
            bookCoverage = books.map { (id, book) ->
                BookCoverage(id, book.name, book.totalWords, book.learnedWords)
            }.sortedBy { it.bookId },
            posCoverage = posCoverage.filter { it.totalWords > 0L },
            suggestions = remainingByLemma.entries.sortedWith(
                compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key }
            ).take(10).map { (lemma, count) ->
                SuggestedLemma(lemma, count, lemmaPos[lemma]?.maxByOrNull { it.value }?.key)
            }
        )
    }

    private class SourceAccumulator(val key: String, val level: Int?) {
        val lemmaKeys = mutableSetOf<String>()
        var wordCount = 0L
    }

    private class BookAccumulator(val name: String) {
        var totalWords = 0L
        var learnedWords = 0L
    }
}
