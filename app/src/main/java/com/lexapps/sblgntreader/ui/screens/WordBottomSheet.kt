package com.lexapps.sblgntreader.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lexapps.sblgntreader.data.model.Word
import com.lexapps.sblgntreader.ui.reader.ReaderViewModel
import com.lexapps.sblgntreader.ui.text.PosInterpreter
import com.lexapps.sblgntreader.ui.theme.AppFonts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordBottomSheet(word: Word, onDismiss: () -> Unit, vm: ReaderViewModel = hiltViewModel()) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = {},
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp
    ) {
        val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.26f)
        val activeLevels = vm.activeVocabLevels.collectAsStateWithLifecycle().value
        val userVocabLemmas = vm.userVocabLemmas.collectAsStateWithLifecycle().value
        val lemma = word.lemma ?: word.text
        val isUserWord = userVocabLemmas.contains(lemma)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 12.dp)
                .navigationBarsPadding()
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = word.lemma ?: word.text,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = AppFonts.sblGreek,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HorizontalDivider(color = dividerColor)
            Section(label = "Gloss", value = word.gloss)
            Section(label = "Literal", value = word.literal)
            HorizontalDivider(color = dividerColor)
            Section(label = "Part of Speech", value = PosInterpreter.interpret(word.posTag))
            HorizontalDivider(color = dividerColor)
            Section(label = "Louw-Nida", value = word.louw)
            Section(label = "Strong's", value = word.strong)
            // Only show vocab lines that are active and have a valid level
            for ((name, level) in word.vocabLevels) {
                val active = activeLevels[name] != null
                if (active) {
                    HorizontalDivider(color = dividerColor)
                    Section(
                        label = "${word.courses[name]?.name ?: name} ${word.courses[name]?.levelLabel ?: "Chapter"}",
                        value = level.toString()
                    )
                }
            }
            if (isUserWord) {
                HorizontalDivider(color = dividerColor)
                UserIndicator()
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Section(label: String, value: String?) {
    Column(Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
        Text(value ?: "N/A", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun UserIndicator() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "User added",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
    }
}
