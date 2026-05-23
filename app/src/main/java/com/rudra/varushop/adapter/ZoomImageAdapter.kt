package com.rudra.varushop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.rudra.varushop.databinding.ItemZoomImageBinding

class ZoomImageAdapter(private val images: List<String>) :
    RecyclerView.Adapter<ZoomImageAdapter.ZoomViewHolder>() {

    inner class ZoomViewHolder(val binding: ItemZoomImageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoomViewHolder {
        val binding = ItemZoomImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ZoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ZoomViewHolder, position: Int) {
        val url = images[position]

        Glide.with(holder.itemView.context)
            .load(url)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.binding.photoView)
    }

    override fun getItemCount() = images.size
}