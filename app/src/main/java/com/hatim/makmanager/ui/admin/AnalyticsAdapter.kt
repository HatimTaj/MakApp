package com.hatim.makmanager.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hatim.makmanager.databinding.ItemAnalyticsBinding

class AnalyticsAdapter(private val data: List<Pair<String, Double>>) :
    RecyclerView.Adapter<AnalyticsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAnalyticsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAnalyticsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, litres) = data[position]
        holder.binding.tvDealerName.text = name

        // FIX: Display 2 decimal places (e.g., 12.50 L)
        holder.binding.tvLitres.text = String.format("%.2f L", litres)
    }

    override fun getItemCount() = data.size
}