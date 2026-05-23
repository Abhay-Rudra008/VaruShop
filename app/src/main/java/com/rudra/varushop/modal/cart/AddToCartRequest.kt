package com.rudra.varushop.modal.cart

import com.google.gson.annotations.SerializedName


data class AddToCartRequest(
    @SerializedName("product_id")
    val productId: Int,

    @SerializedName("quantity")
    val quantity: Int
)