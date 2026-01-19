package com.hatim.makmanager.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.databinding.ActivityLoginBinding
import com.hatim.makmanager.ui.admin.AdminDashboardActivity
import com.hatim.makmanager.ui.dealer.DealerDashboardActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. AUTO-LOGIN CHECK ---
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in, fetch role and redirect immediately
            checkRoleAndRedirect(currentUser.uid)
            return // Stop loading the Login UI
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- 2. LOGIN BUTTON LOGIC ---
        binding.btnLogin.setOnClickListener {
            val rawPhone = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (rawPhone.length != 10) {
                binding.etEmail.error = "Please enter a valid 10-digit mobile number"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.etPassword.error = "Password cannot be empty"
                return@setOnClickListener
            }

            // Fake Email conversion
            val loginId = "$rawPhone@mak.com"
            viewModel.login(loginId, password)
        }

        // --- 3. OBSERVE LOGIN RESULT ---
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, "Login Failed: ${state.message}", Toast.LENGTH_LONG).show()
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    navigateBasedOnRole(state.data.role)
                }
            }
        }
    }

    // Helper to check role silently on startup
    private fun checkRoleAndRedirect(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val role = document.getString("role") ?: "DEALER"
                    navigateBasedOnRole(role)
                } else {
                    // Session invalid, force login
                    setContentView(binding.root)
                }
            }
            .addOnFailureListener {
                // Network error, force login
                setContentView(binding.root)
            }
    }

    private fun navigateBasedOnRole(role: String) {
        if (role == "ADMIN") {
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        } else {
            val intent = Intent(this, DealerDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        finish()
    }
}