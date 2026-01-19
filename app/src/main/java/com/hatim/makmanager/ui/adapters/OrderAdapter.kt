package com.hatim.makmanager.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hatim.makmanager.data.model.Order
import com.hatim.makmanager.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.text.NumberFormat

class OrderAdapter(
    private val onOrderClick: (Order) -> Unit)
    : ListAdapter<Order, OrderAdapter.OrderViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(order: Order) {
            // 1. Dealer Name / Order ID
            binding.tvDealerName.text = if(order.dealerName.isNotEmpty()) order.dealerName else "Order #${order.id.takeLast(5)}"

            // 2. Date
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            binding.tvDate.text = sdf.format(order.timestamp.toDate())

            // 3. Totals
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            binding.tvTotal.text = format.format(order.totalPrice)
            binding.tvItemCount.text = "${order.items.size} Items (${order.totalLitres.toInt()} L)"

            // 4. Status Coloring
            binding.tvStatus.text = order.status
            when (order.status) {
                "APPROVED" -> {
                    binding.tvStatus.setTextColor(Color.parseColor("#2E7D32")) // Green
                    binding.tvStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
                }
                "REJECTED" -> {
                    binding.tvStatus.setTextColor(Color.parseColor("#C62828")) // Red
                    binding.tvStatus.setBackgroundColor(Color.parseColor("#FFEBEE"))
                }
                else -> { // PENDING
                    binding.tvStatus.setTextColor(Color.parseColor("#EF6C00")) // Orange
                    binding.tvStatus.setBackgroundColor(Color.parseColor("#FFF3E0"))
                }
            }
            binding.root.setOnClickListener {
                onOrderClick(order)
            }
        }
    }
    class DiffCallback : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Order, newItem: Order) = oldItem == newItem
    }
}