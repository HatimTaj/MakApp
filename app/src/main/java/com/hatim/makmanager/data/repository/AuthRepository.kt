package com.hatim.makmanager.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hatim.makmanager.data.Resource
import com.hatim.makmanager.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, pass: String): Resource<User> {
        return try {
            // 1. Try to Log in
            val authResult = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: return Resource.Error("Login successful, but UID is null")

            // 2. Log in worked! Now fetching data for UID: $uid
            val snapshot = db.collection("users").document(uid).get().await()

            if (!snapshot.exists()) {
                // This is the error you are getting. It means the ID doesn't exist in the 'users' folder.
                return Resource.Error("Auth UID ($uid) not found in 'users' collection.")
            }

            val user = snapshot.toObject(User::class.java)
            if (user == null) {
                return Resource.Error("User data is corrupt or empty.")
            }

            Resource.Success(user)

        } catch (e: com.google.firebase.FirebaseNetworkException) {
            Resource.Error("Network Error. Check WiFi/Data.")
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            Resource.Error("Database Permission Denied. Check Rules.")
        } catch (e: Exception) {
            Resource.Error("Error: ${e.message}")
        }
    }
}