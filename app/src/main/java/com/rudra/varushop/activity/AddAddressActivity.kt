package com.rudra.varushop.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.permissionx.guolindev.PermissionX
import com.rudra.varushop.R
import com.rudra.varushop.databinding.ActivityAddAddressBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.modal.AddressEntity
import com.rudra.varushop.modal.AddressRequest
import com.rudra.varushop.mvvm.AddressViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@AndroidEntryPoint
class AddAddressActivity : BaseActivity() {

    private lateinit var binding: ActivityAddAddressBinding

    private val viewModel: AddressViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var editingAddressId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupListeners()
        observeViewModel()
        checkEditMode()
    }

    @SuppressLint("SetTextI18n")
    private fun checkEditMode() {
        if (intent.getBooleanExtra("EDIT_MODE", false)) {
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("ADDRESS_DATA", AddressEntity::class.java)
            } else {
                @Suppress("DEPRECATION") intent.getParcelableExtra<AddressEntity>("ADDRESS_DATA")
            }

            address?.let {
                editingAddressId = it.id
                binding.apply {
                    toolbar.title = "Update Address"
                    btnSaveAddress.text = "Update Address"

                    etFullName.setText(it.userName)
                    etPhone.setText(it.phone)

                    val fullAddr = it.fullAddress
                    val pin = fullAddr.substringAfterLast("- ").trim()
                    val remaining = fullAddr.substringBeforeLast(" -")
                    val parts = remaining.split(", ")

                    when (parts.size) {
                        3 -> {
                            etHouse.setText(parts[0])
                            etLandmark.setText(parts[1])
                            etCity.setText(parts[2])
                        }

                        2 -> {
                            etHouse.setText(parts[0])
                            etCity.setText(parts[1])
                        }
                    }
                    etPincode.setText(pin)

                    when (it.label) {
                        "HOME" -> chipHome.isChecked = true
                        "WORK" -> chipWork.isChecked = true
                    }
                }
            }
        }
    }

    private fun validateAndSave() {
        val name = binding.etFullName.text.toString().trim()
        val phoneInput = binding.etPhone.text.toString().trim()
        val pincode = binding.etPincode.text.toString().trim()
        val city = binding.etCity.text.toString().trim()
        val house = binding.etHouse.text.toString().trim()
        val landmark = binding.etLandmark.text.toString().trim()

        if (name.isEmpty() || phoneInput.isEmpty() || pincode.isEmpty() || city.isEmpty() || house.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val finalPhone = if (phoneInput.startsWith("+91")) phoneInput else "+91 $phoneInput"
        val fullAddress =
            if (landmark.isNotEmpty()) "$house, $landmark, $city - $pincode" else "$house, $city - $pincode"

        val label = when (binding.chipGroupAddressType.checkedChipId) {
            R.id.chipHome -> "HOME"
            R.id.chipWork -> "WORK"
            else -> "OTHER"
        }

        val request = AddressRequest(
            label = label, userName = name, fullAddress = fullAddress, phone = finalPhone
        )

        if (editingAddressId != null) {
            viewModel.updateAddress(editingAddressId!!, request)
        } else {
            viewModel.addAddress(request)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.addressSavedEvent.collect {
                        Toast.makeText(
                            this@AddAddressActivity,
                            "Address Saved Successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                }

                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.btnSaveAddress.isEnabled = !loading
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.error.collect { errorMessage ->
                        Toast.makeText(this@AddAddressActivity, errorMessage, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnSaveAddress.setOnClickListener { validateAndSave() }
        binding.btnLocateMe.setOnClickListener { handleLocationTask() }

        binding.etPincode.doAfterTextChanged { s ->
            if (s?.length == 6) {
                fetchCityFromPincode(s.toString())
            }
        }
    }

    private fun fetchCityFromPincode(pincode: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        binding.tilCity.isEndIconVisible = true
        binding.tilCity.setEndIconDrawable(R.drawable.ic_loading_spinner)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocationName(pincode, 1)
                withContext(Dispatchers.Main) {
                    binding.tilCity.isEndIconVisible = false
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        binding.etCity.setText(addr.locality ?: addr.subAdminArea ?: "")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { binding.tilCity.isEndIconVisible = false }
            }
        }
    }

    private fun handleLocationTask() {
        PermissionX.init(this).permissions(
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            ).onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "VaruShop uses location to make delivery easier for you.",
                    "OK",
                    "Cancel"
                )
            }.request { allGranted, _, _ ->
                if (allGranted) getCurrentLocation()
                else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        setLocateLoading(true)

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    reverseGeocode(location.latitude, location.longitude)
                } else {
                    setLocateLoading(false)
                    Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                setLocateLoading(false)
                Toast.makeText(this, "Location Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                withContext(Dispatchers.Main) {
                    setLocateLoading(false)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        binding.apply {
                            etPincode.setText(addr.postalCode ?: "")
                            etCity.setText(addr.locality ?: addr.subAdminArea ?: "")
                            etLandmark.setText(addr.subLocality ?: addr.thoroughfare ?: "")
                            if (!addr.featureName.isNullOrEmpty() && addr.featureName != addr.thoroughfare) {
                                etHouse.setText(addr.featureName)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { setLocateLoading(false) }
            }
        }
    }

    private fun setLocateLoading(isLoading: Boolean) {
        binding.btnLocateMe.apply {
            isEnabled = !isLoading
            text = if (isLoading) "Locating..." else "Use Current Location"
            icon = if (isLoading) null else ContextCompat.getDrawable(
                context, R.drawable.ic_my_location
            )
        }
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}