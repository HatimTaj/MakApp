package com.hatim.makmanager.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.model.Order
import com.hatim.makmanager.data.repository.OrderRepository
import com.hatim.makmanager.databinding.ActivityAdminAnalyticsBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class AdminAnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminAnalyticsBinding
    private val repository = OrderRepository()
    private var allApprovedOrders = listOf<Order>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvAnalytics.layoutManager = LinearLayoutManager(this)

        setupSpinner()
        loadData()

        binding.ivBack.setOnClickListener { finish() }
    }

    private fun setupSpinner() {
        val periods = arrayOf("All Time", "This Quarter", "This Year")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, periods)
        binding.spPeriod.adapter = adapter

        binding.spPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterData(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            when (val result = repository.getApprovedStats()) {
                is Resource.Success -> {
                    allApprovedOrders = result.data
                    filterData(0)
                }
                is Resource.Error -> {
                    // Handle error
                }
                else -> {} // FIX: Exhaustive
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun filterData(periodIndex: Int) {
        val now = Calendar.getInstance()
        val filteredOrders = when (periodIndex) {
            1 -> { // This Quarter
                val currentMonth = now.get(Calendar.MONTH)
                val quarterStartMonth = (currentMonth / 3) * 3
                val startOfQuarter = Calendar.getInstance().apply {
                    set(Calendar.MONTH, quarterStartMonth)
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                }
                allApprovedOrders.filter { it.timestamp.toDate().after(startOfQuarter.time) }
            }
            2 -> { // This Year
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                }
                allApprovedOrders.filter { it.timestamp.toDate().after(startOfYear.time) }
            }
            else -> allApprovedOrders
        }

        val dealerStats = filteredOrders.groupBy { it.dealerName }
            .mapValues { entry ->
                entry.value.sumOf { it.totalLitres }
            }
            .toList()
            .sortedByDescending { (_, litres) -> litres }

        val adapter = AnalyticsAdapter(dealerStats)
        binding.rvAnalytics.adapter = adapter
    }
}