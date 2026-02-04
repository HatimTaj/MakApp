package com.hatim.makmanager.ui.admin // Ensure this package is correct

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hatim.makmanager.R
import com.hatim.makmanager.data.model.Order
import com.hatim.makmanager.databinding.ItemOrderBinding // This is generated from item_order.xml
import java.text.SimpleDateFormat
import java.util.Locale

class OrderAdapter(
    private val onClick: (Order) -> Unit
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

            // These IDs must match item_order.xml
            binding.tvDealerName.text = order.dealerName
            binding.tvDate.text = dateFormat.format(order.timestamp.toDate())

            // This line caused the error before because the ID was missing in XML
            binding.tvDetails.text = String.format("%.2f Litres  |  ₹%.0f", order.totalLitres, order.totalPrice)

            binding.tvStatus.text = order.status
            when (order.status) {
                "APPROVED" -> {
                    binding.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_approved)
                }
                "REJECTED" -> {
                    binding.tvStatus.setTextColor(Color.parseColor("#C62828"))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_rejected)
                }
                else -> {
                    binding.tvStatus.setTextColor(Color.parseColor("#EF6C00"))
                    binding.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
                }
            }

            binding.root.setOnClickListener { onClick(order) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
    }
}