package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // Check authentication status after a brief delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthenticationStatus()
        }, 1500) // 1.5 second splash screen
    }

    private fun checkAuthenticationStatus() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User is logged in → Go directly to Map
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // User is NOT logged in → Go to Welcome/Main screen
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}