package com.hatim.makmanager.ui.admin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.hatim.makmanager.R
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.data.model.ProductVariant
import com.hatim.makmanager.databinding.DialogEditProductBinding

class EditProductDialog(
    private val productToEdit: Product,
    private val onUpdateProduct: (Product) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogEditProductBinding
    // We store references to the input fields to read them later
    private val variantInputs = mutableListOf<VariantInputHolder>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        binding = DialogEditProductBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupUI()
        return dialog
    }

    private fun setupUI() {
        binding.etName.setText(productToEdit.name)
        binding.etType.setText(productToEdit.type)

        // Load existing sizes
        productToEdit.variants.forEach { addVariantRow(it) }

        // "Add Size" Button Logic
        binding.btnAddVariant.setOnClickListener {
            addVariantRow(ProductVariant("", 1, 0.0, 0.0, 0))
        }

        binding.btnSave.setOnClickListener { saveChanges() }
    }

    private fun addVariantRow(variant: ProductVariant) {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_variant_edit, binding.llVariantsContainer, false)

        val etSize = itemView.findViewById<EditText>(R.id.etSize)
        val etCap = itemView.findViewById<EditText>(R.id.etCapacity)
        val etPrice = itemView.findViewById<EditText>(R.id.etPrice)
        val etMrp = itemView.findViewById<EditText>(R.id.etMrp)
        val etStock = itemView.findViewById<EditText>(R.id.etStock)
        val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDeleteVariant)

        etSize.setText(variant.size)
        etCap.setText(variant.cartonCapacity.toString())

        // Show Unit Price (Rate per piece)
        val unitPrice = if (variant.cartonCapacity > 0) variant.pricePerCarton / variant.cartonCapacity else 0.0
        etPrice.setText(unitPrice.toString())

        etMrp.setText(variant.mrp.toString())

        // Show Stock in Units (Total pieces)
        val stockUnits = variant.stockCartons * variant.cartonCapacity
        etStock.setText(stockUnits.toString())

        // Track this row
        val holder = VariantInputHolder(itemView, etSize, etCap, etPrice, etMrp, etStock)
        variantInputs.add(holder)
        binding.llVariantsContainer.addView(itemView)

        // Delete Logic
        btnDelete.setOnClickListener {
            binding.llVariantsContainer.removeView(itemView)
            variantInputs.remove(holder)
        }
    }

    private fun saveChanges() {
        val name = binding.etName.text.toString().trim()
        val type = binding.etType.text.toString().trim()

        if (name.isEmpty()) return

        val newVariants = mutableListOf<ProductVariant>()

        for (holder in variantInputs) {
            val size = holder.etSize.text.toString().trim()
            if (size.isEmpty()) continue // Skip empty rows

            val cap = holder.etCap.text.toString().toIntOrNull() ?: 1
            val unitPrice = holder.etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val mrp = holder.etMrp.text.toString().toDoubleOrNull() ?: 0.0
            val stockUnits = holder.etStock.text.toString().toIntOrNull() ?: 0

            // Recalculate Carton Price & Stock
            val cartonPrice = unitPrice * cap
            val stockCartons = stockUnits / cap // Integer division (e.g., 25 units / 12 cap = 2 cartons)

            newVariants.add(ProductVariant(size, cap, cartonPrice, mrp, stockCartons))
        }

        val updatedProduct = productToEdit.copy(
            name = name,
            type = type,
            variants = newVariants
        )

        onUpdateProduct(updatedProduct)
        dismiss()
    }

    data class VariantInputHolder(
        val view: android.view.View,
        val etSize: EditText, val etCap: EditText, val etPrice: EditText, val etMrp: EditText, val etStock: EditText
    )
}