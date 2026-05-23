package com.rudra.varushop.modal.review

import com.google.gson.annotations.SerializedName

data class ReviewRequest(
    @SerializedName("productId") val productId: Int,
    @SerializedName("rating") val rating: Int,
    @SerializedName("comment") val comment: String
)