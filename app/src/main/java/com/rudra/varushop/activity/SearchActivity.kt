package com.rudra.varushop.activity

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.Chip
import com.rudra.varushop.adapter.ProductAdapter
import com.rudra.varushop.databinding.ActivitySearchBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.mvvm.ProductViewModel
import kotlinx.coroutines.launch
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var prefManager: PrefManager

    private val viewModel: ProductViewModel by viewModels()

    private val searchAdapter by lazy {
        ProductAdapter(
            onProductClick = { product ->
                saveRecentSearch(product.name)
                val intent = Intent(this, ShowProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                }
                startActivity(intent)
            },
            onWishlistClick = { product ->
                viewModel.toggleWishlist(product.id)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        prefManager = PrefManager(this)

        setupWindowInsets()
        setupListeners()
        setupRecentSearches()
        observeData()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text.clear()
            toggleSearchMode(false)
        }

        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            binding.btnClear.isVisible = query.isNotEmpty()

            if (query.length >= 2) {
                toggleSearchMode(true)
            } else if (query.isEmpty()) {
                toggleSearchMode(false)
            }
        }

        binding.etSearch.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()

                if (query.isNotEmpty()) {
                    saveRecentSearch(query)

                    val intent = Intent(this@SearchActivity, LoadProductActivity::class.java).apply {
                        putExtra("SEARCH_QUERY", query)
                    }
                    startActivity(intent)
                }
                true
            } else false
        }

        binding.tvClearAll.setOnClickListener {
            prefManager.clearRecentSearches()
            setupRecentSearches()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productState.collect { state ->
                    if (state is ProductViewModel.ProductUiState.Success) {
                        searchAdapter.submitList(state.products)
                    }
                }
            }
        }
    }

    private fun toggleSearchMode(isSearching: Boolean) {
        binding.rvSearchResults.isVisible = isSearching
        binding.rvSearchResults.apply {
            if (adapter == null) {
                layoutManager = GridLayoutManager(this@SearchActivity, 2)
                adapter = searchAdapter
            }
        }
    }

    private fun setupRecentSearches() {
        binding.recentChipGroup.removeAllViews()
        val searches = prefManager.getRecentSearches()

        searches.forEach { query ->
            val chip = Chip(this).apply {
                text = query
                isClickable = true
                setOnClickListener {
                    binding.etSearch.setText(query)
                    binding.etSearch.setSelection(query.length)

                    val intent = Intent(this@SearchActivity, LoadProductActivity::class.java).apply {
                        putExtra("SEARCH_QUERY", query)
                    }
                    startActivity(intent)
                }
            }
            binding.recentChipGroup.addView(chip)
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveRecentSearch(query: String) {
        if (query.isBlank()) return
        prefManager.saveSearchQuery(query)
        setupRecentSearches()
    }
}