package com.lexapps.sblgntreader.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lexapps.sblgntreader.R
import com.lexapps.sblgntreader.data.model.SuggestedLemma
import com.lexapps.sblgntreader.ui.text.PosInterpreter
import com.lexapps.sblgntreader.ui.theme.AppFonts
import java.text.NumberFormat
import kotlin.math.roundToInt

@Composable
internal fun SuggestionCard(
    suggestion: SuggestedLemma,
    integerFormatter: NumberFormat,
    animationProgress: Float
) {
    val progress = animationProgress.coerceIn(0f, 1f)
    val animatedGain = (suggestion.wordGain * progress).roundToInt()
    val gainText = stringResource(
        R.string.stats_suggestion_gain,
        integerFormatter.format(animatedGain)
    )
    val posDescription = suggestion.primaryPos
        ?.takeIf { it.isNotBlank() }
        ?.let { PosInterpreter.interpret(it) }
    ElevatedCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = suggestion.lemma,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFonts.sblGreek
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = gainText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End
                )
            }
            if (!posDescription.isNullOrBlank()) {
                Text(
                    text = posDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
