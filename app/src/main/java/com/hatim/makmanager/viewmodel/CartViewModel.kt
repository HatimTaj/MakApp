package com.hatim.makmanager.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hatim.makmanager.data.model.CartItem
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.data.model.ProductVariant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CartViewModel : ViewModel() {

    private val _cartItems = MutableLiveData<MutableList<CartItem>>(mutableListOf())
    val cartItems: LiveData<MutableList<CartItem>> = _cartItems

    // Returns TRUE if added, FALSE if stock insufficient
    fun addToCart(product: Product, variant: ProductVariant, qty: Int): Boolean {
        // 1. Strict Stock Check
        if (qty > variant.stockCartons) {
            return false // Fail immediately
        }

        val currentList = _cartItems.value ?: mutableListOf()
        val existingIndex = currentList.indexOfFirst {
            it.productId == product.id && it.variantSize == variant.size
        }

        if (existingIndex != -1) {
            val existingItem = currentList[existingIndex]
            val newTotalQty = existingItem.cartonQuantity + qty

            // Check combined stock
            if (newTotalQty > variant.stockCartons) {
                return false
            }

            val updatedItem = existingItem.copy(cartonQuantity = newTotalQty)
            currentList[existingIndex] = updatedItem
        } else {
            val newItem = CartItem(
                productId = product.id,
                productName = product.name,
                variantSize = variant.size,
                cartonCapacity = variant.cartonCapacity,
                cartonQuantity = qty,
                priceAtOrder = variant.pricePerCarton
            )
            currentList.add(newItem)
        }
        _cartItems.value = currentList
        return true
    }

    fun updateItemQuantity(item: CartItem, newQty: Int) {
        val currentList = _cartItems.value ?: return
        val index = currentList.indexOf(item)
        if (index != -1) {
            if (newQty <= 0) {
                currentList.removeAt(index)
            } else {
                currentList[index] = item.copy(cartonQuantity = newQty)
            }
            _cartItems.value = currentList
        }
    }

    fun removeItem(item: CartItem) {
        val currentList = _cartItems.value ?: return
        currentList.remove(item)
        _cartItems.value = currentList
    }

    fun clearCart() {
        _cartItems.value = mutableListOf()
    }

    fun getTotalPrice(): Double = _cartItems.value?.sumOf { it.priceAtOrder * it.cartonQuantity } ?: 0.0

    fun getTotalLitres(): Double = _cartItems.value?.sumOf {
        calculateLitres(it.variantSize, it.cartonCapacity, it.cartonQuantity)
    } ?: 0.0

    private fun calculateLitres(size: String, cap: Int, qty: Int): Double {
        return try {
            val norm = size.lowercase()
            val numRegex = Regex("[0-9]+(\\.[0-9]+)?")
            val num = numRegex.find(norm)?.value?.toDouble() ?: 0.0
            val unitVol = if (norm.contains("m")) num / 1000.0 else num
            unitVol * cap * qty
        } catch (e: Exception) { 0.0 }
    }
    fun updateDealerProfile(name: String, address: String, city: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userMap = mapOf(
            "name" to name,
            "address" to address,
            "city" to city
        )
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update(userMap)
    }
}