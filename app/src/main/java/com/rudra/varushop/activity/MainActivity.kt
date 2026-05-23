package com.rudra.varushop.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.rudra.varushop.R
import com.rudra.varushop.databinding.ActivityMainBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.helper.PrefManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (intent.getStringExtra("navigate_to") == "home") {
                navigateToHome()
            }
            v.updatePadding(top = systemBars.top)

            binding.bottomNavigation.updatePadding(bottom = systemBars.bottom)

            insets
        }

        setupNavigation()

        handleNavigationIntent(intent)
    }

    fun navigateToHome() {
        binding.bottomNavigation.selectedItemId = R.id.nav_home

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        handleNavigationIntent(intent)
    }

    private fun handleNavigationIntent(intent: Intent?) {
        val shouldGoToCart = intent?.getBooleanExtra("NAVIGATE_TO_CART", false) ?: false

        if (shouldGoToCart) {
            binding.bottomNavigation.selectedItemId = R.id.nav_cart
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_home, R.id.nav_category, R.id.nav_cart, R.id.nav_account -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }

                else -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }

    // Language Support
    override fun attachBaseContext(newBase: Context) {
        val manager = PrefManager(newBase)
        val locale = Locale(manager.langCode)
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }
}