package com.rudra.varushop.modal

data class Coupon(
    val id: Int,
    val code: String,
    val discount_type: String,
    val discount_value: Double,
    val min_order_value: Double,
    val usage_limit: Int,

    val used_count: Int,
    val created_at: String?,
    val updated_at: String?,

    val valid_until: String,
    val is_active: Int
) {
    val isActive: Boolean
        get() = is_active == 1

    val remainingUses: Int
        get() = usage_limit - used_count
}