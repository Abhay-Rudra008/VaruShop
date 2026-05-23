package com.rudra.varushop.bottomsheet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rudra.varushop.activity.LoginActivity
import com.rudra.varushop.databinding.FragmentUpdatePasswordBottomSheetBinding
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.mvvm.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpdatePasswordBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentUpdatePasswordBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdatePasswordBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSubmit.setOnClickListener { validateAndSubmit() }
        observeViewModel()
    }

    private fun validateAndSubmit() {
        val oldPass = binding.etOldPassword.text.toString()
        val newPass = binding.etNewPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()

        if (oldPass.isEmpty() || newPass.isEmpty()) {
            Toast.makeText(context, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPass != confirmPass) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return
        }

        val passData = mapOf("oldPassword" to oldPass, "newPassword" to newPass)
        viewModel.changePassword(passData)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.updateStatus.collect { message ->
                if (message.isEmpty()) return@collect

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                if (message.contains("Success", ignoreCase = true)) {
                    val prefManager = PrefManager(requireContext())
                    prefManager.logout()
                    redirectToLogin()
                    dismiss()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isUpdating.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.btnSubmit.isEnabled = !isLoading
            }
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}