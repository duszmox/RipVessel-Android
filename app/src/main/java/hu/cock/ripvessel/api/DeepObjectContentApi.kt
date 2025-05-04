package hu.cock.ripvessel.api

import android.content.Context
import hu.cock.ripvessel.network.createAuthenticatedClient
import hu.gyulakiri.ripvessel.model.ContentCreatorListLastItems
import hu.gyulakiri.ripvessel.model.ContentCreatorListV3Response
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import hu.cock.ripvessel.api.adapters.UriAdapter
import hu.cock.ripvessel.api.adapters.BigDecimalAdapter
import hu.cock.ripvessel.api.adapters.OffsetDateTimeAdapter

object DeepObjectContentApi {
    fun getMultiCreatorBlogPostsDeepObject(
        context: Context,
        ids: List<String>,
        limit: Int,
        fetchAfter: List<ContentCreatorListLastItems>?
    ): ContentCreatorListV3Response {
        val client = createAuthenticatedClient(context)
        val baseUrl = "https://www.floatplane.com/api/v3/content/creator/list"
        val urlBuilder = baseUrl.toHttpUrlOrNull()!!.newBuilder()
        ids.forEach { urlBuilder.addQueryParameter("ids", it) }
        urlBuilder.addQueryParameter("limit", limit.toString())
        fetchAfter?.forEachIndexed { idx, item ->
            urlBuilder.addQueryParameter("fetchAfter[$idx][creatorId]", item.creatorId)
            urlBuilder.addQueryParameter("fetchAfter[$idx][blogPostId]", item.blogPostId ?: "")
            urlBuilder.addQueryParameter("fetchAfter[$idx][moreFetchable]", item.moreFetchable.toString())
        }
        val url = urlBuilder.build().toString()
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            val moshi = Moshi.Builder()
                .add(UriAdapter())
                .add(BigDecimalAdapter())
                .add(OffsetDateTimeAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()
            val adapter = moshi.adapter(ContentCreatorListV3Response::class.java)
            val body = response.body?.string() ?: throw Exception("Empty response body")
            return adapter.fromJson(body) ?: throw Exception("Failed to parse response")
        }
    }
} 