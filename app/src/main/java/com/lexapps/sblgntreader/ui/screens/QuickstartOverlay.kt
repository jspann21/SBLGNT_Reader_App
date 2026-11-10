package com.lexapps.sblgntreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.lexapps.sblgntreader.R

enum class QuickstartStep {
    BookMenu,
    Settings,
    VocabSystems,
    TapWord,
    LongPressWord,
    SwipeChapters
}

data class QuickstartTargets(
    val menuRect: Rect? = null,
    val settingsRect: Rect? = null,
    val contentRect: Rect? = null,
)

@Composable
fun QuickstartOverlay(
    step: QuickstartStep,
    targets: QuickstartTargets,
    onNext: () -> Unit,
    onAdvance: () -> Unit,
    onTapTarget: (() -> Unit)? = null,
    onOpenVocabSettings: (() -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {

        // Tooltip / Card positioning
        val bubblePadding = 16.dp
        val bubbleMaxWidth = 320.dp

        val steps = QuickstartStep.entries
        val totalSteps = steps.size
        val currentStepNumber = steps.indexOf(step).takeIf { it >= 0 }?.plus(1) ?: totalSteps

        val bubblePos = computeBubblePosition(step, targets)
        val bubbleModifier = Modifier
            .padding(bubblePadding)
            .align(bubblePos.align)
            .padding(top = bubblePos.topPad, bottom = bubblePos.bottomPad)

        Box(bubbleModifier) {
            when (step) {
                QuickstartStep.BookMenu -> QuickstartBubble(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    stepNumber = currentStepNumber,
                    totalSteps = totalSteps,
                    title = stringResource(R.string.quickstart_choose_book_title),
                    body = stringResource(R.string.quickstart_choose_book_body),
                    primaryLabel = if (onTapTarget != null) {
                        stringResource(R.string.action_open_menu)
                    } else {
                        stringResource(R.string.action_next)
                    },
                    onPrimary = {
                        onNext()
                        onTapTarget?.invoke()
                    },
                    onTrailing = onAdvance,
                    trailingLabel = stringResource(R.string.action_next)
                )
                QuickstartStep.Settings -> QuickstartBubble(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    stepNumber = currentStepNumber,
                    totalSteps = totalSteps,
                    title = stringResource(R.string.quickstart_settings_title),
                    body = stringResource(R.string.quickstart_settings_body),
                    primaryLabel = if (onTapTarget != null) {
                        stringResource(R.string.action_open_settings)
                    } else {
                        stringResource(R.string.action_next)
                    },
                    onPrimary = {
                        onNext()
                        onTapTarget?.invoke()
                    },
                    onTrailing = onAdvance,
                    trailingLabel = stringResource(R.string.action_next)
                )
                QuickstartStep.VocabSystems -> QuickstartBubble(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    stepNumber = currentStepNumber,
                    totalSteps = totalSteps,
                    title = stringResource(R.string.quickstart_vocab_title),
                    body = stringResource(R.string.quickstart_vocab_body),
                    primaryLabel = if (onOpenVocabSettings != null) {
                        stringResource(R.string.action_open_vocabulary)
                    } else {
                        stringResource(R.string.action_got_it)
                    },
                    onPrimary = {
                        onNext()
                        onOpenVocabSettings?.invoke()
                    },
                    onTrailing = onAdvance,
                    trailingLabel = stringResource(R.string.action_next)
                )
                QuickstartStep.TapWord -> QuickstartBubble(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    stepNumber = currentStepNumber,
                    totalSteps = totalSteps,
                    title = stringResource(R.string.quickstart_word_details_title),
                    body = stringResource(R.string.quickstart_word_details_body),
                    primaryLabel = stringResource(R.string.action_got_it),
                    onPrimary = onNext,
                    onTrailing = onAdvance,
                    trailingLabel = stringResource(R.string.action_next)
                )
                QuickstartStep.LongPressWord -> QuickstartBubble(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    stepNumber = currentStepNumber,
                    totalSteps = totalSteps,
                    title = stringResource(R.string.quickstart_add_vocab_title),
                    body = stringResource(R.string.quickstart_add_vocab_body),
                    primaryLabel = stringResource(R.string.action_got_it),
                    onPrimary = onNext,
                    onTrailing = onAdvance,
                    trailingLabel = stringResource(R.string.action_next)
                )
                QuickstartStep.SwipeChapters -> QuickstartBubble(
                    modifier = Modifier.widthIn(max = bubbleMaxWidth),
                    stepNumber = currentStepNumber,
                    totalSteps = totalSteps,
                    title = stringResource(R.string.quickstart_swipe_chapters_title),
                    body = stringResource(R.string.quickstart_swipe_chapters_body),
                    primaryLabel = stringResource(R.string.action_finish),
                    onPrimary = onNext,
                    onTrailing = onAdvance,
                    trailingLabel = stringResource(R.string.action_next)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun QuickstartBubble(
    stepNumber: Int,
    totalSteps: Int,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onTrailing: () -> Unit,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.quickstart_step_label, stepNumber, totalSteps),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onPrimary) { Text(primaryLabel) }
                TextButton(
                    onClick = onTrailing
                ) {
                    val resolvedLabel = trailingLabel ?: stringResource(R.string.action_next)
                    Text(resolvedLabel)
                }
            }
        }
    }
}

private data class BubblePlacement(
    val align: Alignment,
    val topPad: Dp = 0.dp,
    val bottomPad: Dp = 0.dp,
)

@Composable
private fun computeBubblePosition(
    step: QuickstartStep,
    targets: QuickstartTargets
): BubblePlacement {
    val density = LocalDensity.current
    val menu = targets.menuRect
    val settings = targets.settingsRect
    return when (step) {
        QuickstartStep.BookMenu -> BubblePlacement(align = Alignment.TopStart, topPad = with(density) { ((menu?.bottom ?: 0f)).toDp() + 8.dp })
        QuickstartStep.Settings -> BubblePlacement(align = Alignment.TopEnd, topPad = with(density) { ((settings?.bottom ?: 0f)).toDp() + 8.dp })
        QuickstartStep.VocabSystems -> {
            val settingsBottom = settings?.bottom ?: 0f
            val anchorTop = with(density) { settingsBottom.toDp() }
            val topPad = if (settingsBottom > 0f) {
                val candidate = anchorTop + 16.dp
                if (candidate < 96.dp) 96.dp else candidate
            } else 96.dp
            BubblePlacement(align = Alignment.TopEnd, topPad = topPad)
        }
        QuickstartStep.TapWord -> BubblePlacement(align = Alignment.Center)
        QuickstartStep.LongPressWord -> BubblePlacement(align = Alignment.Center)
        QuickstartStep.SwipeChapters -> BubblePlacement(align = Alignment.BottomCenter, bottomPad = 32.dp)
    }
}
