package com.rudra.varushop.modal.order

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderItem(
    val productId: Int,
    val retailerOrderId: Int,
    val status: String,
    val productName: String,
    val image: String?,
    val quantity: Int,
    val originalPrice: Double,
    val price: Double
) : Parcelable