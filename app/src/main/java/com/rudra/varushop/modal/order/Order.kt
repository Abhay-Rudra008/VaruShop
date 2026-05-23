package com.rudra.varushop.modal.order

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class Order(
    val id: Int,
    val orderId: Int,
    val status: String,
    val date: String,
    val totalAmount: Double,
    val items: List<OrderItem>
) : Parcelable