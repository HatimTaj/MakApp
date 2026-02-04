package com.hatim.makmanager.ui.admin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.AlertDialog
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.hatim.makmanager.R // Import your R file
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

        // Create Channel for Notifications
        createNotificationChannel()

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

        binding.cardAnalytics.setOnClickListener {
            startActivity(Intent(this, AdminAnalyticsActivity::class.java))
        }

        binding.cardLedger.setOnClickListener {
            startActivity(Intent(this, UserListActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = productAdapter
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            // 1. Inventory & Low Stock Check
            when (val result = productRepository.getProducts()) {
                is Resource.Success -> {
                    productAdapter.submitList(result.data)
                    binding.tvOrderCount.text = "${result.data.size}"

                    // --- NEW: LOW STOCK LOGIC ---
                    checkLowStock(result.data)
                }
                is Resource.Error -> {
                    Toast.makeText(this@AdminDashboardActivity, result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }

            // 2. Sales (Approved Only)
            when (val orderResult = orderRepository.getApprovedStats()) {
                is Resource.Success -> {
                    val totalLitresSold = orderResult.data.sumOf { it.totalLitres }
                    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
                    binding.tvTotalLitres.text = String.format("%.2f L", totalLitresSold)
                }
                is Resource.Error -> {
                    binding.tvTotalLitres.text = "Error"
                }
                else -> {}
            }
        }
    }

    private fun checkLowStock(products: List<Product>) {
        var lowStockCount = 0

        // Loop through all products and their variants
        for (product in products) {
            for (variant in product.variants) {
                if (variant.stockCartons < 2) { // Alert Limit < 2
                    lowStockCount++
                }
            }
        }

        if (lowStockCount > 0) {
            // Show Visual Card
            binding.cardLowStock.visibility = android.view.View.VISIBLE
            binding.tvLowStockMsg.text = "⚠️ Alert: $lowStockCount items have Low Stock (<2)"

            // Trigger Notification
            sendLowStockNotification(lowStockCount)
        } else {
            binding.cardLowStock.visibility = android.view.View.GONE
        }
    }

    private fun sendLowStockNotification(count: Int) {
        // Permission check for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
                return
            }
        }

        val builder = NotificationCompat.Builder(this, "low_stock_channel")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Low Stock Alert")
            .setContentText("Attention: $count items are running low on stock.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(1, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Stock Alerts"
            val descriptionText = "Notifications for low inventory"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("low_stock_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // ... (Keep existing showOptionsDialog, openEditDialog, confirmDelete, uploadProductToFirebase) ...

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