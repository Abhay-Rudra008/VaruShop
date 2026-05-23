package com.rudra.varushop.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rudra.varushop.R
import com.rudra.varushop.databinding.ItemSavedAddressBinding
import com.rudra.varushop.modal.AddressEntity

class AddressAdapter(
    private val onAddressClick: (AddressEntity) -> Unit,
    private val onEditClick: (AddressEntity) -> Unit,
    private val onDeleteClick: (AddressEntity) -> Unit
) : ListAdapter<AddressEntity, AddressAdapter.AddressViewHolder>(AddressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemSavedAddressBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AddressViewHolder(private val binding: ItemSavedAddressBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(address: AddressEntity) {
            binding.apply {
                val isSelected = address.isSelected

                tvAddressLabel.text = address.label.uppercase()
                tvUserName.text = address.userName
                tvFullAddress.text = address.fullAddress
                tvPhone.text = address.phone

                rbSelected.isChecked = isSelected

                val context = root.context
                val color = if (isSelected) R.color.colorPrimary else R.color.colorOutlineVariant
                val stroke = if (isSelected) 4 else 2

                cardAddress.strokeColor = ContextCompat.getColor(context, color)
                cardAddress.strokeWidth = stroke
                btnEdit.setOnClickListener { onEditClick(address) }

                btnDelete.setOnClickListener { onDeleteClick(address) }

                root.setOnClickListener { onAddressClick(address) }
            }
        }
    }

    class AddressDiffCallback : DiffUtil.ItemCallback<AddressEntity>() {
        override fun areItemsTheSame(oldItem: AddressEntity, newItem: AddressEntity) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: AddressEntity, newItem: AddressEntity) =
            oldItem == newItem
    }
}