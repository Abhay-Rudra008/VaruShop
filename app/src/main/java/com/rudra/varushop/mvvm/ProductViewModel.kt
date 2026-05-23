package com.rudra.varushop.mvvm

import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.product.Product
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.rudra.varushop.modal.BaseResponse
import com.rudra.varushop.modal.product.ProductDetail
import com.rudra.varushop.modal.product.ProductListData
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel // 🔥 1. Required for Hilt
class ProductViewModel @Inject constructor(
    private val repository: HomeRepository // 🔥 2. Hilt automatically injects this
) : ViewModel() {

    private val _productState = MutableStateFlow<ProductUiState>(ProductUiState.Idle)
    val productState = _productState.asStateFlow()

    private var allProducts = listOf<Product>()

    private var currentSortOption = 0
    private var currentFilterOption = 0

    private val _isWishlisted = MutableStateFlow(false)
    val isWishlisted = _isWishlisted.asStateFlow()

    private val _isInCart = MutableStateFlow(false)
    val isInCart = _isInCart.asStateFlow()

    private val _productDetailState = MutableStateFlow<DetailUiState>(DetailUiState.Idle)
    val productDetailState = _productDetailState.asStateFlow()

    fun loadProducts() {
        viewModelScope.launch {
            _productState.value = ProductUiState.Loading
            try {
                val response = repository.getAllProducts()
                handleProductListResponse(response)
            } catch (e: Exception) {
                _productState.value = ProductUiState.Error("Check your connection")
            }
        }
    }

    private fun handleProductListResponse(response: Response<BaseResponse<ProductListData>>) {
        if (response.isSuccessful && response.body()?.success == true) {
            allProducts = response.body()?.data?.products ?: emptyList()
            applyFiltersAndSort()
        } else {
            val errorMsg = response.message().takeIf { it.isNotBlank() } ?: response.body()?.message ?: "Server Error"
            _productState.value = ProductUiState.Error(errorMsg)
        }
    }

    fun loadProductsByCategory(categoryId: Int) {
        viewModelScope.launch {
            _productState.value = ProductUiState.Loading
            try {
                val response = repository.getProductsByCategory(categoryId)
                handleProductListResponse(response)
            } catch (e: Exception) {
                _productState.value = ProductUiState.Error("Network error")
            }
        }
    }

    fun searchProducts(query: String) {
        if (query.isBlank()) {
            loadProducts()
            return
        }
        viewModelScope.launch {
            _productState.value = ProductUiState.Loading
            try {
                val response = repository.searchProducts(query)
                handleProductListResponse(response)
            } catch (e: Exception) {
                _productState.value = ProductUiState.Error("Search failed: ${e.localizedMessage}")
            }
        }
    }

    fun fetchProductDetails(id: Int) {
        viewModelScope.launch {
            _productDetailState.value = DetailUiState.Loading
            try {
                val response = repository.getProductDetails(id)
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    _productDetailState.value = DetailUiState.Success(response.body()!!.data!!)

                    checkWishlistStatus(id)
                    checkIfProductInCart(id)
                } else {
                    val errorMsg = response.message().takeIf { it.isNotBlank() } ?: response.body()?.message ?: "Failed to load details"
                    _productDetailState.value = DetailUiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _productDetailState.value = DetailUiState.Error("Connection error: ${e.localizedMessage}")
            }
        }
    }

    // 🔥 FIXED: Added .data wrapper extraction
    fun checkWishlistStatus(productId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.checkWishlistStatus(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _isWishlisted.value = response.body()?.data?.isWishlisted ?: false
                } else {
                    _isWishlisted.value = false
                }
            } catch (e: Exception) {
                _isWishlisted.value = false
            }
        }
    }

    // 🔥 FIXED: Added .data wrapper extraction + Optimistic UI (Instant toggle)
    fun toggleWishlist(productId: Int) {
        // Instantly update UI for a snappy feel
        _isWishlisted.value = !_isWishlisted.value

        viewModelScope.launch {
            try {
                val response = repository.toggleWishlist(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    // Confirm with server truth
                    _isWishlisted.value = response.body()?.data?.isWishlisted ?: false
                } else {
                    // Revert if server failed
                    _isWishlisted.value = !_isWishlisted.value
                }
            } catch (e: Exception) {
                // Revert if network failed
                _isWishlisted.value = !_isWishlisted.value
            }
        }
    }

    // 🔥 FIXED: Added .data wrapper extraction
    fun checkIfProductInCart(productId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.checkCartStatus(productId)
                if (response.isSuccessful && response.body()?.success == true) {
                    _isInCart.value = response.body()?.data?.isInCart ?: false
                } else {
                    _isInCart.value = false
                }
            } catch (e: Exception) {
                _isInCart.value = false
            }
        }
    }

    fun setSortOption(option: Int) {
        currentSortOption = option
        applyFiltersAndSort()
    }

    fun setFilterOption(option: Int) {
        currentFilterOption = option
        applyFiltersAndSort()
    }

    fun clearFilters() {
        currentSortOption = 0
        currentFilterOption = 0
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        var processedList = allProducts

        processedList = when (currentFilterOption) {
            1 -> processedList.filter { it.price < 100 }
            2 -> processedList.filter { it.price in 100.0..1000.0 }
            3 -> processedList.filter { it.price > 1000 && it.price <= 10000 }
            4 -> processedList.filter { it.price > 10000 }
            else -> processedList
        }

        processedList = when (currentSortOption) {
            1 -> processedList.sortedBy { it.price }
            2 -> processedList.sortedByDescending { it.price }
            else -> processedList
        }

        if (processedList.isEmpty()) {
            _productState.value = ProductUiState.Empty
        } else {
            _productState.value = ProductUiState.Success(processedList)
        }
    }

    sealed interface ProductUiState {
        data object Idle : ProductUiState
        data object Loading : ProductUiState
        data object Empty : ProductUiState
        data class Success(val products: List<Product>) : ProductUiState
        data class Error(val message: String) : ProductUiState
    }

    sealed interface DetailUiState {
        data object Idle : DetailUiState
        data object Loading : DetailUiState
        data class Success(val product: ProductDetail) : DetailUiState
        data class Error(val message: String) : DetailUiState
    }
}