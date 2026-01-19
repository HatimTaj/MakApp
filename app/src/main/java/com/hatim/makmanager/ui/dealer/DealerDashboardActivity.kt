package com.hatim.makmanager.ui.dealer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.hatim.makmanager.R
import com.hatim.makmanager.databinding.ActivityDealerDashboardBinding
import com.hatim.makmanager.ui.auth.LoginActivity
import android.net.Uri
class DealerDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDealerDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDealerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // --- NEW: EDIT PROFILE BUTTON ---
        binding.ivProfile.setOnClickListener {
            ProfileDialog().show(supportFragmentManager, "ProfileDialog")
        }
    }
    private fun payViaUpi(amount: String, name: String, upiId: String) {
        val uri = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", upiId) // YOUR UPI ID HERE (e.g. 9824052821@okbizaxis)
            .appendQueryParameter("pn", "MAK Manager")
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .build()

        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri

        val chooser = Intent.createChooser(intent, "Pay with")
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(chooser)
        } else {
            // Toast: No UPI app found
        }
    }
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}