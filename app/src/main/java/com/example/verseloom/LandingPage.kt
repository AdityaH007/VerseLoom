package com.example.verseloom

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.verseloom.database.UserViewModel
import com.example.verseloom.view.AIHelpActivity
import com.example.verseloom.view.DraftActivity
import com.example.verseloom.view.ExploreActivity
import com.example.verseloom.view.WorksActivity
import com.example.verseloom.view.WriteActivity
import com.example.verseloom.viewmodel.LandingViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class LandingPage : AppCompatActivity() {

    private lateinit var profileBtn: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var nameTextView: TextView
    private val viewModel: UserViewModel by viewModels()
    private val viewModell: LandingViewModel by viewModels()
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_landing_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI elements
        profileBtn = findViewById(R.id.Profile)
        nameTextView = findViewById(R.id.textView2)
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        bottomNavigation = findViewById(R.id.bottom_navigation)

        bottomNavigation.selectedItemId = R.id.nav_write
        //default


        //navigation listnere
        bottomNavigation.setOnItemSelectedListener { item ->
            viewModell.onNavigationItemSelected(item.itemId)
            true

        }

        //Observe the selected item to update and navigate
        viewModell.sellectedItem.observe(this) { navigationItem ->
            when (navigationItem)
            {
                LandingViewModel.NavigationItem.AI_HELP ->
                {
                    startActivity(Intent(this, AIHelpActivity::class.java))

                }

                LandingViewModel.NavigationItem.DRAFTS ->
                {
                    startActivity(Intent(this, DraftActivity::class.java))

                }

                LandingViewModel.NavigationItem.WRITE ->
                {
                    startActivity(Intent(this, WriteActivity::class.java))

                }

                LandingViewModel.NavigationItem.WORKS ->
                {
                    startActivity(Intent(this, WorksActivity::class.java))

                }

                LandingViewModel.NavigationItem.EXPLORE ->
                {
                    startActivity(Intent(this, ExploreActivity::class.java))

                }
            }

        }



        // Set up profile button click listener
        profileBtn.setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        // Collect users data from ViewModel
        lifecycleScope.launch {
            viewModel.allUsers.collectLatest { users ->
                // Update UI with users list (implement UI update logic here)
            }
        }

        // Load saved name initially
        loadSavedNameWithCoroutine()
    }

    override fun onResume() {
        super.onResume()
        // Reload name data when returning to this activity
        loadSavedNameWithCoroutine()
    }

    private fun loadSavedNameWithCoroutine() {
        lifecycleScope.launch {
            val savedName = withContext(Dispatchers.IO) {
                sharedPreferences.getString("USER_NAME", "Venom Snake") ?: "Venom Snake"
            }

            nameTextView.text = savedName
        }
    }
}
