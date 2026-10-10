package com.qingyi5427.ngaqing.ui.chrome

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Exercises actual NavHost back stacks through the same tab controls used in the app. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w425dp-h623dp")
class RootTabNavigationRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun discoverBoardChildReturnsToHomeAndRepeatedHomeTapDoesNotDuplicateRoot() {
        val window = mutableStateOf(false)
        val nav = render(window)
        compose.onNodeWithText("标记首页 0").performClick()
        compose.onNodeWithText("常逛版块").performClick()
        assertEquals(Routes.THREADS, nav.currentDestination?.route)
        compose.onNodeWithTag("root-tab-${Routes.DISCOVER}").performClick()
        assertEquals(Routes.DISCOVER, nav.currentDestination?.route)
        compose.onNodeWithText("标记首页 1").assertIsDisplayed()
        compose.onNodeWithTag("root-tab-${Routes.DISCOVER}").performClick()
        assertEquals(Routes.DISCOVER, nav.currentDestination?.route)
        assertFalse("A repeated root tap must not create a second home", nav.popBackStack())
    }

    @Test
    @Config(sdk = [33], qualifiers = "w860dp-h608dp")
    fun railReturnsFromBoardChildAfterWindowChangeAndPreservesHomeState() {
        val window = mutableStateOf(false)
        val nav = render(window)
        compose.onNodeWithText("标记首页 0").performClick()
        compose.onNodeWithText("常逛版块").performClick()
        compose.runOnUiThread { window.value = true }
        compose.waitForIdle()
        compose.onNodeWithTag("root-navigation-group").assertIsDisplayed()
        compose.onNodeWithTag("root-tab-${Routes.DISCOVER}").performClick()
        assertEquals(Routes.DISCOVER, nav.currentDestination?.route)
        compose.onNodeWithText("标记首页 1").assertIsDisplayed()
    }

    @Test
    fun changingTabsWithSavedChildAlwaysOpensRootsAndKeepsBoardState() {
        val nav = render(mutableStateOf(false))
        compose.onNodeWithTag("root-tab-${Routes.BOARDS}").performClick()
        assertEquals(Routes.BOARDS, nav.currentDestination?.route)
        compose.onNodeWithText("标记版块 0").performClick()
        compose.onNodeWithText("打开版块帖子").performClick()
        assertEquals(Routes.THREADS, nav.currentDestination?.route)
        compose.onNodeWithTag("root-tab-${Routes.FAVORITES}").performClick()
        assertEquals(Routes.FAVORITES, nav.currentDestination?.route)
        compose.onNodeWithTag("root-tab-${Routes.DISCOVER}").performClick()
        assertEquals(Routes.DISCOVER, nav.currentDestination?.route)
        compose.onNodeWithTag("root-tab-${Routes.BOARDS}").performClick()
        assertEquals("A tab tap should open the board directory root", Routes.BOARDS,
            nav.currentDestination?.route)
        compose.onNodeWithText("标记版块 1").assertIsDisplayed()
        compose.onNodeWithTag("root-tab-${Routes.DISCOVER}").performClick()
        assertEquals(Routes.DISCOVER, nav.currentDestination?.route)
    }

    @Test
    fun directReturnFromBoardChildPreservesBoardRootState() {
        val nav = render(mutableStateOf(false))
        compose.onNodeWithTag("root-tab-${Routes.BOARDS}").performClick()
        compose.onNodeWithText("标记版块 0").performClick()
        compose.onNodeWithText("打开版块帖子").performClick()
        assertEquals(Routes.THREADS, nav.currentDestination?.route)
        compose.onNodeWithTag("root-tab-${Routes.DISCOVER}").performClick()
        assertEquals(Routes.DISCOVER, nav.currentDestination?.route)
        compose.onNodeWithTag("root-tab-${Routes.BOARDS}").performClick()
        assertEquals(Routes.BOARDS, nav.currentDestination?.route)
        compose.onNodeWithText("标记版块 1").assertIsDisplayed()
    }

    private fun render(wide: androidx.compose.runtime.MutableState<Boolean>): NavHostController {
        lateinit var controller: NavHostController
        compose.setContent {
            NgaQingTheme(themeMode = "light") {
                val nav = rememberNavController()
                controller = nav
                NavHost(navController = nav, startDestination = Routes.DISCOVER) {
                    composable(Routes.DISCOVER) {
                        var homeState by rememberSaveable { mutableIntStateOf(0) }
                        Screen(nav, Routes.DISCOVER, wide.value) {
                            Button(onClick = { homeState++ }) { Text("标记首页 $homeState") }
                            Button(onClick = { nav.navigate(Routes.threadRoute("501", "示例版块")) }) {
                                Text("常逛版块")
                            }
                        }
                    }
                    composable(Routes.BOARDS) {
                        var boardState by rememberSaveable { mutableIntStateOf(0) }
                        Screen(nav, Routes.BOARDS, wide.value) {
                            Button(onClick = { boardState++ }) { Text("标记版块 $boardState") }
                            Button(onClick = { nav.navigate(Routes.threadRoute("501", "示例版块")) }) {
                                Text("打开版块帖子")
                            }
                        }
                    }
                    composable(
                        route = Routes.THREADS,
                        arguments = listOf(
                            navArgument("fid") { type = NavType.StringType },
                            navArgument("name") { type = NavType.StringType },
                            navArgument("stid") { type = NavType.StringType; nullable = true; defaultValue = null }
                        )
                    ) {
                        Screen(nav, Routes.THREADS, wide.value, selected = Routes.BOARDS) {
                            Text("版块帖子")
                        }
                    }
                    composable(Routes.FAVORITES) {
                        Screen(nav, Routes.FAVORITES, wide.value) { Text("收藏页") }
                    }
                    composable(Routes.PROFILE) {
                        Screen(nav, Routes.PROFILE, wide.value) { Text("我的页") }
                    }
                }
            }
        }
        compose.waitForIdle()
        return controller
    }

    @Composable
    private fun Screen(
        nav: NavHostController,
        route: String,
        wide: Boolean,
        selected: String = route,
        content: @Composable () -> Unit
    ) {
        Box(Modifier.fillMaxSize().testTag("route-$route")) {
            Column { content() }
            if (wide) {
                Box(Modifier.align(Alignment.CenterStart)) {
                    AppNavigationRail(nav, currentRoute = route, selectedRoute = selected,
                        safeInsets = WindowInsets(0))
                }
            } else {
                Box(Modifier.align(Alignment.BottomCenter)) {
                    AppBottomBar(nav, currentRoute = route, selectedRoute = selected)
                }
            }
        }
    }
}
