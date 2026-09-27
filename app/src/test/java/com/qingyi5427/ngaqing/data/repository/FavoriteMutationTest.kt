package com.qingyi5427.ngaqing.data.repository

import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FavoriteMutationTest {
    @Test fun requiresMatchingSuccessEnvelope() {
        parseFavoriteMutationResponse("""{"data":{"0":0,"1":"收藏成功"}}""", add = true)
        parseFavoriteMutationResponse("""{"data":{"0":0,"1":"取消收藏成功"}}""", add = false)
    }

    @Test fun rejectsHttp200ErrorsAndUnknownResults() {
        listOf(
            """{"data":{"0":0,"1":"收藏失败"}}""",
            """{"error":"未登录","data":{"0":0,"1":"收藏成功"}}""",
            """{"error":["权限不足"],"data":{"0":0,"1":"收藏成功"}}""",
            """{"data":{"tid":123}}""",
            "<html>收藏成功</html>"
        ).forEach { response ->
            assertThrows(IllegalStateException::class.java) {
                parseFavoriteMutationResponse(response, add = true)
            }
        }
    }
}
