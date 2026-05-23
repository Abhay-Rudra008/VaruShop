package com.rudra.varushop.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.rudra.varushop.adapter.LanguageAdapter
import com.rudra.varushop.databinding.ActivitySelectLangBinding
import com.rudra.varushop.helper.BaseActivity
import com.rudra.varushop.helper.PrefManager
import com.rudra.varushop.modal.LanguageModel

class SelectLangActivity : BaseActivity() {

    private lateinit var binding: ActivitySelectLangBinding
    private lateinit var prefManager: PrefManager
    private lateinit var langAdapter: LanguageAdapter
    private var selectedLangCode: String = "en"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize View Binding
        binding = ActivitySelectLangBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        prefManager = PrefManager(this)
        selectedLangCode = prefManager.langCode

        setupRecyclerView()

        binding.btnApply.setOnClickListener {
            prefManager.langCode = selectedLangCode
            prefManager.isLangSelected = true

            applyLanguage(selectedLangCode)

            val intent = Intent(this@SelectLangActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        // Initialize the adapter with a callback to update our local selectedLangCode
        langAdapter = LanguageAdapter { langCode ->
            selectedLangCode = langCode
        }

        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(this@SelectLangActivity)
            adapter = langAdapter
        }

        val languages = listOf(
            LanguageModel("English", "en", "🇺🇸", selectedLangCode == "en"),
            LanguageModel("Hindi", "hi", "🇮🇳", selectedLangCode == "hi"),
            LanguageModel("Bengali", "bn", "🇮🇳", selectedLangCode == "bn"),
            LanguageModel("Gujarati", "gu", "🇮🇳", selectedLangCode == "gu")
        )

        langAdapter.submitList(languages)
    }

    private fun applyLanguage(langCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}