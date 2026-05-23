package com.rudra.varushop.modal.category

import com.google.gson.annotations.SerializedName

data class CategoryListData(
    @SerializedName("categories")
    val categories: List<Category>
)