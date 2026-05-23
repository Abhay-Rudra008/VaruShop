package com.rudra.varushop.modal.cart

import com.google.gson.annotations.SerializedName

data class CartItem(
    val id: Int,
    val productId: Int,
    val name: String,
    val originalPrice: Double,
    val discountPercent: Double,
    val price: Double,
    val quantity: Int,

    @SerializedName("image_url") val mainImage: String?,

    val stock: Int,
    val categoryName: String?,
    val itemTotal: Double?
)