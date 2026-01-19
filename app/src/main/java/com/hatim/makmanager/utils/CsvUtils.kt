package com.hatim.makmanager.utils

import android.net.Uri
import android.content.Context
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.data.model.ProductVariant
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvUtils {
    fun parseAndMergeProducts(context: Context, uri: Uri, currentProducts: List<Product>): List<Product> {
        val updatedProducts = currentProducts.toMutableList()
        val inputStream = context.contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))

        // Skip Header (Row 1-4 based on your file)
        repeat(4) { reader.readLine() }

        reader.forEachLine { line ->
            val cols = line.split(",") // Simple CSV split
            if (cols.size > 7) {
                // Map Columns: A=Name(0), B=Size(1), C=Pack(2), F=Rate(5), G=MRP(6)
                val rawName = cols[0].trim()
                val size = cols[1].trim()
                val packStr = cols[2].trim()
                val rateStr = cols[5].trim() // "Price Per Pack"
                val mrpStr = cols[6].trim()

                if (rawName.isNotEmpty() && size.isNotEmpty()) {
                    val pack = packStr.toIntOrNull() ?: 1
                    val rate = rateStr.toDoubleOrNull() ?: 0.0
                    val mrp = mrpStr.toDoubleOrNull() ?: 0.0

                    // Logic: Find matching Product and Variant
                    val productIndex = updatedProducts.indexOfFirst { it.name.equals(rawName, ignoreCase = true) }

                    if (productIndex != -1) {
                        val product = updatedProducts[productIndex]
                        val newVariants = product.variants.map { variant ->
                            if (variant.size.equals(size, ignoreCase = true)) {
                                // UPDATE PRICE & MRP
                                variant.copy(
                                    cartonCapacity = pack,
                                    pricePerCarton = rate * pack, // Convert Unit Price to Carton Price
                                    mrp= mrp
                                )
                            } else variant
                        }.toMutableList()

                        // If variant doesn't exist, you might want to add it (Optional)
                        updatedProducts[productIndex] = product.copy(variants = newVariants)
                    }
                }
            }
        }
        reader.close()
        return updatedProducts
    }
}