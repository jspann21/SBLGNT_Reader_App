package com.lexapps.sblgntreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.lexapps.sblgntreader.R
import com.lexapps.sblgntreader.ui.stats.StatsViewModel
import com.lexapps.sblgntreader.ui.screens.stats.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    vm: StatsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val widthClass = windowSizeClass.widthSizeClass
    val horizontalPadding = when (widthClass) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 32.dp
        WindowWidthSizeClass.Expanded -> 48.dp
        else -> 16.dp
    }
    val maxContentWidth = when (widthClass) {
        WindowWidthSizeClass.Compact -> null
        WindowWidthSizeClass.Medium -> 720.dp
        WindowWidthSizeClass.Expanded -> 900.dp
        else -> null
    }
    val showFullBookNames = widthClass != WindowWidthSizeClass.Compact
    val statsData = state.stats
    val canShowContent = statsData != null && statsData.sources.isNotEmpty()
    var contentVisible by remember(statsData) { mutableStateOf(false) }
    LaunchedEffect(statsData) {
        if (canShowContent) {
            contentVisible = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.stats_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> StatsLoading()
                state.error != null -> StatsError(vm::retry)
                statsData == null || statsData.sources.isEmpty() -> StatsEmpty()
                else -> AnimatedVisibility(
                    visible = contentVisible,
                    enter = fadeIn(tween(durationMillis = 250)),
                    exit = fadeOut(tween(durationMillis = 150)),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    StatsContent(
                        stats = statsData,
                        bookSort = state.bookCoverageSort,
                        onBookSortChanged = vm::setBookCoverageSort,
                        showFullBookNames = showFullBookNames,
                        modifier = Modifier
                            .let { base -> if (maxContentWidth != null) base.widthIn(max = maxContentWidth) else base }
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    )
                }
            }
        }
    }
}
