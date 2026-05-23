package com.rudra.varushop.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rudra.varushop.databinding.ItemCouponBinding
import com.rudra.varushop.modal.Coupon
import androidx.core.graphics.toColorInt


class CouponAdapter : ListAdapter<Coupon, CouponAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCouponBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCouponBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: Coupon) {
            binding.tvCode.text = item.code

            val discountText = if (item.discount_type == "PERCENTAGE") {
                "${item.discount_value}% OFF"
            } else {
                "Flat \$${item.discount_value} OFF"
            }

            binding.tvDescription.text = "$discountText on orders above \$${item.min_order_value}"

            val usesLeft = item.usage_limit - item.used_count
            val isDateValid = item.valid_until.substring(0, 10) >= java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val isLive = item.isActive && isDateValid && usesLeft > 0
            if (usesLeft > 0) {
                binding.tvExpiry.text = "Valid until: ${item.valid_until.substring(0, 10)} • $usesLeft uses left!"
                binding.tvExpiry.setTextColor("#757575".toColorInt())
            } else {
                binding.tvExpiry.text = "Coupon Fully Claimed"
                binding.tvExpiry.setTextColor("#F44336".toColorInt())
            }

            binding.root.alpha = if (isLive) 1.0f else 0.5f
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Coupon>() {
        override fun areItemsTheSame(old: Coupon, new: Coupon) = old.id == new.id
        override fun areContentsTheSame(old: Coupon, new: Coupon) = old == new
    }
}