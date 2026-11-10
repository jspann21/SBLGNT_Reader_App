package com.lexapps.sblgntreader.data.model

import com.lexapps.sblgntreader.data.db.WordEntity

fun WordEntity.toWord(courses: Map<String, VocabularyCourse> = emptyMap()): Word = Word(
    id = requireNotNull(wordId),
    verseId = verseId,
    wordIndex = wordIndex,
    text = text,
    normalizedText = normalizedText,
    posTag = posTag,
    gloss = gloss,
    lemma = lemma,
    literal = literal,
    louw = louw,
    strong = strong,
    type = type,
    vocabLevels = vocabLevels,
    courses = courses
)
