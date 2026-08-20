package com.ngaclient.app.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarUrlTest {
    @Test
    fun `empty avatar stays empty`() {
        assertEquals("", normalizeNgaAvatarUrl("  "))
    }

    @Test
    fun `protocol relative NGA avatar uses working CDN scheme`() {
        assertEquals(
            "http://img4.nga.cn/avatars/123/456.jpg",
            normalizeNgaAvatarUrl("//img4.nga.cn/avatars/123/456.jpg")
        )
    }

    @Test
    fun `legacy image host is migrated`() {
        assertEquals(
            "http://img.nga.cn/avatars/123/456.jpg",
            normalizeNgaAvatarUrl("http://img.nga.178.com/avatars/123/456.jpg")
        )
    }

    @Test
    fun `relative avatar receives current image host`() {
        assertEquals(
            "http://img.nga.cn/avatars/123/456.jpg?x=1&y=2",
            normalizeNgaAvatarUrl("/avatars/123/456.jpg?x=1&amp;y=2")
        )
    }

    @Test
    fun `current https avatar uses working CDN scheme`() {
        assertEquals(
            "http://img.nga.cn/avatars/123/456.jpg?x=1",
            normalizeNgaAvatarUrl("https://img.nga.cn/avatars/123/456.jpg?x=1")
        )
    }

    @Test
    fun `wrapped legacy avatar extracts first image URL`() {
        assertEquals(
            "http://img.nga.cn/avatars/123/first.jpg",
            normalizeNgaAvatarUrl(
                """{ "t":1,"l":2,"0":{ "0":"https:\/\/img.nga.cn\/avatars\/123\/first.jpg"},"1":{ "0":"https:\/\/img.nga.cn\/avatars\/123\/second.jpg"}}"""
            )
        )
    }
}
