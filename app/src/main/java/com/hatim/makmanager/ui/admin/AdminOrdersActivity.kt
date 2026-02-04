package com.hatim.makmanager.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hatim.makmanager.data.model.Order
import com.hatim.makmanager.databinding.ActivityAdminOrdersBinding
import com.hatim.makmanager.ui.admin.OrderAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminOrdersBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)

        loadOrders()
    }

    private fun loadOrders() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val snapshot = db.collection("orders")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                // FIX: Manually map the Document ID to the Order object
                val orders = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }

                val adapter = OrderAdapter { order ->
                    val intent = Intent(this@AdminOrdersActivity, OrderDetailsActivity::class.java)
                    intent.putExtra("ORDER_ID", order.id) // Now this ID is not empty
                    intent.putExtra("IS_ADMIN", true)
                    startActivity(intent)
                }

                binding.rvOrders.adapter = adapter
                adapter.submitList(orders)

            } catch (e: Exception) {
                Toast.makeText(this@AdminOrdersActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        loadOrders()
    }
}