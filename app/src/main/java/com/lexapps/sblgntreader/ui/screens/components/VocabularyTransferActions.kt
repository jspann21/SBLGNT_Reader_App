package com.lexapps.sblgntreader.ui.screens.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lexapps.sblgntreader.ui.settings.VocabularyBackupViewModel

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun VocabularyTransferActions(vm: VocabularyBackupViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
        it?.let(vm::export)
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        it?.let(vm::importFrom)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(enabled = !state.busy, onClick = { exporter.launch("gnt-vocabulary.json") }) { Text("Export vocabulary") }
            OutlinedButton(enabled = !state.busy, onClick = { importer.launch(arrayOf("application/json", "text/plain")) }) { Text("Import vocabulary") }
        }
        state.message?.let { Text(it) }
    }
}
