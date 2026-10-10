package com.qingyi5427.ngaqing.ui.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.qingyi5427.ngaqing.ui.Routes
import com.qingyi5427.ngaqing.ui.adaptive.LocalV8LayoutPolicy

/** The lightweight root masthead from the V8 HTML. It shares the real root navigation. */
@Composable
fun EditorialRootTopBar(
    nav: NavHostController,
    currentRoute: String,
    section: String,
    modifier: Modifier = Modifier
) {
    val policy = LocalV8LayoutPolicy.current
    val wide = policy.availableWidthDp >= 840f && !policy.compactHeight
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
            .heightIn(min = if (wide) 63.dp else 61.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("NGA Qing", fontSize = if (wide) 13.sp else 12.sp,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (wide) {
            Text("  /  $section", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        if (wide) {
            Surface(shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                        Routes.DISCOVER to "发现", Routes.BOARDS to "版块",
                        Routes.FAVORITES to "收藏", Routes.PROFILE to "我的"
                    ).forEach { (route, label) ->
                        val selected = route == currentRoute
                        Box(Modifier.heightIn(min = 48.dp)
                            .selectable(selected = selected, role = Role.Tab,
                                onClick = { if (!selected) nav.openRootTab(route) }),
                            contentAlignment = Alignment.Center) {
                            Box(Modifier.background(if (selected) MaterialTheme.colorScheme.surface
                                else Color.Transparent, RoundedCornerShape(10.dp))
                                .heightIn(min = 36.dp).padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center) {
                                Text(label, fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.size(16.dp))
        }
        Box(Modifier.size(48.dp).clickable { menuOpen = true }, contentAlignment = Alignment.Center) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Menu, contentDescription = "打开导航和更多选项",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp))
                }
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf(
                    Routes.DISCOVER to "发现", Routes.BOARDS to "版块",
                    Routes.FAVORITES to "收藏", Routes.PROFILE to "我的",
                    Routes.HISTORY to "阅读记录", Routes.SETTINGS to "设置"
                ).forEach { (route, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = {
                        menuOpen = false
                        if (route in listOf(Routes.DISCOVER, Routes.BOARDS,
                                Routes.FAVORITES, Routes.PROFILE)) nav.openRootTab(route)
                        else nav.navigate(route)
                    })
                }
            }
        }
    }
}
