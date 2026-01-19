package com.hatim.makmanager.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val role: String = "dealer", // "admin" or "dealer"
    val currentBalance: Double = 0.0 // Positive means Debt (Dealer has to pay)
)