package com.qingyi5427.ngaqing.ui

/** Positive means the destination enters from the right; negative from the left. */
internal fun navigationDirection(fromRoute: String?, toRoute: String?, popping: Boolean): Int {
    val fromTab = rootTabIndex(fromRoute)
    val toTab = rootTabIndex(toRoute)
    if (fromTab != null && toTab != null && fromTab != toTab) {
        return if (toTab > fromTab) 1 else -1
    }
    return if (popping) -1 else 1
}

internal fun isRootTabSwitch(fromRoute: String?, toRoute: String?): Boolean {
    val fromTab = rootTabIndex(fromRoute)
    val toTab = rootTabIndex(toRoute)
    return fromTab != null && toTab != null && fromTab != toTab
}

internal fun isSharedListTransition(fromRoute: String?, toRoute: String?): Boolean =
    fromRoute in setOf(Routes.THREADS, Routes.POSTS) &&
        toRoute in setOf(Routes.THREADS, Routes.POSTS)

private fun rootTabIndex(route: String?): Int? = when (route) {
    Routes.BOARDS -> 0
    Routes.FAVORITES -> 1
    Routes.PROFILE -> 2
    else -> null
}
