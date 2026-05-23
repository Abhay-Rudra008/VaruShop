package com.rudra.varushop.modal.user

import com.google.gson.annotations.SerializedName


data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    @SerializedName("profile_image") val profileImage: String? = null
)