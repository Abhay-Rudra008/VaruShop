package com.rudra.varushop.bottomsheet

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.varushop.R
import com.rudra.varushop.databinding.FragmentShowOrderStatusBinding
import com.rudra.varushop.databinding.ItemStatusStepBinding
import com.rudra.varushop.mvvm.OrderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShowOrderStatusFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentShowOrderStatusBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderViewModel by viewModels()

    private var retailerOrderId: Int = -1
    private var productId: Int = -1

    companion object {
        private const val ARG_RETAILER_ORDER_ID = "RETAILER_ORDER_ID"
        private const val ARG_PRODUCT_ID = "PRODUCT_ID"

        fun newInstance(retailerOrderId: Int, productId: Int): ShowOrderStatusFragment {
            val fragment = ShowOrderStatusFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_RETAILER_ORDER_ID, retailerOrderId)
                putInt(ARG_PRODUCT_ID, productId)
            }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowOrderStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Capture BOTH IDs
        this.retailerOrderId = arguments?.getInt(ARG_RETAILER_ORDER_ID, -1) ?: -1
        this.productId = arguments?.getInt(ARG_PRODUCT_ID, -1) ?: -1

        Log.d(
            "CancelDebug",
            "Fragment init with RetailerOrderID: ${this.retailerOrderId}, ProductID: ${this.productId}"
        )

        if (this.retailerOrderId == -1 || this.productId == -1) {
            Log.e("CancelDebug", "Error: Missing IDs. Dismissing fragment.")
            dismiss()
            return
        }

        observeStatus()

        // Ensure your ViewModel is updated to fetch by both IDs
        viewModel.fetchOrderDetail(this.retailerOrderId, this.productId)
    }

    private fun observeStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.orderDetailState.collect { data ->
                        data?.let {
                            updateUI(
                                it.status, it.updatedAt, it.productId, it.userRating, it.userComment
                            )
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.btnCancelOrder.isEnabled = !isLoading
                        binding.btnCancelOrder.text =
                            if (isLoading) "Cancelling..." else "Cancel Item"
                    }
                }

                launch {
                    viewModel.cancelSuccess.collect {
                        Toast.makeText(
                            requireContext(), "Item Cancelled Successfully", Toast.LENGTH_SHORT
                        ).show()
                        setFragmentResult("ORDER_UPDATE_REQUEST", Bundle())
                        dismiss()
                    }
                }

                launch {
                    viewModel.error.collect { errorMessage ->
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateUI(
        status: String?, time: String?, productId: Int, userRating: Int?, userComment: String?
    ) {
        val currentStatus = status ?: "ORDERED"
        val currentTime = time ?: ""

        binding.apply {
            tvOrderId.text = "ID: #OD$retailerOrderId"

            // 1. Map the new PENDING_PAYMENT status to a Lottie animation
            val animationResId = when (currentStatus) {
                "PENDING_PAYMENT" -> R.raw.pending_payment // Make sure you have pending_payment.json in res/raw!
                "ORDERED", "CONFIRMED" -> R.raw.packaging
                "SHIPPED" -> R.raw.delivery_truck
                "DELIVERED" -> R.raw.success_check
                else -> R.raw.error_cancelled
            }

            // 2. Pass the resource ID directly
            lottieAnimation.setAnimation(animationResId)
            lottieAnimation.playAnimation()

            updateStepper(currentStatus, currentTime)

            // 3. Update button visibility rules to include PENDING_PAYMENT
            when (currentStatus) {
                "PENDING_PAYMENT", "ORDERED", "CONFIRMED" -> {
                    btnCancelOrder.visibility = View.VISIBLE
                    btnRateProduct.visibility = View.GONE
                    cardUserReview.visibility = View.GONE
                }

                "DELIVERED" -> {
                    btnCancelOrder.visibility = View.GONE
                    if (userRating != null && userRating > 0) {
                        btnRateProduct.visibility = View.GONE
                        cardUserReview.visibility = View.VISIBLE
                        reviewRatingBar.rating = userRating.toFloat()
                        tvUserComment.text = userComment ?: "No comment provided."
                    } else {
                        btnRateProduct.visibility = View.VISIBLE
                        cardUserReview.visibility = View.GONE
                        btnRateProduct.setOnClickListener { openReviewSheet(productId) }
                    }
                }

                else -> {
                    btnCancelOrder.visibility = View.GONE
                    btnRateProduct.visibility = View.GONE
                    cardUserReview.visibility = View.GONE
                }
            }

            btnCancelOrder.setOnClickListener { showCancelConfirmation() }
        }
    }

    private fun openReviewSheet(productId: Int) {
        val reviewSheet = WriteReviewBottomSheet.newInstance(productId) {
            viewModel.fetchOrderDetail(this.retailerOrderId, this.productId)
            Toast.makeText(requireContext(), "Thanks for your feedback!", Toast.LENGTH_SHORT).show()
        }
        reviewSheet.show(parentFragmentManager, "WriteReviewSheet")
    }

    private fun updateStepper(status: String, time: String) {
        binding.apply {
            resetAllSteps()
            when (status) {
                "PENDING_PAYMENT" -> {
                    setStepActive(stepPending, "Awaiting Payment", time)
                }

                "ORDERED" -> {
                    setStepActive(stepPending, "Payment Confirmed", "")
                    setStepActive(stepConfirmed, "Order Placed", time)
                }

                "CONFIRMED" -> {
                    setStepActive(stepPending, "Payment Confirmed", "")
                    setStepActive(stepConfirmed, "Order Confirmed", time)
                }

                "SHIPPED" -> {
                    setStepActive(stepPending, "Payment Confirmed", "")
                    setStepActive(stepConfirmed, "Order Confirmed", "")
                    setStepActive(stepShipped, "Shipped", time)
                }

                "DELIVERED" -> {
                    setStepActive(stepPending, "Payment Confirmed", "")
                    setStepActive(stepConfirmed, "Order Confirmed", "")
                    setStepActive(stepShipped, "Shipped", "")
                    setStepActive(stepOutForDelivery, "In Transit", "")
                    setStepActive(stepDelivered, "Delivered", time)
                }

                "CANCELLED" -> {
                    // Show cancellation right at the beginning and hide the rest
                    setStepCancelled(stepPending, "Order Cancelled", time)
                    stepConfirmed.root.visibility = View.GONE
                    stepShipped.root.visibility = View.GONE
                    stepOutForDelivery.root.visibility = View.GONE
                    stepDelivered.root.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun resetAllSteps() {
        binding.apply {
            // 🔥 ADDED stepPending to the list of steps
            val steps = listOf(stepPending, stepConfirmed, stepShipped, stepOutForDelivery, stepDelivered)

            steps.forEach { step ->
                step.root.visibility = View.VISIBLE
                step.dot.setBackgroundResource(R.drawable.bg_status_dot_inactive)
                step.line.setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(), R.color.slate_500
                    )
                )
                step.tvStatusTitle.alpha = 0.5f
                step.line.visibility = View.VISIBLE
            }

            // Hide the line for the very last step
            stepDelivered.line.visibility = View.GONE

            // Set default labels
            stepPending.tvStatusTitle.text = "Pending Payment"
            stepConfirmed.tvStatusTitle.text = "Order Confirmed"
            stepShipped.tvStatusTitle.text = "Shipped"
            stepOutForDelivery.tvStatusTitle.text = "In Transit"
            stepDelivered.tvStatusTitle.text = "Delivered"
        }
    }

    private fun setStepActive(stepBinding: ItemStatusStepBinding, title: String, time: String) {
        stepBinding.dot.setBackgroundResource(R.drawable.bg_status_dot_active)
        stepBinding.line.setBackgroundColor(
            ContextCompat.getColor(
                requireContext(), R.color.primary_brand
            )
        )
        stepBinding.tvStatusTitle.text = title
        stepBinding.tvStatusTitle.alpha = 1.0f
    }

    private fun setStepCancelled(stepBinding: ItemStatusStepBinding, title: String, time: String) {
        stepBinding.dot.setBackgroundResource(R.drawable.bg_status_dot_cancelled)
        stepBinding.line.visibility = View.GONE
        stepBinding.tvStatusTitle.text = title
        stepBinding.tvStatusTitle.setTextColor(
            ContextCompat.getColor(
                requireContext(), R.color.error
            )
        )
        stepBinding.tvStatusTitle.alpha = 1.0f
    }

    private fun showCancelConfirmation() {
        Log.d(
            "CancelDebug",
            "Cancel confirmation opened for RetailerOrderID: $retailerOrderId, ProductID: $productId"
        )

        MaterialAlertDialogBuilder(requireContext()).setTitle("Cancel Item?")
            .setMessage("Are you sure you want to cancel this specific item from your order?")
            .setNegativeButton("No", null).setPositiveButton("Yes, Cancel") { _, _ ->
                Log.d(
                    "CancelDebug", "Executing viewModel.cancelOrder($retailerOrderId, $productId)"
                )

                viewModel.cancelOrder(retailerOrderId, productId)
            }.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}