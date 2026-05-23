package com.rudra.varushop.modal.order

data class OrderCreationData(
    val orderId: Int,
    val totalAmount: Double,
    val pointsEarned: Int,
    val razorpayOrderId: String?
)