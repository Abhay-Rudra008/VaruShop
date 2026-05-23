package com.rudra.varushop.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.rudra.varushop.R
import com.rudra.varushop.databinding.ItemProductGridBinding
import com.rudra.varushop.modal.product.Product

class ProductAdapter(
    private val onProductClick: (Product) -> Unit, private val onWishlistClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position), onProductClick, onWishlistClick)
    }

    class ProductViewHolder(private val binding: ItemProductGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n", "DefaultLocale")
        fun bind(
            product: Product, onProductClick: (Product) -> Unit, onWishlistClick: (Product) -> Unit
        ) {
            binding.apply {
                tvProductName.text = product.name

                val originalPrice = product.price
                val discountPercent = product.discount

                if (discountPercent > 0) {
                    val savings = originalPrice * (discountPercent / 100)
                    val currentPrice = originalPrice - savings

                    tvCurrentPrice.text = "₹${String.format("%.0f", currentPrice)}"
                    tvOriginalPrice.text = "₹${String.format("%.0f", originalPrice)}"
                    tvDiscount.text = "${discountPercent.toInt()}% OFF"

                    tvOriginalPrice.visibility = View.VISIBLE
                    tvDiscount.visibility = View.VISIBLE
                } else {
                    tvCurrentPrice.text = "₹${String.format("%.0f", originalPrice)}"
                    tvOriginalPrice.visibility = View.GONE
                    tvDiscount.visibility = View.GONE
                }

                if (product.totalReviews > 0) {
                    layoutRating.visibility = View.VISIBLE
                    tvRating.text = String.format("%.1f", product.averageRating)
                    ivStar.visibility = View.VISIBLE
                } else {
                    layoutRating.visibility = View.VISIBLE
                    tvRating.text = "New"
                    ivStar.visibility = View.GONE
                }

                Glide.with(ivProduct.context).load(product.mainImage)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.sample_product).error(R.drawable.sample_product)
                    .into(ivProduct)

                root.setOnClickListener { onProductClick(product) }
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}