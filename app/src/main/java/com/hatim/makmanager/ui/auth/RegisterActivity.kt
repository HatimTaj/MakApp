package com.hatim.makmanager.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.data.model.User
import com.hatim.makmanager.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Using Phone as dummy email to use Firebase Auth
            val fakeEmail = "$phone@mak.com"

            binding.btnRegister.isEnabled = false
            binding.btnRegister.text = "Creating Account..."

            auth.createUserWithEmailAndPassword(fakeEmail, pass)
                .addOnSuccessListener { result ->
                    // Account Created, Now Save User Data
                    val newUser = User(
                        uid = result.user!!.uid,
                        name = name,
                        phone = phone,
                        role = "dealer",
                        isApproved = false // Default: Blocked until Admin approves
                    )

                    db.collection("users").document(result.user!!.uid).set(newUser)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration Successful! Please Login.", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    binding.btnRegister.isEnabled = true
                    binding.btnRegister.text = "Sign Up"
                }
        }

        binding.tvLogin.setOnClickListener { finish() }
    }
}