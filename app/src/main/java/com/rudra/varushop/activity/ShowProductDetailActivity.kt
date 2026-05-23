package com.rudra.varushop.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayoutMediator
import com.rudra.varushop.R
import com.rudra.varushop.adapter.ImageSliderAdapter
import com.rudra.varushop.databinding.ActivityShowProductDetailBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.modal.product.ProductDetail
import com.rudra.varushop.mvvm.CartViewModel
import kotlinx.coroutines.launch
import com.rudra.varushop.mvvm.ProductViewModel
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import android.graphics.Paint

@AndroidEntryPoint
class ShowProductDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityShowProductDetailBinding

    private val productViewModel: ProductViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()

    private lateinit var currentProduct: ProductDetail
    private var currentProductId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        applyWindowInsets(binding.root)

        currentProductId = intent.getIntExtra("PRODUCT_ID", -1)

        if (currentProductId != -1) {
            fetchData()
        } else {
            Toast.makeText(this, "Error: Product ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupListeners()
        observeProductDetails()
        observeCartState()
        observeWishlistState()
    }

    private fun fetchData() {
        productViewModel.fetchProductDetails(currentProductId)
        productViewModel.checkWishlistStatus(currentProductId)
        cartViewModel.checkIfProductInCart(currentProductId)
    }

    private fun setupListeners() {
        binding.btnRetry.setOnClickListener {
            fetchData()
        }

        binding.btnAddToCart.setOnClickListener {
            if (binding.btnAddToCart.text.toString() == "Go to Cart") {
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("NAVIGATE_TO_CART", true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            } else {
                cartViewModel.addToCart(currentProductId, 1)
            }
        }

        binding.btnWishlist.setOnClickListener {
            if (::currentProduct.isInitialized) {
                productViewModel.toggleWishlist(currentProductId)
            }
        }

        binding.btnBuyNow.setOnClickListener {
            if (::currentProduct.isInitialized) {
                cartViewModel.addToCart(currentProductId, 1)
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("NAVIGATE_TO_CART", true)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
    }

    private fun observeWishlistState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                productViewModel.isWishlisted.collect { wishlisted ->
                    if (wishlisted) {
                        binding.btnWishlist.setImageResource(R.drawable.ic_heart_smile)
                        binding.btnWishlist.imageTintList = ColorStateList.valueOf(Color.RED)
                    } else {
                        binding.btnWishlist.setImageResource(R.drawable.ic_wishlist)
                        binding.btnWishlist.imageTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this@ShowProductDetailActivity, R.color.primary_brand) // Or whatever your default color is
                        )
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeCartState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    cartViewModel.isInCart.collect { inCart ->
                        if (inCart) {
                            binding.btnAddToCart.text = "Go to Cart"
                        } else {
                            if (binding.btnAddToCart.text != "Adding...") {
                                binding.btnAddToCart.text = "Add to Cart"
                            }
                        }
                    }
                }

                launch {
                    cartViewModel.cartState.collect { state ->
                        when (state) {
                            is CartViewModel.CartUiState.Loading -> {
                                binding.btnAddToCart.isEnabled = false
                                binding.btnAddToCart.text = "Adding..."
                            }
                            is CartViewModel.CartUiState.Success -> {
                                binding.btnAddToCart.isEnabled = true
                                binding.btnAddToCart.text = "Go to Cart"

                                Toast.makeText(this@ShowProductDetailActivity, "Added to Cart!", Toast.LENGTH_SHORT).show()
                            }
                            is CartViewModel.CartUiState.Error -> {
                                binding.btnAddToCart.isEnabled = true
                                binding.btnAddToCart.text = "Add to Cart"
                                Toast.makeText(this@ShowProductDetailActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
    private fun observeProductDetails() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                productViewModel.productDetailState.collect { state ->
                    when (state) {
                        is ProductViewModel.DetailUiState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.mainContentLayout.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.bottomBar.isVisible = false
                        }
                        is ProductViewModel.DetailUiState.Success -> {
                            binding.progressBar.isVisible = false
                            binding.layoutError.isVisible = false
                            binding.mainContentLayout.isVisible = true
                            binding.bottomBar.isVisible = true

                            currentProduct = state.product
                            populateUI(state.product)
                        }
                        is ProductViewModel.DetailUiState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.mainContentLayout.isVisible = false
                            binding.bottomBar.isVisible = false

                            binding.layoutError.isVisible = true
                            binding.tvErrorMessage.text = state.message
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun populateUI(product: ProductDetail) {
        binding.apply {
            tvTitle.text = product.name
            tvDescription.text = product.description
            tvBrand.text = product.category_name ?: "VaruShop"
            val originalPrice = product.price.toDoubleOrNull() ?: 0.0
            val discountPercent = product.discount ?: 0.0

            if (discountPercent > 0.0) {
                val discountAmount = originalPrice * (discountPercent / 100.0)
                val finalSellingPrice = originalPrice - discountAmount

                tvPrice.text = "₹${finalSellingPrice.toInt()}"

                tvOldPrice.text = "₹${originalPrice.toInt()}"
                tvOldPrice.paintFlags = tvOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvOldPrice.isVisible = true

                tvDiscount.text = "${discountPercent.toInt()}% OFF"
                tvDiscount.isVisible = true
            } else {
                tvPrice.text = "₹${originalPrice.toInt()}"
                tvOldPrice.isVisible = false
                tvDiscount.isVisible = false
            }

            if (product.images.isNotEmpty()) {
                viewPagerImages.isVisible = true
                tabIndicator.isVisible = true
                setupImageSlider(product.images)
            } else {
                viewPagerImages.isVisible = false
                tabIndicator.isVisible = false
            }
        }
    }

    private fun setupImageSlider(imageUrls: List<String>) {
        val sliderAdapter = ImageSliderAdapter(imageUrls) { position ->
            val intent = Intent(this, FullScreenImageActivity::class.java).apply {
                putStringArrayListExtra("IMAGES", ArrayList(imageUrls))
                putExtra("START_INDEX", position)
            }
            startActivity(intent)
        }
        binding.viewPagerImages.adapter = sliderAdapter
        TabLayoutMediator(binding.tabIndicator, binding.viewPagerImages) { _, _ -> }.attach()
    }




    override fun onResume() {
        super.onResume()
        if (currentProductId != -1) {
            cartViewModel.checkIfProductInCart(currentProductId)
            productViewModel.checkWishlistStatus(currentProductId)
        }
    }
}