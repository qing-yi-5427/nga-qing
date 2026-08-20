package com.qingyi5427.ngaqing.data.remote

import org.junit.Assert.assertEquals
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
}
