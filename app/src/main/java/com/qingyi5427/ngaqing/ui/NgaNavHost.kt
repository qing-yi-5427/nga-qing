package com.qingyi5427.ngaqing.ui

import android.app.Activity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.rememberDrawerState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qingyi5427.ngaqing.ui.board.BoardScreen
import com.qingyi5427.ngaqing.ui.board.boardIdentityKey
import com.qingyi5427.ngaqing.ui.adaptive.PaneLayout
import com.qingyi5427.ngaqing.ui.adaptive.FoldBounds
import com.qingyi5427.ngaqing.ui.adaptive.FoldGeometry
import com.qingyi5427.ngaqing.ui.adaptive.calculatePaneLayout
import com.qingyi5427.ngaqing.ui.adaptive.decideV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy
import com.qingyi5427.ngaqing.ui.discover.DiscoverScreen
import com.qingyi5427.ngaqing.ui.login.LoginScreen
import com.qingyi5427.ngaqing.ui.favorites.FavoritesScreen
import com.qingyi5427.ngaqing.ui.post.PostScreen
import com.qingyi5427.ngaqing.ui.profile.ProfileScreen
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.search.SearchScreen
import com.qingyi5427.ngaqing.ui.settings.SettingsScreen
import com.qingyi5427.ngaqing.ui.history.HistoryScreen
import com.qingyi5427.ngaqing.ui.thread.ThreadListScreen
import com.qingyi5427.ngaqing.ui.thread.ThreadListViewModel
import com.qingyi5427.ngaqing.data.model.Board
import com.qingyi5427.ngaqing.ui.compose.NewTopicScreen
import com.qingyi5427.ngaqing.ui.community.CommunityScreen
import com.qingyi5427.ngaqing.ui.user.UserScreen
import com.qingyi5427.ngaqing.ui.web.WebEditorScreen
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import com.qingyi5427.ngaqing.ui.chrome.AppNavigationRail
import com.qingyi5427.ngaqing.ui.chrome.LocalRootNavigationRail
import com.qingyi5427.ngaqing.ui.chrome.openRootTab
import com.qingyi5427.ngaqing.ui.chrome.RootNavigationRailWidth
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.min

