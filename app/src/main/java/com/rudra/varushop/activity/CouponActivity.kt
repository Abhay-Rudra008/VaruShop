package com.rudra.varushop.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.rudra.varushop.adapter.CouponAdapter
import com.rudra.varushop.databinding.ActivityCouponBinding
import com.rudra.varushop.mvvm.CouponViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.rudra.varushop.mvvm.CouponViewModel.CouponUiState


@AndroidEntryPoint
class CouponActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCouponBinding
    private val viewModel: CouponViewModel by viewModels()
    private lateinit var adapter: CouponAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCouponBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        setupTabs()
        observeViewModel()

        binding.btnRetry.setOnClickListener {
            viewModel.fetchCoupons(isNetworkAvailable())
        }

        viewModel.fetchCoupons(isNetworkAvailable())
    }

    private fun setupRecyclerView() {
        adapter = CouponAdapter()
        binding.recyclerView.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isLiveTab = tab?.position == 0
                viewModel.filterCoupons(isLiveTab)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->

                binding.progressBar.isVisible = state is CouponUiState.Loading
                binding.recyclerView.isVisible = state is CouponUiState.Success
                binding.layoutEmpty.isVisible = state is CouponUiState.Empty
                binding.layoutError.isVisible = state is CouponUiState.Error || state is CouponUiState.NoInternet

                when (state) {
                    is CouponUiState.Success -> {
                        adapter.submitList(state.coupons)
                    }
                    is CouponUiState.Empty -> {
                        binding.tvEmptyTitle.text = state.message
                    }
                    is CouponUiState.Error -> {
                        binding.tvErrorMsg.text = state.message
                        binding.btnRetry.isVisible = true
                    }
                    is CouponUiState.NoInternet -> {
                        binding.tvErrorMsg.text = "Connection Lost"
                        binding.btnRetry.isVisible = true
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}