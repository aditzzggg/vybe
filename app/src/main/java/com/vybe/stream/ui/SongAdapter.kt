package com.vybe.stream.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vybe.stream.R
import com.vybe.stream.data.StreamItem
import com.vybe.stream.data.formatDuration

class SongAdapter(private val onClick: (StreamItem) -> Unit) :
    ListAdapter<StreamItem, SongAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StreamItem>() {
            override fun areItemsTheSame(a: StreamItem, b: StreamItem) = a.id == b.id
            override fun areContentsTheSame(a: StreamItem, b: StreamItem) = a == b
        }
    }

    class VH(val view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.s_thumb)
        val title: TextView = view.findViewById(R.id.s_title)
        val meta: TextView = view.findViewById(R.id.s_meta)
        val play: ImageButton = view.findViewById(R.id.s_play)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.thumb.load(item.thumbnail) {
            crossfade(true)
            placeholder(R.drawable.bg_thumb_placeholder)
        }
        holder.title.text = item.title
        val parts = listOf(item.uploader, formatDuration(item.duration)).filter { it.isNotBlank() }
        holder.meta.text = parts.joinToString("  •  ")
        val click = View.OnClickListener { onClick(item) }
        holder.view.setOnClickListener(click)
        holder.play.setOnClickListener(click)
    }
}
