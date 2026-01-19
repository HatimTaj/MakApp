package com.hatim.makmanager.ui.dealer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.hatim.makmanager.R
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.repository.OrderRepository
import com.hatim.makmanager.databinding.FragmentMyOrdersBinding
import com.hatim.makmanager.ui.adapters.OrderAdapter
import kotlinx.coroutines.launch

class MyOrdersFragment : Fragment(R.layout.fragment_my_orders) {

    private lateinit var binding: FragmentMyOrdersBinding
    private val repository = OrderRepository()
    private val orderAdapter = OrderAdapter{ order ->
        val intent = android.content.Intent(context, com.hatim.makmanager.ui.admin.OrderDetailsActivity::class.java)
        intent.putExtra("ORDER_ID", order.id)
        intent.putExtra("IS_ADMIN", false) // Hide Approve buttons
        startActivity(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMyOrdersBinding.bind(view)

        binding.rvOrders.layoutManager = LinearLayoutManager(context)
        binding.rvOrders.adapter = orderAdapter

        loadOrders()

        binding.swipeRefresh.setOnRefreshListener {
            loadOrders()
        }
    }

    private fun loadOrders() {
        val uid = FirebaseAuth.getInstance().uid ?: return

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE

            when (val result = repository.getMyOrders(uid)) {
                is Resource.Success -> {
                    orderAdapter.submitList(result.data)
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false

                    if (result.data.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}