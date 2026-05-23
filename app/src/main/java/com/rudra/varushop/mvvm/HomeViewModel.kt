package com.rudra.varushop.mvvm


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.category.Category
import com.rudra.varushop.modal.product.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _products = MutableStateFlow<List<Product>?>(null)
    val products = _products.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>?>(null)
    val categories = _categories.asStateFlow()

    private val _showConnectionError = MutableStateFlow(false)
    val showConnectionError = _showConnectionError.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    fun fetchData() {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null
        _showConnectionError.value = false

        viewModelScope.launch {
            try {
                val productDeferred = async { repository.getAllProducts() }
                val categoryDeferred = async { repository.getCategories() }

                val pRes = productDeferred.await()
                val cRes = categoryDeferred.await()

                if (pRes.isSuccessful && pRes.body()?.success == true) {
                    val productList = pRes.body()?.data?.products ?: emptyList()
                    _products.value = productList.reversed() // Show newest first
                } else {
                    _showConnectionError.value = true
                    _errorMessage.value = pRes.body()?.message ?: "Failed to load products"
                }

                if (cRes.isSuccessful && cRes.body()?.success == true) {
                    _categories.value = cRes.body()?.data?.categories ?: emptyList()
                } else {
                    _categories.value = emptyList()
                }


            } catch (e: Exception) {
                _showConnectionError.value = true
                _errorMessage.value = "Network Error: Check your connection"
            } finally {
                _isLoading.value = false
            }
        }
    }
}