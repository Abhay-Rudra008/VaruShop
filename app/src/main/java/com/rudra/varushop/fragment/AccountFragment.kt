package com.rudra.varushop.fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.varushop.R
import com.rudra.varushop.activity.CouponActivity
import com.rudra.varushop.activity.EditProfileActivity
import com.rudra.varushop.activity.LoginActivity
import com.rudra.varushop.activity.MyOrderActivity
import com.rudra.varushop.activity.RewardPointActivity
import com.rudra.varushop.activity.SelectLangActivity
import com.rudra.varushop.activity.WishListActivity
import com.rudra.varushop.bottomsheet.SelectSavedAddressFragment
import com.rudra.varushop.bottomsheet.UpdatePasswordBottomSheet
import com.rudra.varushop.databinding.FragmentAccountBinding
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.mvvm.AccountViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefManager: PrefManager
    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefManager = PrefManager(requireContext())
        setupUserUI()
        setupMenuContent()
        setupClickListeners()
        observeViewModel()

        viewModel.fetchUserStats()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statsState.collect { state ->
                    when (state) {
                        is AccountViewModel.AccountUiState.Success -> {
                            binding.apply {
                                tvOrderCount.text = state.stats.orderCount.toString()
                                tvCouponCount.text = state.stats.couponCount.toString()
                                tvPointsCount.text = state.stats.pointsCount.toString()
                            }
                        }

                        is AccountViewModel.AccountUiState.Error -> {
                            binding.tvOrderCount.text = "0"
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupMenuContent() {
        binding.apply {
            menuOrders.menuTitle.text = getString(R.string.my_orders)
            menuOrders.menuIcon.setImageResource(R.drawable.ic_orders)

            menuWishlist.menuTitle.text = getString(R.string.wishlist)
            menuWishlist.menuIcon.setImageResource(R.drawable.ic_wishlist)

            menuCoupon.menuTitle.text = getString(R.string.coupons)
            menuCoupon.menuIcon.setImageResource(R.drawable.ic_coupon)

            menuPoint.menuTitle.text = getString(R.string.points)
            menuPoint.menuIcon.setImageResource(R.drawable.ic_point)

            menuProfile.menuTitle.text = getString(R.string.edit_profile)
            menuProfile.menuIcon.setImageResource(R.drawable.ic_person)

            menuChangePassword.menuTitle.text = getString(R.string.change_password)
            menuChangePassword.menuIcon.setImageResource(R.drawable.ic_lock)

            menuLanguage.menuTitle.text = getString(R.string.language)
            menuLanguage.menuIcon.setImageResource(R.drawable.ic_language)

            menuAddress.menuTitle.text = getString(R.string.saved_addresses)
            menuAddress.menuIcon.setImageResource(R.drawable.ic_my_location)

            menuSecurity.menuTitle.text = getString(R.string.security)
            menuSecurity.menuIcon.setImageResource(R.drawable.ic_security)

            menuPrivacy.menuTitle.text = getString(R.string.privacy_policy)
            menuPrivacy.menuIcon.setImageResource(R.drawable.ic_privacy)

            menuHelp.menuTitle.text = getString(R.string.help_center)
            menuHelp.menuIcon.setImageResource(R.drawable.ic_help)
        }
    }

    private fun toggleTheme() {
        val currentMode = prefManager.nightMode
        val newMode = if (currentMode == 2) 1 else 2
        prefManager.nightMode = newMode
        syncThemeIcon()
    }

    private fun syncThemeIcon() {
        val currentMode = prefManager.nightMode
        val isActuallyDark = if (currentMode == 0) {
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        } else {
            currentMode == 2
        }
        binding.btnThemeToggle.setImageResource(if (isActuallyDark) R.drawable.ic_day else R.drawable.ic_night)
    }

    private fun setupClickListeners() {
        binding.apply {
            menuOrders.root.setOnClickListener {
                startActivity(Intent(requireContext(), MyOrderActivity::class.java))
            }
            menuWishlist.root.setOnClickListener {
                startActivity(Intent(requireContext(), WishListActivity::class.java))
            }
            menuPoint.root.setOnClickListener {
                startActivity(Intent(requireContext(), RewardPointActivity::class.java))
            }
            menuCoupon.root.setOnClickListener {
                startActivity(Intent(requireContext(), CouponActivity::class.java))
            }

            menuAddress.root.setOnClickListener {
                val addressSheet = SelectSavedAddressFragment()
                addressSheet.setAddressListener { selectedAddress ->
                    Toast.makeText(
                        requireContext(), "Selected: ${selectedAddress.label}", Toast.LENGTH_SHORT
                    ).show()
                }
                addressSheet.show(parentFragmentManager, "SelectSavedAddressFragment")
            }

            menuLanguage.root.setOnClickListener {
                startActivity(Intent(requireContext(), SelectLangActivity::class.java).apply {
                    putExtra("FROM_SETTINGS", true)
                })
            }
            menuProfile.root.setOnClickListener {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }
            menuChangePassword.root.setOnClickListener {
                UpdatePasswordBottomSheet().show(parentFragmentManager, "UpdatePasswordBottomSheet")
            }

            menuSecurity.root.setOnClickListener {
                showCommonInfoDialog(
                    title = "Account Security",
                    message = "Your account safety is our priority.\n\n" + "• End-to-End Encryption: All transmissions use secure TLS/SSL data channels.\n" + "• Session Safety: Your authentication tokens expire automatically after 7 days of inactivity.\n" + "• Fraud Prevention: VaruShop will never ask for your password via email or SMS.",
                    iconRes = R.drawable.ic_security
                )
            }

            menuPrivacy.root.setOnClickListener {
                showCommonInfoDialog(
                    title = "Privacy Policy Summary",
                    message = "We value your trust. Here is a brief summary of how your data is handled:\n\n" + "1. Data Collection: We only store information essential for completing checkouts (Names, emails, and physical shipping profiles).\n\n" + "2. Zero Spam: We do not sell, rent, or trade your personal data matrices to third-party advertising brokers.",
                    iconRes = R.drawable.ic_privacy,
                    positiveText = "Accept"
                )
            }

            menuHelp.root.setOnClickListener {
                showCommonInfoDialog(
                    title = "Help & Support Desk",
                    message = "Encountering an issue with a transaction, delivery tracking, or active store coupon? Our customer care pipeline operates 24/7.\n\n" + "📧 Email Support:\nsupport@varushop.com\n\n" + "📞 Telephone Helpline:\n+91 99999 99999",
                    iconRes = R.drawable.ic_help,
                    positiveText = "Close Desk",
                    neutralText = "Email Now",
                    onNeutralClick = {
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:support@varushop.com".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, "Customer Support Inquiry")
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(), "No mail app installed", Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
            }

            btnLogout.setOnClickListener { showLogoutDialog() }
            btnThemeToggle.setOnClickListener { toggleTheme() }
        }
    }


    private fun showCommonInfoDialog(
        title: String,
        message: String,
        @DrawableRes iconRes: Int,
        positiveText: String = "Got It",
        neutralText: String? = null,
        onNeutralClick: (() -> Unit)? = null
    ) {
        val builder =
            MaterialAlertDialogBuilder(requireContext()).setTitle(title).setMessage(message)
                .setIcon(iconRes).setPositiveButton(positiveText, null)

        if (neutralText != null && onNeutralClick != null) {
            builder.setNeutralButton(neutralText) { dialog, _ ->
                onNeutralClick()
                dialog.dismiss()
            }
        }

        builder.show()
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext()).setTitle("Logout")
            .setMessage("Are you sure you want to sign out?").setPositiveButton("Logout") { _, _ ->
                prefManager.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
            }.setNegativeButton("Cancel", null).show()
    }

    @SuppressLint("SetTextI18n")
    private fun setupUserUI() {
        binding.tvUserName.text = prefManager.userName ?: "Welcome Guest"
        binding.tvUserEmail.text = prefManager.userEmail ?: "Sign in to your account"

        val imagePath = prefManager.profilePic
        if (!imagePath.isNullOrEmpty()) {
            val finalUrl = when {
                imagePath.startsWith("http", ignoreCase = true) -> imagePath

                imagePath.startsWith("/storage/") || imagePath.startsWith("/data/") -> imagePath

                else -> imagePath
            }

            Glide.with(this).load(finalUrl).placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person).circleCrop().into(binding.ivProfilePic)
        } else {
            binding.ivProfilePic.setImageResource(R.drawable.ic_person)
        }
        syncThemeIcon()

        try {
            val context = requireContext()
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersion = packageInfo.versionName
            binding.tvAppVersion.text = "App Version $currentVersion (Stable)"
        } catch (e: Exception) {
            binding.tvAppVersion.text = "App Version 1.0.0"
        }
    }

    override fun onResume() {
        super.onResume()
        setupUserUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}