// Compose tweens honor the platform MotionDurationScale; reduced motion (scale 0) snaps.
private const val CHILD_TRANSITION_MILLIS = 180
private const val TAB_TRANSITION_MILLIS = 120

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pageEnter(
    expanded: Boolean,
    hasHinge: Boolean,
    shortDistancePx: Int,
    popping: Boolean
): EnterTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return when (navigationMotionKind(from, to, expanded, hasHinge)) {
        NavigationMotionKind.NONE -> EnterTransition.None
        NavigationMotionKind.ROOT_TAB -> fadeIn(
            animationSpec = tween(TAB_TRANSITION_MILLIS), initialAlpha = 0f)
        NavigationMotionKind.CHILD -> {
            if (popping) return EnterTransition.None
            val direction = navigationDirection(from, to, popping)
            slideInHorizontally(
                initialOffsetX = { width -> childEnterOffset(width, shortDistancePx, direction) },
                animationSpec = tween(CHILD_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
            ) + fadeIn(initialAlpha = 0f, animationSpec = tween(150))
        }
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.pageExit(
    expanded: Boolean,
    hasHinge: Boolean,
    shortDistancePx: Int,
    popping: Boolean
): ExitTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return when (navigationMotionKind(from, to, expanded, hasHinge)) {
        NavigationMotionKind.NONE -> ExitTransition.None
        NavigationMotionKind.ROOT_TAB -> fadeOut(animationSpec = tween(TAB_TRANSITION_MILLIS))
        NavigationMotionKind.CHILD -> if (popping) {
            slideOutHorizontally(
                targetOffsetX = { width -> childEnterOffset(width, shortDistancePx, 1) },
                animationSpec = tween(CHILD_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
            ) + fadeOut(targetAlpha = 0f, animationSpec = tween(150))
        } else ExitTransition.None
    }
}

@Composable
fun NgaNavHost(
    modifier: Modifier = Modifier,
    root: RootViewModel = hiltViewModel()
) {
    val authSession by root.authSession.collectAsStateWithLifecycle()
    val themeMode by root.themeMode.collectAsStateWithLifecycle()
    val readingTextScale by root.readingTextScale.collectAsStateWithLifecycle()
    val readingLineSpacing by root.readingLineSpacing.collectAsStateWithLifecycle()
    val showSignatures by root.showSignatures.collectAsStateWithLifecycle()
    val startDestination = if (!authSession?.uid.isNullOrBlank() && !authSession?.cid.isNullOrBlank()) {
        Routes.DISCOVER
    } else Routes.LOGIN
    val activity = LocalContext.current as Activity
    val windowLayoutFlow = remember(activity) {
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity)
    }
    val windowFeatures by remember(windowLayoutFlow) {
        windowLayoutFlow.map { it.displayFeatures }
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    var previousNav by remember { mutableStateOf<NavHostController?>(null) }
    // Revision changes on login, logout and every account switch, including A → B → A.
    // Keying the tree replaces the controller; clear old entries so their ViewModelStores close.
    key(authSession?.uid, authSession?.cid, authSession?.revision) {
    val nav = rememberNavController()
    var readerFocusMode by rememberSaveable { mutableStateOf(false) }
    var readerPaneWidth by rememberSaveable { mutableFloatStateOf(300f) }
    LaunchedEffect(nav) {
        previousNav?.takeIf { it !== nav }?.let { old ->
            runCatching { old.graph }.getOrNull()?.let { graph ->
                old.popBackStack(graph.id, inclusive = true)
            }
        }
        previousNav = nav
    }
    SideEffect {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
    NgaQingTheme(
        themeMode = themeMode,
        readingTextScale = readingTextScale,
        readingLineSpacing = readingLineSpacing,
        showSignatures = showSignatures
    ) {
        if (authSession == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@NgaQingTheme
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground
        ) {
            var windowOrigin by remember { mutableStateOf(Offset.Zero) }
            val density = LocalDensity.current
            BoxWithConstraints(
                Modifier.fillMaxSize().onGloballyPositioned {
                    windowOrigin = it.positionInWindow()
                }
            ) {
                val windowWidth = maxWidth.value
                val windowHeight = maxHeight.value
                val safeTop = with(density) { WindowInsets.safeDrawing.getTop(density).toDp().value }
                val safeBottom = with(density) { WindowInsets.safeDrawing.getBottom(density).toDp().value }
                val safeLeft = with(density) { WindowInsets.safeDrawing.getLeft(density,
                    LocalLayoutDirection.current).toDp().value }
                val safeRight = with(density) { WindowInsets.safeDrawing.getRight(density,
                    LocalLayoutDirection.current).toDp().value }
                val contentHeight = (windowHeight - safeTop - safeBottom).coerceAtLeast(0f)
                val contentWidth = (windowWidth - safeLeft - safeRight).coerceAtLeast(0f)
                val fold = windowFeatures.filterIsInstance<FoldingFeature>()
                    .firstOrNull {
                        it.isSeparating || it.occlusionType == FoldingFeature.OcclusionType.FULL
                    }
                    ?.let { feature ->
                        with(density) {
                            FoldGeometry(
                                bounds = FoldBounds(
                                    (feature.bounds.left - windowOrigin.x).toDp().value,
                                    (feature.bounds.top - windowOrigin.y).toDp().value,
                                    (feature.bounds.right - windowOrigin.x).toDp().value,
                                    (feature.bounds.bottom - windowOrigin.y).toDp().value
                                ),
                                vertical = feature.orientation == FoldingFeature.Orientation.VERTICAL,
                                separating = true
                            )
                        }
                    }
                val panes = calculatePaneLayout(windowWidth, windowHeight, fold,
                    fontScale = density.fontScale, contentHeight = contentHeight,
                    contentWidth = contentWidth)
                val policy = decideV8LayoutPolicy(windowWidth, windowHeight,
                    density.fontScale, fold, contentHeight, contentWidth)
                val shortDistancePx = with(density) { 32.dp.roundToPx() }
                val hasHinge = fold?.vertical == true
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.width(panes.viewportWidth.dp)
                            .offset(x = panes.viewportLeft.dp)
                            .offset(y = panes.viewportTop.dp)
                            .height(panes.viewportHeight.dp)
                            .clipToBounds()
                    ) {
                        CompositionLocalProvider(LocalV8LayoutPolicy provides policy) {
                        NavHost(
                            navController = nav,
                            startDestination = startDestination,
                            modifier = modifier.fillMaxSize(),
                            enterTransition = { pageEnter(panes.isTwoPane, hasHinge, shortDistancePx, popping = false) },
                            exitTransition = { pageExit(panes.isTwoPane, hasHinge, shortDistancePx, popping = false) },
                            popEnterTransition = { pageEnter(panes.isTwoPane, hasHinge, shortDistancePx, popping = true) },
                            popExitTransition = { pageExit(panes.isTwoPane, hasHinge, shortDistancePx, popping = true) }
                        ) {
                        composable(Routes.LOGIN) { SinglePane(panes) { LoginScreen(nav) } }
                        composable(Routes.ADD_ACCOUNT) { SinglePane(panes) { LoginScreen(nav, addingAccount = true) } }

                        composable(Routes.DISCOVER) {
                            RootPane(panes, nav, Routes.DISCOVER) { DiscoverScreen(nav) }
                        }

                        composable(Routes.BOARDS) {
                            RootPane(panes, nav, Routes.BOARDS) { BoardScreen(nav) }
                        }

                        composable(
                            route = Routes.THREADS,
                            arguments = listOf(
                                navArgument("fid") { type = NavType.StringType },
                                navArgument("name") { type = NavType.StringType },
                                navArgument("stid") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStack ->
                            val boardEntry = remember(backStack) {
                                nav.previousBackStackEntry?.takeIf { it.destination.route == Routes.BOARDS }
                            }
                            val contentPanes = if (panes.isTwoPane && panes.gutterWidth <= 1f) {
                                val width = readerPaneWidth.coerceIn(250f,
                                    minOf(370f, panes.viewportWidth - 461f))
                                panes.copy(listWidth = width,
                                    detailWidth = panes.viewportWidth - width - panes.gutterWidth)
                            } else panes
                            val showDual = contentPanes.isTwoPane
                            val openBoard: (com.qingyi5427.ngaqing.data.model.Board) -> Unit = { board ->
                                if (showDual) {
                                    nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid)) {
                                        popUpTo(Routes.THREADS) { inclusive = true }
                                    }
                                } else {
                                    nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
                                }
                            }
                            val drawerState = rememberDrawerState(DrawerValue.Closed)
                            val drawerScope = rememberCoroutineScope()
                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                gesturesEnabled = drawerState.isOpen,
                                drawerContent = {
                                    ModalDrawerSheet(Modifier.widthIn(max = 340.dp)
                                        .fillMaxWidth().fillMaxHeight()) {
                                        Row(Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly) {
                                            listOf(
                                                Routes.DISCOVER to "发现", Routes.BOARDS to "版块",
                                                Routes.FAVORITES to "收藏", Routes.PROFILE to "我的"
                                            ).forEach { (route, label) ->
                                                TextButton(onClick = {
                                                    drawerScope.launch { drawerState.close() }
                                                    nav.openRootTab(route)
                                                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(label) }
                                            }
                                        }
                                        if (drawerState.isOpen) {
                                            BoardScreen(nav,
                                                viewModel = hiltViewModel(boardEntry ?: backStack),
                                                showBottomBar = false,
                                                onOpenBoard = { board ->
                                                    drawerScope.launch { drawerState.close() }
                                                    openBoard(board)
                                                })
                                        }
                                    }
                                }
                            ) {
                            val threadListVm: ThreadListViewModel = hiltViewModel(backStack)
                            ReadingPane(contentPanes, nav, Routes.THREADS) { readingPanes ->
                                ThreadRouteContent(readingPanes, nav, root, threadListVm,
                                    onOpenBoard = openBoard,
                                    onShowBoards = { drawerScope.launch { drawerState.open() } })
                            }
                            }
                        }

                        composable(
                            route = Routes.POSTS,
                            arguments = listOf(navArgument("tid") { type = NavType.StringType })
                        ) { backStack ->
                            val tid = backStack.arguments?.getString("tid") ?: ""
                            val threadEntry = remember(backStack) {
                                nav.previousBackStackEntry?.takeIf { it.destination.route == Routes.THREADS }
                            }
                            val contentPanes = if (panes.isTwoPane && panes.gutterWidth <= 1f) {
                                val width = readerPaneWidth.coerceIn(250f,
                                    minOf(370f, panes.viewportWidth - 461f))
                                panes.copy(listWidth = width,
                                    detailWidth = panes.viewportWidth - width - panes.gutterWidth)
                            } else panes
                            val postLayout = resolvePostRouteLayout(contentPanes,
                                hasThreadSource = threadEntry != null, focusMode = readerFocusMode)
                            val canFocus = postLayout.canFocus
                            val showDual = postLayout.showDual
                            val drawerState = rememberDrawerState(DrawerValue.Closed)
                            val drawerScope = rememberCoroutineScope()
                            ModalNavigationDrawer(
                                drawerState = drawerState,
                                gesturesEnabled = drawerState.isOpen,
                                drawerContent = {
                                    ModalDrawerSheet(Modifier.widthIn(max = 340.dp)
                                        .fillMaxWidth().fillMaxHeight()) {
                                        Row(Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly) {
                                            listOf(
                                                Routes.DISCOVER to "发现", Routes.BOARDS to "版块",
                                                Routes.FAVORITES to "收藏", Routes.PROFILE to "我的"
                                            ).forEach { (route, label) ->
                                                TextButton(onClick = {
                                                    drawerScope.launch { drawerState.close() }
                                                    nav.openRootTab(route)
                                                }, modifier = Modifier.heightIn(min = 48.dp)) { Text(label) }
                                            }
                                        }
                                        if (drawerState.isOpen) {
                                            BoardScreen(nav, viewModel = hiltViewModel(backStack),
                                                showBottomBar = false,
                                                onOpenBoard = { board ->
                                                    drawerScope.launch { drawerState.close() }
                                                    nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
                                                })
                                        }
                                    }
                                }
                            ) {
                            ReadingPane(contentPanes, nav, Routes.POSTS) { readingPanes ->
                                PostRouteContent(readingPanes, postLayout,
                                    readerPaneWidth = readerPaneWidth,
                                    onReaderPaneWidthChange = { readerPaneWidth = it },
                                    threadList = {
                                        threadEntry?.let { sourceEntry ->
                                            ThreadListScreen(
                                                nav = nav,
                                                viewModel = hiltViewModel(sourceEntry),
                                                root = root,
                                                selectedTid = tid,
                                                onOpenPost = { selected ->
                                                    if (selected != tid) {
                                                        nav.navigate(Routes.postRoute(selected)) {
                                                            popUpTo(Routes.POSTS) { inclusive = true }
                                                        }
                                                    }
                                                },
                                                onOpenBoard = { board ->
                                                    nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid)) {
                                                        popUpTo(Routes.THREADS) { inclusive = true }
                                                    }
                                                },
                                                onShowBoards = { drawerScope.launch { drawerState.open() } }
                                            )
                                        }
                                    },
                                    post = {
                                        PostScreen(nav, dark = dark, root = root,
                                            isFocusMode = canFocus && readerFocusMode,
                                            onToggleFocus = if (canFocus) ({ readerFocusMode = !readerFocusMode }) else null,
                                            onShowBoards = { drawerScope.launch { drawerState.open() } },
                                            boardName = threadEntry?.arguments?.getString("name"))
                                    })
                            }
                            }
                        }

                        composable(Routes.FAVORITES) {
                            RootPane(panes, nav, Routes.FAVORITES) { FavoritesScreen(nav) }
                        }

                        composable(Routes.PROFILE) {
                            RootPane(panes, nav, Routes.PROFILE) { ProfileScreen(nav) }
                        }

                        composable(Routes.SETTINGS) { SinglePane(panes) { SettingsScreen(nav) } }

                        composable(Routes.HISTORY) { SinglePane(panes) { HistoryScreen(nav) } }

                        composable(
                            route = Routes.SEARCH,
                            arguments = listOf(
                                navArgument("fid") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("stid") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStack ->
                            val fid = backStack.arguments?.getString("fid")
                            val stid = backStack.arguments?.getString("stid")
                            SinglePane(panes) { SearchScreen(nav, fid, stid) }
                        }

                        composable(
                            route = Routes.NEW_TOPIC,
                            arguments = listOf(
                                navArgument("fid") { type = NavType.StringType },
                                navArgument("name") { type = NavType.StringType },
                                navArgument("stid") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) {
                            SinglePane(panes) { NewTopicScreen(nav) }
                        }

                        composable(Routes.COMMUNITY) { SinglePane(panes) { CommunityScreen(nav) } }

                        composable(
                            route = Routes.USER,
                            arguments = listOf(
                                navArgument("uid") { type = NavType.StringType },
                                navArgument("name") { type = NavType.StringType }
                            )
                        ) { SinglePane(panes) { UserScreen(nav) } }

                        composable(
                            route = Routes.WEB_EDITOR,
                            arguments = listOf(
                                navArgument("url") { type = NavType.StringType },
                                navArgument("title") { type = NavType.StringType }
                            )
                        ) { SinglePane(panes) { WebEditorScreen(nav) } }
                    }
                    }
                }
            }
        }
    }
}

}

}

