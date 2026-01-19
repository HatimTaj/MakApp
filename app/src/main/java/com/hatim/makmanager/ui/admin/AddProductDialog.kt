package com.hatim.makmanager.ui.admin

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.hatim.makmanager.R
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.data.model.ProductVariant
import com.hatim.makmanager.databinding.DialogAddProductBinding
import com.hatim.makmanager.utils.ImageUtils

class AddProductDialog(
    private val onAddProduct: (Product) -> Unit
) : DialogFragment() {

    private lateinit var binding: DialogAddProductBinding
    private var selectedBitmap: Bitmap? = null
    // Track dynamic rows
    private val variantInputs = mutableListOf<VariantInputHolder>()

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, it)
                selectedBitmap = bitmap
                binding.ivProductImage.setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        binding = DialogAddProductBinding.inflate(layoutInflater)
        dialog.setContentView(binding.root)

        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.ivProductImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        // Add first empty row by default
        addVariantRow()

        // Button to add more rows
        binding.btnAddVariant.setOnClickListener {
            addVariantRow()
        }

        binding.btnSave.setOnClickListener { saveProduct() }

        return dialog
    }

    private fun addVariantRow() {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_variant_edit, binding.llVariantsContainer, false)

        val etSize = itemView.findViewById<EditText>(R.id.etSize)
        val etCap = itemView.findViewById<EditText>(R.id.etCapacity)
        val etPrice = itemView.findViewById<EditText>(R.id.etPrice)
        val etMrp = itemView.findViewById<EditText>(R.id.etMrp)
        val etStock = itemView.findViewById<EditText>(R.id.etStock)
        val btnDelete = itemView.findViewById<ImageButton>(R.id.btnDeleteVariant)

        // Add to list
        val holder = VariantInputHolder(itemView, etSize, etCap, etPrice, etMrp, etStock)
        variantInputs.add(holder)
        binding.llVariantsContainer.addView(itemView)

        btnDelete.setOnClickListener {
            binding.llVariantsContainer.removeView(itemView)
            variantInputs.remove(holder)
        }
    }

    private fun saveProduct() {
        val name = binding.etName.text.toString().trim()
        val type = binding.etType.text.toString().trim()

        if (name.isEmpty() || variantInputs.isEmpty()) {
            Toast.makeText(context, "Name and at least one size required", Toast.LENGTH_SHORT).show()
            return
        }

        val variants = mutableListOf<ProductVariant>()

        for (holder in variantInputs) {
            val size = holder.etSize.text.toString().trim()
            if (size.isEmpty()) continue

            val cap = holder.etCap.text.toString().toIntOrNull() ?: 1
            val unitPrice = holder.etPrice.text.toString().toDoubleOrNull() ?: 0.0
            val mrp = holder.etMrp.text.toString().toDoubleOrNull() ?: 0.0
            val stockUnits = holder.etStock.text.toString().toIntOrNull() ?: 0

            val cartonPrice = unitPrice * cap
            val stockCartons = stockUnits / cap

            variants.add(ProductVariant(size, cap, cartonPrice, mrp, stockCartons))
        }

        if (variants.isEmpty()) return

        val imageBase64 = if (selectedBitmap != null) ImageUtils.bitmapToBase64(selectedBitmap!!) else ""

        val product = Product(
            name = name,
            type = type,
            imageBase64 = imageBase64,
            variants = variants
        )

        onAddProduct(product)
        dismiss()
    }

    data class VariantInputHolder(
        val view: android.view.View,
        val etSize: EditText, val etCap: EditText, val etPrice: EditText, val etMrp: EditText, val etStock: EditText
    )
}