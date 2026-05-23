package com.rudra.varushop.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.varushop.adapter.ProductAdapter
import com.rudra.varushop.databinding.ActivityLoadProductBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.mvvm.ProductViewModel
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint



@AndroidEntryPoint
class LoadProductActivity : BaseActivity() {

    private var selectedSortIndex = 0
    private var selectedFilterIndex = 0
    private lateinit var binding: ActivityLoadProductBinding
    private val viewModel: ProductViewModel by viewModels()

    private val productAdapter by lazy {
        ProductAdapter(
            onProductClick = { product ->
                val intent = Intent(this, ShowProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                }
                startActivity(intent)
            },
            onWishlistClick = { product ->
                viewModel.toggleWishlist(product.id)
                Toast.makeText(this, "Updating Wishlist...", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoadProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        applyWindowInsets()
        setupSortAndFilter()
        setupToolbar()
        setupRecyclerView()
        observeData()

        handleIntentData()
    }

    private fun handleIntentData() {
        val catId = intent.getIntExtra("CATEGORY_ID", -1)
        val categoryName = intent.getStringExtra("CATEGORY_NAME")
        val searchQuery = intent.getStringExtra("SEARCH_QUERY")

        when {
            !searchQuery.isNullOrEmpty() -> {
                binding.toolbar.title = "Results for \"$searchQuery\""
                viewModel.searchProducts(searchQuery)
            }
            catId != -1 -> {
                binding.toolbar.title = categoryName ?: "Products"
                viewModel.loadProductsByCategory(catId)
            }
            else -> {
                binding.toolbar.title = "All Products"
                viewModel.loadProducts()
            }
        }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBar.setPadding(0, systemBars.top, 0, 0)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(this@LoadProductActivity, 2)
            adapter = productAdapter
            setHasFixedSize(true)
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productState.collect { state ->
                    when (state) {
                        is ProductViewModel.ProductUiState.Loading -> toggleLoading(true)
                        is ProductViewModel.ProductUiState.Success -> {
                            toggleLoading(false)
                            binding.rvProducts.isVisible = true
                            binding.layoutNoProduct.isVisible = false
                            productAdapter.submitList(state.products)
                        }
                        is ProductViewModel.ProductUiState.Empty -> {
                            toggleLoading(false)
                            binding.rvProducts.isVisible = false
                            binding.layoutNoProduct.isVisible = true
                        }
                        is ProductViewModel.ProductUiState.Error -> {
                            toggleLoading(false)
                            Toast.makeText(this@LoadProductActivity, state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun setupSortAndFilter() {
        binding.btnSort.setOnClickListener { showSortDialog() }
        binding.btnFilter.setOnClickListener { showFilterDialog() }
    }

    @SuppressLint("SetTextI18n")
    private fun showSortDialog() {
        val sortOptions = arrayOf("Default", "Price: Low to High", "Price: High to Low")
        MaterialAlertDialogBuilder(this)
            .setTitle("Sort By")
            .setSingleChoiceItems(sortOptions, selectedSortIndex) { dialog, which ->
                selectedSortIndex = which
                viewModel.setSortOption(which)
                binding.btnSort.text = if (which == 0) "Sort" else "Sorted"
                dialog.dismiss()
            }
            .setNeutralButton("Clear") { dialog, _ ->
                selectedSortIndex = 0
                viewModel.setSortOption(0)
                binding.btnSort.text = "Sort"
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun showFilterDialog() {
        val filterOptions = arrayOf("All Prices", "Under ₹100", "₹100 to ₹1000", "₹1000 to ₹10000", "Above ₹10000")
        MaterialAlertDialogBuilder(this)
            .setTitle("Filter By Price")
            .setSingleChoiceItems(filterOptions, selectedFilterIndex) { dialog, which ->
                selectedFilterIndex = which
                viewModel.setFilterOption(which)
                binding.btnFilter.text = if (which == 0) "Filter" else filterOptions[which]
                dialog.dismiss()
            }
            .setNeutralButton("Clear") { dialog, _ ->
                selectedFilterIndex = 0
                viewModel.setFilterOption(0)
                binding.btnFilter.text = "Filter"
                dialog.dismiss()
            }
            .show()
    }

    private fun toggleLoading(isLoading: Boolean) {
        binding.shimmerView.apply {
            if (isLoading) {
                isVisible = true
                startShimmer()
            } else {
                stopShimmer()
                isVisible = false
            }
        }
    }
}