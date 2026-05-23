package com.rudra.varushop.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.rudra.varushop.adapter.HorizontalCouponAdapter
import com.rudra.varushop.adapter.OrderSummaryAdapter
import com.rudra.varushop.bottomsheet.SelectSavedAddressFragment
import com.rudra.varushop.databinding.ActivityOrderPaymentSummaryBinding
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.modal.AddressEntity
import com.rudra.varushop.modal.Coupon
import com.rudra.varushop.modal.PaymentVerificationRequest
import com.rudra.varushop.modal.cart.CartItem
import com.rudra.varushop.modal.order.OrderItemRequest
import com.rudra.varushop.modal.order.OrderRequest
import com.rudra.varushop.mvvm.AddressViewModel
import com.rudra.varushop.mvvm.CartViewModel
import com.rudra.varushop.mvvm.CouponViewModel
import com.rudra.varushop.mvvm.OrderSummaryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.floor


@AndroidEntryPoint
class OrderPaymentSummaryActivity : AppCompatActivity(), PaymentResultWithDataListener {

    private lateinit var binding: ActivityOrderPaymentSummaryBinding
    private lateinit var prefManager: PrefManager

    private val addressViewModel: AddressViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()
    private val orderSummaryViewModel: OrderSummaryViewModel by viewModels()
    private val couponViewModel: CouponViewModel by viewModels()

    private var selectedAddressId: Int? = null
    private var orderItems: MutableList<CartItem> = mutableListOf()
    private var isSingleItemPurchase: Boolean = false
    private var activeAppliedCoupon: Coupon? = null
    private var currentBackendOrderId: Int? = null

    // Adapters
    private val summaryAdapter by lazy { OrderSummaryAdapter() }
    private val horizontalCouponAdapter by lazy {
        HorizontalCouponAdapter { coupon -> applySelectedCoupon(coupon) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOrderPaymentSummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)
        Checkout.preload(applicationContext)

        setupWindowInsets()
        setupToolbar()
        setupRecyclerView()
        loadOrderData()
        setupListeners()
        observeViewModel()

        observeAddressData()

        addressViewModel.fetchAddresses()
        couponViewModel.fetchCoupons(isNetworkAvailable())
    }

