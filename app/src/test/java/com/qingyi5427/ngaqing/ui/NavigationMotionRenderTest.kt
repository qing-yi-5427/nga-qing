package com.qingyi5427.ngaqing.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qingyi5427.ngaqing.ui.design.saveNativeScreenshot
import com.qingyi5427.ngaqing.ui.theme.NgaQingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Frame-stepped navigation using the production NavHost transitions. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w320dp-h568dp")
class NavigationMotionRenderTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun childPushAndPopMoveOnlyAShortDistanceAtIntermediateFrame() {
        lateinit var nav: NavHostController
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MotionTestHost(expanded = false, onController = { nav = it },
                first = "list", second = "detail") { route ->
                Box(Modifier.fillMaxSize().background(
                    if (route == "list") Color(0xFFE8EDF3) else Color(0xFF3F78B2))
                    .testTag(route))
            }
        }
        var listEdgePixel = 0
        var listCenterPixel = 0
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            listEdgePixel = bitmap.getPixel(3, bitmap.height / 2)
            listCenterPixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        }
        compose.runOnUiThread { nav.navigate("detail") }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(64L)
        val entered = compose.onNodeWithTag("detail").getUnclippedBoundsInRoot()
        assertTrue("Forward destination must enter within 32dp, not from a screen edge: $entered",
            entered.left >= 0.dp && entered.left <= 32.dp)
        var pushEdgePixel = 0
        var pushCenterPixel = 0
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            pushEdgePixel = bitmap.getPixel(3, bitmap.height / 2)
            pushCenterPixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        }
        assertEquals("The outgoing list must be visible at the exposed edge during push",
            listEdgePixel, pushEdgePixel)
        assertNotEquals("The new page must fade into the middle frame", listCenterPixel, pushCenterPixel)
        screenshot("motion-child-push-mid.png")
        compose.mainClock.advanceTimeBy(240L)
        assertEquals(0.dp, compose.onNodeWithTag("detail").getUnclippedBoundsInRoot().left)
        var detailCenterPixel = 0
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            assertNotEquals("The destination must actually replace the list", listEdgePixel,
                bitmap.getPixel(3, bitmap.height / 2))
            detailCenterPixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        }
        assertNotEquals("Middle frame must not jump straight to final opacity",
            detailCenterPixel, pushCenterPixel)

        compose.runOnUiThread { nav.popBackStack() }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(64L)
        val returned = compose.onNodeWithTag("list").getUnclippedBoundsInRoot()
        val departing = compose.onNodeWithTag("detail").getUnclippedBoundsInRoot()
        assertEquals("The underlying list stays fixed on pop", 0.dp, returned.left)
        assertTrue("The departing page reveals the list within 32dp: $departing",
            departing.left >= 0.dp && departing.left <= 32.dp)
        var popEdgePixel = 0
        var popCenterPixel = 0
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            popEdgePixel = bitmap.getPixel(3, bitmap.height / 2)
            popCenterPixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        }
        assertEquals("The returning list must be visible during the pop frame",
            listEdgePixel, popEdgePixel)
        assertNotEquals("Departing page must fade toward the list during pop", detailCenterPixel,
            popCenterPixel)
        assertNotEquals("Pop middle frame must not jump straight to the list", listCenterPixel,
            popCenterPixel)
        screenshot("motion-child-pop-mid.png")
        compose.mainClock.advanceTimeBy(240L)
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            assertEquals("Pop must finish on the unmodified list pixel", listCenterPixel,
                bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
        }
    }

    @Test fun rootTabsFadeWithoutHorizontalTravel() {
        lateinit var nav: NavHostController
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MotionTestHost(expanded = false, onController = { nav = it },
                first = Routes.BOARDS, second = Routes.FAVORITES) { route ->
                Box(Modifier.fillMaxSize().background(
                    if (route == Routes.BOARDS) Color(0xFFE8EDF3) else Color(0xFF3F78B2))
                    .testTag(route))
            }
        }
        var beforePixel = 0
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            beforePixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        }
        compose.runOnUiThread { nav.navigate(Routes.FAVORITES) }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(48L)
        assertEquals(0.dp, compose.onNodeWithTag(Routes.FAVORITES).getUnclippedBoundsInRoot().left)
        var middlePixel = 0
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            middlePixel = bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)
        }
        assertNotEquals("Tab content should be midway through a fade", beforePixel, middlePixel)
        screenshot("motion-root-tab-mid.png")
        compose.mainClock.advanceTimeBy(160L)
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            assertNotEquals("Tab fade should progress after the middle frame", middlePixel,
                bitmap.getPixel(bitmap.width / 2, bitmap.height / 2))
        }
    }

    @Test
    @Config(sdk = [33], qualifiers = "w840dp-h600dp")
    fun wideSharedPaneKeepsLeftPixelsFixedWhileDetailChanges() {
        lateinit var nav: NavHostController
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MotionTestHost(expanded = true, onController = { nav = it },
                first = Routes.BOARDS, second = "detail") { route ->
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(220.dp).fillMaxHeight()
                        .background(Color(0xFFB4C4D2)).testTag("shared-left"))
                    Box(Modifier.weight(1f).fillMaxHeight()
                        .background(if (route == Routes.BOARDS) Color(0xFFE8EDF3)
                            else Color(0xFF3F78B2)).testTag("detail"))
                }
            }
        }
        var before = 0
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            before = bitmap.getPixel(bitmap.width / 10, bitmap.height / 2)
        }
        compose.runOnUiThread { nav.navigate("detail") }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(48L)
        var during = 0
        compose.runOnUiThread {
            val bitmap = compose.activity.window.decorView.drawToBitmap()
            during = bitmap.getPixel(bitmap.width / 10, bitmap.height / 2)
        }
        assertEquals("Shared left content must not fade or travel with the detail", before, during)
        assertEquals(0.dp, compose.onNodeWithTag("shared-left").getUnclippedBoundsInRoot().left)
        screenshot("motion-wide-shared-mid.png")
    }

    @Composable
    private fun MotionTestHost(
        expanded: Boolean,
        onController: (NavHostController) -> Unit,
        first: String,
        second: String,
        content: @Composable (String) -> Unit
    ) {
        NgaQingTheme(themeMode = "light") {
            Surface(Modifier.fillMaxSize()) {
                val nav = rememberNavController()
                onController(nav)
                val distance = with(LocalDensity.current) { 32.dp.roundToPx() }
                NavHost(
                    navController = nav,
                    startDestination = first,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { pageEnter(expanded, false, distance, popping = false) },
                    exitTransition = { pageExit(expanded, false, distance, popping = false) },
                    popEnterTransition = { pageEnter(expanded, false, distance, popping = true) },
                    popExitTransition = { pageExit(expanded, false, distance, popping = true) }
                ) {
                    composable(first) { content(first) }
                    composable(second) { content(second) }
                }
            }
        }
    }

    private fun screenshot(name: String) {
        compose.runOnUiThread { saveNativeScreenshot(compose.activity, name) }
    }
}
