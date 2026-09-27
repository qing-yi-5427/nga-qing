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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
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
import com.qingyi5427.ngaqing.ui.login.LoginScreen
import com.qingyi5427.ngaqing.ui.favorites.FavoritesScreen
import com.qingyi5427.ngaqing.ui.post.PostScreen
import com.qingyi5427.ngaqing.ui.profile.ProfileScreen
import com.qingyi5427.ngaqing.ui.root.RootViewModel
import com.qingyi5427.ngaqing.ui.search.SearchScreen
import com.qingyi5427.ngaqing.ui.settings.SettingsScreen
import com.qingyi5427.ngaqing.ui.history.HistoryScreen
import com.qingyi5427.ngaqing.ui.thread.ThreadListScreen
import com.qingyi5427.ngaqing.ui.compose.NewTopicScreen
import com.qingyi5427.ngaqing.ui.community.CommunityScreen
import com.qingyi5427.ngaqing.ui.user.UserScreen
import com.qingyi5427.ngaqing.ui.web.WebEditorScreen
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.flow.map

private const val COMPACT_TRANSITION_MILLIS = 280
private const val EXPANDED_TRANSITION_MILLIS = 220

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageEnter(
    expanded: Boolean,
    hasHinge: Boolean,
    shortDistancePx: Int,
    popping: Boolean
): EnterTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    val duration = if (expanded) EXPANDED_TRANSITION_MILLIS else COMPACT_TRANSITION_MILLIS
    if (hasHinge || (expanded && isSharedListTransition(from, to))) {
        return fadeIn(animationSpec = tween(duration), initialAlpha = 0f)
    }
    val direction = navigationDirection(from, to, popping)
    val backStyle = popping && !isRootTabSwitch(from, to)
    return slideInHorizontally(
        initialOffsetX = { width ->
            direction * if (expanded) shortDistancePx.coerceAtMost(width)
            else if (backStyle) width / 5 else width
        },
        animationSpec = tween(duration, easing = FastOutSlowInEasing)
    ) + fadeIn(
        initialAlpha = if (expanded) 0.82f else 0.9f,
        animationSpec = tween(duration)
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pageExit(
    expanded: Boolean,
    hasHinge: Boolean,
    shortDistancePx: Int,
    popping: Boolean
): ExitTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    val duration = if (expanded) EXPANDED_TRANSITION_MILLIS else COMPACT_TRANSITION_MILLIS
    if (hasHinge || (expanded && isSharedListTransition(from, to))) {
        return fadeOut(animationSpec = tween(duration))
    }
    val direction = navigationDirection(from, to, popping)
    val backStyle = popping && !isRootTabSwitch(from, to)
    return slideOutHorizontally(
        targetOffsetX = { width ->
            -direction * if (expanded) (shortDistancePx / 2).coerceAtMost(width)
            else if (backStyle) width else width / 5
        },
        animationSpec = tween(duration, easing = FastOutSlowInEasing)
    ) + fadeOut(
        targetAlpha = if (expanded) 0.7f else 0.78f,
        animationSpec = tween(duration)
    )
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
        Routes.BOARDS
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
    LaunchedEffect(nav) {
        previousNav?.takeIf { it !== nav }?.let { old ->
            runCatching { old.graph }.getOrNull()?.let { graph ->
                old.popBackStack(graph.id, inclusive = true)
            }
        }
        previousNav = nav
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
                val panes = calculatePaneLayout(windowWidth, windowHeight, fold)
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

                        composable(Routes.BOARDS) { SinglePane(panes) { BoardScreen(nav) } }

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
                            val showDual = panes.isTwoPane && boardEntry != null
                            val openBoard: (com.qingyi5427.ngaqing.data.model.Board) -> Unit = { board ->
                                if (showDual) {
                                    nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid)) {
                                        popUpTo(Routes.THREADS) { inclusive = true }
                                    }
                                } else {
                                    nav.navigate(Routes.threadRoute(board.fid, board.name, board.stid))
                                }
                            }
                            Row(Modifier.fillMaxSize()) {
                                boardEntry?.takeIf { showDual }?.let { sourceEntry ->
                                    Box(Modifier.width(panes.listWidth.dp).fillMaxHeight()) {
                                        BoardScreen(
                                            nav,
                                            viewModel = hiltViewModel(sourceEntry),
                                            showBottomBar = false,
                                            selectedBoardKey = boardIdentityKey(
                                                backStack.arguments?.getString("fid").orEmpty(),
                                                backStack.arguments?.getString("stid")
                                            ),
                                            onOpenBoard = openBoard
                                        )
                                    }
                                    Spacer(Modifier.width(panes.gutterWidth.dp))
                                }
                                Box(Modifier.weight(1f).fillMaxHeight()) {
                                    PaneContent(panes, safeSingle = !showDual) {
                                        ThreadListScreen(nav, onOpenBoard = openBoard)
                                    }
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
                            val showDual = panes.isTwoPane && threadEntry != null
                            Row(Modifier.fillMaxSize()) {
                                threadEntry?.takeIf { showDual }?.let { sourceEntry ->
                                    Box(Modifier.width(panes.listWidth.dp).fillMaxHeight()) {
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
                                            }
                                        )
                                    }
                                    Spacer(Modifier.width(panes.gutterWidth.dp))
                                }
                                Box(Modifier.weight(1f).fillMaxHeight()) {
                                    PaneContent(panes, safeSingle = !showDual) { PostScreen(nav, dark = dark) }
                                }
                            }
                        }

                        composable(Routes.FAVORITES) { SinglePane(panes) { FavoritesScreen(nav) } }

                        composable(Routes.PROFILE) { SinglePane(panes) { ProfileScreen(nav) } }

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

@Composable
private fun SinglePane(panes: PaneLayout, content: @Composable () -> Unit) {
    PaneContent(panes, safeSingle = true, content = content)
}

@Composable
private fun PaneContent(
    panes: PaneLayout,
    safeSingle: Boolean,
    content: @Composable () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.offset(x = (if (safeSingle) panes.singlePaneLeft else 0f).dp)
                .width((if (safeSingle) panes.singlePaneWidth else panes.detailWidth).dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(Modifier.widthIn(max = 760.dp).fillMaxSize()) { content() }
        }
    }
}
