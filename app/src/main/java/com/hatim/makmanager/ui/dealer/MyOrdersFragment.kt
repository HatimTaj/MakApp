package com.hatim.makmanager.ui.dealer

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hatim.makmanager.R
import com.hatim.makmanager.data.model.Order
import com.hatim.makmanager.databinding.FragmentMyOrdersBinding
import com.hatim.makmanager.ui.admin.OrderAdapter
import com.hatim.makmanager.ui.admin.OrderDetailsActivity
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyOrdersFragment : Fragment(R.layout.fragment_my_orders) {

    private lateinit var binding: FragmentMyOrdersBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentMyOrdersBinding.bind(view)

        binding.rvOrders.layoutManager = LinearLayoutManager(context)

        loadOrders()
    }

    private fun loadOrders() {
        val userId = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val snapshot = db.collection("orders")
                    .whereEqualTo("dealerId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                // FIX: Manually map the Document ID here too
                val orders = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }

                if (orders.isEmpty()) {
                    binding.tvNoOrders.visibility = View.VISIBLE
                } else {
                    binding.tvNoOrders.visibility = View.GONE

                    val adapter = OrderAdapter { order ->
                        val intent = Intent(requireContext(), OrderDetailsActivity::class.java)
                        intent.putExtra("ORDER_ID", order.id)
                        intent.putExtra("IS_ADMIN", false)
                        startActivity(intent)
                    }

                    binding.rvOrders.adapter = adapter
                    adapter.submitList(orders)
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            binding.progressBar.visibility = View.GONE
        }
    }
}