package com.hatim.makmanager.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hatim.makmanager.R
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.data.model.ProductVariant
import com.hatim.makmanager.databinding.ItemProductBinding
import com.hatim.makmanager.utils.ImageUtils
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val onAddToCartClick: (Product, ProductVariant, Int) -> Unit,
    private val onItemClick: ((Product) -> Unit)? = null
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

            binding.root.setOnClickListener { onItemClick?.invoke(product) }

            binding.tvProductName.text = product.name

            if (product.imageBase64.isNotEmpty()) {
                val bitmap = ImageUtils.base64ToBitmap(product.imageBase64)
                if (bitmap != null) {
                    binding.ivProductThumb.setImageBitmap(bitmap)
                    binding.ivProductThumb.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                }
            } else {
                binding.ivProductThumb.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            if (product.variants.isNotEmpty()) {
                // ... (Adapter setup remains same) ...
                val variantNames = product.variants.map { "${it.size} (${it.cartonCapacity} pcs)" }

                val adapter = ArrayAdapter(
                    binding.root.context,
                    R.layout.spinner_item,
                    variantNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                binding.spVariants.adapter = adapter

                binding.spVariants.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val variant = product.variants[position]

                        binding.tvCartonPrice.text = format.format(variant.pricePerCarton)

                        val unitPrice = variant.pricePerCarton / variant.cartonCapacity
                        binding.tvUnitPrice.text = "(${format.format(unitPrice)}/pc)"

                        // --- NEW: BIND MRP ---
                        binding.tvMrp.text = "MRP: ${format.format(variant.mrp)}"

                        if (variant.stockCartons > 0) {
                            binding.tvStock.text = "In Stock: ${variant.stockCartons}"
                            binding.tvStock.setTextColor(Color.parseColor("#2E7D32"))
                            binding.tvStock.setBackgroundColor(Color.parseColor("#E8F5E9"))
                            binding.btnAdd.isEnabled = true
                        } else {
                            binding.tvStock.text = "Out of Stock"
                            binding.tvStock.setTextColor(Color.parseColor("#C62828"))
                            binding.tvStock.setBackgroundColor(Color.parseColor("#FFEBEE"))
                            binding.btnAdd.isEnabled = false
                        }

                        binding.btnAdd.setOnClickListener {
                            val qtyStr = binding.etQuantity.text.toString()
                            if (qtyStr.isNotEmpty()) {
                                onAddToCartClick(product, variant, qtyStr.toInt())
                                binding.etQuantity.setText("")
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
    }
}