package com.lexapps.sblgntreader.data.backup

import com.lexapps.sblgntreader.data.db.UserVocabEntity
import java.text.Normalizer
import org.json.JSONArray
import org.json.JSONObject

/** Portable vocabulary only; Android backup separately preserves app preferences. */
object VocabularyBackup {
    const val MAX_BYTES = 2 * 1024 * 1024
    const val MAX_ENTRIES = 10_000

    fun encode(entries: List<UserVocabEntity>): String = JSONObject().apply {
        put("format", "sblgnt-vocabulary")
        put("version", 1)
        put("entries", JSONArray().apply {
            entries.sortedBy { it.lemma }.forEach { entry ->
                put(JSONObject().put("lemma", entry.lemma).put("createdAt", entry.createdAt))
            }
        })
    }.toString(2)

    fun decode(text: String): List<UserVocabEntity> {
        require(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Vocabulary file is too large." }
        val root = JSONObject(text)
        require(root.get("format") == "sblgnt-vocabulary" && root.get("version") == 1) {
            "Choose a vocabulary file exported by this app."
        }
        val entries = root.getJSONArray("entries")
        require(entries.length() <= MAX_ENTRIES) { "Vocabulary file has too many entries." }
        val unique = linkedMapOf<String, UserVocabEntity>()
        for (index in 0 until entries.length()) {
            val item = entries.getJSONObject(index)
            val rawLemma = item.get("lemma") as? String
                ?: throw IllegalArgumentException("Each vocabulary entry must contain a lemma.")
            val lemma = Normalizer.normalize(rawLemma.trim(), Normalizer.Form.NFC)
            require(lemma.isNotEmpty() && lemma.length <= 256 && lemma.none { it.isISOControl() }) {
                "Vocabulary file contains an invalid lemma."
            }
            val timestamp = (item.get("createdAt") as? Number)?.toString()?.toLongOrNull()
            require(timestamp != null && timestamp >= 0) { "Vocabulary file contains an invalid timestamp." }
            val entry = UserVocabEntity(lemma, timestamp)
            if (unique[lemma]?.createdAt?.let { it <= timestamp } != true) unique[lemma] = entry
        }
        return unique.values.toList()
    }
}
