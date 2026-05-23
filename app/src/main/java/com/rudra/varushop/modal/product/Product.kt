package com.rudra.varushop.modal.product

import com.google.gson.annotations.SerializedName

import java.io.Serializable

data class Product(
    val id: Int,
    val name: String,
    val price: Double,
    val discount: Double,

    @SerializedName(value = "main_image", alternate = ["image_url", "image"])
    val mainImage: String?,

    @SerializedName("average_rating")
    val averageRating: Double,

    @SerializedName("total_reviews")
    val totalReviews: Int,

    @SerializedName("description")
    val description: String?,

    @SerializedName("stock")
    val stock: Int,

    var isWishlisted: Boolean = false
) : Serializable