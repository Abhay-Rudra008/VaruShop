package com.rudra.varushop.activity

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.varushop.R
import com.rudra.varushop.databinding.ActivityEditProfileBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.helper.NetworkModule
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.mvvm.UserViewModel
import com.yalantis.ucrop.UCrop
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File


@AndroidEntryPoint
class EditProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var prefManager: PrefManager
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { startCrop(it) }
    }

    private val cropImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val resultUri = UCrop.getOutput(result.data!!)
                resultUri?.let {
                    imageUri = it
                    binding.ivProfile.setImageURI(it)
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val cropError = UCrop.getError(result.data!!)
                Toast.makeText(this, "Crop Error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefManager = PrefManager(this)

        observeViewModel()

        viewModel.fetchProfile()

        binding.tvRemovePhoto.setOnClickListener { showRemovePhotoDialog() }
        binding.btnPickImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnUpdate.setOnClickListener { validateAndUpload() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.profileState.collect { state ->
                    when (state) {
                        is UserViewModel.ProfileUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }

                        is UserViewModel.ProfileUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.etName.setText(state.user.name)

                            prefManager.userName = state.user.name
                            prefManager.profilePic = state.user.profileImage
                            updateRemovePhotoVisibility()

                            loadProfileImage(state.user.profileImage)
                        }

                        is UserViewModel.ProfileUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                this@EditProfileActivity, state.message, Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {}
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isUpdating.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.btnUpdate.isEnabled = !isLoading
                    binding.tvRemovePhoto.isEnabled = !isLoading
                }
            }
        }

        lifecycleScope.launch {
            viewModel.updateStatus.collect { message ->
                if (message.contains("Success", ignoreCase = true)) {
                    prefManager.userName = binding.etName.text.toString().trim()
                    Toast.makeText(this@EditProfileActivity, "Profile Updated!", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                } else if (message.isNotEmpty()) {
                    Toast.makeText(this@EditProfileActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.removeStatus.collect { status ->
                if (status == "Success") {
                    prefManager.profilePic = null
                    imageUri = null

                    Glide.get(this@EditProfileActivity).clearMemory()
                    lifecycleScope.launch(Dispatchers.IO) {
                        Glide.get(this@EditProfileActivity).clearDiskCache()
                    }

                    binding.ivProfile.setImageResource(R.drawable.ic_person)
                    updateRemovePhotoVisibility()
                    Toast.makeText(
                        this@EditProfileActivity, "Photo Removed Successfully", Toast.LENGTH_SHORT
                    ).show()
                } else if (status.isNotEmpty()) {
                    Toast.makeText(this@EditProfileActivity, status, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadProfileImage(imagePath: String?) {
        if (!imagePath.isNullOrEmpty()) {
            val finalUrl = when {
                imagePath.startsWith("https://", ignoreCase = true) -> imagePath
                imagePath.startsWith("http://", ignoreCase = true) -> {
                    imagePath.replaceFirst("http://", "https://", ignoreCase = true)
                }

                else -> {
                    val cleanPath = imagePath.removePrefix("/")
                    "http://${NetworkModule.BASE_URL}/$cleanPath"
                }
            }

            Glide.with(this).load(finalUrl).placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person).circleCrop().into(binding.ivProfile)

        } else {
            binding.ivProfile.setImageResource(R.drawable.ic_person)
        }
    }

    private fun startCrop(uri: Uri) {
        val destinationFileName = "CroppedImage_${System.currentTimeMillis()}.jpg"
        val destinationUri = Uri.fromFile(File(cacheDir, destinationFileName))

        val options = UCrop.Options().apply {
            setCompressionFormat(Bitmap.CompressFormat.JPEG)
            setCompressionQuality(90)
            setHideBottomControls(false)
            setFreeStyleCropEnabled(false)
            setCircleDimmedLayer(true)
            setShowCropGrid(false)
            setToolbarColor(ContextCompat.getColor(this@EditProfileActivity, R.color.primary_brand))
            setToolbarWidgetColor(ContextCompat.getColor(this@EditProfileActivity, R.color.white))
            setActiveControlsWidgetColor(
                ContextCompat.getColor(
                    this@EditProfileActivity, R.color.primary_brand
                )
            )
        }

        val uCropIntent =
            UCrop.of(uri, destinationUri).withAspectRatio(1f, 1f).withMaxResultSize(1000, 1000)
                .withOptions(options).getIntent(this)

        cropImage.launch(uCropIntent)
    }

    private fun showRemovePhotoDialog() {
        MaterialAlertDialogBuilder(this).setTitle("Remove Photo")
            .setMessage("Are you sure you want to remove your profile picture?")
            .setPositiveButton("Remove") { _, _ -> viewModel.removeProfilePhoto() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun updateRemovePhotoVisibility() {
        binding.tvRemovePhoto.visibility =
            if (!prefManager.profilePic.isNullOrEmpty()) View.VISIBLE else View.GONE
    }

    private fun validateAndUpload() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etName.error = "Name cannot be empty"
            return
        }

        val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())

        var imagePart: MultipartBody.Part? = null
        imageUri?.let { uri ->
            val file = File(uri.path!!)
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            imagePart = MultipartBody.Part.createFormData("profile_image", file.name, requestFile)
        }

        viewModel.updateProfile(nameBody, imagePart)
    }
}