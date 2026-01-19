package com.hatim.makmanager.ui.dealer

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.R
import com.hatim.makmanager.data.model.CartItem
import com.hatim.makmanager.data.model.Order
import com.hatim.makmanager.databinding.FragmentCartBinding
import com.hatim.makmanager.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class CartFragment : Fragment(R.layout.fragment_cart) {

    private lateinit var binding: FragmentCartBinding
    private val cartViewModel: CartViewModel by activityViewModels()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCartBinding.bind(view)

        setupObserver()

        binding.btnPlaceOrder.setOnClickListener {
            placeOrder()
        }
    }

    private fun setupObserver() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            if (items.isEmpty()) {
                binding.lvCartItems.adapter = null
                binding.tvTotal.text = "Cart is Empty"
                binding.btnPlaceOrder.isEnabled = false
                return@observe
            }

            // Display List
            val displayList = items.map {
                "${it.productName} (${it.variantSize})\nQty: ${it.cartonQuantity}  |  ₹${it.priceAtOrder * it.cartonQuantity}"
            }

            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, displayList)
            binding.lvCartItems.adapter = adapter

            // NEW: Click to Edit/Remove
            binding.lvCartItems.setOnItemClickListener { _, _, position, _ ->
                showEditDialog(items[position])
            }

            val total = cartViewModel.getTotalPrice()
            val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            binding.tvTotal.text = "Total Pay: ${format.format(total)}"
            binding.btnPlaceOrder.isEnabled = true
        }
    }

    private fun showEditDialog(item: CartItem) {
        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.setText(item.cartonQuantity.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Edit ${item.productName}")
            .setMessage("Change Quantity (0 to remove)")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newQty = input.text.toString().toIntOrNull() ?: 0
                cartViewModel.updateItemQuantity(item, newQty)
            }
            .setNegativeButton("Remove") { _, _ ->
                cartViewModel.removeItem(item)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun placeOrder() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnPlaceOrder.isEnabled = false

        lifecycleScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) throw Exception("Not logged in")

                // 1. Fetch Dealer Info SAFELY
                val userDoc = db.collection("users").document(user.uid).get().await()

                // FALLBACK LOGIC: If name/phone is missing in DB, use placeholders
                val dealerPhone = userDoc.getString("phone") ?: user.phoneNumber ?: "Unknown Phone"
                val dealerName = userDoc.getString("name") ?: "Dealer ($dealerPhone)"

                // 2. Create Order
                val order = Order(
                    dealerId = user.uid,
                    dealerName = dealerName,
                    dealerPhone = dealerPhone,
                    timestamp = Timestamp.now(),
                    status = "PENDING",
                    items = cartViewModel.cartItems.value ?: emptyList(),
                    totalPrice = cartViewModel.getTotalPrice(),
                    totalLitres = cartViewModel.getTotalLitres()
                )

                // 3. Send
                db.collection("orders").add(order).await()

                Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_LONG).show()
                cartViewModel.clearCart()

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, CatalogFragment())
                    .commit()

            } catch (e: Exception) {
                Toast.makeText(context, "Order Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}