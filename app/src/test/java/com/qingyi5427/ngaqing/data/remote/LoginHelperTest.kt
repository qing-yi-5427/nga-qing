package com.qingyi5427.ngaqing.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginHelperTest {
    @Test
    fun `numeric account uid wins over parent-domain guest marker`() {
        val parsed = parseNgaPassportCookies(
            "ngaPassportUid=guestJsTokenExample; " +
                "ngaPassportCid=0123456789abcdef0123456789abcdef01234567; " +
                "ngaPassportUid=12345678"
        )

        assertEquals("12345678", parsed.uid)
        assertEquals("0123456789abcdef0123456789abcdef01234567", parsed.cid)
    }

    @Test
    fun `guest uid is never persisted as account uid`() {
        val parsed = parseNgaPassportCookies("ngaPassportUid=guestJsTokenExample")

        assertEquals("", parsed.uid)
        assertEquals("", parsed.cid)
    }

    @Test
    fun `different account cookies in one header are rejected`() {
        val parsed = parseNgaPassportCookies(
            "ngaPassportUid=123; ngaPassportCid=cidA; ngaPassportUid=456; ngaPassportCid=cidB"
        )
        assertEquals(NgaPassportCookies(), parsed)
    }

    @Test
    fun `login capture only uses exact secure forum origins`() {
        assertEquals(
            listOf("https://bbs.nga.cn/login.php", "https://ngabbs.com/nuke.php"),
            loginCaptureUrls(listOf(
                "https://bbs.nga.cn/login.php?ticket=private",
                "https://ngabbs.com/nuke.php",
                "http://bbs.nga.cn/",
                "https://bbs.nga.cn.evil.example/",
                "https://img.nga.cn/",
                "https://bbs.nga.cn:444/"
            ))
        )
    }

    @Test
    fun `cookie reset is needed only for conflicting account`() {
        assertFalse(hasConflictingPassportCookies(
            "ngaPassportUid=123; ngaPassportCid=cidA; waf_token=keep", "123", "cidA"
        ))
        assertTrue(hasConflictingPassportCookies(
            "ngaPassportUid=456; ngaPassportCid=cidA", "123", "cidA"
        ))
        assertTrue(hasConflictingPassportCookies(
            "ngaPassportUid=123; ngaPassportCid=cidB", "123", "cidA"
        ))
    }
}
