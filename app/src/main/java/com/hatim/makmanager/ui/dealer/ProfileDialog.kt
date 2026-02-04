package com.hatim.makmanager.ui.dealer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.databinding.DialogProfileBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class ProfileDialog : DialogFragment() {

    private var _binding: DialogProfileBinding? = null
    private val binding get() = _binding!!

    // REPLACE WITH YOUR ACTUAL UPI ID
    private val MY_UPI_ID = "9824052821@okbizaxis"
    private val MY_NAME = "MAK Manager"

    // 1. Safe View Creation (Prevents Crash)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        loadProfileData()

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfileData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val doc = db.collection("users").document(userId).get().await()
                val balance = doc.getDouble("currentBalance") ?: 0.0

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext

                    // Populate Fields
                    binding.etFirmName.setText(doc.getString("name"))
                    binding.etAddress.setText(doc.getString("address"))
                    binding.etCity.setText(doc.getString("city"))
                    binding.etGst.setText(doc.getString("gstNumber"))

                    // Payment Button Logic
                    if (balance > 1.0) {
                        binding.llBalanceContainer.visibility = View.VISIBLE
                        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                        binding.tvBalance.text = format.format(balance)

                        binding.btnPayNow.setOnClickListener {
                            initiateUpiPayment(balance.toString())
                        }
                    } else {
                        binding.llBalanceContainer.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                // Ignore loading errors
            }
        }
    }

    // 2. Fix City Not Saving (Reads text WHEN CLICKED)
    private fun saveProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // READ INPUTS HERE, INSIDE THE FUNCTION
        val newData = mapOf(
            "name" to binding.etFirmName.text.toString(),
            "address" to binding.etAddress.text.toString(),
            "city" to binding.etCity.text.toString(),
            "gstNumber" to binding.etGst.text.toString() // NEW
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseFirestore.getInstance().collection("users").document(userId).update(newData).await()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Update Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initiateUpiPayment(amount: String) {
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", MY_UPI_ID)
            .appendQueryParameter("pn", MY_NAME)
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val intent = Intent(Intent.ACTION_VIEW).apply { data = uri }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI App found", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}