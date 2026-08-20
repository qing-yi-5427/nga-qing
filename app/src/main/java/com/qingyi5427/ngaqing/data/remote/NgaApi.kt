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
        @Query("user") user: Int? = null
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
}
