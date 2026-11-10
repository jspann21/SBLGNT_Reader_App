package com.lexapps.sblgntreader.ui.screens.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lexapps.sblgntreader.R
import com.lexapps.sblgntreader.data.model.LearnedStats
import com.lexapps.sblgntreader.ui.stats.BookCoverageSort
import java.text.NumberFormat
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsContent(
    stats: LearnedStats,
    bookSort: BookCoverageSort,
    onBookSortChanged: (BookCoverageSort) -> Unit,
    showFullBookNames: Boolean,
    modifier: Modifier = Modifier
) {
    val locale = LocalLocale.current.platformLocale
    val integerFormatter = remember(locale) { NumberFormat.getIntegerInstance(locale) }
    val percentFormatter = remember(locale) {
        NumberFormat.getPercentInstance(locale).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
        }
    }
    val animation = remember { Animatable(0f) }
    LaunchedEffect(stats) {
        animation.snapTo(0f)
        animation.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
    }
    val animationProgress = animation.value.coerceIn(0f, 1f)

    val learnedRatio = stats.learnedWordRatio.coerceIn(0f, 1f)
    val animatedLearnedWords = (stats.totalLearnedWordCount * animationProgress).roundToInt()
    val animatedTotalWords = (stats.totalWordCount * animationProgress).roundToInt()
    val animatedLemmaCount = (stats.totalLearnedLemmaCount * animationProgress).roundToInt()
    val animatedLearnedRatio = learnedRatio * animationProgress
    val learnedWordsText = integerFormatter.format(animatedLearnedWords)
    val totalWordsText = integerFormatter.format(animatedTotalWords)
    val percentText = percentFormatter.format(animatedLearnedRatio.toDouble())
    val totalLemmaText = integerFormatter.format(animatedLemmaCount)
    val bookCoverageList = remember(stats.bookCoverage, bookSort) {
        when (bookSort) {
            BookCoverageSort.CANONICAL -> stats.bookCoverage.toList()
            BookCoverageSort.MOST_READABLE -> stats.bookCoverage.sortedByDescending { it.readability }
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            StatsSummaryCard(
                learnedWords = learnedWordsText,
                totalWords = totalWordsText,
                percentText = percentText,
                progress = animatedLearnedRatio,
                lemmaSummary = stringResource(R.string.stats_overall_lemmas, totalLemmaText)
            )
        }

        if (stats.sources.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.stats_breakdown_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    stats.sources.forEach { source ->
                        SourceStatsCard(
                            source = source,
                            stats = stats,
                            integerFormatter = integerFormatter,
                            percentFormatter = percentFormatter,
                            animationProgress = animationProgress
                        )
                    }
                }
            }
        }

        if (stats.posCoverage.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.stats_pos_section_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            item {
                PosCoverageSection(
                    coverages = stats.posCoverage,
                    percentFormatter = percentFormatter,
                    animationProgress = animationProgress
                )
            }
        }

        if (bookCoverageList.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.stats_books_section_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    SingleChoiceSegmentedButtonRow {
                        val options = listOf(BookCoverageSort.CANONICAL, BookCoverageSort.MOST_READABLE)
                        options.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = option == bookSort,
                                onClick = { if (option != bookSort) onBookSortChanged(option) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                label = {
                                    val label = when (option) {
                                        BookCoverageSort.CANONICAL -> stringResource(R.string.stats_book_sort_canonical)
                                        BookCoverageSort.MOST_READABLE -> stringResource(R.string.stats_book_sort_most_readable)
                                    }
                                    Text(text = label, style = MaterialTheme.typography.bodySmall)
                                }
                            )
                        }
                    }
                }
            }
            item {
                BookCoverageGrid(
                    coverages = bookCoverageList,
                    percentFormatter = percentFormatter,
                    showFullBookNames = showFullBookNames,
                    animationProgress = animationProgress
                )
            }
        }

        item {
            Text(
                text = stringResource(R.string.stats_suggestions_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (stats.suggestions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.stats_suggestions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(stats.suggestions, key = { it.lemma }) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    integerFormatter = integerFormatter,
                    animationProgress = animationProgress
                )
            }
        }
    }
}
