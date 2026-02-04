package com.hatim.makmanager.ui.admin

import android.app.AlertDialog
import android.graphics.Color
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
        binding.progressBar.visibility = View.VISIBLE

        // CHANGED: Use addSnapshotListener for Realtime Updates
        db.collection("users")
            .addSnapshotListener { snapshot, e ->
                binding.progressBar.visibility = View.GONE
                if (e != null || snapshot == null) {
                    Toast.makeText(this, "Error loading data", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val users = snapshot.documents.mapNotNull { doc ->
                    val user = doc.toObject(User::class.java)
                    user?.copy(uid = doc.id)
                }

                binding.rvUsers.adapter = UserLedgerAdapter(users,
                    onReceiveClick = { user -> showPaymentDialog(user) },
                    onApproveClick = { user -> approveUser(user) }
                )
            }
    }

    private fun approveUser(user: User) {
        lifecycleScope.launch {
            try {
                // Just update DB. The listener above will auto-refresh the UI.
                db.collection("users").document(user.uid).update("isApproved", true).await()
                Toast.makeText(this@UserListActivity, "Approved Successfully!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@UserListActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPaymentDialog(user: User) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Amount Received"
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 10)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Record Payment")
            .setMessage("Dealer: ${user.name}\nCurrent Debt: ₹${user.currentBalance}")
            .setView(layout)
            .setPositiveButton("Confirm") { _, _ ->
                val amount = input.text.toString().toDoubleOrNull()
                if (amount != null && amount > 0) processPayment(user, amount)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun processPayment(user: User, amount: Double) {
        val newBalance = if ((user.currentBalance - amount) < 0) 0.0 else (user.currentBalance - amount)
        lifecycleScope.launch {
            db.collection("users").document(user.uid).update("currentBalance", newBalance).await()
            Toast.makeText(this@UserListActivity, "Payment Recorded!", Toast.LENGTH_SHORT).show()
        }
    }
}

class UserLedgerAdapter(
    private val users: List<User>,
    private val onReceiveClick: (User) -> Unit,
    private val onApproveClick: (User) -> Unit
) : RecyclerView.Adapter<UserLedgerAdapter.Holder>() {

    class Holder(val binding: ItemUserLedgerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ItemUserLedgerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val user = users[position]
        holder.binding.tvName.text = user.name.ifEmpty { "New User" }
        holder.binding.tvPhone.text = user.phone

        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        // APPROVAL LOGIC
        if (!user.isApproved && user.role != "admin") {
            holder.binding.tvStatus.text = "PENDING APPROVAL"
            holder.binding.tvStatus.setTextColor(Color.parseColor("#FF6D00")) // Orange
            holder.binding.tvBalance.visibility = View.GONE
            holder.binding.btnReceive.text = "APPROVE"
            holder.binding.btnReceive.setBackgroundColor(Color.parseColor("#FF6D00"))
            holder.binding.btnReceive.setTextColor(Color.WHITE)
            holder.binding.btnReceive.setOnClickListener { onApproveClick(user) }
        } else {
            // NORMAL LEDGER LOGIC
            holder.binding.tvBalance.visibility = View.VISIBLE
            holder.binding.tvBalance.text = format.format(user.currentBalance)

            if (user.currentBalance > 1.0) {
                holder.binding.tvStatus.text = "DUE"
                holder.binding.tvBalance.setTextColor(Color.RED)
                holder.binding.btnReceive.text = "Clear Bill"
            } else {
                holder.binding.tvStatus.text = "PAID"
                holder.binding.tvBalance.setTextColor(Color.parseColor("#2E7D32"))
                holder.binding.btnReceive.text = "Paid"
            }
            // Reset Button Style
            holder.binding.btnReceive.setBackgroundColor(Color.parseColor("#E0E0E0")) // Default gray
            holder.binding.btnReceive.setTextColor(Color.BLACK)
            holder.binding.btnReceive.setOnClickListener { onReceiveClick(user) }
        }
    }

    override fun getItemCount() = users.size
}