    private fun setupRecyclerView() {
        binding.rvOrderSummaryItems.apply {
            layoutManager = LinearLayoutManager(this@OrderPaymentSummaryActivity)
            adapter = summaryAdapter
        }
        binding.rvHorizontalCoupons.apply {
            layoutManager = LinearLayoutManager(
                this@OrderPaymentSummaryActivity, LinearLayoutManager.HORIZONTAL, false
            )
            adapter = horizontalCouponAdapter
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                cartViewModel.cartState.collect { state ->
                    if (!isSingleItemPurchase && state is CartViewModel.CartUiState.Success) {
                        orderItems.clear()
                        orderItems.addAll(state.items)
                        summaryAdapter.submitList(orderItems)
                        calculatePricing()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                couponViewModel.uiState.collect { state ->
                    when (state) {
                        is CouponViewModel.CouponUiState.Success -> {
                            binding.tvCouponHeading.isVisible = true
                            binding.rvHorizontalCoupons.isVisible = true
                            horizontalCouponAdapter.submitList(state.coupons)
                        }

                        is CouponViewModel.CouponUiState.Empty -> {
                            binding.tvCouponHeading.isVisible = false
                            binding.rvHorizontalCoupons.isVisible = false
                        }

                        else -> {}
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                orderSummaryViewModel.orderState.collect { state ->
                    when (state) {
                        is OrderSummaryViewModel.OrderSummaryUiState.Loading -> {
                            binding.btnPayNow.isEnabled = false
                            binding.btnPayNow.text = "Processing Order Setup..."
                        }

                        is OrderSummaryViewModel.OrderSummaryUiState.OrderCreated -> {
                            binding.btnPayNow.isEnabled = true
                            binding.btnPayNow.text = "Awaiting Gateway..."
                            currentBackendOrderId = state.orderId

                            startRazorpayGatewayCheckout(state.razorpayOrderId, state.totalAmount)
                        }

                        is OrderSummaryViewModel.OrderSummaryUiState.PaymentVerifiedSuccess -> {
                            navigateToSuccessActivity()
                        }

                        is OrderSummaryViewModel.OrderSummaryUiState.Error -> {
                            binding.btnPayNow.isEnabled = true
                            binding.btnPayNow.text = "Place Order"
                            Toast.makeText(
                                this@OrderPaymentSummaryActivity, state.message, Toast.LENGTH_LONG
                            ).show()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnRemoveCoupon.setOnClickListener {
            activeAppliedCoupon = null
            binding.cardAppliedCoupon.isVisible = false
            calculatePricing()
        }

        binding.btnChangeAddress.setOnClickListener {
            val addressSheet = SelectSavedAddressFragment()
            addressSheet.setAddressListener { selectedAddress ->
                selectedAddressId = selectedAddress.id
                binding.tvAddressName.text = selectedAddress.userName
                binding.tvAddressDetail.text = selectedAddress.fullAddress
            }
            addressSheet.show(supportFragmentManager, "AddressSheet")
        }

        binding.btnPayNow.setOnClickListener {
            val finalAddressId = selectedAddressId ?: run {
                Toast.makeText(this, "Please select a valid delivery address", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val apiItems = orderItems.map { OrderItemRequest(it.productId, it.quantity) }
            val orderRequest = OrderRequest(
                addressId = finalAddressId,
                items = apiItems,
                paymentMethod = "ONLINE",
                couponId = activeAppliedCoupon?.id,
                isSingleItem = isSingleItemPurchase
            )

            orderSummaryViewModel.placeOrder(orderRequest)
        }
    }

    private fun startRazorpayGatewayCheckout(razorpayOrderId: String, totalAmount: Double) {
        val checkout = Checkout()
        try {
            val options = JSONObject().apply {
                put("name", "VaruShop")
                put("description", "Secure Order Payment")
                put("theme.color", "#6200EE")
                put("currency", "INR")
                put("order_id", razorpayOrderId)
                put("amount", (totalAmount * 100).toInt())
            }

            val prefill = JSONObject().apply {
                put("email", prefManager.userEmail)
                put("contact", "9999999999")
            }
            options.put("prefill", prefill)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Payment integration fault: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?, paymentData: PaymentData?) {
        val bOrderId = currentBackendOrderId ?: run {
            Toast.makeText(
                this,
                "Internal verification error: Missing backend tracking ID context.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (paymentData != null) {
            val verificationRequest = PaymentVerificationRequest(
                razorpay_order_id = paymentData.orderId ?: "",
                razorpay_payment_id = paymentData.paymentId ?: "",
                razorpay_signature = paymentData.signature ?: "",
                orderId = bOrderId
            )

            orderSummaryViewModel.verifyPayment(verificationRequest)
        }
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        Toast.makeText(this, "Payment Cancelled or Failed", Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun calculatePricing() {
        var subtotal = 0.0
        orderItems.forEach { subtotal += (it.price * it.quantity) }

        var couponDiscount = 0.0
        activeAppliedCoupon?.let { coupon ->
            if (subtotal >= coupon.min_order_value) {
                couponDiscount = if (coupon.discount_type == "PERCENTAGE") {
                    subtotal * (coupon.discount_value / 100.0)
                } else {
                    coupon.discount_value
                }
            } else {
                activeAppliedCoupon = null
                binding.cardAppliedCoupon.isVisible = false
                Toast.makeText(
                    this, "Order values fell below active coupon constraints", Toast.LENGTH_SHORT
                ).show()
            }
        }

        val baseAfterDiscount = subtotal - couponDiscount
        val deliveryFee = if (baseAfterDiscount > 500 || subtotal == 0.0) 0.0 else 40.0
        val tax = baseAfterDiscount * 0
        val finalTotal = baseAfterDiscount + deliveryFee + tax

        val totalStr = "₹${String.format("%.2f", finalTotal)}"
        binding.tvFinalTotal.text = totalStr
        binding.tvBottomPrice.text = totalStr

        val rewardsEarned = floor(finalTotal / 10.0).toInt()
        binding.tvPointsSummary.text = "+$rewardsEarned pts"

        binding.layoutPrice.apply {
            tvSubtotal.text = "₹${String.format("%.2f", subtotal)}"
            tvTaxAmount.text = "₹${String.format("%.2f", tax)}"


            if (deliveryFee == 0.0) {
                tvDeliveryFee.text = "FREE"
                tvDeliveryFee.setTextColor("#4CAF50".toColorInt())
            } else {
                tvDeliveryFee.text = "₹40.00"
                tvDeliveryFee.setTextColor(android.graphics.Color.BLACK)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun applySelectedCoupon(coupon: Coupon) {
        var currentSubtotal = 0.0
        orderItems.forEach { currentSubtotal += (it.price * it.quantity) }

        if (currentSubtotal < coupon.min_order_value) {
            Toast.makeText(
                this,
                "Minimum order of ₹${coupon.min_order_value} required for this coupon!",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        activeAppliedCoupon = coupon
        binding.cardAppliedCoupon.isVisible = true
        binding.tvAppliedCouponText.text = "Code '${coupon.code}' Applied Successfully!"
        calculatePricing()
    }

    @SuppressLint("SetTextI18n")
    private fun observeAddressData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                addressViewModel.addresses.collect { addresses ->
                    if (addresses.isNotEmpty()) {
                        val defaultAddress = addresses.find { it.isSelected } ?: addresses[0]
                        updateAddressUI(defaultAddress)
                    } else {
                        selectedAddressId = null
                        binding.tvAddressName.text = "No address found"
                        binding.tvAddressDetail.text = "Tap 'Change' to add a delivery address"
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateAddressUI(address: AddressEntity) {
        selectedAddressId = address.id

        binding.apply {
            tvAddressName.text = "${address.userName} (${address.label})"
            tvAddressDetail.text = "${address.fullAddress}\nPhone: ${address.phone}"
        }
    }

    private fun loadOrderData() {
        isSingleItemPurchase = intent.getBooleanExtra("IS_SINGLE_ITEM", false)
        if (isSingleItemPurchase) {
            val singleItem = CartItem(
                id = 0,
                productId = intent.getIntExtra("PRODUCT_ID", 0),
                name = intent.getStringExtra("PRODUCT_NAME") ?: "",
                price = intent.getDoubleExtra("PRODUCT_PRICE", 0.0),
                originalPrice = intent.getDoubleExtra("PRODUCT_PRICE", 0.0), // Base default assign
                discountPercent = 0.0,
                quantity = intent.getIntExtra("QUANTITY", 1),
                mainImage = intent.getStringExtra("PRODUCT_IMAGE") ?: "",
                stock = 10,
                categoryName = null,
                itemTotal = null
            )
            orderItems.clear()
            orderItems.add(singleItem)
            summaryAdapter.submitList(orderItems)
            calculatePricing()
        } else {
            cartViewModel.getCart()
        }
        binding.tvAddressName.text = prefManager.userName
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val nc = cm.getNetworkCapabilities(n) ?: return false
        return nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || nc.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR
        )
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sb = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = sb.top)
            insets
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun navigateToSuccessActivity() {
        val intent = Intent(this, OrderSuccessActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}