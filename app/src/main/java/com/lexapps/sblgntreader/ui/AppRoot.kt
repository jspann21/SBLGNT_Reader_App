package com.lexapps.sblgntreader.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lexapps.sblgntreader.ui.screens.AboutScreen
import com.lexapps.sblgntreader.ui.screens.HomeScreen
import com.lexapps.sblgntreader.ui.screens.SettingsScreen
import com.lexapps.sblgntreader.ui.screens.StatsScreen
import com.lexapps.sblgntreader.ui.screens.VocabSettingsScreen
import com.lexapps.sblgntreader.ui.theme.AppTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun AppRoot(
    windowSizeClass: WindowSizeClass,
    vm: AppViewModel = hiltViewModel()
) {
    val themeModeIndex by vm.themeModeIndex.collectAsStateWithLifecycle()
    AppTheme(themeModeIndex = themeModeIndex) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val snackbar = remember { SnackbarHostState() }
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            LaunchedEffect(vm, lifecycle) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    vm.messages.collect { snackbar.showSnackbar(it) }
                }
            }
            Box(Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = "home",
                enterTransition = { fadeIn(animationSpec = tween(0)) },
                exitTransition = { fadeOut(animationSpec = tween(0)) },
                popEnterTransition = { fadeIn(animationSpec = tween(0)) },
                popExitTransition = { fadeOut(animationSpec = tween(0)) }
            ) {
                composable("home") {
                    HomeScreen(navController, windowSizeClass)
                }
                composable("settings") {
                    SettingsScreen(navController, windowSizeClass)
                }
                composable("stats") {
                    StatsScreen(navController, windowSizeClass)
                }
                composable("about") {
                    AboutScreen(navController, windowSizeClass)
                }
                composable("vocabSettings") {
                    VocabSettingsScreen(navController, windowSizeClass)
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp))
            }
        }
    }
}
