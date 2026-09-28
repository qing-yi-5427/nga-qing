package com.qingyi5427.ngaqing.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.qingyi5427.ngaqing.BuildConfig

/**
 * 变体枚举：build.gradle productFlavors 在编译期通过 BuildConfig.UI_VARIANT 固定。
 * 旧 flavor 标识仍用于已有安装包兼容；三个变体统一使用中性设计系统。
 */
enum class Variant { CLASSIC, MODERN, MINIMAL }

/**
 * 旧调用方的布局令牌接口。新的页面颜色与形状由 NgaQingTheme 统一提供。
 */
data class UiTokens(
    val cornerRadius: Dp,
    val listItemPadding: Dp,
    val cardElevation: Dp,
    val denseList: Boolean,
    val dividerOnly: Boolean,   // 仅分隔线、无卡片背景
    val hasBottomBar: Boolean,  // 根页面底部导航
    val defaultDark: Boolean,
    val groupCard: Boolean      // 分组容器
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
            cornerRadius = 10.dp,
            listItemPadding = 14.dp,
            cardElevation = 0.dp,
            denseList = false,
            dividerOnly = true,
            hasBottomBar = true,
            defaultDark = false,
            groupCard = false
        )

    val isClassic: Boolean get() = current == Variant.CLASSIC
    val isModern: Boolean get() = current == Variant.MODERN
    val isMinimal: Boolean get() = current == Variant.MINIMAL
}
