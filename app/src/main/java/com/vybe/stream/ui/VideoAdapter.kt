package com.vybe.stream.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.vybe.stream.R
import com.vybe.stream.data.StreamItem
import com.vybe.stream.data.formatCount
import com.vybe.stream.data.formatDuration

class VideoAdapter(private val onClick: (StreamItem) -> Unit) :
    ListAdapter<StreamItem, VideoAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StreamItem>() {
            override fun areItemsTheSame(a: StreamItem, b: StreamItem) = a.id == b.id
            override fun areContentsTheSame(a: StreamItem, b: StreamItem) = a == b
        }
    }

    class VH(val view: View) : RecyclerView.ViewHolder(view) {
        val thumb: ImageView = view.findViewById(R.id.v_thumb)
        val duration: TextView = view.findViewById(R.id.v_duration)
        val title: TextView = view.findViewById(R.id.v_title)
        val avatar: ImageView = view.findViewById(R.id.v_avatar)
        val meta: TextView = view.findViewById(R.id.v_meta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.thumb.load(item.thumbnail) {
            crossfade(true)
            placeholder(R.drawable.bg_thumb_placeholder)
        }
        holder.duration.text = formatDuration(item.duration)
        holder.title.text = item.title
        holder.avatar.load(item.uploaderAvatar) { crossfade(true) }
        val parts = listOf(item.uploader, formatCount(item.views) + " views", item.uploaded)
            .filter { it.isNotBlank() }
        holder.meta.text = parts.joinToString("  •  ")
        holder.view.setOnClickListener { onClick(item) }
    }
}