@Composable
private fun SinglePane(panes: PaneLayout, content: @Composable () -> Unit) {
    PaneContent(panes, safeSingle = true, content = content)
}

@Composable
private fun effectiveRootRailWidth(insets: WindowInsets = WindowInsets.safeDrawing): androidx.compose.ui.unit.Dp {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val startInset = with(density) { insets.getLeft(density, layoutDirection).toDp() }
    return RootNavigationRailWidth + startInset
}

/** The rail consumes real width; preserve two readable panes whenever both minimums still fit. */
internal fun railAdjustedPaneLayout(panes: PaneLayout, railWidth: Float): PaneLayout {
    if (!panes.isTwoPane || panes.gutterWidth > 1f) return panes
    val remaining = (panes.viewportWidth - railWidth).coerceAtLeast(0f)
    val minimumList = 280f
    val minimumDetail = 360f
    val gutter = 1f
    if (remaining < minimumList + minimumDetail + gutter) {
        return panes.copy(
            viewportLeft = 0f, viewportWidth = remaining,
            listWidth = 0f, gutterWidth = 0f, detailWidth = remaining,
            singlePaneLeft = 0f, singlePaneWidth = remaining
        )
    }
    val list = min(360f, (remaining * 0.38f).coerceAtLeast(minimumList))
    return panes.copy(
        viewportLeft = 0f, viewportWidth = remaining,
        listWidth = list, gutterWidth = gutter, detailWidth = remaining - list - gutter,
        singlePaneLeft = 0f, singlePaneWidth = remaining
    )
}

