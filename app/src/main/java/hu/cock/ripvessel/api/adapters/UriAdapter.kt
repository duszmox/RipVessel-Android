package hu.cock.ripvessel.api.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.net.URI

class UriAdapter {
    @ToJson
    fun toJson(uri: URI?): String? = uri?.toString()

    @FromJson
    fun fromJson(uri: String?): URI? = uri?.let { URI.create(it) }
} 