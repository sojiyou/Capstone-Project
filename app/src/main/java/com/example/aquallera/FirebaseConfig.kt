package com.example.aquallera

import com.google.firebase.database.FirebaseDatabase

object FirebaseConfig {
    // I added this kt file because I used the wrong region on the creation of the firebase
    private const val DATABASE_URL = "https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app"

    fun getDatabaseReference() = FirebaseDatabase.getInstance(DATABASE_URL).reference
}