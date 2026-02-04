package com.hatim.makmanager.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val gstNumber: String = "", // NEW: Optional
    val role: String = "dealer",
    val currentBalance: Double = 0.0,
    val isApproved: Boolean = false // NEW: Block access by default
)