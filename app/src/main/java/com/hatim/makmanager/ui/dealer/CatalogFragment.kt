package com.hatim.makmanager.ui.dealer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.R
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.model.Product
import com.hatim.makmanager.data.repository.ProductRepository
import com.hatim.makmanager.databinding.FragmentCatalogBinding
import com.hatim.makmanager.ui.adapters.ProductAdapter
import com.hatim.makmanager.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class CatalogFragment : Fragment(R.layout.fragment_catalog) {

    private lateinit var binding: FragmentCatalogBinding
    private val repository = ProductRepository()
    private val cartViewModel: CartViewModel by activityViewModels()

    private var allProducts = listOf<Product>()
    private var currentFilter = "All"

    // Flag to control access
    private var isBlocked = false
    private var outstandingAmount = 0.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCatalogBinding.bind(view)

        // 1. CHECK DEALER BALANCE FIRST
        checkDealerBalance()

        val adapter = ProductAdapter(
            onAddToCartClick = { product, variant, qty ->
                if (isBlocked) {
                    // BLOCK LOGIC
                    android.app.AlertDialog.Builder(requireContext())
                        .setTitle("Account Blocked")
                        .setMessage("You have an outstanding balance of ₹${outstandingAmount}.\nPlease clear your bill to place new orders.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    // Normal Logic
                    val success = cartViewModel.addToCart(product, variant, qty)
                    if (success) {
                        Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                    } else {
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle("Limit Reached")
                            .setMessage("Only ${variant.stockCartons} cartons available.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            },
            onItemClick = null
        )

        binding.rvProducts.layoutManager = LinearLayoutManager(context)
        binding.rvProducts.adapter = adapter

        // Search & Filter Logic (Keep existing...)
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString(), currentFilter, adapter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        lifecycleScope.launch {
            when(val result = repository.getProducts()) {
                is Resource.Success -> {
                    allProducts = result.data
                    adapter.submitList(allProducts)
                    setupCategoryChips(allProducts, adapter)
                }
                else -> {}
            }
        }

        // Cart Observer (Keep existing...)
        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            if (items.isNotEmpty()) {
                binding.cvCartSummary.visibility = View.VISIBLE
                val total = cartViewModel.getTotalPrice()
                val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                binding.tvCartTotal.text = "Total: ${format.format(total)}"
                binding.tvCartCount.text = "${items.size} Items"
            } else {
                binding.cvCartSummary.visibility = View.GONE
            }
        }

        binding.btnViewCart.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, CartFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun checkDealerBalance() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        lifecycleScope.launch {
            try {
                val doc = db.collection("users").document(user.uid).get().await()
                outstandingAmount = doc.getDouble("currentBalance") ?: 0.0

                // If balance is greater than 10 rupees (small buffer), block.
                if (outstandingAmount > 10.0) {
                    isBlocked = true
                    binding.tvBlockedWarning.visibility = View.VISIBLE
                    binding.tvBlockedWarning.text = "⚠️ Outstanding Bill: ₹${outstandingAmount}. Ordering Blocked."
                } else {
                    isBlocked = false
                    binding.tvBlockedWarning.visibility = View.GONE
                }
            } catch (e: Exception) { }
        }
    }

    // ... (Keep setupCategoryChips and filterList from previous code) ...
    // Note: I omitted them here to save space, but DO NOT DELETE THEM from your file.
    private fun setupCategoryChips(products: List<Product>, adapter: ProductAdapter) {
        val categories = products.map { it.type.trim() }.distinct().filter { it.isNotEmpty() }.sorted()
        val allChip = layoutInflater.inflate(R.layout.item_chip_category, binding.chipGroup, false) as Chip
        allChip.text = "All"
        allChip.isChecked = true
        allChip.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) { currentFilter = "All"; filterList(binding.etSearch.text.toString(), "All", adapter) }
        }
        binding.chipGroup.addView(allChip)
        for (cat in categories) {
            val chip = layoutInflater.inflate(R.layout.item_chip_category, binding.chipGroup, false) as Chip
            chip.text = cat
            chip.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) { currentFilter = buttonView.text.toString(); filterList(binding.etSearch.text.toString(), currentFilter, adapter) }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun filterList(query: String, category: String, adapter: ProductAdapter) {
        val q = query.lowercase().trim()
        val filtered = allProducts.filter { p ->
            val matchesSearch = p.name.lowercase().contains(q) || p.type.lowercase().contains(q)
            val matchesCategory = if (category == "All") true else p.type.equals(category, ignoreCase = true)
            matchesSearch && matchesCategory
        }
        adapter.submitList(filtered)
    }
}