package com.qingyi5427.ngaqing.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NgaApi {
    @GET("app_api.php")
    suspend fun homeCategory(
        @Query("__lib") lib: String = "home",
        @Query("__act") act: String = "category",
        @Query("__output") output: Int = 8
    ): String

    @GET("thread.php")
    suspend fun threadList(
        @Query("fid") fid: String? = null,
        @Query("stid") stid: String? = null,
        @Query("page") page: Int = 1,
        @Query("__output") output: Int = 8,
        @Query("noprefix") noprefix: String = "",
        @Query("authorid") authorId: String? = null,
        @Query("recommend") recommend: Int? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("user") user: Int? = null,
        @Query("favor") favor: Int? = null
    ): String

    @GET("read.php")
    suspend fun read(
        @Query("tid") tid: String,
        @Query("page") page: Int = 1,
        @Query("__output") output: Int = 8,
        @Query("noprefix") noprefix: String = "",
        @Query("v2") v2: String = "",
        @Query("authorid") authorId: String? = null
    ): String

    @GET("forum.php")
    suspend fun search(
        @Query("__output") output: Int = 8,
        @Query("key") key: String,
        @Query("fid") fid: String? = null,
        @Query("stid") stid: String? = null
    ): String

    /** 发回复：post.php?__lib=post&__act=reply */
    @FormUrlEncoded
    @POST("post.php")
    suspend fun reply(
        @Query("__lib") lib: String = "post",
        @Query("__act") act: String = "reply",
        @Query("__output") output: Int = 8,
        @Query("__inchst") inchst: String = "UTF8",
        @Field("fid") fid: String,
        @Field("tid") tid: String,
        @Field("content") content: String,
        @Field("step") step: Int = 1
    ): String

    /** NGA 标准发布流程的最终提交；action 支持 new/reply/quote。 */
    @FormUrlEncoded
    @POST("post.php")
    suspend fun publish(
        @Query("action") action: String,
        @Query("fid") fid: String? = null,
        @Query("stid") stid: String? = null,
        @Query("tid") tid: String? = null,
        @Query("pid") pid: String? = null,
        @Query("__output") output: Int = 8,
        @Query("__inchst") inchst: String = "UTF8",
        @Field("step") step: Int = 2,
        @Field("post_subject") subject: String = "",
        @Field("post_content") content: String
    ): String

    @GET("nuke.php")
    suspend fun notifications(
        @Query("__lib") lib: String = "noti",
        @Query("__act") act: String = "get_all",
        @Query("raw") raw: Int = 3,
        @Query("time_limit") since: Long = 0,
        @Query("__output") output: Int = 8
    ): String

    @GET("nuke.php")
    suspend fun messages(
        @Query("__lib") lib: String = "message",
        @Query("__act") act: String = "message",
        @Query("action") action: String = "list",
        @Query("page") page: Int = 1,
        @Query("__output") output: Int = 8
    ): String

    @GET("nuke.php")
    suspend fun userInfo(
        @Query("__lib") lib: String = "ucp",
        @Query("__act") act: String = "get",
        @Query("uid") uid: String,
        @Query("__output") output: Int = 8
    ): String

    @GET("nuke.php")
    suspend fun userTopics(
        @Query("__lib") lib: String = "load_topic",
        @Query("__act") act: String = "load_topic_by_uid",
        @Query("uid") uid: String,
        @Query("page") page: Int = 1,
        @Query("__output") output: Int = 8
    ): String

    @FormUrlEncoded
    @POST("nuke.php")
    suspend fun follow(
        @Query("__lib") lib: String = "follow_v2",
        @Query("__act") act: String = "follow",
        @Field("type") type: Int,
        @Field("id") id: String,
        @Query("__output") output: Int = 8
    ): String

    @FormUrlEncoded
    @POST("nuke.php")
    suspend fun addServerFavorite(
        @Query("__lib") lib: String = "topic_favor_v2",
        @Query("__act") act: String = "add",
        @Field("action") action: String = "add",
        @Field("folder") folder: Int = 1,
        @Field("tid") tid: String,
        @Field("pid") pid: String = "",
        @Query("__output") output: Int = 8
    ): String

    @FormUrlEncoded
    @POST("nuke.php")
    suspend fun removeServerFavorite(
        @Query("__lib") lib: String = "topic_favor",
        @Query("__act") act: String = "topic_favor",
        @Field("action") action: String = "del",
        @Field("raw") raw: Int = 3,
        @Field("tid") tid: String,
        @Field("page") page: Int = 1,
        @Field("tidarray") tidArray: String,
        @Query("__output") output: Int = 8
    ): String
}
