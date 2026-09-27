package com.qingyi5427.ngaqing.data.remote

import com.qingyi5427.ngaqing.data.local.RequestPreferences
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class NgaAuthPolicyTest {
    @Test
    fun `only exact https nga origins may receive credentials`() {
        listOf(
            "https://bbs.nga.cn/", "https://ngabbs.com/",
            "https://nga.178.com/", "https://bbs.ngacn.cc/",
            "https://img.nga.cn/", "https://img4.nga.cn/", "https://img9.nga.cn/"
        ).forEach { assertTrue(it, NgaAuthPolicy.isTrusted(it.toHttpUrl())) }
        listOf(
            "http://bbs.nga.cn/", "https://bbs.nga.cn:444/",
            "https://bbs.nga.cn.evil.example/", "https://other.nga.cn/",
            "https://evil.example/"
        ).forEach { assertFalse(it, NgaAuthPolicy.isTrusted(it.toHttpUrl())) }
    }

    @Test
    fun `manual credentials are stripped at each http redirect hop`() {
        MockWebServer().use { first ->
            MockWebServer().use { second ->
                first.start()
                second.start()
                val otherHost = second.url("/image.jpg").newBuilder()
                    .host("127.0.0.1").build()
                first.enqueue(MockResponse().setResponseCode(302)
                    .addHeader("Location", otherHost))
                second.enqueue(MockResponse().setBody("image"))
                val client = testClient()
                client.newCall(Request.Builder().url(first.url("/start"))
                    .header("Cookie", "ngaPassportUid=123; ngaPassportCid=secret")
                    .header("Authorization", "Bearer secret")
                    .header("Proxy-Authorization", "Basic secret")
                    .header("X-Auth-Token", "secret")
                    .build()).execute().use { response -> assertEquals(200, response.code) }
                listOf(first.takeRequest(), second.takeRequest()).forEach { received ->
                    assertNull(received.getHeader("Cookie"))
                    assertNull(received.getHeader("Authorization"))
                    assertNull(received.getHeader("Proxy-Authorization"))
                    assertNull(received.getHeader("X-Auth-Token"))
                }
            }
        }
    }

    @Test
    fun `unsafe redirect cannot send the write twice`() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setResponseCode(302)
                .addHeader("Location", server.url("/second")))
            val session = NgaRequestSession("123", "cid", 1L, true, false)
            val request = Request.Builder().url(server.url("/first"))
                .post("body".toRequestBody())
                .tag(NgaRequestSession::class.java, session)
                .build()
            try {
                testClient().newCall(request).execute().close()
                throw AssertionError("Expected write replay to be rejected")
            } catch (expected: IOException) {
                assertTrue(expected.message.orEmpty().contains("replay"))
            }
            assertEquals("/first", server.takeRequest().path)
            assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun `307 write redirect is never transmitted twice`() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setResponseCode(307)
                .addHeader("Location", server.url("/second")))
            val request = Request.Builder().url(server.url("/first"))
                .post("body".toRequestBody())
                .tag(NgaRequestSession::class.java,
                    NgaRequestSession("123", "cid", 1L, true, false))
                .build()
            runCatching { testClient().newCall(request).execute().close() }
                .onFailure { assertTrue(it is IOException) }
            assertEquals("/first", server.takeRequest().path)
            assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun `trusted https hop gets only its pinned account cookie`() {
        val url = "https://bbs.nga.cn/read.php".toHttpUrl()
        val request = Request.Builder().url(url)
            .header("Cookie", "ngaPassportUid=wrong; ngaPassportCid=wrong")
            .header("Authorization", "Bearer secret")
            .tag(NgaRequestSession::class.java,
                NgaRequestSession("123", "cidA", 1L, false, true))
            .build()
        val chain = RecordingChain(request)
        NgaCredentialInterceptor {
            RequestPreferences("123", "cidA", "bbs.nga.cn", 1L)
        }.intercept(chain).close()
        assertEquals("ngaPassportUid=123; ngaPassportCid=cidA",
            chain.proceeded?.header("Cookie"))
        assertNull(chain.proceeded?.header("Authorization"))
    }

    @Test
    fun `leaving trusted origin strips cookie and cannot restore it on return`() {
        val session = NgaRequestSession("123", "cidA", 1L, false, true)
        val interceptor = NgaCredentialInterceptor {
            RequestPreferences("123", "cidA", "bbs.nga.cn", 1L)
        }
        val outside = RecordingChain(Request.Builder()
            .url("http://outside.example/image.jpg")
            .header("Cookie", "ngaPassportUid=123; ngaPassportCid=cidA")
            .tag(NgaRequestSession::class.java, session).build())
        interceptor.intercept(outside).close()
        assertNull(outside.proceeded?.header("Cookie"))

        val back = RecordingChain(Request.Builder()
            .url("https://bbs.nga.cn/read.php")
            .header("Cookie", "ngaPassportUid=123; ngaPassportCid=cidA")
            .tag(NgaRequestSession::class.java, session).build())
        interceptor.intercept(back).close()
        assertNull(back.proceeded?.header("Cookie"))
    }

    @Test
    fun `account revision change rejects in flight trusted hop`() {
        val request = Request.Builder()
            .url("https://img.nga.cn/attachment.jpg")
            .tag(NgaRequestSession::class.java,
                NgaRequestSession("123", "cidA", 1L, false, true))
            .build()
        val chain = RecordingChain(request)
        try {
            NgaCredentialInterceptor {
                RequestPreferences("456", "cidB", "bbs.nga.cn", 2L)
            }.intercept(chain)
            throw AssertionError("Expected changed account to reject request")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().contains("account changed"))
        }
        assertNull(chain.proceeded)
    }

    private fun testClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addNetworkInterceptor(NgaCredentialInterceptor {
                RequestPreferences("123", "cid", "bbs.nga.cn", 1L)
            })
            .retryOnConnectionFailure(false)
            .build()

    private class RecordingChain(private val input: Request) : Interceptor.Chain {
        var proceeded: Request? = null
        override fun request(): Request = input
        override fun proceed(request: Request): Response {
            proceeded = request
            return Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("".toResponseBody()).build()
        }
        override fun connection(): Connection? = null
        override fun call(): Call = OkHttpClient().newCall(input)
        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
