package com.rudra.varushop.modal.user

import com.google.gson.annotations.SerializedName

data class DataWrapper(
    @SerializedName("userId") val userId: AuthData
)