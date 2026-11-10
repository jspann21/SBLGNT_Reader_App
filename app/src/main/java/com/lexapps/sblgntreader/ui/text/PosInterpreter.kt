package com.lexapps.sblgntreader.ui.text

object PosInterpreter {
    private val caseMap = mapOf(
        'N' to "Nominative",
        'G' to "Genitive",
        'D' to "Dative",
        'A' to "Accusative",
        'V' to "Vocative"
    )
    private val numberMap = mapOf('S' to "Singular", 'P' to "Plural", 'D' to "Dual")
    private val genderMap = mapOf('M' to "Masculine", 'F' to "Feminine", 'N' to "Neuter")
    private val degreeMap = mapOf('C' to "Comparative", 'S' to "Superlative", 'O' to "Other")

    private val tenseMap = mapOf(
        'P' to "Present",
        'I' to "Imperfect",
        'F' to "Future",
        'T' to "Future-perfect",
        'A' to "Aorist",
        'R' to "Perfect",
        'L' to "Pluperfect"
    )
    private val voiceMap = mapOf('A' to "Active", 'M' to "Middle", 'P' to "Passive", 'U' to "Middle-Passive")
    private val moodMap = mapOf(
        'I' to "Indicative",
        'S' to "Subjunctive",
        'O' to "Optative",
        'M' to "Imperative",
        'N' to "Infinitive",
        'P' to "Participle"
    )
    private val personMap = mapOf('1' to "1st Person", '2' to "2nd Person", '3' to "3rd Person")

    private val pronounTypeMap = mapOf(
        'R' to "Relative",
        'C' to "Reciprocal",
        'D' to "Demonstrative",
        'K' to "Correlative",
        'I' to "Interrogative",
        'X' to "Indefinite",
        'F' to "Reflexive",
        'S' to "Possessive",
        'P' to "Personal"
    )
    private val pronounSubtypeMap = mapOf('A' to "Intensive Attributive", 'P' to "Intensive Predicative")

    private val conjunctionTypeMap = mapOf('L' to "Logical", 'A' to "Adverbial", 'S' to "Substantival")
    private val logicalSubtypeMap = mapOf(
        'A' to "Ascensive",
        'N' to "Connective",
        'C' to "Contrastive",
        'K' to "Correlative",
        'D' to "Disjunctive",
        'M' to "Emphatic",
        'X' to "Explanatory",
        'I' to "Inferential",
        'T' to "Transitional"
    )
    private val adverbialSubtypeMap = mapOf(
        'Z' to "Causal",
        'M' to "Comparative",
        'N' to "Concessive",
        'C' to "Conditional",
        'D' to "Declarative",
        'L' to "Local",
        'P' to "Purpose",
        'R' to "Result",
        'T' to "Temporal"
    )
    private val substantivalSubtypeMap = mapOf('C' to "Content", 'E' to "Epexegetical")

    private val adverbTypeMap = mapOf(
        'C' to "Conditional",
        'K' to "Correlative",
        'E' to "Emphatic",
        'X' to "Indefinite",
        'I' to "Interrogative",
        'N' to "Negative",
        'P' to "Place",
        'S' to "Superlative"
    )

    private val indeclinableTypeMap = mapOf(
        'L' to "Letter",
        'P' to "Proper Noun",
        'N' to "Numeral",
        'F' to "Foreign Word",
        'O' to "Other"
    )

    fun interpret(posTag: String?): String {
        if (posTag.isNullOrBlank()) return "No POS data available"
        val partOfSpeech = posTag.first()
        val details = posTag.drop(1)

        val (description, additional) = when (partOfSpeech) {
            'N' -> "Noun" to interpretCaseNumberGender(details)
            'V' -> "Verb" to interpretVerbDetails(details)
            'J' -> "Adjective" to interpretAdjectiveDetails(details)
            'R' -> "Pronoun" to interpretPronounDetails(details)
            'D' -> "Definite Article" to interpretCaseNumberGender(details)
            'C' -> "Conjunction" to interpretConjunctionDetails(details)
            'B' -> "Adverb" to interpretAdverbDetails(details)
            'T' -> "Particle" to interpretParticleDetails(details)
            'P' -> "Preposition" to ""
            'I' -> "Interjection" to ""
            'X' -> "Indeclinable" to interpretIndeclinableDetails(details)
            else -> "Unknown Part of Speech" to ""
        }

        return if (additional.isBlank()) description else "$description - $additional"
    }

    private fun interpretCaseNumberGender(details: String): String {
        val kase = details.getOrNull(0)?.let { caseMap[it] } ?: ""
        val number = details.getOrNull(1)?.let { numberMap[it] } ?: ""
        val gender = details.getOrNull(2)?.let { genderMap[it] } ?: ""
        // Align with lexicon convention: Case, Gender, Number
        return listOf(kase, gender, number).filter { it.isNotBlank() }.joinToString(", ")
    }

    private fun interpretAdjectiveDetails(details: String): String {
        val caseNumberGender = interpretCaseNumberGender(details)
        val degree = details.getOrNull(3)?.let { degreeMap[it] } ?: ""
        return listOf(caseNumberGender, degree).filter { it.isNotBlank() }.joinToString(", ")
    }

    private fun interpretVerbDetails(details: String): String {
        val tense = details.getOrNull(0)?.let { tenseMap[it] } ?: ""
        val voice = details.getOrNull(1)?.let { voiceMap[it] } ?: ""
        val mood = details.getOrNull(2)?.let { moodMap[it] } ?: ""
        val person = details.getOrNull(3)?.let { personMap[it] } ?: ""
        val number = details.getOrNull(4)?.let { numberMap[it] } ?: ""
        val caseNumberGender = if (details.length > 5) interpretCaseNumberGender(details.drop(5)) else ""
        return listOf(tense, voice, mood, person, number, caseNumberGender)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }

    private fun interpretPronounDetails(details: String): String {
        val pronounType = details.getOrNull(0)?.let { pronounTypeMap[it] } ?: ""
        val person = details.getOrNull(1)?.takeIf { it != '-' }?.let { personMap[it] } ?: ""
        val caseNumberGender = interpretCaseNumberGender(details.drop(2))
        val subtype = details.getOrNull(5)?.let { pronounSubtypeMap[it] } ?: ""
        return listOf(pronounType, person, caseNumberGender, subtype)
            .filter { it.isNotBlank() }
            .joinToString(", ")
    }

    private fun interpretConjunctionDetails(details: String): String {
        val type = details.getOrNull(0)?.let { conjunctionTypeMap[it] } ?: ""
        val subtype = when (type) {
            "Logical" -> details.getOrNull(1)?.let { logicalSubtypeMap[it] } ?: ""
            "Adverbial" -> details.getOrNull(1)?.let { adverbialSubtypeMap[it] } ?: ""
            "Substantival" -> details.getOrNull(1)?.let { substantivalSubtypeMap[it] } ?: ""
            else -> ""
        }
        return listOf(type, subtype).filter { it.isNotBlank() }.joinToString(", ")
    }

    private fun interpretAdverbDetails(details: String): String {
        return details.getOrNull(0)?.let { adverbTypeMap[it] } ?: ""
    }

    private fun interpretParticleDetails(details: String): String {
        return interpretAdverbDetails(details)
    }

    private fun interpretIndeclinableDetails(details: String): String {
        return details.getOrNull(0)?.let { indeclinableTypeMap[it] } ?: ""
    }
}
