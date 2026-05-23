package com.rudra.varushop.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.rudra.varushop.R
import com.rudra.varushop.databinding.ItemOrderRowBinding
import com.rudra.varushop.modal.order.Order

class OrderAdapter(
    private val onOrderClick: (Order) -> Unit,
    private val onTrackOrderClick: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(OrderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Inside OrderAdapter.kt -> OrderViewHolder
        @SuppressLint("SetTextI18n")
        fun bind(order: Order) {
            binding.apply {
                tvOrderID.text = "Order #${order.orderId}"
                tvOrderDate.text = "Ordered on ${order.date}"

                val hasMultipleItems = order.items.size > 1

                if (hasMultipleItems) {
                    tvOrderStatus.visibility = View.GONE

                    btnTrackOrder.isVisible = false

                    val firstItem = order.items.firstOrNull()
                    if (firstItem != null) {
                        tvProductName.text =
                            "${firstItem.productName} +${order.items.size - 1} more"
                        loadProductImage(firstItem.image)
                    }
                } else {
                    // Show status for single-item orders
                    tvOrderStatus.visibility = View.VISIBLE
                    tvOrderStatus.text = order.status

                    val statusColor = when (order.status.lowercase()) {
                        "delivered", "completed" -> "#388E3C" // Green
                        "cancelled" -> "#D32F2F"              // Red
                        "shipped", "ongoing" -> "#1976D2"      // Blue
                        else -> "#F57C00"                      // Orange
                    }
                    tvOrderStatus.setTextColor(statusColor.toColorInt())

                    btnTrackOrder.isVisible =
                        order.status.lowercase() !in listOf("delivered", "cancelled")

                    val firstItem = order.items.firstOrNull()
                    if (firstItem != null) {
                        tvProductName.text = firstItem.productName
                        loadProductImage(firstItem.image)
                    }
                }

                // Click Listeners
                root.setOnClickListener { onOrderClick(order) }
                btnTrackOrder.setOnClickListener { onTrackOrderClick(order) }
            }
        }

        // Helper function to keep the bind method clean
        private fun loadProductImage(imageUrl: String?) {
            val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
            Glide.with(binding.ivProduct.context)
                .load(imageUrl)
                .centerCrop()
                .transition(DrawableTransitionOptions.with(factory))
                .placeholder(R.drawable.ic_no_products)
                .into(binding.ivProduct)
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order) =
            oldItem.orderId == newItem.orderId

        override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
    }
}