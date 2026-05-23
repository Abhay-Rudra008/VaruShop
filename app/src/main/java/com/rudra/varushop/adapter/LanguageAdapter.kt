package com.rudra.varushop.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rudra.varushop.databinding.ItemLanguageBinding
import com.rudra.varushop.modal.LanguageModel

class LanguageAdapter(
    private val onLangSelected: (String) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LangViewHolder>() {

    private var languages = listOf<LanguageModel>()
    private var selectedPosition = -1

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<LanguageModel>) {
        languages = newList
        selectedPosition = newList.indexOfFirst { it.isSelected }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LangViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LangViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LangViewHolder, position: Int) {
        val lang = languages[position]
        holder.bind(lang, position == selectedPosition)
    }

    override fun getItemCount() = languages.size

    inner class LangViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(lang: LanguageModel, isSelected: Boolean) {
            binding.tvLangName.text = lang.name
            binding.tvFlag.text = lang.flag
            binding.rbSelected.isChecked = isSelected

            binding.root.isChecked = isSelected

            binding.root.setOnClickListener {
                if (selectedPosition != adapterPosition) {
                    val oldPos = selectedPosition
                    selectedPosition = adapterPosition

                    notifyItemChanged(oldPos)
                    notifyItemChanged(selectedPosition)

                    onLangSelected(lang.code)
                }
            }
        }
    }
}