/** Reading routes retain the same controller and entries while adding the board tab rail. */
@Composable
internal fun ReadingPane(
    panes: PaneLayout,
    nav: NavHostController,
    route: String,
    railInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable (PaneLayout) -> Unit
) {
    // HTML V8 keeps exactly the content panes; navigation is available through
    // the root masthead and the reader's temporary board drawer.
    CompositionLocalProvider(LocalRootNavigationRail provides
        (route == Routes.THREADS && panes.isTwoPane)) { content(panes) }
}

internal data class PostRouteLayout(val canFocus: Boolean, val showDual: Boolean)

/** A direct post from favorites, history or a deep link has no list to restore. */
internal fun resolvePostRouteLayout(
    panes: PaneLayout,
    hasThreadSource: Boolean,
    focusMode: Boolean
): PostRouteLayout {
    val canFocus = panes.isTwoPane && hasThreadSource
    return PostRouteLayout(canFocus = canFocus, showDual = canFocus && !focusMode)
}

/** The production post composition, shared with full-window visual regression tests. */
@Composable
internal fun PostRouteContent(
    panes: PaneLayout,
    layout: PostRouteLayout,
    readerPaneWidth: Float,
    onReaderPaneWidthChange: (Float) -> Unit,
    threadList: @Composable () -> Unit,
    post: @Composable () -> Unit
) {
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxSize()) {
            if (layout.showDual) {
                Box(Modifier.width(panes.listWidth.dp).fillMaxHeight()) { threadList() }
                Spacer(Modifier.width(panes.gutterWidth.dp).fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant))
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                PaneContent(panes, safeSingle = !layout.showDual, maxContentWidth = 720.dp) {
                    post()
                }
            }
        }
        if (layout.showDual && panes.gutterWidth <= 1f) {
            val maxWidth = minOf(370f, panes.viewportWidth - 461f)
            Box(Modifier.offset(x = (panes.listWidth - 12f).dp)
                .width(24.dp).fillMaxHeight()
                .semantics {
                    contentDescription = "调整主题列表宽度"
                    customActions = listOf(
                        CustomAccessibilityAction("加宽主题列表") {
                            onReaderPaneWidthChange((readerPaneWidth + 20f).coerceAtMost(maxWidth))
                            true
                        },
                        CustomAccessibilityAction("缩窄主题列表") {
                            onReaderPaneWidthChange((readerPaneWidth - 20f).coerceAtLeast(250f))
                            true
                        }
                    )
                }
                .pointerInput(panes.viewportWidth) {
                    detectDragGestures { change, drag ->
                        onReaderPaneWidthChange((readerPaneWidth + drag.x / density.density)
                            .coerceIn(250f, maxWidth))
                        change.consume()
                    }
                }, contentAlignment = Alignment.Center) {
                Box(Modifier.width(4.dp).height(48.dp)
                    .background(MaterialTheme.colorScheme.outline,
                        androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
            }
        }
    }
}

