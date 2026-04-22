package com.n1cropper.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.n1cropper.R
import com.n1cropper.device.DeviceInfo

class DeviceAdapter(
    private val onClick: (DeviceInfo) -> Unit
) : ListAdapter<DeviceInfo, DeviceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)

        fun bind(device: DeviceInfo) {
            tvName.text = device.name
            tvAddress.text = "${device.host}:${device.port}"
            itemView.setOnClickListener { onClick(device) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DeviceInfo>() {
        override fun areItemsTheSame(old: DeviceInfo, new: DeviceInfo) =
            old.host == new.host && old.port == new.port

        override fun areContentsTheSame(old: DeviceInfo, new: DeviceInfo) = old == new
    }
}
