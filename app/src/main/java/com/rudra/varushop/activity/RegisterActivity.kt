package com.rudra.varushop.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.permissionx.guolindev.PermissionX
import com.rudra.varushop.databinding.ActivityRegisterBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.helper.FileUtil
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.mvvm.RegisterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody


@AndroidEntryPoint
class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var prefManager: PrefManager
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            binding.ivRegisterProfile.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)

        setupWindowInsets()
        setupListeners()
        observeViewModel()

    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnPickImage.setOnClickListener {
            handleImagePermission()
        }

        binding.btnRegister.setOnClickListener {
            validateAndRegister()
        }

        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }


    }

    private fun handleImagePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        PermissionX.init(this).permissions(permissions)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "VaruShop needs access to your gallery to upload a profile picture.",
                    "OK",
                    "Cancel"
                )
            }.request { allGranted, _, _ ->
                if (allGranted) {
                    pickImage.launch("image/*")
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun validateAndRegister() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()

        if (name.isEmpty()) {
            binding.tilName.error = "Name required"; return
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email required"; return
        }
        if (pass.length < 6) {
            binding.tilPassword.error = "Min 6 characters"; return
        }

        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val emailBody = email.toRequestBody("text/plain".toMediaTypeOrNull())
        val passBody = pass.toRequestBody("text/plain".toMediaTypeOrNull())

        var imagePart: MultipartBody.Part? = null
        imageUri?.let { uri ->
            val file = FileUtil.getFile(this, uri)
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            imagePart = MultipartBody.Part.createFormData("profile_image", file.name, requestFile)
        }

        viewModel.register(nameBody, emailBody, passBody, imagePart)
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registrationState.collect { state ->
                    when (state) {
                        is RegisterViewModel.UiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnRegister.isEnabled = false
                        }

                        is RegisterViewModel.UiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            val auth = state.authData.userId

                            prefManager.apply {
                                token = auth.token
                                userId = auth.user.id
                                userName = auth.user.name
                                userEmail = auth.user.email
                                profilePic = auth.user.profileImage
                                isLoggedIn = true
                            }

                            startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                            finish()
                        }

                        is RegisterViewModel.UiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnRegister.isEnabled = true
                            showErrorDialog(state.message)
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}