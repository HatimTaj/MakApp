package com.hatim.makmanager.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.data.model.User
import com.hatim.makmanager.databinding.ActivityUserListBinding
import com.hatim.makmanager.databinding.ItemUserLedgerBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.util.Locale

class UserListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserListBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.ivBack.setOnClickListener { finish() }

        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                // CHANGED: Removed .whereEqualTo("role", "dealer")
                // Now it fetches ALL users so you can definitely see them.
                val snapshot = db.collection("users").get().await()

                if (snapshot.isEmpty) {
                    Toast.makeText(this@UserListActivity, "No users found in Database", Toast.LENGTH_LONG).show()
                }

                val users = snapshot.documents.mapNotNull { doc ->
                    // Manual mapping to handle missing fields safely
                    val name = doc.getString("name") ?: "Unknown"
                    val phone = doc.getString("phone") ?: ""
                    val balance = doc.getDouble("currentBalance") ?: 0.0

                    User(
                        uid = doc.id,
                        name = name,
                        phone = phone,
                        currentBalance = balance
                    )
                }

                binding.rvUsers.adapter = UserLedgerAdapter(users) { user ->
                    showPaymentDialog(user)
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserListActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun showPaymentDialog(user: User) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Amount Received (₹)"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Record Manual Payment") // Context for NEFT
            .setMessage("Dealer: ${user.name}\nCurrent Debt: ₹${user.currentBalance}\n\nEnter amount received via Cash/NEFT/Cheque:")
            .setView(layout)
            .setPositiveButton("Confirm") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) processPayment(user, amount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processPayment(user: User, amount: Double) {
        val newBalance = (user.currentBalance - amount)
        // We allow negative balance (credit) if they overpay, or limit to 0. Let's limit to 0.
        val finalBalance = if (newBalance < 0) 0.0 else newBalance

        lifecycleScope.launch {
            try {
                db.collection("users").document(user.uid).update("currentBalance", finalBalance).await()
                Toast.makeText(this@UserListActivity, "Payment Recorded! User Unblocked.", Toast.LENGTH_LONG).show()
                loadUsers()
            } catch(e: Exception) {
                Toast.makeText(this@UserListActivity, "Update Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// ADAPTER (Same as before, included for completeness)
class UserLedgerAdapter(
    private val users: List<User>,
    private val onReceiveClick: (User) -> Unit
) : RecyclerView.Adapter<UserLedgerAdapter.Holder>() {

    class Holder(val binding: ItemUserLedgerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ItemUserLedgerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val user = users[position]
        holder.binding.tvName.text = user.name
        holder.binding.tvPhone.text = user.phone

        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        holder.binding.tvBalance.text = format.format(user.currentBalance)

        if (user.currentBalance > 1.0) {
            holder.binding.tvStatus.text = "DUE"
            holder.binding.tvBalance.setTextColor(android.graphics.Color.RED)
            holder.binding.btnReceive.text = "Clear Bill"
            holder.binding.btnReceive.isEnabled = true
        } else {
            holder.binding.tvStatus.text = "CLEARED"
            holder.binding.tvBalance.setTextColor(android.graphics.Color.parseColor("#2E7D32")) // Green
            holder.binding.btnReceive.text = "Paid"
            holder.binding.btnReceive.isEnabled = false // Disable if no debt
        }

        // Allow clicking specifically to fix mistakes even if paid?
        // Let's keep it enabled so you can adjust if needed, or disable as above.
        holder.binding.btnReceive.isEnabled = true
        holder.binding.btnReceive.setOnClickListener { onReceiveClick(user) }
    }

    override fun getItemCount() = users.size
}