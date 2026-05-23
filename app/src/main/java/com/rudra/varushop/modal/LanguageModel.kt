package com.rudra.varushop.modal

data class LanguageModel(
    val name: String,
    val code: String, // e.g., "en", "hi", "bn"
    val flag: String, // Emoji flag
    var isSelected: Boolean = false
)