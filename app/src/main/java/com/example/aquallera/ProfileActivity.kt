package com.example.aquallera

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvNumber: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnLogOut: Button
    private lateinit var btnMaps: Button
    private lateinit var btnSeeList: Button
    private lateinit var ivProfileImage: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        loadUserData()
    }

    private fun initializeViews() {
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvNumber = findViewById(R.id.tvUserNumber)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnLogOut = findViewById(R.id.btnLogOut)
        btnMaps = findViewById(R.id.btnMaps)
        btnSeeList = findViewById(R.id.btnSeeList)
        ivProfileImage = findViewById(R.id.ivProfileImage)
    }

    private fun setupClickListeners() {
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        btnLogOut.setOnClickListener {
            showLogoutConfirmation()
        }

        btnMaps.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        btnSeeList.setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadUserData() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userEmail = sharedPref.getString("user_email", "")

        if (!userEmail.isNullOrEmpty()) {
            // Show loading state
            tvUserEmail.text = "Loading..."
            tvUserName.text = "Loading..."
            tvNumber.text = "Loading..."

            fetchUserDataFromDatabase(userEmail)
        } else {
            // No user logged in, show default data
            showDefaultData()
        }
    }

    private fun fetchUserDataFromDatabase(userEmail: String) {
        // ✅ FIXED: Use FirebaseConfig instead of direct FirebaseDatabase
        val database = FirebaseConfig.getDatabaseReference()
        val usersRef = database.child("users")

        usersRef.orderByChild("email").equalTo(userEmail)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.exists()) {
                        for (userSnapshot in dataSnapshot.children) {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null) {
                                // Update UI with real user data
                                updateUIWithUserData(user)
                                updateSharedPreference(user)
                                return
                            }
                        }
                    }
                    // If not found, show default data
                    showDefaultData()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Silent fail - load from SharedPreferences
                    loadFromSharedPreferences()
                }
            })
    }

    private fun updateUIWithUserData(user: User) {
        tvUserName.text = user.fullName
        tvUserEmail.text = user.email
        tvNumber.text = user.number
    }

    private fun updateSharedPreference(user: User) {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_name", user.fullName)
            putString("user_email", user.email)
            putString("user_number", user.number)
            apply()
        }
    }

    private fun loadFromSharedPreferences() {
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "Juan A. Tamad")
        val userEmail = sharedPref.getString("user_email", "juantamad@gmail.com")
        val userNumber = sharedPref.getString("user_number", "09277263218")

        tvUserName.text = userName
        tvUserEmail.text = userEmail
        tvNumber.text = userNumber
    }

    private fun showDefaultData() {
        tvUserName.text = "Juan A. Tamad"
        tvUserEmail.text = "juantamad@gmail.com"
        tvNumber.text = "09277263218"
    }

    private fun showLogoutConfirmation() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { dialog, _ ->
                performLogout()
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Clear user data
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        // Back to MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from EditProfileActivity
        refreshUserData()
    }

    private fun refreshUserData() {
        // Refresh user data from SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = sharedPref.getString("user_name", "Juan A. Tamad")
        val userEmail = sharedPref.getString("user_email", "juanatamad@gmail.com")
        val userNumber = sharedPref.getString("user_number", "09277263218")

        tvUserName.text = userName
        tvUserEmail.text = userEmail
        tvNumber.text = userNumber
    }

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        // Set Profile as active
        setActiveTab(navProfile)
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
            // Already on profile page, just refresh if needed
            setActiveTab(navProfile)
        }
    }

    private fun setActiveTab(activeTab: LinearLayout) {
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

            if (tab == activeTab) {
                textView?.setTextColor(Color.parseColor("#2196F3"))
                tab.background = ContextCompat.getDrawable(this, R.drawable.tab_active_border)
            } else {
                textView?.setTextColor(Color.parseColor("#757575"))
                tab.background = ContextCompat.getDrawable(this, R.drawable.tab_border_highlight)
            }
        }
    }
}