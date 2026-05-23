package com.rudra.varushop.modal.order

import com.google.gson.annotations.SerializedName

data class OrderStatusResponse(
    @SerializedName("orderId") val orderId: Int,
    @SerializedName("order_status") val status: String?,
    @SerializedName("productId") val productId: Int,
    @SerializedName("formatted_time") val updatedAt: String?,
    @SerializedName("userRating") val userRating: Int?,
    @SerializedName("userComment") val userComment: String?
)