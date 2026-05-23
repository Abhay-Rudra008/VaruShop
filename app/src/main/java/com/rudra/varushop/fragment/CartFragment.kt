package com.rudra.varushop.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.varushop.activity.OrderPaymentSummaryActivity
import com.rudra.varushop.adapter.CartAdapter
import com.rudra.varushop.databinding.FragmentCartBinding
import com.rudra.varushop.modal.cart.CartItem
import com.rudra.varushop.mvvm.CartViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CartFragment : Fragment() {
    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CartViewModel by viewModels()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeData()
        viewModel.getCart()
    }

    private fun setupListeners() {
        binding.btnCheckout.setOnClickListener {
            val intent = Intent(requireContext(), OrderPaymentSummaryActivity::class.java).apply {
                putExtra("IS_SINGLE_ITEM", false)
            }
            startActivity(intent)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.getCart()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter(onQuantityChanged = { id, currentQty, stock, isIncrease ->
            handleQuantityChange(id, currentQty, stock, isIncrease)
        }, onRemoveItem = { id ->
            showDeleteConfirmation(id)
        })

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun handleQuantityChange(id: Int, currentQty: Int, stock: Int, isIncrease: Boolean) {
        if (isIncrease) {
            if (currentQty < stock) {
                viewModel.updateCartQuantity(id, currentQty + 1)
            } else {
                Toast.makeText(requireContext(), "Stock limit reached!", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (currentQty > 1) {
                viewModel.updateCartQuantity(id, currentQty - 1)
            } else {
                showDeleteConfirmation(id)
            }
        }
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cartState.collect { state ->
                        when (state) {
                            is CartViewModel.CartUiState.Loading -> {
                                binding.btnCheckout.isEnabled = false
                                binding.btnCheckout.text = "Syncing..."
                            }

                            is CartViewModel.CartUiState.Success -> {
                                binding.btnCheckout.isEnabled = true
                                binding.btnCheckout.text = "Proceed to Checkout"
                                handleCartContent(state.items)
                            }

                            is CartViewModel.CartUiState.Error -> {
                                binding.btnCheckout.isEnabled = false
                                showErrorState(state.message)
                            }

                            else -> Unit
                        }
                    }
                }

                launch {
                    viewModel.totalPrice.collect { total ->
                        binding.tvTotalPrice.text = "₹${String.format("%.2f", total)}"
                    }
                }
            }
        }
    }

    private fun handleCartContent(items: List<CartItem>) {
        if (items.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.layoutError.visibility = View.GONE
            binding.cardCheckout.visibility = View.GONE
            binding.scrollView.visibility = View.GONE
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.layoutError.visibility = View.GONE
            binding.cardCheckout.visibility = View.VISIBLE
            binding.scrollView.visibility = View.VISIBLE
            cartAdapter.submitList(items)
        }
    }

    private fun showErrorState(message: String) {
        binding.scrollView.visibility = View.GONE
        binding.cardCheckout.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE

        binding.layoutError.visibility = View.VISIBLE
        binding.tvErrorMessage.text = message
    }

    private fun showDeleteConfirmation(productId: Int) {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Remove from Cart?")
            .setMessage("Do you want to remove this item from your shopping cart?")
            .setPositiveButton("Remove") { _, _ -> viewModel.removeItem(productId) }
            .setNegativeButton("Cancel", null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}