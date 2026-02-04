package com.hatim.makmanager.ui.dealer

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.R
import com.hatim.makmanager.databinding.ActivityDealerDashboardBinding
import com.hatim.makmanager.ui.auth.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DealerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDealerDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDealerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // CHECK APPROVAL
        checkApprovalStatus()

        loadFragment(CatalogFragment())

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_catalog -> loadFragment(CatalogFragment())
                R.id.nav_orders -> loadFragment(MyOrdersFragment())
                R.id.nav_cart -> loadFragment(CartFragment())
            }
            true
        }

        binding.ivLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.ivProfile.setOnClickListener {
            ProfileDialog().show(supportFragmentManager, "ProfileDialog")
        }
    }

    private fun checkApprovalStatus() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users").document(uid).get().await()

                // If field doesn't exist, default to FALSE (Blocked)
                val isApproved = doc.getBoolean("isApproved") ?: false
                val role = doc.getString("role") ?: "dealer"

                if (!isApproved && role != "admin") {
                    showBlockDialog()
                }
            } catch (e: Exception) { }
        }
    }

    private fun showBlockDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Account Pending")
            .setMessage("Your account is waiting for Admin Approval.\nPlease contact the administrator.")
            .setCancelable(false) // User CANNOT close this
            .setPositiveButton("Refresh") { _, _ ->
                checkApprovalStatus() // Try again
            }
            .setNegativeButton("Logout") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            .create()
        dialog.show()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}