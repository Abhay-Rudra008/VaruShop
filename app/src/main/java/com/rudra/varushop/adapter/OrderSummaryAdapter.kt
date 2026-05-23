package com.rudra.varushop.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rudra.varushop.R
import com.rudra.varushop.databinding.ItemOrderSummaryBinding
import com.rudra.varushop.modal.cart.CartItem

class OrderSummaryAdapter :
    ListAdapter<CartItem, OrderSummaryAdapter.SummaryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val binding = ItemOrderSummaryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SummaryViewHolder(private val binding: ItemOrderSummaryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(item: CartItem) {
            binding.apply {
                tvProductName.text = item.name
                tvQuantityAndPrice.text = "${item.quantity} x ₹${item.price}"

                val total = item.price * item.quantity
                tvItemTotal.text = "₹${String.format("%.2f", total)}"

                val imageUrl = item.mainImage

                Glide.with(ivProduct.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_no_products)
                    .error(R.drawable.ic_no_products)
                    .centerCrop()
                    .into(ivProduct)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem) =
            oldItem.productId == newItem.productId

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem) = oldItem == newItem
    }
}