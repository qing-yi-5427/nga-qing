package com.qingyi5427.ngaqing.di

import com.qingyi5427.ngaqing.data.local.UserPreferences
import com.qingyi5427.ngaqing.data.local.NgaDomains
import com.qingyi5427.ngaqing.data.remote.GbkConverterFactory
import com.qingyi5427.ngaqing.data.remote.NgaApi
import com.qingyi5427.ngaqing.data.remote.NgaInterceptor
import com.qingyi5427.ngaqing.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL = "https://${NgaDomains.DEFAULT_HOST}/"

    @Provides
    @Singleton
    fun provideNgaInterceptor(prefs: UserPreferences): NgaInterceptor = NgaInterceptor(prefs)

    @Provides
    @Singleton
    fun provideOkHttpClient(interceptor: NgaInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(interceptor as Interceptor)
            .apply {
                // Coil 也复用这个客户端；Release 若记录 BASIC 日志，会为每张头像/正文图
                // 产生两次 Logcat I/O，在快速滚动时形成可见抖动。
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GbkConverterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideNgaApi(retrofit: Retrofit): NgaApi = retrofit.create(NgaApi::class.java)
}
