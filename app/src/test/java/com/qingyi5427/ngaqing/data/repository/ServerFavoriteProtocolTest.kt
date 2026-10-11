package com.qingyi5427.ngaqing.data.repository

import com.qingyi5427.ngaqing.data.remote.NgaApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class ServerFavoriteProtocolTest {
    @Test fun folderListAndMutationsUseSelectedServerIdOnWire() = runBlocking {
        MockWebServer().use { server ->
            repeat(4) { server.enqueue(MockResponse().setBody("""{"data":{"0":0,"1":"操作成功"}}""")) }
            val api = Retrofit.Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(ScalarsConverterFactory.create())
                .build().create(NgaApi::class.java)

            api.listServerFavoriteFolders()
            val list = server.takeRequest()
            assertEquals("POST", list.method)
            assertEquals("topic_favor_v2", list.requestUrl?.queryParameter("__lib"))
            assertEquals("list_folder", list.requestUrl?.queryParameter("__act"))

            api.addServerFavorite(folder = "7", tid = "47697364")
            val add = server.takeRequest()
            assertEquals("POST", add.method)
            assertEquals("add", add.requestUrl?.queryParameter("__act"))
            assertTrue(add.body.readUtf8().contains("folder=7"))

            api.addServerFavorite(folder = "-1", tid = "47697364")
            val create = server.takeRequest()
            assertEquals("8", create.requestUrl?.queryParameter("__output"))
            val createBody = create.body.readUtf8()
            assertTrue(createBody.contains("folder=-1"))
            assertTrue(createBody.contains("tid=47697364"))
            assertTrue(createBody.contains("pid=0"))
            assertTrue(!createBody.contains("action="))

            api.removeServerFavorite(folder = "7", tidArray = "47697364")
            val remove = server.takeRequest()
            assertEquals("POST", remove.method)
            assertEquals("del", remove.requestUrl?.queryParameter("__act"))
            val body = remove.body.readUtf8()
            assertTrue(body.contains("folder=7"))
            assertTrue(body.contains("tidarray=47697364"))
        }
    }
}
