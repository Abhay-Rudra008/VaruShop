package com.rudra.varushop.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.google.android.material.tabs.TabLayoutMediator
import com.rudra.varushop.adapter.ZoomImageAdapter
import com.rudra.varushop.databinding.ActivityFullScreenImageBinding
import com.rudra.varushop.helper.BaseActivity
import kotlin.math.abs

class FullScreenImageActivity : BaseActivity() {

    private lateinit var binding: ActivityFullScreenImageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setupSwipeToDismiss()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        val imageUrls = intent.getStringArrayListExtra("IMAGES") ?: arrayListOf()
        val startIndex = intent.getIntExtra("START_INDEX", 0)

        val adapter = ZoomImageAdapter(imageUrls)
        binding.viewPagerFullScreen.adapter = adapter

        binding.viewPagerFullScreen.setCurrentItem(startIndex, false)

        TabLayoutMediator(binding.tabIndicator, binding.viewPagerFullScreen) { _, _ -> }.attach()

        binding.btnClose.setOnClickListener { finish() }

        binding.btnShare.setOnClickListener {
            shareProductLink(imageUrls[binding.viewPagerFullScreen.currentItem])
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipeToDismiss() {
        val gestureDetector =
            GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    vx: Float,
                    vy: Float
                ): Boolean {
                    val distanceY = e2.y - (e1?.y ?: 0f)
                    if (distanceY > 150 && abs(vy) > 100) {
                        finish()
                        overridePendingTransition(0, android.R.anim.fade_out)
                        return true
                    }
                    return false
                }
            })

        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }
    }

    private fun shareProductLink(currentImageUrl: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out this product image: $currentImageUrl")
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }
}