package com.lexapps.sblgntreader.ui.screens.stats

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lexapps.sblgntreader.R
import com.lexapps.sblgntreader.data.model.LearnedStats
import com.lexapps.sblgntreader.data.model.SourceStats
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun StatsSummaryCard(
    learnedWords: String,
    totalWords: String,
    percentText: String,
    progress: Float,
    lemmaSummary: String
) {
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                SblCircularProgress(
                    progress = progress,
                    modifier = Modifier.fillMaxSize(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 24.dp
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.stats_overall_progress_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.stats_overall_words, learnedWords, totalWords),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = lemmaSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun SourceStatsCard(
    source: SourceStats,
    stats: LearnedStats,
    integerFormatter: NumberFormat,
    percentFormatter: NumberFormat,
    animationProgress: Float
) {
    ElevatedCard {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val label = when (source.key.lowercase(Locale.ROOT)) {
                "user" -> stringResource(R.string.stats_source_user)
                "proper" -> stringResource(R.string.stats_source_proper)
                else -> source.displayName
            }
            val subtitle = when {
                source.level != null -> stringResource(R.string.stats_course_level, source.levelLabel, source.level)
                source.key.equals("user", ignoreCase = true) -> stringResource(R.string.stats_source_user_subtitle)
                else -> null
            }
            val headerColor = MaterialTheme.colorScheme.onSurface
            val clampedProgress = animationProgress.coerceIn(0f, 1f)
            val ratio = stats.sourceRatioOfTotal(source).coerceIn(0f, 1f) * clampedProgress
            val contribution = percentFormatter.format(ratio.toDouble())

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = true)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = headerColor
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (source.hasProgress) {
                        val animatedLemmaCount = (source.lemmaCount * clampedProgress).roundToInt()
                        val animatedWordCount = (source.wordCount * clampedProgress).roundToInt()
                        Spacer(modifier = Modifier.height(6.dp))
                        val countsText = stringResource(
                            R.string.stats_source_counts,
                            integerFormatter.format(animatedLemmaCount),
                            integerFormatter.format(animatedWordCount)
                        )
                        Text(
                            text = countsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.stats_source_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = contribution,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

