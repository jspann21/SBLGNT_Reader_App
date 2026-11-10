package com.lexapps.sblgntreader.ui.screens.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.lexapps.sblgntreader.R
import java.util.Locale

@Composable
internal fun posLabelForKey(key: String): String = when (key.uppercase(Locale.ROOT)) {
    "V" -> stringResource(R.string.stats_pos_verbs)
    "NON_V" -> stringResource(R.string.stats_pos_non_verbs)
    "N" -> stringResource(R.string.stats_pos_nouns)
    "J" -> stringResource(R.string.stats_pos_adjectives)
    "T" -> stringResource(R.string.stats_pos_particles)
    "P" -> stringResource(R.string.stats_pos_prepositions)
    else -> key.uppercase(Locale.ROOT)
}
