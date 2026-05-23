package com.rudra.varushop.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.rudra.varushop.R
import com.rudra.varushop.activity.LoadProductActivity
import com.rudra.varushop.activity.SearchActivity
import com.rudra.varushop.activity.ShowProductDetailActivity
import com.rudra.varushop.adapter.CategoryAdapter
import com.rudra.varushop.adapter.ProductAdapter
import com.rudra.varushop.databinding.FragmentHomeBinding
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.helper.hideError
import com.rudra.varushop.helper.showError
import com.rudra.varushop.mvvm.HomeViewModel
import java.util.Calendar
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeData()
        setupListeners()
        updateGreetingUI()

        binding.swipeRefresh.setOnRefreshListener { viewModel.fetchData() }

        if (viewModel.products.value == null) {
            viewModel.fetchData()
        }
    }

    private fun setupRecyclerViews() {
        categoryAdapter = CategoryAdapter { category ->
            val intent = Intent(requireContext(), LoadProductActivity::class.java).apply {
                putExtra("CATEGORY_ID", category.id)
                putExtra("CATEGORY_NAME", category.name)
            }
            startActivity(intent)
        }

        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        productAdapter = ProductAdapter(
            onProductClick = { product ->
                val intent = Intent(requireContext(), ShowProductDetailActivity::class.java).apply {
                    putExtra("PRODUCT_ID", product.id)
                }
                startActivity(intent)
            },
            onWishlistClick = { product -> /* Not have idea what to do with wishlist , so not making anything*/ }
        )

        binding.rvProducts.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.products.collect { productList ->
                        if (!productList.isNullOrEmpty()) {
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.rvProducts.visibility = View.VISIBLE
                            productAdapter.submitList(productList)
                        } else if (!viewModel.isLoading.value && !viewModel.showConnectionError.value) {
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.rvProducts.visibility = View.GONE
                        }
                    }
                }
                launch {
                    viewModel.categories.collect { categoryList ->
                        categoryAdapter.submitList(categoryList)
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (binding.swipeRefresh.isRefreshing) {
                            if (!isLoading) binding.swipeRefresh.isRefreshing = false
                        } else {
                            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        }
                    }
                }
                launch {
                    viewModel.showConnectionError.collect { isError ->
                        if (isError) {
                            binding.swipeRefresh.visibility = View.GONE
                            binding.layoutError.root.visibility = View.VISIBLE
                            binding.layoutError.root.showError("Server Connection Lost") { viewModel.fetchData() }
                        } else {
                            binding.layoutError.root.hideError()
                            binding.layoutError.root.visibility = View.GONE
                            binding.swipeRefresh.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.searchBar.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
    }

    private fun updateGreetingUI() {
        val prefManager = PrefManager(requireContext())
        binding.welcomeName.text = prefManager.userName ?: "User"

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (stringRes, colorRes) = when (hour) {
            in 0..11 -> R.string.greeting_morning to R.color.color_morning
            in 12..16 -> R.string.greeting_afternoon to R.color.color_afternoon
            in 17..20 -> R.string.greeting_evening to R.color.color_evening
            else -> R.string.greeting_night to R.color.color_night
        }
        binding.wishingTxt.apply {
            text = getString(stringRes)
            setTextColor(ContextCompat.getColor(context, colorRes))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}