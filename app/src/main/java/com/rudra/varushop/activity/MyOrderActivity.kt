package com.rudra.varushop.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.rudra.varushop.adapter.OrderAdapter
import com.rudra.varushop.bottomsheet.OrderSummaryBottomSheet
import com.rudra.varushop.bottomsheet.ShowOrderStatusFragment
import com.rudra.varushop.databinding.ActivityMyOrderBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.modal.order.Order
import com.rudra.varushop.mvvm.OrderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MyOrderActivity : BaseActivity() {

    private lateinit var binding: ActivityMyOrderBinding
    private val viewModel: OrderViewModel by viewModels()

    private val orderAdapter by lazy {
        OrderAdapter(
            onOrderClick = { order -> handleOrderNavigation(order) },
            onTrackOrderClick = { order -> handleOrderNavigation(order) })
    }
    private fun handleOrderNavigation(order: Order) {
        if (order.items.size > 1) {
            OrderSummaryBottomSheet.newInstance(order).show(supportFragmentManager, "SummarySheet")
        } else if (order.items.isNotEmpty()) {
            val item = order.items[0]
            ShowOrderStatusFragment.newInstance(item.retailerOrderId, item.productId)
                .show(supportFragmentManager, "OrderStatusSheet")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        setupWindowInsets()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeData()

        viewModel.fetchOrders()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            insets
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        binding.rvMyOrders.apply {
            layoutManager = LinearLayoutManager(this@MyOrderActivity)
            adapter = orderAdapter
        }
    }

    private fun setupListeners() {

        supportFragmentManager.setFragmentResultListener("ORDER_UPDATE_REQUEST", this) { _, _ ->
            viewModel.fetchOrders()
        }
        binding.tabLayoutStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                applyFilters()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters()
                return true
            }
        })

        binding.btnContinueShopping.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
        binding.btnRetry.setOnClickListener {
            viewModel.fetchOrders()
        }
    }

    private fun applyFilters() {
        val status = when (binding.tabLayoutStatus.selectedTabPosition) {
            1 -> "Ongoing"
            2 -> "Completed"
            3 -> "Cancelled"
            else -> "All"
        }
        val query = binding.searchView.query.toString().trim()
        viewModel.filterOrders(status, query)
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.orderState.collect { state ->
                    binding.progressBar.isVisible = state is OrderViewModel.OrderUiState.Loading

                    when (state) {
                        is OrderViewModel.OrderUiState.Success -> {
                            binding.layoutEmpty.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.rvMyOrders.isVisible = true
                            orderAdapter.submitList(state.orders)
                        }

                        is OrderViewModel.OrderUiState.Empty -> {
                            binding.rvMyOrders.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.layoutEmpty.isVisible = true
                        }

                        is OrderViewModel.OrderUiState.Error -> {
                            binding.rvMyOrders.isVisible = false
                            binding.layoutEmpty.isVisible = false
                            binding.layoutError.isVisible = true

                            binding.tvErrorMessage.text = state.message
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}