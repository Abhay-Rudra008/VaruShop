package com.rudra.varushop.modal.cart


data class CartData(
    val items: List<CartItem>,
    val total_items: Int,
    val subtotal: String
)