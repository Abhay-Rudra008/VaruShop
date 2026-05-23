package com.rudra.varushop.mvvm


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.PaymentVerificationRequest
import com.rudra.varushop.modal.order.OrderRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderSummaryViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<OrderSummaryUiState>(OrderSummaryUiState.Idle)
    val orderState = _orderState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()

    fun placeOrder(orderRequest: OrderRequest) {
        viewModelScope.launch {
            _orderState.value = OrderSummaryUiState.Loading
            try {
                val response = repository.placeOrder(orderRequest)

                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    if (data != null) {
                        _orderState.value = OrderSummaryUiState.OrderCreated(
                            orderId = data.orderId,
                            razorpayOrderId = data.razorpayOrderId ?: "",
                            totalAmount = data.totalAmount
                        )
                    } else {
                        _orderState.value =
                            OrderSummaryUiState.Error("Server responded with empty payload.")
                    }
                } else {
                    val errorMsg =
                        response.body()?.message ?: response.message().takeIf { it.isNotBlank() }
                        ?: "Failed to place order"
                    _orderState.value = OrderSummaryUiState.Error(errorMsg)
                    _snackbarMessage.emit(errorMsg)
                }
            } catch (e: Exception) {
                _orderState.value =
                    OrderSummaryUiState.Error("Connection Error: ${e.localizedMessage}")
                _snackbarMessage.emit("Network Error. Please try again.")
            }
        }
    }

    fun verifyPayment(verificationRequest: PaymentVerificationRequest) {
        viewModelScope.launch {
            _orderState.value = OrderSummaryUiState.Loading
            try {
                val response = repository.verifyPayment(verificationRequest)

                if (response.isSuccessful && response.body()?.success == true) {
                    _orderState.value = OrderSummaryUiState.PaymentVerifiedSuccess
                } else {
                    val errorMsg = response.body()?.message ?: "Payment signature check failed."
                    _orderState.value = OrderSummaryUiState.Error(errorMsg)
                    _snackbarMessage.emit(errorMsg)
                }
            } catch (e: Exception) {
                _orderState.value =
                    OrderSummaryUiState.Error("Verification Error: ${e.localizedMessage}")
                _snackbarMessage.emit("Unable to confirm transaction status. Please contact support.")
            }
        }
    }

    sealed interface OrderSummaryUiState {
        data object Idle : OrderSummaryUiState
        data object Loading : OrderSummaryUiState

        data class OrderCreated(
            val orderId: Int, val razorpayOrderId: String, val totalAmount: Double
        ) : OrderSummaryUiState

        data object PaymentVerifiedSuccess : OrderSummaryUiState

        data class Error(val message: String) : OrderSummaryUiState
    }
}