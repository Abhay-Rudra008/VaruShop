package com.rudra.varushop.modal.cart

data class CartCheckResponse(
    val success: Boolean,
    val message: String,
    val data: CartCheckData?
)