/** One thread-list VM stays on the back stack as the article's left pane. */
@Composable
internal fun ThreadRouteContent(
    panes: PaneLayout,
    nav: NavHostController,
    root: RootViewModel,
    viewModel: ThreadListViewModel,
    onOpenBoard: (Board) -> Unit,
    onShowBoards: () -> Unit
) {
    if (panes.isTwoPane) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(panes.listWidth.dp).fillMaxHeight()) {
                ThreadListScreen(nav, viewModel = viewModel, root = root,
                    onOpenBoard = onOpenBoard, onShowBoards = onShowBoards)
            }
            Spacer(Modifier.width(panes.gutterWidth.dp).fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant))
            Box(Modifier.weight(1f).fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .testTag("thread-empty-reader"), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("选择主题开始阅读", style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("从左侧主题列表选择一篇帖子",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    } else {
        PaneContent(panes, safeSingle = true) {
            ThreadListScreen(nav, viewModel = viewModel, root = root,
                onOpenBoard = onOpenBoard, onShowBoards = onShowBoards)
        }
    }
}

@Composable
internal fun RootPane(
    panes: PaneLayout,
    nav: NavHostController,
    route: String,
    railInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable () -> Unit
) {
    // Each root route owns one safe canvas; the masthead provides wide navigation.
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.offset(x = panes.singlePaneLeft.dp)
            .width(panes.singlePaneWidth.dp).fillMaxHeight()) {
            RootContentPolicy(content = content)
        }
    }
}

