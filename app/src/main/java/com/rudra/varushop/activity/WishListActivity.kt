package com.rudra.varushop.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.rudra.varushop.R
import com.rudra.varushop.adapter.ProductAdapter
import com.rudra.varushop.databinding.ActivityWishListBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.mvvm.WishlistViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WishListActivity : BaseActivity() {

    private lateinit var binding: ActivityWishListBinding
    private val viewModel: WishlistViewModel by viewModels()

    private val wishlistAdapter by lazy {
        ProductAdapter(onProductClick = { product ->
            val intent = Intent(this, ShowProductDetailActivity::class.java)
            intent.putExtra("PRODUCT_ID", product.id)
            startActivity(intent)
        }, onWishlistClick = { product ->
            viewModel.toggleWishlist(product.id, isFromWishlistScreen = true)
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWishListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupRefreshLayout()
        observeViewModel()
        viewModel.fetchWishlist()
        binding.btnExplore.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "home")
            }
            startActivity(intent)
            finish()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.rvWishlist.apply {
            layoutManager = GridLayoutManager(this@WishListActivity, 2)
            adapter = wishlistAdapter
            itemAnimator = DefaultItemAnimator()
        }
    }

    private fun setupRefreshLayout() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchWishlist() }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.wishlistState.collect { state ->
                    when (state) {
                        is WishlistViewModel.WishlistUiState.Loading -> {
                            if (!binding.swipeRefresh.isRefreshing) {
                                binding.progressBar.visibility = View.VISIBLE
                            }
                        }

                        is WishlistViewModel.WishlistUiState.Success -> {
                            hideLoaders()
                            binding.rvWishlist.visibility = View.VISIBLE
                            binding.layoutEmptyWishlist.visibility = View.GONE

                            val items = state.items.map { it.copy(isWishlisted = true) }
                            wishlistAdapter.submitList(items)
                        }

                        is WishlistViewModel.WishlistUiState.Empty -> {
                            hideLoaders()
                            binding.rvWishlist.visibility = View.GONE
                            binding.layoutEmptyWishlist.visibility = View.VISIBLE
                        }

                        is WishlistViewModel.WishlistUiState.Error -> {
                            hideLoaders()
                            Toast.makeText(this@WishListActivity, state.message, Toast.LENGTH_SHORT)
                                .show()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun hideLoaders() {
        binding.progressBar.visibility = View.GONE
        binding.swipeRefresh.isRefreshing = false
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchWishlist()
    }
}