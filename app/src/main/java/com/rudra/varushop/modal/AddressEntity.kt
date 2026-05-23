package com.rudra.varushop.modal

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddressEntity(
    @SerializedName("id") val id: Int,
    @SerializedName("label") val label: String,
    @SerializedName("user_name") val userName: String,
    @SerializedName("full_address") val fullAddress: String,
    @SerializedName("phone") val phone: String,
    @SerializedName("is_default") val isDefault: Int
) : Parcelable {
    val isSelected: Boolean get() = isDefault == 1
}