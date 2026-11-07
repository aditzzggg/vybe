package com.vybe.stream.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.vybe.stream.R
import com.vybe.stream.data.PipedApi
import kotlinx.coroutines.launch

class MusicFragment : Fragment(R.layout.fragment_music) {

    private val genres = listOf(
        "Pop", "Rock", "EDM", "Hip Hop", "Dangdut", "K-Pop", "Indie", "Lo-fi", "Jazz", "Viral Indonesia"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val chips = view.findViewById<ChipGroup>(R.id.genre_chips)
        val rv = view.findViewById<RecyclerView>(R.id.music_rv)
        val progress = view.findViewById<View>(R.id.music_progress)

        val adapter = SongAdapter { PlayerActivity.start(requireContext(), it.id, audioOnly = true) }
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        fun load(genre: String) {
            progress.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    val songs = PipedApi.search(genre + " music", "music_songs")
                    adapter.submitList(songs)
                } catch (e: Exception) {
                    // keep old list; community instances can be flaky
                } finally {
                    progress.visibility = View.GONE
                }
            }
        }

        genres.forEachIndexed { index, genre ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle)
            chip.text = genre
            chip.isCheckable = true
            chip.chipBackgroundColor = resources.getColorStateList(R.color.chip_bg, null)
            chip.setTextColor(resources.getColor(R.color.text, null))
            chip.isChecked = index == 0
            chip.setOnCheckedChangeListener { _, checked ->
                if (checked) load(genre)
            }
            chips.addView(chip)
        }
        load(genres.first())
    }
}
