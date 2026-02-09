package com.example.aquallera

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class LoginActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignup: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        initializeViews()
        setupClickListeners()

        // Check if user is already logged in
        checkExistingSession()
    }

    private fun initializeViews() {
        etEmail = findViewById(R.id.LoginEmailInput)
        etPassword = findViewById(R.id.LoginPasswordInput)
        btnLogin = findViewById(R.id.LoginButton)
        tvSignup = findViewById(R.id.GoToSignupBtn)
    }

    private fun checkExistingSession() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)

        if (isLoggedIn && auth.currentUser != null) {
            // User is already logged in, redirect to map
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun loginUser() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Processing..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Firebase Auth login successful, now get user data from Realtime Database
                    val user = auth.currentUser
                    if (user != null) {
                        fetchUserDataFromDatabase(user.uid)
                    }
                } else {
                    loginError("Login failed: wrong email or password")
                }
            }
    }

    private fun fetchUserDataFromDatabase(uid: String) {
        val database = FirebaseConfig.getDatabaseReference()
        val userRef = database.child("users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(User::class.java)
                    if (user != null) {
                        loginSuccess(user)
                    } else {
                        loginError("User data not found.")
                    }
                } else {
                    loginError("User profile not found.")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                loginError("Database error. Please try again.")
            }
        })
    }

    private fun loginSuccess(user: User) {
        saveUserSession(user)
        btnLogin.isEnabled = true
        btnLogin.text = "Login"
        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun loginError(errorMessage: String) {
        btnLogin.isEnabled = true
        btnLogin.text = "Login"
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun saveUserSession(user: User) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_uid", user.uid)
            putString("user_name", user.fullName)
            putString("user_email", user.email)
            putString("user_number", user.number)
            putBoolean("is_logged_in", true)
            apply()
        }
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            loginUser()
        }

        tvSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }
}