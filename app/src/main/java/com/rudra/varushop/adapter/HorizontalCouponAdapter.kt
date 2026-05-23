package com.rudra.varushop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rudra.varushop.databinding.ItemCouponHorizontalBinding
import com.rudra.varushop.modal.Coupon

class HorizontalCouponAdapter(
    private val onCouponClicked: (Coupon) -> Unit
) : ListAdapter<Coupon, HorizontalCouponAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCouponHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCouponHorizontalBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Coupon) {
            binding.tvHorizontalCode.text = item.code

            val details = if (item.discount_type == "PERCENTAGE") {
                "${item.discount_value}% OFF on orders above ₹${item.min_order_value}"
            } else {
                "Flat ₹${item.discount_value} OFF on orders above ₹${item.min_order_value}"
            }
            binding.tvHorizontalDesc.text = details

            binding.root.setOnClickListener { onCouponClicked(item) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Coupon>() {
        override fun areItemsTheSame(old: Coupon, new: Coupon) = old.id == new.id
        override fun areContentsTheSame(old: Coupon, new: Coupon) = old == new
    }
}