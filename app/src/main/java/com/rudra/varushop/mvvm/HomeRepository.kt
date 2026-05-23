package com.rudra.varushop.mvvm

import com.rudra.varushop.helper.ApiService
import com.rudra.varushop.modal.AddressRequest
import com.rudra.varushop.modal.BaseResponse
import com.rudra.varushop.modal.PaymentVerificationRequest
import com.rudra.varushop.modal.cart.AddToCartRequest
import com.rudra.varushop.modal.category.CategoryListData
import com.rudra.varushop.modal.order.OrderRequest
import com.rudra.varushop.modal.review.ReviewRequest
import com.rudra.varushop.modal.user.LoginRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import javax.inject.Inject

class HomeRepository @Inject constructor(
    private val apiService: ApiService
) {

    suspend fun login(request: LoginRequest) = apiService.login(request)

    suspend fun register(
        name: RequestBody, email: RequestBody, pass: RequestBody, image: MultipartBody.Part?
    ) = apiService.register(name, email, pass, image)

    suspend fun getProfileDetail() = apiService.getProfileDetail()

    suspend fun updateProfile(name: RequestBody, image: MultipartBody.Part?) =
        apiService.updateProfile(name, image)

    suspend fun removeProfileImage() = apiService.removeProfileImage()

    suspend fun changePassword(passData: Map<String, String>) = apiService.changePassword(passData)


    suspend fun getAllProducts() = apiService.getAllProducts()

    suspend fun getProductDetails(id: Int) = apiService.getProductDetails(id)

    suspend fun getCategories(): Response<BaseResponse<CategoryListData>> {
        return apiService.getCategories()
    }

    suspend fun getProductsByCategory(categoryId: Int) =
        apiService.getProductsByCategory(categoryId)

    suspend fun searchProducts(query: String) = apiService.searchProducts(query)


    suspend fun getCart() = apiService.getCart()

    suspend fun addToCart(request: AddToCartRequest) = apiService.addToCart(request)

    suspend fun deleteItem(productId: Int) = apiService.removeCartItem(productId)

    suspend fun checkCartStatus(productId: Int) = apiService.checkCartStatus(productId)


    suspend fun getWishlist() = apiService.getWishlist()

    suspend fun toggleWishlist(productId: Int) =
        apiService.toggleWishlist(mapOf("product_id" to productId))

    suspend fun checkWishlistStatus(productId: Int) = apiService.checkWishlistStatus(productId)


    suspend fun placeOrder(request: OrderRequest) = apiService.placeOrder(request)
    suspend fun verifyPayment(
        request: PaymentVerificationRequest
    ): Response<BaseResponse<Unit>> {
        return apiService.verifyPayment(request)
    }

    suspend fun getOrders() = apiService.getOrders()

    suspend fun getOrderStatus(retailerOrderId: Int, productId: Int) =
        apiService.getOrderStatus(retailerOrderId, productId)

    suspend fun cancelOrder(retailerOrderId: Int, productId: Int) = apiService.cancelOrder(
        mapOf(
            "retailerOrderId" to retailerOrderId,
            "productId" to productId
        )
    )
    suspend fun getUserStats() = apiService.getUserStats()

    suspend fun submitProductReview(productId: Int, rating: Int, comment: String) =
        apiService.submitReview(ReviewRequest(productId, rating, comment))


    suspend fun getAddressesFromCloud() = apiService.getAddresses()

    suspend fun addAddressToCloud(request: AddressRequest) = apiService.addAddress(request)

    suspend fun updateAddressInCloud(id: Int, request: AddressRequest) =
        apiService.updateAddress(id, request)

    suspend fun setDefaultAddressInCloud(addressId: Int) = apiService.setDefaultAddress(addressId)

    suspend fun deleteAddressFromCloud(addressId: Int) = apiService.deleteAddress(addressId)

    suspend fun getPointsHistoryFromCloud(userId: Int) = apiService.getPointsHistory(userId)

    suspend fun getAllCouponsFromCloud() = apiService.getAllCoupons()
}