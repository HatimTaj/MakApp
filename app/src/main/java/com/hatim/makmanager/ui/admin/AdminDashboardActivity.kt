package com.hatim.makmanager.ui.admin

import android.content.Intent
import android.os.Bundle
import android.app.AlertDialog
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.data.repository.ProductRepository
import com.hatim.makmanager.data.repository.OrderRepository
import com.hatim.makmanager.databinding.ActivityAdminDashboardBinding
import com.hatim.makmanager.ui.adapters.ProductAdapter
import com.hatim.makmanager.ui.auth.LoginActivity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val productRepository = ProductRepository()
    private val orderRepository = OrderRepository()

    private val productAdapter = ProductAdapter(
        onAddToCartClick = { _, _, _ -> },
        onItemClick = { product -> showOptionsDialog(product) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadDashboardData()

        binding.fabAddProduct.setOnClickListener {
            AddProductDialog { newProduct ->
                uploadProductToFirebase(newProduct)
            }.show(supportFragmentManager, "AddProduct")
        }

        binding.ivLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.cardOrders.setOnClickListener {
            startActivity(Intent(this, AdminOrdersActivity::class.java))
        }

        // Analytics Button
        binding.cardAnalytics.setOnClickListener {
            startActivity(Intent(this, AdminAnalyticsActivity::class.java))
        }
        binding.cardLedger.setOnClickListener {
            // Ensure UserListActivity is registered in Manifest
            startActivity(Intent(this, UserListActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = productAdapter
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            // 1. Inventory
            when (val result = productRepository.getProducts()) {
                is Resource.Success -> {
                    productAdapter.submitList(result.data)
                    binding.tvOrderCount.text = "${result.data.size}"
                }
                is Resource.Error -> {
                    Toast.makeText(this@AdminDashboardActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }

            // 2. Sales (Approved Only - Optimized for Free Tier)
            when (val orderResult = orderRepository.getApprovedStats()) {
                is Resource.Success -> {
                    val totalLitresSold = orderResult.data.sumOf { it.totalLitres }
                    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

                    // --- FIX 2: Format to 2 decimal places ---
                    binding.tvTotalLitres.text = String.format("%.2f L", totalLitresSold)
                }
                is Resource.Error -> {
                    binding.tvTotalLitres.text = "Error"
                }
                else -> {}
            }
        }
    }

    private fun showOptionsDialog(product: Product) {
        val options = arrayOf("Edit Product", "Delete Product", "Cancel")
        AlertDialog.Builder(this)
            .setTitle("Manage ${product.name}")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openEditDialog(product)
                    1 -> confirmDelete(product)
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun openEditDialog(product: Product) {
        EditProductDialog(product) { updatedProduct ->
            lifecycleScope.launch {
                productRepository.updateProduct(updatedProduct)
                loadDashboardData()
            }
        }.show(supportFragmentManager, "EditProduct")
    }

    private fun confirmDelete(product: Product) {
        lifecycleScope.launch {
            productRepository.deleteProduct(product.id)
            loadDashboardData()
        }
    }

    private fun uploadProductToFirebase(product: Product) {
        lifecycleScope.launch {
            productRepository.addProduct(product)
            loadDashboardData()
        }
    }
}