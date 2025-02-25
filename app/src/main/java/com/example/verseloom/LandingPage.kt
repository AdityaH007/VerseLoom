package com.example.verseloom

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LandingPage : AppCompatActivity() {

    private lateinit var profileBtn: ImageButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var nameTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        profileBtn = findViewById(R.id.Profile)
        profileBtn.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        nameTextView = findViewById(R.id.textView2)
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Initial load
        loadSavedNameWithCoroutine()
    }

    // Override onResume to update the name every time the activity comes to the foreground
    override fun onResume() {
        super.onResume()
        // Reload name data when returning to this activity
        loadSavedNameWithCoroutine()
    }

    private fun loadSavedNameWithCoroutine() {
        lifecycleScope.launch {
            // Perform the data loading in the background
            val savedName = withContext(Dispatchers.IO) {
                // This block runs on the IO thread
                sharedPreferences.getString("USER_NAME", "Venom Snake") // Default name if none saved
            }

            // Update UI on the main thread
            withContext(Dispatchers.Main) {
                nameTextView.text = savedName
            }
        }
    }
}