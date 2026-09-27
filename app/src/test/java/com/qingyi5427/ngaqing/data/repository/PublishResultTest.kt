package com.qingyi5427.ngaqing.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PublishResultTest {
    @Test fun confirmsOnlyActionMatchingSuccessEnvelope() {
        assertEquals("回复成功", parseExplicitPublishSuccess("""{"data":{"0":0,"1":"回复成功"}}""", "reply"))
        assertEquals("发帖成功", parseExplicitPublishSuccess("""{"data":{"0":0,"1":"发帖成功"}}""", "new"))
    }

    @Test fun rejectsMisleadingOrAmbiguousResponses() {
        val uncertain = listOf(
            """{"data":{"0":1,"1":"回复成功"}}""",
            """{"data":{"0":0,"1":"回复失败，请勿重复回复成功"}}""",
            """{"data":{"tid":12345}}""",
            "<html>回复成功</html>",
            """{"error":{"0":"1:失败"},"data":{"0":0,"1":"回复成功"}}""",
            """{"error":"未登录","data":{"0":0,"1":"回复成功"}}""",
            """{"error":["权限不足"],"data":{"0":0,"1":"回复成功"}}"""
        )
        uncertain.forEach { response ->
            assertThrows(IllegalStateException::class.java) {
                parseExplicitPublishSuccess(response, "reply")
            }
        }
        assertThrows(IllegalStateException::class.java) {
            parseExplicitPublishSuccess("""{"data":{"0":0,"1":"回复成功"}}""", "new")
        }
    }
}
