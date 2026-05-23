package com.rudra.varushop.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rudra.varushop.databinding.FragmentWriteReviewBottomSheetBinding
import com.rudra.varushop.mvvm.ReviewViewModel
import kotlinx.coroutines.launch
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class WriteReviewBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentWriteReviewBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReviewViewModel by viewModels()

    private var onReviewSubmitted: (() -> Unit)? = null
    private var productId: Int = -1

    companion object {
        private const val ARG_PRODUCT_ID = "product_id"

        fun newInstance(productId: Int, onSubmitted: () -> Unit): WriteReviewBottomSheet {
            val fragment = WriteReviewBottomSheet()
            val args = Bundle().apply { putInt(ARG_PRODUCT_ID, productId) }
            fragment.arguments = args
            fragment.onReviewSubmitted = onSubmitted
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productId = arguments?.getInt(ARG_PRODUCT_ID) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWriteReviewBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSubmitReview.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val comment = binding.etComment.text.toString().trim()

            if (rating == 0) {
                Toast.makeText(context, "Please select at least 1 star", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.submitReview(productId, rating, comment)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.btnSubmitReview.isEnabled = !loading
                        binding.btnSubmitReview.text =
                            if (loading) "Publishing..." else "Submit Review"
                    }
                }
                launch {
                    viewModel.reviewSuccess.collect {
                        Toast.makeText(context, "Review Published!", Toast.LENGTH_SHORT).show()
                        onReviewSubmitted?.invoke()
                        dismiss()
                    }
                }
                launch {
                    viewModel.error.collect { errorMessage ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
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