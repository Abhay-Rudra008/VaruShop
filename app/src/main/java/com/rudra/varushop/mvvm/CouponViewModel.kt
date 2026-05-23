package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.Coupon
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CouponViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private var allCoupons: List<Coupon> = emptyList()

    private val _uiState = MutableStateFlow<CouponUiState>(CouponUiState.Loading)
    val uiState: StateFlow<CouponUiState> = _uiState.asStateFlow()

    fun fetchCoupons(hasInternet: Boolean) {
        if (!hasInternet) {
            _uiState.value = CouponUiState.NoInternet
            return
        }

        _uiState.value = CouponUiState.Loading

        viewModelScope.launch {
            try {
                val response = repository.getAllCouponsFromCloud()

                if (response.isSuccessful && response.body()?.success == true) {
                    allCoupons = response.body()?.data ?: emptyList()
                    filterCoupons(isLiveTab = true)
                } else {
                    _uiState.value = CouponUiState.Error(response.body()?.message ?: "Server Error")
                }
            } catch (e: Exception) {
                _uiState.value = CouponUiState.Error(e.localizedMessage ?: "Unknown Error Occurred")
                e.printStackTrace()
            }
        }
    }

    fun filterCoupons(isLiveTab: Boolean) {
        if (allCoupons.isEmpty()) {
            _uiState.value = CouponUiState.Empty("No coupons available")
            return
        }

        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val filteredList = allCoupons.filter { coupon ->
            val isDateValid = coupon.valid_until.substring(0, 10) >= currentDate

            val usesLeft = coupon.usage_limit - coupon.used_count
            val hasUsesLeft = usesLeft > 0

            val isLive = coupon.isActive && isDateValid && hasUsesLeft
            if (isLiveTab) isLive else !isLive
        }

        if (filteredList.isEmpty()) {
            _uiState.value = CouponUiState.Empty(
                message = if (isLiveTab) "No Active Coupons" else "No Expired or Claimed Coupons"
            )
        } else {
            _uiState.value = CouponUiState.Success(filteredList)
        }
    }

    sealed interface CouponUiState {
        data object Loading : CouponUiState
        data class Success(val coupons: List<Coupon>) : CouponUiState
        data class Empty(val message: String) : CouponUiState
        data object NoInternet : CouponUiState
        data class Error(val message: String) : CouponUiState
    }
}