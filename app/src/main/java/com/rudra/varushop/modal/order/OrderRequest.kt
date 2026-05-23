package com.rudra.varushop.modal.order

data class OrderRequest(
    val addressId: Int,
    val items: List<OrderItemRequest>,
    val paymentMethod: String,
    val couponId: Int?,
    val isSingleItem: Boolean
)