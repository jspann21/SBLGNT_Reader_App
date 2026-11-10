package com.lexapps.sblgntreader.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexapps.sblgntreader.data.model.PosCoverage
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun PosCoverageSection(
    coverages: List<PosCoverage>,
    percentFormatter: NumberFormat,
    animationProgress: Float
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val groups = remember(coverages) { buildPosCoverageGroups(coverages) }
        groups.forEach { group ->
            PosCoverageGroupCard(
                group = group,
                percentFormatter = percentFormatter,
                animationProgress = animationProgress
            )
        }
    }
}

@Composable
internal fun PosCoverageGroupCard(
    group: PosCoverageGroup,
    percentFormatter: NumberFormat,
    animationProgress: Float
) {
    val titleLabels = mutableListOf<String>()
    for (entry in group.entries) {
        titleLabels += posLabelForKey(entry.key)
    }
    val titleText = titleLabels.joinToString(" • ")
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (group.entries.size == 1) {
                val entry = group.entries.first()
                val ratio = entry.coverage.coerceIn(0f, 1f) * animationProgress.coerceIn(0f, 1f)
                val percentText = percentFormatter.format(ratio.toDouble())
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f, fill = true)
                        )
                        Text(
                            text = percentText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SblLinearProgress(
                        progress = ratio,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        height = 8.dp
                    )
                }
            } else {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
                group.entries.forEachIndexed { index, entry ->
                    PosCoverageEntryRow(
                        coverage = entry,
                        percentFormatter = percentFormatter,
                        animationProgress = animationProgress
                    )
                    if (index != group.entries.lastIndex) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun PosCoverageEntryRow(
    coverage: PosCoverage,
    percentFormatter: NumberFormat,
    animationProgress: Float
) {
    val ratio = coverage.coverage.coerceIn(0f, 1f) * animationProgress.coerceIn(0f, 1f)
    val percentText = percentFormatter.format(ratio.toDouble())
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = posLabelForKey(coverage.key),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f, fill = true)
            )
            Text(
                text = percentText,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        SblLinearProgress(
            progress = ratio,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            height = 8.dp
        )
    }
}

internal data class PosCoverageGroup(
    val entries: List<PosCoverage>
)

private val orderedPosKeyGroups = listOf(
    listOf("V", "NON_V"),
    listOf("N"),
    listOf("J"),
    listOf("T"),
    listOf("P")
)

internal fun buildPosCoverageGroups(coverages: List<PosCoverage>): List<PosCoverageGroup> {
    val map = coverages.associateBy { it.key.uppercase(Locale.ROOT) }
    val used = mutableSetOf<String>()
    val groups = mutableListOf<PosCoverageGroup>()
    orderedPosKeyGroups.forEach { groupKeys ->
        val entries = groupKeys.mapNotNull { key ->
            map[key]?.also { used.add(key) }
        }
        if (entries.isNotEmpty()) {
            groups += PosCoverageGroup(entries)
        }
    }
    coverages.forEach { coverage ->
        val key = coverage.key.uppercase(Locale.ROOT)
        if (key !in used) {
            groups += PosCoverageGroup(listOf(coverage))
        }
    }
    return groups
}
