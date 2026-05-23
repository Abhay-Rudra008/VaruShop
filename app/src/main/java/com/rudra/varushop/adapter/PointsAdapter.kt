package com.rudra.varushop.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rudra.varushop.databinding.ItemPointTransactionBinding
import com.rudra.varushop.modal.PointsTransaction


class PointsAdapter :
    ListAdapter<PointsTransaction, PointsAdapter.PointsViewHolder>(PointsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PointsViewHolder {
        val binding = ItemPointTransactionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PointsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PointsViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class PointsViewHolder(private val binding: ItemPointTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: PointsTransaction) {
            binding.tvDescription.text = item.description
            binding.tvDate.text = item.created_at.substring(0, 10)

            val prefix = if (item.points > 0) "+" else ""
            binding.tvPoints.text = "$prefix${item.points}"

            when {
                item.status == "PENDING" -> {
                    binding.tvPoints.setTextColor("#FF9800".toColorInt())
                }

                item.status == "CANCELLED" -> {
                    binding.tvPoints.setTextColor("#9E9E9E".toColorInt())
                }

                item.points > 0 -> {
                    binding.tvPoints.setTextColor("#4CAF50".toColorInt())
                }

                else -> {
                    binding.tvPoints.setTextColor("#F44336".toColorInt())
                }
            }
        }
    }
}

class PointsDiffCallback : DiffUtil.ItemCallback<PointsTransaction>() {
    override fun areItemsTheSame(oldItem: PointsTransaction, newItem: PointsTransaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: PointsTransaction, newItem: PointsTransaction
    ): Boolean {
        return oldItem == newItem
    }
}