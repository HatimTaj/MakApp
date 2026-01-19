package com.hatim.makmanager.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hatim.makmanager.databinding.ItemAnalyticsBinding
import java.text.NumberFormat
import java.util.Locale

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

        val format = NumberFormat.getNumberInstance(Locale.US)
        holder.binding.tvLitres.text = "${format.format(litres.toInt())} L"
    }

    override fun getItemCount() = data.size
}