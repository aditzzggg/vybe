package com.vybe.stream.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.vybe.stream.R
import com.vybe.stream.data.PipedApi
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rv = view.findViewById<RecyclerView>(R.id.home_rv)
        val refresh = view.findViewById<SwipeRefreshLayout>(R.id.home_refresh)
        val errorView = view.findViewById<View>(R.id.home_error)
        val progress = view.findViewById<View>(R.id.home_progress)
        val regionLabel = view.findViewById<TextView>(R.id.home_region)

        val adapter = VideoAdapter { item -> PlayerActivity.start(requireContext(), item.id) }
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = adapter

        fun load() {
            errorView.visibility = View.GONE
            progress.visibility = View.VISIBLE
            val region = requireContext()
                .getSharedPreferences("vybe_prefs", Context.MODE_PRIVATE)
                .getString("region", "ID") ?: "ID"
            lifecycleScope.launch {
                try {
                    val items = PipedApi.trending(region)
                    adapter.submitList(items)
                    regionLabel.text = "Trending • " + region
                } catch (e: Exception) {
                    if (adapter.itemCount == 0) errorView.visibility = View.VISIBLE
                } finally {
                    progress.visibility = View.GONE
                    refresh.isRefreshing = false
                }
            }
        }

        refresh.setColorSchemeResources(R.color.accent, R.color.accent2)
        refresh.setOnRefreshListener { load() }
        view.findViewById<View>(R.id.retry_btn).setOnClickListener { load() }
        load()
    }
}
