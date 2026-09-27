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
}
