package com.lexapps.sblgntreader.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.lexapps.sblgntreader.data.db.SblgntDatabase
import com.lexapps.sblgntreader.data.db.UserDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VocabularyBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val personalDatabase: UserDatabase,
    private val corpus: SblgntDatabase
) {
    suspend fun exportTo(uri: Uri): Unit = withContext(Dispatchers.IO) {
        val text = VocabularyBackup.encode(personalDatabase.userVocabDao().getAll())
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Could not open the selected file for writing.")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }

    suspend fun importFrom(uri: Uri): Int = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open the selected vocabulary file.")
        val bytes = stream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                require(output.size() + count <= VocabularyBackup.MAX_BYTES) { "Vocabulary file is too large." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        val entries = VocabularyBackup.decode(bytes.toString(Charsets.UTF_8))
        val knownLemmas = corpus.wordDao().allLemmas().toSet()
        require(entries.all { it.lemma in knownLemmas }) { "Vocabulary file contains words outside this text." }
        personalDatabase.withTransaction {
            val dao = personalDatabase.userVocabDao()
            val existing = dao.getAll().associateBy { it.lemma }
            val changes = entries.filter { entry ->
                existing[entry.lemma]?.createdAt?.let { entry.createdAt < it } ?: true
            }
            dao.insertAll(changes)
            entries.count { it.lemma !in existing }
        }
    }
}
