package com.lexapps.sblgntreader.ui.screens.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexapps.sblgntreader.data.model.BookCoverage
import java.text.NumberFormat
import com.lexapps.sblgntreader.ui.util.formatBookNameForDisplay

@Composable
internal fun BookCoverageGrid(
    coverages: List<BookCoverage>,
    percentFormatter: NumberFormat,
    showFullBookNames: Boolean,
    animationProgress: Float
) {
    val columns = 3
    val spacing = 12.dp
    AnimatedContent(
        targetState = coverages,
        modifier = Modifier.fillMaxWidth()
    ) { animatedCoverages ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            val rows = animatedCoverages.chunked(columns)
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    rowItems.forEach { coverage ->
                        BookCoverageCard(
                            coverage = coverage,
                            percentFormatter = percentFormatter,
                            modifier = Modifier.weight(1f, fill = true),
                            showFullBookNames = showFullBookNames,
                            animationProgress = animationProgress
                        )
                    }
                    if (rowItems.size < columns) {
                        repeat(columns - rowItems.size) {
                            Spacer(
                                modifier = Modifier.weight(1f, fill = true)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BookCoverageCard(
    coverage: BookCoverage,
    percentFormatter: NumberFormat,
    modifier: Modifier = Modifier,
    showFullBookNames: Boolean,
    animationProgress: Float
) {
    val animatedProgress = animationProgress.coerceIn(0f, 1f)
    val ratio = coverage.readability.coerceIn(0f, 1f) * animatedProgress
    val percentText = percentFormatter.format(ratio.toDouble())
    val progressColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val bookTitle = if (showFullBookNames) {
        formatBookNameForDisplay(coverage.bookName)
    } else {
        bookAbbreviation(coverage.bookName)
    }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor,
                    modifier = Modifier.weight(1f, fill = true),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = percentText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                    color = textColor,
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            SblLinearProgress(
                progress = ratio,
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                height = 8.dp
            )
        }
    }
}
