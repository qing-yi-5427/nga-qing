package com.qingyi5427.ngaqing.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qingyi5427.ngaqing.ui.board.BoardScreen
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

private const val PAGE_TRANSITION_MILLIS = 280

@Composable
fun NgaNavHost(
    modifier: Modifier = Modifier,
    nav: NavHostController = rememberNavController(),
    root: RootViewModel = hiltViewModel()
) {
    val themeMode by root.themeMode.collectAsStateWithLifecycle()
    val authState by root.authState.collectAsStateWithLifecycle()
    val readingTextScale by root.readingTextScale.collectAsStateWithLifecycle()
    val readingLineSpacing by root.readingLineSpacing.collectAsStateWithLifecycle()
    val showSignatures by root.showSignatures.collectAsStateWithLifecycle()
    val startDestination = if (authState == true) Routes.BOARDS else Routes.LOGIN

    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    NgaQingTheme(
        themeMode = themeMode,
        readingTextScale = readingTextScale,
        readingLineSpacing = readingLineSpacing,
        showSignatures = showSignatures
    ) {
        if (authState == null) {
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
            NavHost(
                navController = nav,
                startDestination = startDestination,
                modifier = modifier,
                // Directional horizontal motion only: no crossfade. 280 ms keeps the
                // movement legible without making navigation feel delayed.
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(PAGE_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(PAGE_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
                    )
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(PAGE_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
                    )
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(PAGE_TRANSITION_MILLIS, easing = FastOutSlowInEasing)
                    )
                }
            ) {
            composable(Routes.LOGIN) { LoginScreen(nav) }
            composable(Routes.ADD_ACCOUNT) { LoginScreen(nav, addingAccount = true) }

            composable(Routes.BOARDS) { BoardScreen(nav) }

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
                val fid = backStack.arguments?.getString("fid") ?: ""
                val name = backStack.arguments?.getString("name") ?: ""
                ThreadListScreen(nav)
            }

            composable(
                route = Routes.POSTS,
                arguments = listOf(navArgument("tid") { type = NavType.StringType })
            ) { backStack ->
                val tid = backStack.arguments?.getString("tid") ?: ""
                PostScreen(nav, dark = dark)
            }

            composable(Routes.FAVORITES) { FavoritesScreen(nav) }

            composable(Routes.PROFILE) { ProfileScreen(nav) }

            composable(Routes.SETTINGS) { SettingsScreen(nav) }

            composable(Routes.HISTORY) { HistoryScreen(nav) }

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
                SearchScreen(nav, fid, stid)
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
                NewTopicScreen(nav)
            }

            composable(Routes.COMMUNITY) { CommunityScreen(nav) }

            composable(
                route = Routes.USER,
                arguments = listOf(
                    navArgument("uid") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { UserScreen(nav) }

            composable(
                route = Routes.WEB_EDITOR,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { WebEditorScreen(nav) }
        }
    }
}
}
