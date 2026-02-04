package com.hatim.makmanager.ui.admin

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.R
import com.hatim.makmanager.data.model.CartItem
import com.hatim.makmanager.data.model.Order
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.databinding.ActivityOrderDetailsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class OrderDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOrderDetailsBinding
    private val db = FirebaseFirestore.getInstance()
    private var orderId: String = ""
    private var isUserAdmin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        orderId = intent.getStringExtra("ORDER_ID") ?: ""
        isUserAdmin = intent.getBooleanExtra("IS_ADMIN", false)

        if (orderId.isEmpty()) { finish(); return }

        binding.toolbar.setNavigationOnClickListener { finish() }

        if (isUserAdmin) {
            binding.llAdminActions.visibility = View.VISIBLE
            binding.btnApprove.setOnClickListener { approveOrderWithStockDeduction() }
            binding.btnReject.setOnClickListener { updateOrderStatus("REJECTED") }
        } else {
            binding.llAdminActions.visibility = View.GONE
        }

        loadOrderDetails()
    }

    private fun loadOrderDetails() {
        lifecycleScope.launch {
            try {
                val snapshot = db.collection("orders").document(orderId).get().await()
                val order = snapshot.toObject(Order::class.java)
                if (order != null) {
                    displayData(order)
                    fetchDealerDetails(order.dealerId)
                }
            } catch (e: Exception) {
                Toast.makeText(this@OrderDetailsActivity, "Load Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchDealerDetails(dealerId: String) {
        lifecycleScope.launch {
            try {
                val doc = db.collection("users").document(dealerId).get().await()
                val address = doc.getString("address") ?: ""
                val city = doc.getString("city") ?: ""

                if (address.isNotEmpty() || city.isNotEmpty()) {
                    val currentText = binding.tvDealerInfo.text.toString()
                    binding.tvDealerInfo.text = "$currentText\n$address, $city"
                }
            } catch (e: Exception) { }
        }
    }

    private fun displayData(order: Order) {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        binding.tvOrderId.text = "Order #${order.id.takeLast(6).uppercase()}"
        binding.tvDate.text = dateFormat.format(order.timestamp.toDate())
        binding.tvDealerInfo.text = "${order.dealerName}\n${order.dealerPhone}"

        binding.tvTotalAmount.text = currencyFormat.format(order.totalPrice)
        binding.tvTotalLitres.text = "Total Volume: ${String.format("%.2f", order.totalLitres)} L"

        binding.tvStatus.text = order.status
        when (order.status) {
            "APPROVED" -> {
                binding.tvStatus.setTextColor(Color.parseColor("#2E7D32"))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_approved)
                binding.llAdminActions.visibility = View.GONE
            }
            "REJECTED" -> {
                binding.tvStatus.setTextColor(Color.parseColor("#C62828"))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_rejected)
                binding.llAdminActions.visibility = View.GONE
            }
            else -> {
                binding.tvStatus.setTextColor(Color.parseColor("#EF6C00"))
                binding.tvStatus.setBackgroundResource(R.drawable.bg_status_pending)
            }
        }

        binding.llOrderItems.removeAllViews()
        for (item in order.items) {
            addItemView(item, currencyFormat)
        }
    }

    private fun addItemView(item: CartItem, format: NumberFormat) {
        val itemView = View.inflate(this, android.R.layout.simple_list_item_2, null)
        val text1 = itemView.findViewById<TextView>(android.R.id.text1)
        val text2 = itemView.findViewById<TextView>(android.R.id.text2)

        text1.text = "${item.productName} (${item.variantSize})"
        text1.textSize = 16f

        // --- FIX: USE DYNAMIC COLORS (White in Dark Mode, Black in Light Mode) ---
        text1.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurface))

        val totalItemPrice = item.priceAtOrder * item.cartonQuantity
        text2.text = "${item.cartonQuantity} Cartons x ${format.format(item.priceAtOrder)} = ${format.format(totalItemPrice)}"

        // Use a variant color (Grey in Light, Light Grey in Dark)
        text2.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnSurfaceVariant))

        itemView.setPadding(0, 16, 0, 16)
        binding.llOrderItems.addView(itemView)
    }

    // Helper to get correct color based on Theme (Light/Dark)
    @ColorInt
    private fun Context.getColorFromAttr(@AttrRes attrColor: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrColor, typedValue, true)
        return typedValue.data
    }

    private fun approveOrderWithStockDeduction() {
        binding.btnApprove.isEnabled = false
        binding.btnApprove.text = "Processing..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                db.runTransaction { transaction ->
                    val orderRef = db.collection("orders").document(orderId)
                    val orderSnapshot = transaction.get(orderRef)
                    val order = orderSnapshot.toObject(Order::class.java)
                        ?: throw Exception("Order not found")

                    if (order.status != "PENDING") throw Exception("Order already processed")

                    val dealerRef = db.collection("users").document(order.dealerId)
                    val dealerSnapshot = transaction.get(dealerRef)
                    val currentBalance = dealerSnapshot.getDouble("currentBalance") ?: 0.0

                    // Read Products
                    val productMap = mutableMapOf<String, Product>()
                    val productRefs = mutableMapOf<String, com.google.firebase.firestore.DocumentReference>()

                    for (item in order.items) {
                        val ref = db.collection("products").document(item.productId)
                        val snap = transaction.get(ref)
                        val prod = snap.toObject(Product::class.java)
                        if (prod != null) {
                            productMap[item.productId] = prod
                            productRefs[item.productId] = ref
                        }
                    }

                    // Write Updates
                    for (item in order.items) {
                        val product = productMap[item.productId] ?: continue
                        val ref = productRefs[item.productId] ?: continue

                        val updatedVariants = product.variants.map { variant ->
                            if (variant.size == item.variantSize) {
                                val newStock = variant.stockCartons - item.cartonQuantity
                                if (newStock < 0) throw Exception("Insufficient stock: ${product.name} ${variant.size}")
                                variant.copy(stockCartons = newStock)
                            } else {
                                variant
                            }
                        }
                        transaction.update(ref, "variants", updatedVariants)
                    }

                    transaction.update(dealerRef, "currentBalance", currentBalance + order.totalPrice)
                    transaction.update(orderRef, "status", "APPROVED")

                }.await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderDetailsActivity, "Approved & Balance Updated!", Toast.LENGTH_LONG).show()
                    loadOrderDetails()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OrderDetailsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.btnApprove.isEnabled = true
                    binding.btnApprove.text = "Approve"
                }
            }
        }
    }

    private fun updateOrderStatus(newStatus: String) {
        lifecycleScope.launch {
            try {
                db.collection("orders").document(orderId).update("status", newStatus).await()
                loadOrderDetails()
            } catch (e: Exception) {
                Toast.makeText(this@OrderDetailsActivity, "Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}