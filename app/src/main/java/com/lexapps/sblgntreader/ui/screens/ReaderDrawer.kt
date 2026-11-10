package com.lexapps.sblgntreader.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lexapps.sblgntreader.data.model.Book
import com.lexapps.sblgntreader.ui.theme.AppFonts
import com.lexapps.sblgntreader.ui.util.formatBookNameForDisplay
import kotlinx.coroutines.delay
import com.lexapps.sblgntreader.ui.util.greekBookName

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)


@Composable
internal fun BookDrawerItem(
    book: Book,
    isExpanded: Boolean,
    isCurrentBook: Boolean,
    useGreekBookNames: Boolean,
    onToggle: () -> Unit,
    currentChapter: Int?,
    onSelectChapter: (Int) -> Unit,
    isScreenWide: Boolean
) {
    // Use consistent chip widths to ensure 6 columns fit in all cases
    val chapterChipMinWidth = 40.dp
    val chapterChipMaxWidth = 48.dp
    val chapterChipSpacing = 6.dp

    Column(Modifier.fillMaxWidth()) {
        val displayName = if (useGreekBookNames) greekBookName(book.name) else formatBookNameForDisplay(book.name)
        NavigationDrawerItem(
            label = {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrentBook) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = if (useGreekBookNames) AppFonts.sblGreek else FontFamily.Default
                )
            },
            selected = isCurrentBook,
            onClick = { if (book.chapterCount == 1) onSelectChapter(1) else onToggle() },
            badge = {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (isExpanded) "Collapse chapters" else "Expand chapters"
                )
            },
            colors = NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )
        val bringRequester = remember { BringIntoViewRequester() }
        LaunchedEffect(isExpanded) {
            if (isExpanded) {
                // Wait for expansion animation/layout to finish, then scroll
                delay(150)
                bringRequester.bringIntoView()
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = sharedAxisYEnter(),
            exit = sharedAxisYExit()
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .bringIntoViewRequester(bringRequester),
                horizontalArrangement = Arrangement.spacedBy(chapterChipSpacing),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (ch in 1..book.chapterCount) {
                    val selected = currentChapter == ch && isCurrentBook
                    FilterChip(
                        modifier = Modifier.testTag("chapter-${book.id}-$ch").widthIn(min = chapterChipMinWidth, max = chapterChipMaxWidth),
                        selected = selected,
                        onClick = { onSelectChapter(ch) },
                        label = {
                            Text(
                                ch.toString(),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                fontSize = if (isScreenWide) 12.sp else 14.sp
                            )
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}


private val ExpressiveIncomingEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val ExpressiveOutgoingEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private const val ExpressiveExpandDuration = 450
private const val ExpressiveCollapseDuration = 320

private fun sharedAxisYEnter(): EnterTransition {
    return fadeIn(animationSpec = tween(durationMillis = ExpressiveExpandDuration, easing = ExpressiveIncomingEasing)) +
        slideInVertically(
            animationSpec = tween(durationMillis = ExpressiveExpandDuration, easing = ExpressiveIncomingEasing)
        ) { fullHeight -> fullHeight / 4 }
}

private fun sharedAxisYExit(): ExitTransition {
    return fadeOut(animationSpec = tween(durationMillis = ExpressiveCollapseDuration, easing = ExpressiveOutgoingEasing)) +
        slideOutVertically(
            animationSpec = tween(durationMillis = ExpressiveCollapseDuration, easing = ExpressiveOutgoingEasing)
        ) { fullHeight -> fullHeight / 4 }
}
