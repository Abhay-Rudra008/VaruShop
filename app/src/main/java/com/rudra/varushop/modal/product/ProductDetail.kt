package com.rudra.varushop.modal.product

import com.google.gson.annotations.SerializedName

data class ProductDetail(
    val id: Int,
    val name: String,
    val price: String,
    val description: String?,
    val stock: Int,
    val category_name: String?,

    @SerializedName("discount_percent")
    val discount: Double?,

    val images: List<String>
)