package com.rudra.varushop.bottomsheet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.varushop.activity.AddAddressActivity
import com.rudra.varushop.adapter.AddressAdapter
import com.rudra.varushop.databinding.FragmentSelectSavedAddressBinding
import com.rudra.varushop.modal.AddressEntity
import com.rudra.varushop.mvvm.AddressViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectSavedAddressFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentSelectSavedAddressBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddressViewModel by viewModels()
    private var onAddressSelected: ((AddressEntity) -> Unit)? = null

    private lateinit var adapter: AddressAdapter

    fun setAddressListener(listener: (AddressEntity) -> Unit) {
        this.onAddressSelected = listener
    }

    private val addAddressLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.fetchAddresses()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectSavedAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()

        // Fetch initial data
        viewModel.fetchAddresses()
    }

    private fun setupRecyclerView() {
        adapter = AddressAdapter(onAddressClick = { address ->
            viewModel.setDefaultAddressInCloud(address.id)
            onAddressSelected?.invoke(address)
            dismiss()
        }, onEditClick = { address ->
            val intent = Intent(requireContext(), AddAddressActivity::class.java).apply {
                putExtra("EDIT_MODE", true)
                putExtra("ADDRESS_DATA", address)
            }
            addAddressLauncher.launch(intent)
        }, onDeleteClick = { address ->
            MaterialAlertDialogBuilder(requireContext()).setTitle("Delete Address?")
                .setMessage("Remove this address from your profile?")
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteAddressFromCloud(address.id) }
                .setNegativeButton("Cancel", null).show()
        })

        binding.rvSavedAddresses.apply {
            this.adapter = this@SelectSavedAddressFragment.adapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupClickListeners() {
        binding.btnAddAddress.setOnClickListener {
            val intent = Intent(requireContext(), AddAddressActivity::class.java)
            addAddressLauncher.launch(intent)
        }

        binding.btnRetry.setOnClickListener {
            binding.layoutNoInternet.visibility = View.GONE
            viewModel.fetchAddresses()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.addresses.collect { addresses ->
                    adapter.submitList(addresses)

                    binding.layoutNoInternet.visibility = View.GONE

                    if (addresses.isEmpty() && !viewModel.isLoading.value) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvSavedAddresses.visibility = View.GONE
                    } else if (addresses.isNotEmpty()) {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvSavedAddresses.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

                    if (isLoading) {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.layoutNoInternet.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    val isNetworkError = errorMsg.contains("Network", true) || errorMsg.contains(
                        "Connection",
                        true
                    ) || errorMsg.contains("Failed", true)

                    if (isNetworkError && adapter.currentList.isEmpty()) {
                        binding.layoutNoInternet.visibility = View.VISIBLE
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvSavedAddresses.visibility = View.GONE
                    } else {
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
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