package com.vybe.stream.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.vybe.stream.data.StreamInfo

object PlayerManager {

    fun buildController(context: Context): ListenableFuture<MediaController> =
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, PlaybackService::class.java))
        ).buildAsync()

    fun buildItem(info: StreamInfo, url: String, audioOnly: Boolean): MediaItem {
        val extras = Bundle().apply {
            putString("videoId", info.id)
            putBoolean("audioOnly", audioOnly)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(info.title)
            .setArtist(info.uploader)
            .setArtworkUri(Uri.parse(info.thumbnail.ifBlank { info.uploaderAvatar }))
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(info.id)
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
    }
}
