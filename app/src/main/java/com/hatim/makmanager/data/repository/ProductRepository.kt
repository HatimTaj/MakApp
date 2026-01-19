package com.hatim.makmanager.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.data.model.ProductVariant
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
import java.io.InputStreamReader

class ProductRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getProducts(): Resource<List<Product>> {
        return try {
            val snapshot = db.collection("products").get().await()
            val products = snapshot.toObjects(Product::class.java)
            // Add IDs
            val productsWithIds = snapshot.documents.mapIndexed { index, doc ->
                products[index].copy(id = doc.id)
            }
            Resource.Success(productsWithIds)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error")
        }
    }

    suspend fun addProduct(product: Product): Resource<Boolean> {
        return try {
            db.collection("products").add(product).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error")
        }
    }

    suspend fun updateProduct(product: Product): Resource<Boolean> {
        return try {
            db.collection("products").document(product.id).set(product).await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Update failed")
        }
    }

    suspend fun deleteProduct(productId: String): Resource<Boolean> {
        return try {
            db.collection("products").document(productId).delete().await()
            Resource.Success(true)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Delete failed")
        }
    }

    // --- NEW: BULK UPLOAD ---
    suspend fun bulkUpdateFromCsv(context: Context, uri: Uri): Resource<String> {
        return try {
            // 1. Fetch ALL current products first (to match against)
            val currentProductsResult = getProducts()
            if (currentProductsResult !is Resource.Success) return Resource.Error("Failed to fetch products")
            val productList = currentProductsResult.data.toMutableList()

            var updatedCount = 0
            val contentResolver = context.contentResolver

            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        val tokens = parseCsvLine(line)
                        // Mapping: A=0(Name), B=1(Size), C=2(Cap), F=5(Rate), G=6(MRP)
                        if (tokens.size >= 7) {
                            val csvName = tokens[0].trim()
                            val csvSize = tokens[1].trim()
                            val csvCapStr = tokens[2].trim()
                            val csvRateStr = tokens[5].trim()
                            val csvMrpStr = tokens[6].trim()

                            // Basic validation to skip headers
                            if (csvRateStr.toDoubleOrNull() != null) {
                                val csvCap = csvCapStr.toDoubleOrNull()?.toInt() ?: 1
                                val csvRate = csvRateStr.toDoubleOrNull() ?: 0.0
                                val csvMrp = csvMrpStr.toDoubleOrNull() ?: 0.0

                                // Find matching product in DB
                                val productIndex = productList.indexOfFirst {
                                    it.name.equals(csvName, ignoreCase = true)
                                }

                                if (productIndex != -1) {
                                    val product = productList[productIndex]

                                    // Update the specific variant
                                    val updatedVariants = product.variants.map { variant ->
                                        // Match Size AND Capacity to be safe
                                        if (variant.size.equals(csvSize, ignoreCase = true) &&
                                            variant.cartonCapacity == csvCap) {

                                            // Calculate Carton Price (Unit Price * Capacity)
                                            val newCartonPrice = csvRate * csvCap

                                            updatedCount++
                                            variant.copy(
                                                pricePerCarton = newCartonPrice,
                                                mrp = csvMrp
                                            )
                                        } else {
                                            variant
                                        }
                                    }

                                    // Save changes locally in list
                                    productList[productIndex] = product.copy(variants = updatedVariants)
                                }
                            }
                        }
                        line = reader.readLine()
                    }
                }
            }

            // 2. Batch Write Updates back to Firebase
            // We loop through the list and update only those that changed.
            // For simplicity and safety in this example, we iterate and update.
            // In a real huge app, use WriteBatch.
            val batch = db.batch()
            productList.forEach { product ->
                val ref = db.collection("products").document(product.id)
                batch.set(ref, product)
            }
            batch.commit().await()

            Resource.Success("Processed. Matched & Updated $updatedCount variants.")
        } catch (e: Exception) {
            Resource.Error("CSV Error: ${e.message}")
        }
    }

    // Helper to handle commas inside CSV quotes
    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (char in line) {
            if (char == '"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                tokens.add(sb.toString())
                sb.clear()
            } else {
                sb.append(char)
            }
        }
        tokens.add(sb.toString())
        return tokens
    }
}