package com.rudra.varushop.helper

import android.view.View
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.rudra.varushop.R

fun View.showError(message: String, onRetry: () -> Unit) {
    val title = this.findViewById<TextView>(R.id.tvErrorMessage)
    val retryBtn = this.findViewById<MaterialButton>(R.id.btnRetry)
    val lottieAnim = this.findViewById<LottieAnimationView>(R.id.lottie_error)
    this.visibility = View.VISIBLE
    title?.text = message
    retryBtn?.setOnClickListener { onRetry() }
    lottieAnim?.playAnimation()
}

fun View.hideError() {
    this.visibility = View.GONE
    val lottieAnim = this.findViewById<LottieAnimationView>(R.id.lottie_error)
    lottieAnim?.pauseAnimation()
}