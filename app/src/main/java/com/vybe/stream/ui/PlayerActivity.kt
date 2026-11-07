package com.vybe.stream.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.button.MaterialButton
import com.google.common.util.concurrent.ListenableFuture
import com.vybe.stream.R
import com.vybe.stream.data.PipedApi
import com.vybe.stream.data.StreamInfo
import com.vybe.stream.data.bestAudioUrl
import com.vybe.stream.data.bestVideoUrl
import com.vybe.stream.data.formatCount
import com.vybe.stream.player.PlayerManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "video_id"
        const val EXTRA_AUDIO = "audio_only"
        const val EXTRA_ATTACH = "attach_only"

        fun start(
            context: Context,
            id: String,
            audioOnly: Boolean = false,
            attachOnly: Boolean = false
        ) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putExtra(EXTRA_ID, id)
                    .putExtra(EXTRA_AUDIO, audioOnly)
                    .putExtra(EXTRA_ATTACH, attachOnly)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var videoId: String? = null
    private var audioOnly = false
    private var segments: List<Pair<Long, Long>> = emptyList()
    private var loadingJob: Job? = null

    private val handler = Handler(Looper.getMainLooper())
    private val segmentChecker = object : Runnable {
        override fun run() {
            val c = controller
            if (c != null && segments.isNotEmpty() && c.isPlaying) {
                val pos = c.currentPosition / 1000
                for ((start, end) in segments) {
                    if (pos > start && pos < end - 1) {
                        c.seekTo(end * 1000L)
                        Toast.makeText(this@PlayerActivity, "Skipped sponsored segment", Toast.LENGTH_SHORT).show()
                        break
                    }
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        videoId = intent.getStringExtra(EXTRA_ID)
        audioOnly = intent.getBooleanExtra(EXTRA_AUDIO, false) ||
                getSharedPreferences("vybe_prefs", Context.MODE_PRIVATE)
                    .getBoolean("audio_only", false)

        findViewById<ImageButton>(R.id.back_btn).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.audio_btn).setOnClickListener {
            audioOnly = !audioOnly
            val pos = controller?.currentPosition ?: 0L
            loadVideo(videoId, audioOnly, pos)
        }

        findViewById<TextView>(R.id.desc_text).setOnClickListener {
            it as TextView
            it.maxLines = if (it.maxLines == 4) 40 else 4
        }

        val relatedRv = findViewById<RecyclerView>(R.id.related_rv)
        relatedRv.layoutManager = LinearLayoutManager(this)
        relatedRv.adapter = VideoAdapter { PlayerActivity.start(this, it.id) }
    }

    override fun onStart() {
        super.onStart()
        controllerFuture = PlayerManager.buildController(this).also { f ->
            f.addListener({
                runCatching { f.get() }.getOrNull()?.let { c ->
                    controller = c
                    findViewById<PlayerView>(R.id.player_view).player = c
                    if (intent.getBooleanExtra(EXTRA_ATTACH, false) && c.mediaItemCount > 0) {
                        bindExisting(c)
                    } else {
                        loadVideo(videoId, audioOnly, 0L)
                    }
                    handler.post(segmentChecker)
                }
            }, Runnable::run)
        }
    }

    override fun onStop() {
        handler.removeCallbacks(segmentChecker)
        findViewById<PlayerView>(R.id.player_view).player = null
        MediaController.releaseFuture(controllerFuture)
        controller = null
        super.onStop()
    }

    private fun bindExisting(c: MediaController) {
        val md = c.currentMediaItem?.mediaMetadata ?: return
        findViewById<TextView>(R.id.video_title).text = md.title
        findViewById<TextView>(R.id.video_meta).text = md.artist
        findViewById<TextView>(R.id.channel_name).text = md.artist
        findViewById<ImageView>(R.id.channel_avatar).load(md.artworkUri) { crossfade(true) }
        findViewById<TextView>(R.id.desc_text).visibility = View.GONE
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadVideo(id: String?, audio: Boolean, resumeMs: Long) {
        if (id.isNullOrBlank()) return
        loadingJob?.cancel()
        findViewById<View>(R.id.player_progress).visibility = View.VISIBLE
        loadingJob = lifecycleScope.launch {
            try {
                val info = PipedApi.streams(id)
                val url = if (audio) info.bestAudioUrl() ?: info.bestVideoUrl()
                else info.bestVideoUrl() ?: info.bestAudioUrl()
                if (url.isNullOrBlank()) {
                    Toast.makeText(this@PlayerActivity, "No playable stream found", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                segments = info.segments
                populate(info, audio)
                controller?.let { c ->
                    val item = PlayerManager.buildItem(info, url, audio)
                    if (resumeMs > 0) c.setMediaItem(item, resumeMs) else c.setMediaItem(item)
                    c.prepare()
                    c.play()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PlayerActivity,
                    "Could not load stream: " + (e.message?.take(60) ?: "network error"),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                findViewById<View>(R.id.player_progress).visibility = View.GONE
            }
        }
    }

    private fun populate(info: StreamInfo, audio: Boolean) {
        findViewById<TextView>(R.id.video_title).text = info.title
        findViewById<TextView>(R.id.video_meta).text =
            formatCount(info.views) + " views  •  " + formatCount(info.likes) + " likes"
        findViewById<TextView>(R.id.channel_name).text = info.uploader
        findViewById<ImageView>(R.id.channel_avatar).load(info.uploaderAvatar) { crossfade(true) }
        findViewById<TextView>(R.id.desc_text).text =
            info.description.replace("\\n", "\n").take(800)
        val badge = findViewById<TextView>(R.id.quality_badge)
        badge.text = if (audio) "AUDIO" else "VIDEO"
        val btn = findViewById<MaterialButton>(R.id.audio_btn)
        btn.text = if (audio) getString(R.string.audio_mode_on) else getString(R.string.audio_mode_off)
        (findViewById<RecyclerView>(R.id.related_rv).adapter as? VideoAdapter)
            ?.submitList(info.related.take(15))
    }
}
