package com.hatim.makmanager.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.databinding.ActivityLoginBinding
import com.hatim.makmanager.ui.admin.AdminDashboardActivity
import com.hatim.makmanager.ui.dealer.DealerDashboardActivity
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If already logged in, redirect
        if (auth.currentUser != null) {
            checkUserRole(auth.currentUser!!.uid)
        }

        binding.btnLogin.setOnClickListener {
            val phone = binding.etPhone.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (phone.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter phone and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val fakeEmail = "$phone@mak.com"
            binding.btnLogin.isEnabled = false
            binding.btnLogin.text = "Logging in..."

            auth.signInWithEmailAndPassword(fakeEmail, pass)
                .addOnSuccessListener { result ->
                    checkUserRole(result.user!!.uid)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"
                }
        }

        // NEW: Go to Register Screen
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun checkUserRole(uid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val document = db.collection("users").document(uid).get().await()
                val role = document.getString("role") ?: "dealer"

                withContext(Dispatchers.Main) {
                    if (role == "admin") {
                        startActivity(Intent(this@LoginActivity, AdminDashboardActivity::class.java))
                    } else {
                        startActivity(Intent(this@LoginActivity, DealerDashboardActivity::class.java))
                    }
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@LoginActivity, "Error fetching user role", Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                    binding.btnLogin.text = "Login"
                }
            }
        }
    }
}