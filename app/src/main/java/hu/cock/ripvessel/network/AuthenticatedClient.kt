package hu.cock.ripvessel.network

import android.content.Context
import hu.cock.ripvessel.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import okhttp3.Response

fun createAuthenticatedClient(context: Context): OkHttpClient {
    val sessionManager = SessionManager(context)
    val cookie = sessionManager.getAuthCookie()
    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    return OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
            val authCookie = sessionManager.getAuthCookie()
            val cfuvidCookie = sessionManager.getCFUVIDCookie()
            
            if (!authCookie.isNullOrEmpty()) {
                builder.addHeader("Cookie", authCookie)
            }
            if (!cfuvidCookie.isNullOrEmpty()) {
                // If we already have a Cookie header, append the CFUVID cookie
                val existingCookie = builder.build().header("Cookie")
                if (existingCookie != null) {
                    builder.removeHeader("Cookie")
                    builder.addHeader("Cookie", "$existingCookie; $cfuvidCookie")
                } else {
                    builder.addHeader("Cookie", cfuvidCookie)
                }
            }
            chain.proceed(builder.build())
        }
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            // Check for Set-Cookie headers in the response
            val setCookieHeaders = response.headers("Set-Cookie")
            if (setCookieHeaders.isNotEmpty()) {
                // Find the sails.sid cookie if present
                val authCookie = setCookieHeaders.find { it.startsWith("sails.sid") }
                if (authCookie != null) {
                    sessionManager.saveAuthCookie(authCookie)
                }
                // Find and save _cfuvid cookie if present
                val cfuvidCookie = setCookieHeaders.find { it.startsWith("_cfuvid") }
                if (cfuvidCookie != null) {
                    sessionManager.saveCFUVIDCookie(cfuvidCookie)
                }
            }
            response
        }
        .addInterceptor(logging)
        .build()
} 