package com.rudra.varushop.modal.wishlist

data class WishlistResponse(
    val success: Boolean,
    val message: String,
    val data: WishlistCheckData?
)