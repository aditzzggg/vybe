package com.vybe.stream

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.load
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.common.util.concurrent.ListenableFuture
import com.vybe.stream.player.PlayerManager
import com.vybe.stream.ui.HomeFragment
import com.vybe.stream.ui.MusicFragment
import com.vybe.stream.ui.PlayerActivity
import com.vybe.stream.ui.SearchFragment
import com.vybe.stream.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private lateinit var miniPlayer: View

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        miniPlayer = findViewById(R.id.mini_player)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> switchTo(HomeFragment())
                R.id.nav_search -> switchTo(SearchFragment())
                R.id.nav_music -> switchTo(MusicFragment())
                R.id.nav_settings -> switchTo(SettingsFragment())
            }
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_home

        findViewById<ImageButton>(R.id.mini_play).setOnClickListener {
            controller?.let { if (it.isPlaying) it.pause() else it.play() }
        }
        findViewById<ImageButton>(R.id.mini_close).setOnClickListener {
            controller?.run { stop(); clearMediaItems() }
            miniPlayer.visibility = View.GONE
        }
        miniPlayer.setOnClickListener {
            val item = controller?.currentMediaItem ?: return@setOnClickListener
            val id = item.mediaMetadata.extras?.getString("videoId") ?: return@setOnClickListener
            PlayerActivity.start(this, id, item.mediaMetadata.extras?.getBoolean("audioOnly") ?: false, attachOnly = true)
        }
    }

    private fun switchTo(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host, fragment)
            .commit()
    }

    override fun onStart() {
        super.onStart()
        controllerFuture = PlayerManager.buildController(this).also { f ->
            f.addListener({
                runCatching { f.get() }.getOrNull()?.let { c ->
                    controller = c
                    c.addListener(playerListener)
                    updateMini(c)
                }
            }, Runnable::run)
        }
    }

    override fun onStop() {
        controller?.removeListener(playerListener)
        MediaController.releaseFuture(controllerFuture)
        controller = null
        super.onStop()
    }

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            updateMini(controller)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateMini(controller)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateMini(controller)
        }
    }

    private fun updateMini(c: Player?) {
        val item = c?.currentMediaItem
        if (c == null || c.mediaItemCount == 0 || item == null) {
            miniPlayer.visibility = View.GONE
            return
        }
        miniPlayer.visibility = View.VISIBLE
        findViewById<TextView>(R.id.mini_title).text = item.mediaMetadata.title ?: ""
        findViewById<TextView>(R.id.mini_artist).text = item.mediaMetadata.artist ?: "Vybe"
        item.mediaMetadata.artworkUri?.let { uri ->
            findViewById<ImageView>(R.id.mini_art).load(uri) { crossfade(true) }
        }
        findViewById<ImageButton>(R.id.mini_play).setImageResource(
            if (c.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }
}