/** Root screens decide their own columns and type after rail/hinge space is allocated. */
@Composable
private fun RootContentPolicy(
    railConsumesStartInset: Boolean = false,
    content: @Composable () -> Unit
) {
    val parent = LocalV8LayoutPolicy.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val endInset = if (railConsumesStartInset) with(density) {
            WindowInsets.safeDrawing.getRight(density, direction).toDp().value
        } else 0f
        val inheritedWidth = parent.availableWidthDp.takeIf { it > 0f } ?: maxWidth.value
        val inheritedHeight = parent.availableHeightDp.takeIf { it > 0f } ?: maxHeight.value
        val contentWidth = minOf(inheritedWidth,
            (maxWidth.value - endInset).coerceAtLeast(0f))
        val contentHeight = minOf(inheritedHeight, maxHeight.value)
        val rootPolicy = decideV8LayoutPolicy(contentWidth, contentHeight, density.fontScale)
            .copy(readerTwoPane = parent.readerTwoPane)
        CompositionLocalProvider(
            LocalV8LayoutPolicy provides rootPolicy,
            LocalRootNavigationRail provides
                (rootPolicy.availableWidthDp >= 840f && !rootPolicy.compactHeight)
        ) { content() }
    }
}

@Composable
private fun PaneContent(
    panes: PaneLayout,
    safeSingle: Boolean,
    maxContentWidth: androidx.compose.ui.unit.Dp = 760.dp,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.offset(x = (if (safeSingle) panes.singlePaneLeft else 0f).dp)
                .width((if (safeSingle) panes.singlePaneWidth else panes.detailWidth).dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(Modifier.widthIn(max = maxContentWidth).fillMaxSize()) { content() }
        }
    }
}
