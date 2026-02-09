package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class SignupActivity : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etNumber: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSignup: Button
    private lateinit var tvLogin: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        etFullName = findViewById(R.id.FullNameInput)
        etEmail = findViewById(R.id.SignUpEmailInput)
        etNumber = findViewById(R.id.SignupNumberInput)
        etPassword = findViewById(R.id.PasswordInput)
        etConfirmPassword = findViewById(R.id.ConfirmPasswordInput)
        btnSignup = findViewById(R.id.SignUpBtn)
        tvLogin = findViewById(R.id.GoToLoginBtn)
        // Removed: btnGetLocation, tvLocationStatus
    }

    private fun registerUser() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val number = etNumber.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Input validation
        if (fullName.isEmpty() || email.isEmpty() || number.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        } else if (!isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        } else if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        } else if (number.length != 11) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
            return
        } else if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        // Removed: location check

        btnSignup.isEnabled = false
        btnSignup.text = "Processing..."

        // First check if phone number exists
        checkIfPhoneNumberExists(email, fullName, number, password)
    }

    private fun checkIfPhoneNumberExists(email: String, fullName: String, number: String, password: String) {
        val database = FirebaseConfig.getDatabaseReference()
        val usersRef = database.child("users")

        usersRef.orderByChild("number").equalTo(number)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        signupError("Phone number already registered. Please use a different number.")
                    } else {
                        // Phone number available, create user with Firebase Auth
                        createUserWithFirebaseAuth(email, fullName, number, password)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    signupError("Database error. Please try again.")
                }
            })
    }

    private fun createUserWithFirebaseAuth(email: String, fullName: String, number: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()

                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener { profileTask ->
                                if (profileTask.isSuccessful) {
                                    // Save user data to database
                                    saveUserToDatabase(firebaseUser.uid, fullName, email, number)
                                } else {
                                    signupError("Profile setup failed. Please try again.")
                                }
                            }
                    }
                } else {
                    signupError("Registration failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserToDatabase(uid: String, fullName: String, email: String, number: String) {
        val database = FirebaseConfig.getDatabaseReference()
        val user = User(
            uid = uid,
            fullName = fullName,
            email = email,
            number = number
        )

        database.child("users").child(uid).setValue(user)
            .addOnSuccessListener {
                signupSuccess(user)
            }
            .addOnFailureListener { error ->
                auth.currentUser?.delete()
                signupError("Failed to create account: ${error.message}")
            }
    }

    private fun signupSuccess(user: User) {
        saveUserSession(user)
        btnSignup.isEnabled = true
        btnSignup.text = "Sign Up"
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun signupError(errorMessage: String) {
        btnSignup.isEnabled = true
        btnSignup.text = "Sign Up"
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

        Log.d("SignupActivity", "User session saved:")
        Log.d("SignupActivity", "UID: ${user.uid}")
        Log.d("SignupActivity", "Name: ${user.fullName}")
        Log.d("SignupActivity", "Email: ${user.email}")
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun setupClickListeners() {
        btnSignup.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}