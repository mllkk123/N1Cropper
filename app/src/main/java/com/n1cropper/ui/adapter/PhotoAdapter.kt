package com.n1cropper.ui.adapter

import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.n1cropper.R
import com.n1cropper.model.Photo

class PhotoAdapter(
    private val onItemClick: (Photo) -> Unit,
    private val onItemLongClick: (Photo) -> Boolean,
    private var baseUrl: String = ""
) : ListAdapter<Photo, PhotoAdapter.ViewHolder>(DiffCallback()) {

    private var selectionMode = false
    private val selected = mutableSetOf<String>()

    fun setBaseUrl(url: String) {
        baseUrl = url
    }

    fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        if (!enabled) selected.clear()
        notifyDataSetChanged()
    }

    fun setSelected(names: Set<String>) {
        selected.clear()
        selected.addAll(names)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)
        private val overlay: View = itemView.findViewById(R.id.overlay)

        fun bind(photo: Photo) {
            val imageUrl = if (baseUrl.isNotEmpty()) {
                "$baseUrl/api/photos/${photo.name}"
            } else {
                "file://${photo.url}"
            }
            imageView.load(imageUrl) {
                placeholder(ColorDrawable(0xFFE0E0E0.toInt()))
                crossfade(true)
            }

            if (selectionMode && selected.contains(photo.name)) {
                overlay.visibility = View.VISIBLE
                overlay.setBackgroundColor(0x6600BCD4.toInt())
            } else {
                overlay.visibility = View.GONE
            }

            itemView.setOnClickListener { onItemClick(photo) }
            itemView.setOnLongClickListener { onItemLongClick(photo) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Photo>() {
        override fun areItemsTheSame(old: Photo, new: Photo) = old.name == new.name
        override fun areContentsTheSame(old: Photo, new: Photo) = old == new
    }
}
