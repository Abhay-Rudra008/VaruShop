package com.rudra.varushop.mvvm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudra.varushop.modal.category.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    private val _categoryState = MutableStateFlow<CategoryUiState>(CategoryUiState.Idle)
    val categoryState = _categoryState.asStateFlow()

    fun fetchCategories() {
        viewModelScope.launch {
            _categoryState.value = CategoryUiState.Loading
            try {
                val response = repository.getCategories()
                if (response.isSuccessful && response.body()?.success == true) {

                    val categories = response.body()?.data?.categories ?: emptyList()

                    _categoryState.value = CategoryUiState.Success(categories)
                } else {
                    val errorMsg =
                        response.message().takeIf { it.isNotBlank() } ?: "Failed to load categories"
                    _categoryState.value = CategoryUiState.Error(errorMsg)
                }

            } catch (e: Exception) {
                _categoryState.value = CategoryUiState.Error(e.localizedMessage ?: "Network Error")
            }
        }
    }

    sealed interface CategoryUiState {
        data object Idle : CategoryUiState
        data object Loading : CategoryUiState
        data class Success(val categories: List<Category>) : CategoryUiState
        data class Error(val message: String) : CategoryUiState
    }
}