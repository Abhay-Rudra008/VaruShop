package com.rudra.varushop.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rudra.varushop.databinding.ActivityLoginBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.modal.user.AuthData
import com.rudra.varushop.mvvm.LoginViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class LoginActivity : BaseActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefManager: PrefManager

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        prefManager = PrefManager(this)

        setupWindowInsets()
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.tvSignupLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (validateLogin(email, pass)) {
                viewModel.login(email, pass)
            }
        }
    }

    private fun validateLogin(email: String, pass: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email is required"
            return false
        }
        if (pass.isEmpty()) {
            binding.etPassword.error = "Password is required"
            return false
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginViewModel.LoginState.Loading -> {
                            binding.btnLogin.isEnabled = false
                            binding.btnLogin.text = "Authenticating..."
                        }

                        is LoginViewModel.LoginState.Success -> {
                            handleLoginSuccess(state.data)
                        }

                        is LoginViewModel.LoginState.Error -> {
                            binding.btnLogin.isEnabled = true
                            binding.btnLogin.text = "Login"
                            showErrorDialog(state.message)
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun handleLoginSuccess(auth: AuthData) {
        val user = auth.user

        prefManager.apply {
            token = auth.token
            userId = user.id
            userName = user.name
            userEmail = user.email
            userRole = user.role
            profilePic = user.profileImage
            isLoggedIn = true
        }

        Toast.makeText(this, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}