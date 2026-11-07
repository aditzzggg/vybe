package com.vybe.stream.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object PipedApi {

    val instances = listOf(
        "https://api.piped.private.coffee",
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://pipedapi.ducks.party",
        "https://pipedapi.reallyaweso.me",
        "https://pipedapi.leptons.xyz"
    )

    @Volatile
    var currentInstance: String = instances[0]

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private suspend fun fetch(path: String): String = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        val startIndex = (instances.indexOf(currentInstance).coerceAtLeast(0))
        for (attempt in instances.indices) {
            val base = instances[(startIndex + attempt) % instances.size]
            try {
                val req = Request.Builder()
                    .url(base + path)
                    .header("User-Agent", "Vybe/1.0 (Android)")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw IOException("HTTP " + resp.code + " from " + base)
                    val body = resp.body?.string()
                    if (body.isNullOrBlank()) throw IOException("Empty body from " + base)
                    currentInstance = base
                    return@withContext body
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("All Piped instances failed")
    }

    suspend fun trending(region: String = "ID"): List<StreamItem> {
        val body = fetch("/trending?region=" + region)
        val arr = JSONArray(body)
        val out = ArrayList<StreamItem>(arr.length())
        arr.mapNotNullObj { o ->
            if (o.optStr("title").isNotBlank()) out.add(o.toStreamItem())
        }
        return out
    }

    suspend fun search(query: String, filter: String = "videos"): List<StreamItem> {
        val body = fetch("/search?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&filter=" + filter)
        val obj = JSONObject(body)
        val arr = obj.optJSONArray("items") ?: JSONArray()
        val out = ArrayList<StreamItem>(arr.length())
        arr.mapNotNullObj { o ->
            val type = o.optStr("type")
            if (type.isBlank() || type == "stream") out.add(o.toStreamItem())
        }
        return out
    }

    suspend fun streams(id: String): StreamInfo {
        val body = fetch("/streams/" + id)
        val o = JSONObject(body)

        val audio = ArrayList<AudioStream>()
        (o.optJSONArray("audioStreams") ?: JSONArray()).mapNotNullObj { a ->
            val url = a.optStr("url")
            if (url.isNotBlank()) {
                audio.add(AudioStream(url, a.optLong("bitrate", 0L), a.optStr("mimeType")))
            }
        }

        val video = ArrayList<VideoStream>()
        (o.optJSONArray("videoStreams") ?: JSONArray()).mapNotNullObj { v ->
            val url = v.optStr("url")
            if (url.isNotBlank()) {
                video.add(
                    VideoStream(
                        url,
                        parseQuality(v.optStr("quality")),
                        v.optStr("mimeType"),
                        v.optBoolean("videoOnly", true)
                    )
                )
            }
        }

        val related = ArrayList<StreamItem>()
        (o.optJSONArray("relatedStreams") ?: JSONArray()).mapNotNullObj { r ->
            val type = r.optStr("type")
            if (type.isBlank() || type == "stream") related.add(r.toStreamItem())
        }

        val segs = ArrayList<Pair<Long, Long>>()
        (o.optJSONArray("segments") ?: JSONArray()).mapNotNullObj { s ->
            val seg = s.optJSONArray("segment")
            if (seg != null && seg.length() >= 2) {
                segs.add(Pair(seg.optLong(0), seg.optLong(1)))
            }
        }

        return StreamInfo(
            id = o.optStr("id").ifBlank { id },
            title = o.optStr("title"),
            description = o.optStr("description"),
            uploader = o.optStr("uploader"),
            uploaderAvatar = o.optStr("uploaderAvatar"),
            duration = o.optLong("duration", 0L),
            views = o.optLong("views", 0L),
            likes = o.optLong("likes", 0L),
            hls = o.optString("hls").takeIf { it.isNotBlank() && it != "null" },
            audioStreams = audio,
            videoStreams = video,
            related = related,
            segments = segs
        )
    }
}
