package com.hatim.makmanager.data.model

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val dealerId: String = "",
    val dealerName: String = "",
    val dealerPhone: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    // Status: PENDING, APPROVED, REJECTED
    val status: String = "PENDING",
    val items: List<CartItem> = emptyList(),
    val totalPrice: Double = 0.0,
    val totalLitres: Double = 0.0
)

data class CartItem(
    val productId: String = "",
    val productName: String = "",
    val variantSize: String = "",      // e.g., "1L"
    val cartonCapacity: Int = 1,       // e.g., 20
    val cartonQuantity: Int = 0,       // e.g., 5
    val priceAtOrder: Double = 0.0     // e.g., 4000.0
)