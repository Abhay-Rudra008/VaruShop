package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.order.Order
import com.rudra.varushop.modal.order.OrderStatusResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _orderState = MutableStateFlow<OrderUiState>(OrderUiState.Loading)
    val orderState = _orderState.asStateFlow()

    private val _orderDetailState = MutableStateFlow<OrderStatusResponse?>(null)
    val orderDetailState = _orderDetailState.asStateFlow()

    private val _cancelSuccess = MutableSharedFlow<Unit>()
    val cancelSuccess = _cancelSuccess.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error = _error.asSharedFlow()

    private var allOrders = listOf<Order>()

    fun fetchOrders() {
        viewModelScope.launch {
            _orderState.value = OrderUiState.Loading
            try {
                val response = repository.getOrders()

                if (response.isSuccessful && response.body()?.success == true) {
                    allOrders = response.body()?.data ?: emptyList()
                    filterOrders("All", "")
                } else {
                    val errorMsg =
                        response.message().takeIf { it.isNotBlank() } ?: response.body()?.message
                        ?: "Failed to fetch orders"
                    _orderState.value = OrderUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _orderState.value = OrderUiState.Error(e.localizedMessage ?: "Network Error")
            }
        }
    }

    fun filterOrders(status: String, query: String) {
        val filtered = allOrders.filter { order ->

            // ✅ FIXED: Separated Completed and Cancelled states cleanly
            val matchesStatus = when (status) {
                "Ongoing" -> order.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED", "FAILED")
                "Completed" -> order.status == "DELIVERED" // Only successfully finished orders
                "Cancelled" -> order.status in listOf(
                    "CANCELLED", "REFUNDED", "FAILED"
                ) // Dedicated tab for failed/cancelled orders
                else -> true // "All"
            }

            // Keep your search query matching logic as it is
            val matchesQuery = order.orderId.toString()
                .contains(query, ignoreCase = true) || order.items.any {
                it.productName.contains(
                    query,
                    ignoreCase = true
                )
            }

            matchesStatus && matchesQuery
        }

        // Update UI state based on filtered collection size
        if (filtered.isEmpty()) {
            _orderState.value = OrderUiState.Empty
        } else {
            _orderState.value = OrderUiState.Success(filtered)
        }
    }

    fun fetchOrderDetail(retailerOrderId: Int, productId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.getOrderStatus(retailerOrderId, productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _orderDetailState.value = response.body()?.data
                } else {
                    _error.emit(response.body()?.message ?: "Failed to load order status")
                }
            } catch (e: Exception) {
                _error.emit(e.localizedMessage ?: "Network Error")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelOrder(retailerOrderId: Int, productId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.cancelOrder(retailerOrderId, productId)

                if (response.isSuccessful && response.body()?.success == true) {
                    _cancelSuccess.emit(Unit)
                    // Refresh the specific item's status
                    fetchOrderDetail(retailerOrderId, productId)
                } else {
                    val errorMsg = response.body()?.message ?: "Cancellation failed"
                    _error.emit(errorMsg)
                }
            } catch (e: Exception) {
                _error.emit("Network connection failed. Please try again.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 🔥 3. Updated to sealed interface and data objects
    sealed interface OrderUiState {
        data object Loading : OrderUiState
        data object Empty : OrderUiState
        data class Success(val orders: List<Order>) : OrderUiState
        data class Error(val message: String) : OrderUiState
    }
}