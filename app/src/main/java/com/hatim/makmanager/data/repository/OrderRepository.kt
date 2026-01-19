package com.hatim.makmanager.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.model.Order
import kotlinx.coroutines.tasks.await

class OrderRepository {
    private val db = FirebaseFirestore.getInstance()

    // 1. For Dealer: Get my orders
    suspend fun getMyOrders(dealerId: String): Resource<List<Order>> {
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("dealerId", dealerId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = snapshot.toObjects(Order::class.java)
            val ordersWithIds = snapshot.documents.mapIndexed { index, doc ->
                orders[index].copy(id = doc.id)
            }
            Resource.Success(ordersWithIds)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch orders")
        }
    }

    // 2. For Admin List: Get ALL orders (Necessary for the list view)
    suspend fun getAllOrders(): Resource<List<Order>> {
        return try {
            val snapshot = db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = snapshot.toObjects(Order::class.java)
            val ordersWithIds = snapshot.documents.mapIndexed { index, doc ->
                orders[index].copy(id = doc.id)
            }
            Resource.Success(ordersWithIds)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch all orders")
        }
    }

    // 3. NEW OPTIMIZED FUNCTION: For Dashboard Analytics
    // Only fetches APPROVED orders. Saves reads by ignoring Rejected/Pending.
    suspend fun getApprovedStats(): Resource<List<Order>> {
        return try {
            val snapshot = db.collection("orders")
                .whereEqualTo("status", "APPROVED")
                .get()
                .await()

            val orders = snapshot.toObjects(Order::class.java)
            Resource.Success(orders)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to fetch stats")
        }
    }
}