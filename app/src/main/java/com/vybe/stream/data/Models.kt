package com.vybe.stream.data

import org.json.JSONArray
import org.json.JSONObject

data class StreamItem(
    val id: String,
    val title: String,
    val thumbnail: String,
    val uploader: String,
    val uploaderAvatar: String,
    val duration: Long,
    val views: Long,
    val uploaded: String
)

data class StreamInfo(
    val id: String,
    val title: String,
    val description: String,
    val uploader: String,
    val uploaderAvatar: String,
    val duration: Long,
    val views: Long,
    val likes: Long,
    val hls: String?,
    val audioStreams: List<AudioStream>,
    val videoStreams: List<VideoStream>,
    val related: List<StreamItem>,
    val segments: List<Pair<Long, Long>>
)

data class AudioStream(val url: String, val bitrate: Long, val mimeType: String)
data class VideoStream(val url: String, val quality: Int, val mimeType: String, val videoOnly: Boolean)

fun parseId(url: String): String =
    url.substringAfter("v=", "").substringBefore("&").ifBlank { url }

fun JSONObject.optStr(key: String): String = optString(key, "")

fun JSONObject.toStreamItem(): StreamItem = StreamItem(
    id = parseId(optStr("url")),
    title = optStr("title"),
    thumbnail = optStr("thumbnail"),
    uploader = optStr("uploaderName"),
    uploaderAvatar = optStr("uploaderAvatar"),
    duration = optLong("duration", 0L),
    views = optLong("views", 0L),
    uploaded = optStr("uploadedDate").ifBlank { formatDate(optLong("uploaded", 0L)) }
)

fun formatDuration(sec: Long): String {
    if (sec <= 0L) return "LIVE"
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}

fun formatCount(n: Long): String = when {
    n >= 1_000_000_000L -> String.format("%.1fB", n / 1_000_000_000.0)
    n >= 1_000_000L -> String.format("%.1fM", n / 1_000_000.0)
    n >= 1_000L -> String.format("%.1fK", n / 1_000.0)
    else -> n.toString()
}

fun formatDate(ms: Long): String =
    if (ms <= 0L) "" else java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
        .format(java.util.Date(ms))

fun parseQuality(q: String): Int = q.filter { it.isDigit() }.toIntOrNull() ?: 0

fun StreamInfo.bestVideoUrl(): String? {
    val muxed = videoStreams.filter { !it.videoOnly }
    val mp4 = muxed.filter { it.mimeType.contains("mp4", ignoreCase = true) }
    return (mp4.ifEmpty { muxed }).maxByOrNull { it.quality }?.url ?: hls
}

fun StreamInfo.bestAudioUrl(): String? =
    audioStreams.filter {
        it.mimeType.contains("mp4", ignoreCase = true) || it.mimeType.contains("m4a", ignoreCase = true)
    }.maxByOrNull { it.bitrate }?.url ?: audioStreams.maxByOrNull { it.bitrate }?.url

fun JSONArray.mapNotNullObj(action: (JSONObject) -> Unit): Unit {
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        action(o)
    }
}
