package com.qingyi5427.ngaqing.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationMotionTest {
    @Test fun rootTabsFollowVisualOrderEvenWhenNavigationPops() {
        assertEquals(1, navigationDirection(Routes.BOARDS, Routes.FAVORITES, popping = false))
        assertEquals(-1, navigationDirection(Routes.PROFILE, Routes.BOARDS, popping = false))
        assertEquals(-1, navigationDirection(Routes.FAVORITES, Routes.BOARDS, popping = true))
        assertEquals(1, navigationDirection(Routes.BOARDS, Routes.PROFILE, popping = true))
        assertTrue(isRootTabSwitch(Routes.BOARDS, Routes.PROFILE))
    }

    @Test fun childPushAndPopReverseAndSharedListsStayInPlace() {
        assertEquals(1, navigationDirection(Routes.THREADS, Routes.POSTS, popping = false))
        assertEquals(-1, navigationDirection(Routes.POSTS, Routes.THREADS, popping = true))
        assertFalse(isRootTabSwitch(Routes.THREADS, Routes.POSTS))
        assertTrue(isSharedListTransition(Routes.THREADS, Routes.POSTS))
        assertTrue(isSharedListTransition(Routes.POSTS, Routes.POSTS))
        assertTrue(isSharedListTransition(Routes.THREADS, Routes.THREADS))
        assertFalse(isSharedListTransition(Routes.BOARDS, Routes.THREADS))
    }

    @Test fun motionKindsKeepTabsAndExpandedSharedChromeStationary() {
        assertEquals(NavigationMotionKind.ROOT_TAB,
            navigationMotionKind(Routes.BOARDS, Routes.FAVORITES, expanded = false, hasHinge = false))
        assertEquals(NavigationMotionKind.CHILD,
            navigationMotionKind(Routes.THREADS, Routes.POSTS, expanded = false, hasHinge = false))
        for (pair in listOf(Routes.BOARDS to Routes.THREADS,
            Routes.THREADS to Routes.POSTS, Routes.BOARDS to Routes.FAVORITES)) {
            assertEquals(NavigationMotionKind.NONE,
                navigationMotionKind(pair.first, pair.second, expanded = true, hasHinge = false))
        }
        assertEquals(NavigationMotionKind.NONE,
            navigationMotionKind(Routes.THREADS, Routes.POSTS, expanded = true, hasHinge = true))
    }

    @Test fun childOffsetIsShortAndMirroredOnReturn() {
        assertEquals(32, childEnterOffset(width = 1080, distancePx = 32, direction = 1))
        assertEquals(-32, childEnterOffset(width = 1080, distancePx = 32, direction = -1))
        assertEquals(20, childEnterOffset(width = 200, distancePx = 32, direction = 1))
        assertEquals(0, childEnterOffset(width = 0, distancePx = 32, direction = 1))
    }
}
