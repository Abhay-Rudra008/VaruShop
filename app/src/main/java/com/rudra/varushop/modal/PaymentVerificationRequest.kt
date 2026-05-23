package com.rudra.varushop.modal

data class PaymentVerificationRequest(
    val razorpay_order_id: String,
    val razorpay_payment_id: String,
    val razorpay_signature: String,
    val orderId: Int
)