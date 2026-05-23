package com.rudra.varushop.helper

import com.rudra.varushop.modal.AddressEntity
import com.rudra.varushop.modal.AddressRequest
import com.rudra.varushop.modal.BaseResponse
import com.rudra.varushop.modal.Coupon
import com.rudra.varushop.modal.PaymentVerificationRequest
import com.rudra.varushop.modal.PointsData
import com.rudra.varushop.modal.UserStats
import com.rudra.varushop.modal.cart.AddToCartRequest
import com.rudra.varushop.modal.cart.CartCheckResponse
import com.rudra.varushop.modal.cart.CartData
import com.rudra.varushop.modal.category.CategoryListData
import com.rudra.varushop.modal.order.Order
import com.rudra.varushop.modal.order.OrderCreationData
import com.rudra.varushop.modal.order.OrderRequest
import com.rudra.varushop.modal.order.OrderStatusResponse
import com.rudra.varushop.modal.product.ProductDetail
import com.rudra.varushop.modal.product.ProductListData
import com.rudra.varushop.modal.review.ReviewRequest
import com.rudra.varushop.modal.user.AuthData
import com.rudra.varushop.modal.user.DataWrapper
import com.rudra.varushop.modal.user.LoginRequest
import com.rudra.varushop.modal.user.UserDto
import com.rudra.varushop.modal.wishlist.WishlistDataContainer
import com.rudra.varushop.modal.wishlist.WishlistResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<BaseResponse<AuthData>>

    @Multipart
    @POST("auth/register")
    suspend fun register(
        @Part("name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") pass: RequestBody,
        @Part profile_image: MultipartBody.Part?
    ): Response<BaseResponse<DataWrapper>>

    @GET("auth/profile")
    suspend fun getProfileDetail(): Response<BaseResponse<UserDto>>

    @Multipart
    @PUT("auth/profile/update")
    suspend fun updateProfile(
        @Part("name") name: RequestBody, @Part image: MultipartBody.Part?
    ): Response<BaseResponse<Unit>>

    @POST("auth/change-password")
    suspend fun changePassword(@Body passData: Map<String, String>): Response<BaseResponse<Unit>>

    @DELETE("auth/profile/image")
    suspend fun removeProfileImage(): Response<BaseResponse<Unit>>


    @GET("user/categories")
    suspend fun getCategories(): Response<BaseResponse<CategoryListData>>

    @GET("user/products")
    suspend fun getAllProducts(): Response<BaseResponse<ProductListData>>

    @GET("user/products/{id}")
    suspend fun getProductDetails(@Path("id") id: Int): Response<BaseResponse<ProductDetail>>

    @GET("user/products/search")
    suspend fun searchProducts(@Query("q") query: String): Response<BaseResponse<ProductListData>>

    @GET("user/products/category/{id}")
    suspend fun getProductsByCategory(@Path("id") categoryId: Int): Response<BaseResponse<ProductListData>>


    @POST("user/cart")
    suspend fun addToCart(@Body cartRequest: AddToCartRequest): Response<BaseResponse<CartData>>

    @GET("user/cart")
    suspend fun getCart(): Response<BaseResponse<CartData>>

    @DELETE("user/cart/delete/{productId}")
    suspend fun removeCartItem(@Path("productId") productId: Int): Response<BaseResponse<CartData>>

    @GET("user/cart/check/{product_id}")
    suspend fun checkCartStatus(@Path("product_id") productId: Int): Response<CartCheckResponse>


    @GET("user/wishlist/allWishlist")
    suspend fun getWishlist(): Response<BaseResponse<WishlistDataContainer>>

    @POST("user/wishlist/toggle")
    suspend fun toggleWishlist(@Body request: Map<String, Int>): Response<WishlistResponse>

    @GET("user/wishlist/status/{product_id}")
    suspend fun checkWishlistStatus(@Path("product_id") productId: Int): Response<WishlistResponse>


    @POST("user/order")
    suspend fun placeOrder(
        @Body request: OrderRequest
    ): Response<BaseResponse<OrderCreationData>>

    @POST("user/order/verify-payment")
    suspend fun verifyPayment(
        @Body request: PaymentVerificationRequest
    ): Response<BaseResponse<Unit>>

    @GET("user/my-orders")
    suspend fun getOrders(): Response<BaseResponse<List<Order>>>

    @GET("user/order/status/{orderId}")
    suspend fun getOrderStatus(
        @Path("orderId") retailerOrderId: Int,
        @Query("productId") productId: Int
    ): Response<BaseResponse<OrderStatusResponse>>

    @POST("user/order/cancel")
    suspend fun cancelOrder(@Body body: Map<String, Int>): Response<BaseResponse<Unit>>

    @GET("user/stats")
    suspend fun getUserStats(): Response<BaseResponse<UserStats>>

    @POST("user/review")
    suspend fun submitReview(@Body request: ReviewRequest): Response<BaseResponse<Unit>>


    @GET("user/addresses")
    suspend fun getAddresses(): Response<BaseResponse<List<AddressEntity>>>

    @POST("user/address")
    suspend fun addAddress(@Body request: AddressRequest): Response<BaseResponse<Unit>>

    @PUT("user/address/{id}")
    suspend fun updateAddress(
        @Path("id") addressId: Int, @Body request: AddressRequest
    ): Response<BaseResponse<Unit>>

    @PATCH("user/address/default/{id}")
    suspend fun setDefaultAddress(@Path("id") addressId: Int): Response<BaseResponse<Unit>>

    @DELETE("user/address/{id}")
    suspend fun deleteAddress(@Path("id") addressId: Int): Response<BaseResponse<Unit>>


    @GET("user/{userId}/points")
    suspend fun getPointsHistory(
        @Path("userId") userId: Int
    ): Response<BaseResponse<PointsData>>

    @GET("user/coupons")
    suspend fun getAllCoupons(): Response<BaseResponse<List<Coupon>>>

}