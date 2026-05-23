package com.rudra.varushop.modal

data class PointsTransaction(
    val id: Int,
    val order_id: Int?,
    val points: Int,
    val description: String,
    val status: String, // "PENDING", "ACTIVE", or "CANCELLED"
    val created_at: String,
    val available_at: String?
)