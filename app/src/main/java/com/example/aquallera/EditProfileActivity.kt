package com.example.aquallera

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etNumber: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnSaveProfile: Button
    private lateinit var btnCancel: Button

    private var currentUserKey: String = ""
    private var originalEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        loadCurrentUserData()
    }

    private fun initializeViews() {
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etNumber = findViewById(R.id.etNumber)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupClickListeners() {
        btnSaveProfile.setOnClickListener {
            if (validateInputs()) {
                updateUserProfile()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentUserData() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", "")

        if (!userEmail.isNullOrEmpty()) {
            fetchUserDataForEditing(userEmail)
        } else {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchUserDataForEditing(userEmail: String) {
        val database = FirebaseConfig.getDatabaseReference()
        val usersRef = database.child("users")

        usersRef.orderByChild("email").equalTo(userEmail)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            currentUserKey = userSnapshot.key ?: ""
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                populateEditFields(user)
                                return
                            }
                        }
                    }
                    Toast.makeText(this@EditProfileActivity, "User data not found", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@EditProfileActivity, "Failed to load user data", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun populateEditFields(user: User) {
        etFullName.setText(user.fullName)
        etEmail.setText(user.email)
        etNumber.setText(user.number)
        originalEmail = user.email
    }

    private fun validateInputs(): Boolean {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val number = etNumber.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return false
        }

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Enter a valid email address"
            return false
        }

        if (number.isEmpty()) {
            etNumber.error = "Phone number is required"
            return false
        }

        // Password validation (only if user wants to change password)
        if (password.isNotEmpty() || confirmPassword.isNotEmpty()) {
            if (password.length < 6) {
                etPassword.error = "Password must be at least 6 characters"
                return false
            }

            if (password != confirmPassword) {
                etConfirmPassword.error = "Passwords do not match"
                return false
            }
        }

        return true
    }

    private fun updateUserProfile() {
        val fullName = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val number = etNumber.text.toString().trim()
        val password = etPassword.text.toString()

        if (currentUserKey.isEmpty()) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        val database = FirebaseConfig.getDatabaseReference()
        val userRef = database.child("users").child(currentUserKey)

        // Create updated user object
        val updatedUser = User(fullName, email, number, password.ifEmpty { "existing_password" })

        userRef.setValue(updatedUser)
            .addOnSuccessListener {
                // Update SharedPreferences
                updateLocalUserData(fullName, email, number)

                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()

                // Return to ProfileActivity
                val intent = Intent(this, ProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLocalUserData(fullName: String, email: String, number: String) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_name", fullName)
            putString("user_email", email)
            putString("user_number", number)
            apply()
        }
    }

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        // Set no active tab in edit profile
        setInactiveTabs()
        navMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }

        navOrders.setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java)
            startActivity(intent)
            finish()
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setInactiveTabs() {
        val allTabs = listOf(
            findViewById<LinearLayout>(R.id.navMap),
            findViewById<LinearLayout>(R.id.navOrder),
            findViewById<LinearLayout>(R.id.navProfile)
        )

        allTabs.forEach { tab ->
            val textView = when (tab.id) {
                R.id.navMap -> findViewById<TextView>(R.id.tvMap)
                R.id.navOrder -> findViewById<TextView>(R.id.tvOrder)
                R.id.navProfile -> findViewById<TextView>(R.id.tvProfile)
                else -> null
            }

            textView?.setTextColor(Color.parseColor("#757575"))
            tab.background = ContextCompat.getDrawable(this, R.drawable.tab_border_highlight)
        }
    }
}