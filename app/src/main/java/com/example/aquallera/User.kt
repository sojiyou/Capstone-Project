package com.example.aquallera

data class User(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val number: String = "",

    val createdAt: Long = System.currentTimeMillis()
) {
    // No password field - handled by Firebase Auth
    constructor() : this("", "", "", "", 0L)
}