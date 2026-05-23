package com.rudra.varushop.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rudra.varushop.adapter.PlacedOrderSummaryAdapter
import com.rudra.varushop.databinding.FragmentOrderSummaryBottomSheetBinding
import com.rudra.varushop.modal.order.Order
import com.rudra.varushop.modal.order.OrderItem
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderSummaryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentOrderSummaryBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderSummaryBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val order = arguments?.getParcelable<Order>(ARG_ORDER) ?: return

        val adapter = PlacedOrderSummaryAdapter { item: OrderItem ->

            val statusSheet = ShowOrderStatusFragment.newInstance(
                item.retailerOrderId,
                item.productId
            )
            statusSheet.show(parentFragmentManager, "OrderStatusSheet")

            dismiss()
        }

        binding.rvOrderItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrderItems.adapter = adapter
        adapter.submitList(order.items)
    }

    companion object {
        private const val ARG_ORDER = "order_data"

        fun newInstance(order: Order) = OrderSummaryBottomSheet().apply {
            arguments = Bundle().apply { putParcelable(ARG_ORDER, order) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}