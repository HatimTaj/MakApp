package com.hatim.makmanager.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val type: String = "", // Category (Engine Oil, Grease, etc.)
    val imageBase64: String = "",
    val variants: List<ProductVariant> = emptyList()
)

data class ProductVariant(
    val size: String = "",       // e.g., "1L", "900ml"
    val cartonCapacity: Int = 1, // e.g., 20 pcs per carton
    val pricePerCarton: Double = 0.0, // Dealer Price (Rate)
    val mrp: Double = 0.0,       // Maximum Retail Price
    var stockCartons: Int = 0    // Inventory
) {
    // Helper to calculate total value for display if needed
    val unitPrice: Double
        get() = if (cartonCapacity > 0) pricePerCarton / cartonCapacity else 0.0

    // Litre Calculation Logic
    val totalLitresInStock: Double
        get() = stockCartons.toDouble() * cartonCapacity.toDouble() * parseVolume(size)

    private fun parseVolume(sizeStr: String): Double {
        return try {
            val normalized = sizeStr.lowercase().trim()
            val numberRegex = Regex("[0-9]+(\\.[0-9]+)?")
            val match = numberRegex.find(normalized) ?: return 0.0
            val number = match.value.toDouble()
            if (normalized.contains("m") && !normalized.contains("mm")) number / 1000.0 else number
        } catch (e: Exception) { 0.0 }
    }
}