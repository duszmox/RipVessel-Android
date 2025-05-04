package hu.cock.ripvessel.network

import android.content.Context
import hu.cock.ripvessel.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

fun createAuthenticatedClient(context: Context): OkHttpClient {
    val cookie = SessionManager(context).getAuthCookie()
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    return OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            if (!cookie.isNullOrEmpty()) {
                builder.addHeader("Cookie", cookie)
            }
            chain.proceed(builder.build())
        }
        .addInterceptor(logging)
        .build()
} 