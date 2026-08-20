package com.qingyi5427.ngaqing.data.remote

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.nio.charset.Charset

/**
 * NGA returns GBK-encoded bodies. Retrofit's scalar converter would decode as UTF-8,
 * so we decode raw bytes as GBK ourselves for String endpoints.
 */
class GbkConverterFactory : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type == String::class.java) {
            return Converter<ResponseBody, String> { body ->
                val bytes = body.bytes()
                String(bytes, Charset.forName("GBK"))
            }
        }
        return null
    }
}
