package com.rudra.varushop.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.rudra.varushop.activity.LoadProductActivity
import com.rudra.varushop.adapter.CategoryAdapter
import com.rudra.varushop.databinding.FragmentCategoryBinding
import com.rudra.varushop.helper.GridSpacingItemDecoration
import com.rudra.varushop.mvvm.CategoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CategoryViewModel by viewModels()

    private lateinit var categoryAdapter: CategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupRecyclerView()
        observeData()

        viewModel.fetchCategories()
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            val intent = Intent(requireContext(), LoadProductActivity::class.java).apply {
                putExtra("CATEGORY_ID", category.id)
                putExtra("CATEGORY_NAME", category.name)
            }
            startActivity(intent)
        }

        val spacingInPixels = (resources.displayMetrics.density * 16).toInt()

        binding.rvSubCategories.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
            addItemDecoration(GridSpacingItemDecoration(3, spacingInPixels, true))
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categoryState.collect { state ->
                    when (state) {
                        is CategoryViewModel.CategoryUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.rvSubCategories.isVisible = false
                        }

                        is CategoryViewModel.CategoryUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.rvSubCategories.isVisible = true
                            categoryAdapter.submitList(state.categories)
                        }

                        is CategoryViewModel.CategoryUiState.Error -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT)
                                .show()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}