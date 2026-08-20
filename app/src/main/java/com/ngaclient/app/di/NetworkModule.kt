package com.ngaclient.app.di

import com.ngaclient.app.data.local.UserPreferences
import com.ngaclient.app.data.local.NgaDomains
import com.ngaclient.app.data.remote.GbkConverterFactory
import com.ngaclient.app.data.remote.NgaApi
import com.ngaclient.app.data.remote.NgaInterceptor
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
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(interceptor as Interceptor)
            .addInterceptor(logging)
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
