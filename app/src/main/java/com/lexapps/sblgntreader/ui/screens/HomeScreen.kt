package com.lexapps.sblgntreader.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import com.lexapps.sblgntreader.ui.util.greekBookName
import com.lexapps.sblgntreader.ui.reader.ReaderViewModel
import com.lexapps.sblgntreader.ui.reader.ReaderScrollStates
import com.lexapps.sblgntreader.ui.theme.AppFonts
import com.lexapps.sblgntreader.ui.util.formatBookNameForDisplay
import com.lexapps.sblgntreader.R
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.lexapps.sblgntreader.data.model.ChapterNavigation
import com.lexapps.sblgntreader.data.model.ReadingLocation
import androidx.compose.runtime.snapshotFlow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import com.lexapps.sblgntreader.ui.screens.components.ReaderStatus

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    windowSizeClass: WindowSizeClass,
    vm: ReaderViewModel = hiltViewModel()
) {
    val readerState by vm.state.collectAsStateWithLifecycle()
    val books = readerState.books
    val chapterData = readerState.chapters
    val currentBookId = readerState.location?.bookId ?: 1
    val currentChapter = readerState.location?.chapter ?: 1
    val activeLevels by vm.activeVocabLevels.collectAsStateWithLifecycle()
    val userVocabLemmas by vm.userVocabLemmas.collectAsStateWithLifecycle()
    val vocabularyError by vm.vocabularyError.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var expandedBookId by rememberSaveable { mutableStateOf<Int?>(null) }
    val widthSizeClass = windowSizeClass.widthSizeClass
    val heightSizeClass = windowSizeClass.heightSizeClass
    
    // Helper function to get numeric value for size class comparison
    fun sizeClassValue(sizeClass: WindowWidthSizeClass): Int = when (sizeClass) {
        WindowWidthSizeClass.Compact -> 0
        WindowWidthSizeClass.Medium -> 1
        WindowWidthSizeClass.Expanded -> 2
        else -> 0
    }
    
    fun sizeClassValue(sizeClass: WindowHeightSizeClass): Int = when (sizeClass) {
        WindowHeightSizeClass.Compact -> 0
        WindowHeightSizeClass.Medium -> 1
        WindowHeightSizeClass.Expanded -> 2
        else -> 0
    }
    
    // Use modal drawer if:
    // - Width is compact (phones), OR
    // - Height >= Width (portrait orientation on tablets)
    // Use permanent drawer if:
    // - Width > Height (landscape orientation on tablets)
    val useModalDrawer = widthSizeClass == WindowWidthSizeClass.Compact ||
            (widthSizeClass != WindowWidthSizeClass.Compact && 
             sizeClassValue(heightSizeClass) >= sizeClassValue(widthSizeClass))

    val navigation = remember(books) { ChapterNavigation(books) }
    fun pageIndexOf(bookId: Int, chapter: Int) = navigation.indexOf(ReadingLocation(bookId, chapter)).coerceAtLeast(0)
    fun locationFromIndex(index: Int) = navigation.at(index)
    val pagerState = rememberPagerState(
        pageCount = { navigation.locations.size },
        initialPage = pageIndexOf(currentBookId, currentChapter)
    )
    val chapterScrollStates = rememberSaveable(saver = ReaderScrollStates.Saver) { ReaderScrollStates() }
    var pendingPagerTarget by remember { mutableStateOf<Int?>(null) }
    var synchronizingPager by remember { mutableStateOf(false) }
    LaunchedEffect(currentBookId, currentChapter, navigation) {
        if (books.isEmpty()) return@LaunchedEffect
        val target = pageIndexOf(currentBookId, currentChapter)
        synchronizingPager = true
        try {
            if (pagerState.currentPage != target) {
                if (pendingPagerTarget == target) pagerState.animateScrollToPage(target)
                else pagerState.scrollToPage(target)
            }
        } finally {
            synchronizingPager = false
            if (pendingPagerTarget == target) pendingPagerTarget = null
        }
    }
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(pagerState, navigation) {
        var wasScrolling = false
        snapshotFlow { pagerState.isScrollInProgress to pagerState.settledPage }.collect { (scrolling, page) ->
            if (scrolling) wasScrolling = true
            else if (wasScrolling) {
                wasScrolling = false
                if (!synchronizingPager && pendingPagerTarget == null) {
                    val location = navigation.at(page)
                    if (location != null && location != vm.state.value.location) {
                        vm.setCurrent(location.bookId, location.chapter)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
            }
        }
    }
    val snackbar = remember { SnackbarHostState() }
    val retryLabel = stringResource(R.string.action_retry)
    LaunchedEffect(readerState.message, readerState.messageId) {
        readerState.message?.let { message ->
            val retryLocationSave = readerState.retryLocationSave
            val result = snackbar.showSnackbar(
                message = message,
                actionLabel = retryLabel.takeIf { retryLocationSave },
                duration = if (retryLocationSave) SnackbarDuration.Indefinite else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && retryLocationSave) vm.retry()
            else vm.clearMessage()
        }
    }
    LaunchedEffect(vocabularyError) {
        vocabularyError?.let { error ->
            val result = snackbar.showSnackbar(
                message = error.message,
                actionLabel = retryLabel,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) vm.retryVocabulary()
        }
    }

    val readerHorizontalPadding = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> 0.dp
        WindowWidthSizeClass.Medium -> 32.dp
        WindowWidthSizeClass.Expanded -> 64.dp
        else -> 0.dp
    }
    val readerMaxWidth = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> null
        WindowWidthSizeClass.Medium -> 720.dp
        WindowWidthSizeClass.Expanded -> 960.dp
        else -> null
    }

    val prefs by vm.readerPrefs.collectAsStateWithLifecycle()
    // Tutorial interaction flags (must live in composition scope)
    var wasWordTapped by remember { mutableStateOf(false) }
    var wasWordLongPressed by remember { mutableStateOf(false) }
    val currentBook = books.firstOrNull { it.id == currentBookId }
    val appBarTitle = currentBook?.let { b ->
        val name = if (prefs.useGreekBookNames) greekBookName(b.name) else formatBookNameForDisplay(b.name)
        if (b.chapterCount == 1) name else "$name $currentChapter"
    } ?: "SBLGNT"

    // Quickstart coach mark target bounds
    val menuRectState = remember { mutableStateOf<Rect?>(null) }
    val settingsRectState = remember { mutableStateOf<Rect?>(null) }
    val contentRectState = remember { mutableStateOf<Rect?>(null) }
    val menuRect by menuRectState
    val settingsRect by settingsRectState
    val contentRect by contentRectState


    val drawerNotifiedEdge = remember { mutableStateOf<String?>(null) }
    val drawerListState = rememberLazyListState()
    var drawerWasScrolling by remember { mutableStateOf(false) }

    val drawerSheetContent: @Composable ColumnScope.() -> Unit = {
        LaunchedEffect(drawerListState.isScrollInProgress) {
            if (drawerListState.isScrollInProgress) {
                drawerWasScrolling = true
            } else if (drawerWasScrolling) {
                drawerWasScrolling = false
                val atTop = drawerListState.firstVisibleItemIndex == 0 && drawerListState.firstVisibleItemScrollOffset == 0
                val lastVisible = drawerListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                val atBottom = lastVisible != null && books.isNotEmpty() && lastVisible >= (books.size - 1)
                val edge = when {
                    atTop -> "top"
                    atBottom -> "bottom"
                    else -> null
                }
                if (edge != null && edge != drawerNotifiedEdge.value) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    drawerNotifiedEdge.value = edge
                } else if (edge == null) {
                    drawerNotifiedEdge.value = null
                }
            }
        }
        LazyColumn(
            state = drawerListState,
            contentPadding = PaddingValues(top = 48.dp, bottom = 16.dp)
        ) {
            items(
                items = books,
                key = { book -> book.id }
            ) { book ->
                BookDrawerItem(
                    book = book,
                    isExpanded = expandedBookId == book.id,
                    isCurrentBook = currentBookId == book.id,
                    useGreekBookNames = prefs.useGreekBookNames,
                    onToggle = {
                        expandedBookId = if (expandedBookId == book.id) null else book.id
                    },
                    currentChapter = if (currentBookId == book.id) currentChapter else null,
                    onSelectChapter = { ch ->
                        val targetIndex = pageIndexOf(book.id, ch)
                        pendingPagerTarget = targetIndex.takeIf { vm.state.value.location != ReadingLocation(book.id, ch) }
                        vm.setCurrent(book.id, ch)
                        if (useModalDrawer) {
                            scope.launch { drawerState.close() }
                        }
                    },
                    isScreenWide = widthSizeClass != WindowWidthSizeClass.Compact
                )
            }
        }
        LaunchedEffect(expandedBookId) {
            expandedBookId?.let { bookId ->
                val bookIndex = books.indexOfFirst { it.id == bookId }
                if (bookIndex != -1) {
                    drawerListState.animateScrollToItem(index = bookIndex)
                }
            }
        }
    }

    LaunchedEffect(useModalDrawer) {
        if (!useModalDrawer) {
            menuRectState.value = null
        }
    }

    val appScaffold: @Composable () -> Unit = {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            appBarTitle,
                            fontFamily = if (prefs.useGreekBookNames) AppFonts.sblGreek else FontFamily.Default
                        )
                    },
                    navigationIcon = {
                        if (useModalDrawer) {
                            IconButton(
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    menuRectState.value = coords.boundsInRoot()
                                },
                                onClick = {
                                    if (drawerState.isOpen) {
                                        scope.launch { drawerState.close() }
                                    } else {
                                        scope.launch { drawerState.open() }
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu")
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            modifier = Modifier.onGloballyPositioned { coords ->
                                settingsRectState.value = coords.boundsInRoot()
                            },
                            onClick = { navController.navigate("settings") }
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    ReaderStatus(readerState.error, vm::retry)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .testTag("reader-pager")
                        .fillMaxSize()
                        .padding(padding)
                        .onGloballyPositioned { coords -> contentRectState.value = coords.boundsInRoot() }
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = readerHorizontalPadding)
                            .graphicsLayer {
                                val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val scale = lerp(0.85f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                                scaleX = scale
                                scaleY = scale
                                alpha = lerp(0.5f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))
                            }
                    ) {
                        val loc = locationFromIndex(page)
                        if (loc == null) {
                            Text(stringResource(R.string.home_invalid_page))
                        } else {
                            val data = chapterData["${loc.bookId}:${loc.chapter}"]
                            if (data == null) {
                                ReaderStatus(readerState.error, vm::retry)
                            } else {
                                ReaderChapter(
                                    data, prefs, activeLevels, userVocabLemmas, readerMaxWidth,
                                    listState = chapterScrollStates.forChapter(loc, prefs.isParagraphMode),
                                    onWordTapped = { wasWordTapped = true },
                                    onWordLongPressed = { wasWordLongPressed = true },
                                    onSetVocabulary = vm::setUserVocab
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (useModalDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { ModalDrawerSheet { drawerSheetContent() } }
        ) {
            appScaffold()
        }
    } else {
        PermanentNavigationDrawer(
            drawerContent = { PermanentDrawerSheet { drawerSheetContent() } }
        ) {
            appScaffold()
        }
    }

    val quickstartActive by vm.quickstartActive.collectAsStateWithLifecycle()
    ReaderQuickstart(
        active = quickstartActive,
        useModalDrawer = useModalDrawer,
        drawerOpen = drawerState.isOpen,
        currentPage = pagerState.currentPage,
        wordTapped = wasWordTapped,
        wordLongPressed = wasWordLongPressed,
        targets = QuickstartTargets(menuRect, settingsRect, contentRect),
        onOpenDrawer = { scope.launch { drawerState.open() } },
        onOpenSettings = { navController.navigate("settings") },
        onOpenVocabulary = { navController.navigate("vocabSettings") },
        onComplete = vm::markQuickstartCompleted,
        onReset = { wasWordTapped = false; wasWordLongPressed = false }
    )
}
