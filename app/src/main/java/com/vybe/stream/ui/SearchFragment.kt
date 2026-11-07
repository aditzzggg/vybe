package com.vybe.stream.ui

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.vybe.stream.R
import com.vybe.stream.data.PipedApi
import com.vybe.stream.data.formatCount
import com.vybe.stream.data.formatDuration
import kotlinx.coroutines.launch

class SearchFragment : Fragment(R.layout.fragment_search) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val input = view.findViewById<TextInputEditText>(R.id.search_input)
        val chips = view.findViewById<ChipGroup>(R.id.filter_chips)
        val rv = view.findViewById<RecyclerView>(R.id.search_rv)
        val state = view.findViewById<TextView>(R.id.search_state)
        val progress = view.findViewById<View>(R.id.search_progress)

        val videoAdapter = VideoAdapter { PlayerActivity.start(requireContext(), it.id) }
        val songAdapter = SongAdapter { PlayerActivity.start(requireContext(), it.id, audioOnly = true) }
        rv.layoutManager = LinearLayoutManager(context)

        fun doSearch(query: String, musicMode: Boolean) {
            if (query.isBlank()) return
            state.visibility = View.GONE
            progress.visibility = View.VISIBLE
            rv.adapter = if (musicMode) songAdapter else videoAdapter
            lifecycleScope.launch {
                try {
                    val items = PipedApi.search(query, if (musicMode) "music_songs" else "videos")
                    if (musicMode) songAdapter.submitList(items) else videoAdapter.submitList(items)
                    if (items.isEmpty()) state.visibility = View.VISIBLE
                } catch (e: Exception) {
                    state.text = getString(R.string.error_generic)
                    state.visibility = View.VISIBLE
                } finally {
                    progress.visibility = View.GONE
                }
            }
        }

        chips.setOnCheckedStateChangeListener { _, ids ->
            val text = input.text?.toString().orEmpty()
            if (text.isNotBlank()) doSearch(text, ids.firstOrNull() == R.id.chip_music)
        }

        input.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val music = chips.checkedChipIds.firstOrNull() == R.id.chip_music
                doSearch(v.text.toString(), music)
                true
            } else false
        }
    }
}
