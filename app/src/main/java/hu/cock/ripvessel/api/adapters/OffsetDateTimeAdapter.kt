package hu.cock.ripvessel.api.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.OffsetDateTime

class OffsetDateTimeAdapter {
    @ToJson
    fun toJson(value: OffsetDateTime?): String? = value?.toString()

    @FromJson
    fun fromJson(value: String?): OffsetDateTime? = value?.let { OffsetDateTime.parse(it) }
} 