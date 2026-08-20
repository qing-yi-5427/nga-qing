package com.ngaclient.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ngaclient.app.BuildConfig

/**
 * 变体枚举：build.gradle productFlavors 在编译期通过 BuildConfig.UI_VARIANT 固定。
 * 三版（classic/modern/minimal）在「方案 C · 玻璃 Glassmorph」定稿后统一为同一套
 * 玻璃风格设计系统（仅保留 flavor 以便三包共存安装对比）。
 */
enum class Variant { CLASSIC, MODERN, MINIMAL }

/**
 * 驱动全套视觉的令牌：配色之外，列表密度、圆角、是否卡片、导航形态都由它决定。
 * 定稿后三版统一为玻璃风格值。
 */
data class UiTokens(
    val cornerRadius: Dp,
    val listItemPadding: Dp,
    val cardElevation: Dp,
    val denseList: Boolean,
    val dividerOnly: Boolean,   // 仅分隔线、无卡片背景
    val hasBottomBar: Boolean,  // 是否显示底部胶囊导航
    val defaultDark: Boolean,
    val groupCard: Boolean      // 玻璃：内嵌分组容器
)

object UiVariant {
    val current: Variant
        get() = when (BuildConfig.UI_VARIANT) {
            "modern" -> Variant.MODERN
            "minimal" -> Variant.MINIMAL
            else -> Variant.CLASSIC
        }

    val tokens: UiTokens
        get() = UiTokens(
            cornerRadius = 14.dp,
            listItemPadding = 12.dp,
            cardElevation = 0.dp,
            denseList = false,
            dividerOnly = false,
            hasBottomBar = true,
            defaultDark = false,
            groupCard = true
        )

    val isClassic: Boolean get() = current == Variant.CLASSIC
    val isModern: Boolean get() = current == Variant.MODERN
    val isMinimal: Boolean get() = current == Variant.MINIMAL
}
