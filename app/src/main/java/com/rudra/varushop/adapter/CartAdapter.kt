package com.rudra.varushop.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rudra.varushop.R
import com.rudra.varushop.activity.OrderPaymentSummaryActivity
import com.rudra.varushop.databinding.ItemCartProductBinding
import com.rudra.varushop.modal.cart.CartItem


class CartAdapter(
    private val onQuantityChanged: (productId: Int, currentQty: Int, stock: Int, isIncrease: Boolean) -> Unit,
    private val onRemoveItem: (productId: Int) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding =
            ItemCartProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(private val binding: ItemCartProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: CartItem) {
            val context = binding.root.context

            binding.apply {
                tvProductName.text = item.name

                tvProductPrice.text = "₹${item.price}"
                tvQuantity.text = item.quantity.toString()

                if (item.discountPercent > 0) {
                    tvOriginalPrice.isVisible = true
                    tvDiscountBadge.isVisible = true

                    tvOriginalPrice.text = "₹${item.originalPrice}"
                    tvDiscountBadge.text = "${item.discountPercent.toInt()}% OFF"

                    tvOriginalPrice.paintFlags =
                        tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    tvOriginalPrice.isVisible = false
                    tvDiscountBadge.isVisible = false
                }

                Glide.with(ivProduct.context).load(item.mainImage)
                    .placeholder(R.drawable.ic_no_products).into(ivProduct)

                val isOutOfStock = item.stock <= 0

                val isAtMax = item.quantity >= 5 || item.quantity >= item.stock
                btnPlus.alpha = if (isAtMax || isOutOfStock) 0.3f else 1.0f
                btnPlus.isEnabled = !isAtMax && !isOutOfStock

                val isAtMin = item.quantity <= 1
                btnMinus.alpha = if (isAtMin || isOutOfStock) 0.3f else 1.0f
                btnMinus.isEnabled = !isAtMin && !isOutOfStock

                if (isOutOfStock) {
                    btnBuyNow.isEnabled = false
                    btnBuyNow.alpha = 0.5f
                    btnBuyNow.text = "Out of Stock"
                } else {
                    btnBuyNow.isEnabled = true
                    btnBuyNow.alpha = 1.0f
                    btnBuyNow.text = "Buy Now"
                }

                btnPlus.setOnClickListener {
                    onQuantityChanged(item.productId, item.quantity, item.stock, true)
                }

                btnMinus.setOnClickListener {
                    onQuantityChanged(item.productId, item.quantity, item.stock, false)
                }

                btnRemove.setOnClickListener {
                    onRemoveItem(item.productId)
                }

                btnBuyNow.setOnClickListener {
                    val intent = Intent(context, OrderPaymentSummaryActivity::class.java).apply {
                        putExtra("IS_SINGLE_ITEM", true)
                        putExtra("PRODUCT_ID", item.productId)
                        putExtra("PRODUCT_NAME", item.name)

                        // 🔥 CRITICAL: Passes the final discounted price value down to the checkout activity
                        putExtra("PRODUCT_PRICE", item.price)
                        putExtra("QUANTITY", item.quantity)
                        putExtra("PRODUCT_IMAGE", item.mainImage)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}

class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
    override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem) =
        oldItem.productId == newItem.productId

    override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem) = oldItem == newItem
}