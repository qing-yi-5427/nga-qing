package com.qingyi5427.ngaqing.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.design.NgaGlassSurface

private data class TabDef(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

private val tabs = listOf(
    TabDef(Routes.BOARDS, "版块", Icons.Filled.Forum, Icons.Outlined.Forum),
    TabDef(Routes.FAVORITES, "收藏", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    TabDef(Routes.PROFILE, "我的", Icons.Filled.Person, Icons.Outlined.PersonOutline)
)

/** Includes the visible rail and its inset allowance inside the existing safe viewport. */
val RootNavigationRailWidth = 88.dp

/** Wide destinations provide this so their existing bottom dock calls become no-ops. */
val LocalRootNavigationRail = staticCompositionLocalOf { false }

private fun NavHostController.openRootTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.openBoardDirectory() {
    if (!popBackStack(Routes.BOARDS, inclusive = false)) {
        navigate(Routes.BOARDS) {
            popUpTo(graph.findStartDestination().id)
            launchSingleTop = true
        }
    }
}

/** Existing call sites keep their actions and title behavior; only the chrome is replaced. */
@Composable
fun AppTopBar(
    title: String,
    onTitleClick: (() -> Unit)? = null,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(windowInsets)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (navigationIcon != null) {
            NgaGlassSurface(shape = RoundedCornerShape(16.dp)) {
                Box(Modifier.heightIn(min = 52.dp), contentAlignment = Alignment.Center) {
                    navigationIcon()
                }
            }
        }
        Text(
            title,
            modifier = Modifier.weight(1f)
                .then(if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier)
                .padding(horizontal = 4.dp, vertical = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (actions != null) {
            NgaGlassSurface(shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }
    }
}

/** Compact root navigation floats over the content; callers place it in a root Box overlay. */
@Composable
fun AppBottomBar(nav: NavHostController, currentRoute: String) {
    if (LocalRootNavigationRail.current) return
    Box(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        NgaGlassSurface(shape = RoundedCornerShape(24.dp)) {
            Row(
                Modifier.padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    Column(
                        Modifier.width(70.dp).height(56.dp)
                            .testTag("root-tab-${tab.route}")
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                                else Color.Transparent,
                                RoundedCornerShape(18.dp)
                            )
                            .selectable(selected = selected, role = Role.Tab, onClick = {
                                if (!selected) nav.openRootTab(tab.route)
                            }),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            tab.label,
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/** N mark and all three tabs form one vertically centered group below system bars. */
@Composable
fun AppNavigationRail(
    nav: NavHostController,
    currentRoute: String,
    selectedRoute: String = currentRoute,
    safeInsets: WindowInsets = WindowInsets.safeDrawing,
    railWidth: androidx.compose.ui.unit.Dp = RootNavigationRailWidth
) {
    BoxWithConstraints(
        Modifier.width(railWidth).fillMaxHeight()
            .windowInsetsPadding(safeInsets.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)),
        contentAlignment = Alignment.Center
    ) {
        NgaGlassSurface(
            modifier = Modifier.heightIn(max = (maxHeight - 16.dp).coerceAtLeast(0.dp))
                .testTag("root-navigation-group"),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 3.dp
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(Modifier.width(56.dp).height(48.dp), contentAlignment = Alignment.Center) {
                    Text("N", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
                tabs.forEach { tab ->
                    val selected = selectedRoute == tab.route
                    Column(
                        Modifier.width(56.dp).height(60.dp)
                            .testTag("root-tab-${tab.route}")
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                                else Color.Transparent,
                                RoundedCornerShape(18.dp)
                            )
                            .selectable(selected = selected, role = Role.Tab, onClick = {
                                if (currentRoute != tab.route) {
                                    if (tab.route == Routes.BOARDS && selectedRoute == Routes.BOARDS) {
                                        nav.openBoardDirectory()
                                    } else {
                                        nav.openRootTab(tab.route)
                                    }
                                }
                            }),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            if (selected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            tab.label,
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
