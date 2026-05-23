package com.rudra.varushop.modal.user


data class AuthData(
    val token: String,
    val refresh_token: String,
    val user: UserDto
)