package com.rudra.varushop.activity

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rudra.varushop.adapter.PointsAdapter
import com.rudra.varushop.databinding.ActivityRewardPointBinding
import com.rudra.varushop.mvvm.RewardPointViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class RewardPointActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardPointBinding
    private val viewModel: RewardPointViewModel by viewModels()
    private lateinit var adapter: PointsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewBinding
        binding = ActivityRewardPointBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        observeViewModel()

        binding.btnRetry.setOnClickListener {
            viewModel.fetchPoints(isNetworkAvailable())
        }

        viewModel.fetchPoints(isNetworkAvailable())
    }

    private fun setupRecyclerView() {
        adapter = PointsAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->

                    binding.progressBar.isVisible = state is RewardPointViewModel.RewardPointUiState.Loading
                    binding.recyclerView.isVisible = state is RewardPointViewModel.RewardPointUiState.Success
                    binding.layoutEmpty.isVisible = state is RewardPointViewModel.RewardPointUiState.Empty
                    binding.layoutNoInternet.isVisible = state is RewardPointViewModel.RewardPointUiState.NoInternet

                     binding.cardPointsSummary.isVisible = state is RewardPointViewModel.RewardPointUiState.Success

                    when (state) {
                        is RewardPointViewModel.RewardPointUiState.Success -> {
                            // Populate the list
                            adapter.submitList(state.pointsData.transactions)

                             binding.tvTotalPoints.text = state.pointsData.totalActivePoints.toString()
                             binding.tvPendingPoints.text = "Pending: ${state.pointsData.totalPendingPoints}"
                        }

                        is RewardPointViewModel.RewardPointUiState.Error -> {
                            Toast.makeText(this@RewardPointActivity, state.message, Toast.LENGTH_SHORT).show()
                        }

                        else -> {

                        }
                    }
                }
            }
        }
    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}