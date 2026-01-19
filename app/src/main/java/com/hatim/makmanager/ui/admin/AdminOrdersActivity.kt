package com.hatim.makmanager.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.repository.OrderRepository
import com.hatim.makmanager.databinding.ActivityAdminOrdersBinding
import com.hatim.makmanager.ui.adapters.OrderAdapter
import kotlinx.coroutines.launch

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminOrdersBinding
    private val repository = OrderRepository()
    private val orderAdapter = OrderAdapter{
        order ->
    val intent = android.content.Intent(this, OrderDetailsActivity::class.java)
    intent.putExtra("ORDER_ID", order.id)
    intent.putExtra("IS_ADMIN", true) // Enable Approve buttons
    startActivity(intent)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvAllOrders.layoutManager = LinearLayoutManager(this)
        binding.rvAllOrders.adapter = orderAdapter

        // Back Button
        binding.ivBack.setOnClickListener { finish() }

        loadAllOrders()

        binding.swipeRefresh.setOnRefreshListener {
            loadAllOrders()
        }
    }

    private fun loadAllOrders() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            when (val result = repository.getAllOrders()) {
                is Resource.Success -> {
                    orderAdapter.submitList(result.data)
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    binding.tvEmpty.visibility = if (result.data.isEmpty()) View.VISIBLE else View.GONE
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(this@AdminOrdersActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}