package com.rudra.varushop.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rudra.varushop.R
import com.rudra.varushop.databinding.ItemOrderSummaryBinding
import com.rudra.varushop.modal.order.OrderItem
import androidx.recyclerview.widget.ListAdapter

class PlacedOrderSummaryAdapter(
    private val onItemClick: (OrderItem) -> Unit
) : ListAdapter<OrderItem, PlacedOrderSummaryAdapter.SummaryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val binding = ItemOrderSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class SummaryViewHolder(private val binding: ItemOrderSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(item: OrderItem, onItemClick: (OrderItem) -> Unit) {
            binding.apply {
                tvProductName.text = item.productName
                tvQuantityAndPrice.text = "${item.quantity} x ₹${item.price}"

                tvItemStatus.text = item.status
                val statusColor = when (item.status.lowercase()) {
                    "delivered", "completed" -> "#388E3C" // Green
                    "cancelled" -> "#D32F2F"              // Red
                    "shipped", "ongoing" -> "#1976D2"      // Blue
                    else -> "#F57C00"                      // Orange
                }
                tvItemStatus.setTextColor(statusColor.toColorInt())

                val total = item.price * item.quantity
                tvItemTotal.text = "₹${String.format("%.2f", total)}"

                Glide.with(ivProduct.context)
                    .load(item.image)
                    .placeholder(R.drawable.ic_no_products)
                    .error(R.drawable.ic_no_products)
                    .centerCrop()
                    .into(ivProduct)

                root.setOnClickListener { onItemClick(item) }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<OrderItem>() {
        override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem) =
            oldItem.productId == newItem.productId

        override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem) =
            oldItem == newItem
    }
}