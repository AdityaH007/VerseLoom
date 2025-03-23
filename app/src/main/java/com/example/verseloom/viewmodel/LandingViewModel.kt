package com.example.verseloom.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.verseloom.R

@HiltViewModel
class LandingViewModel @Inject constructor() : ViewModel() {

    // Enum to represent the navigation items
    enum class NavigationItem {
        AI_HELP, DRAFTS, WRITE, WORKS, EXPLORE
    }

    // LiveData to hold currently selected navigation item
    private val _selectedItem = MutableLiveData<NavigationItem>(NavigationItem.DRAFTS) // Default to WRITE
    val sellectedItem: LiveData<NavigationItem> get() = _selectedItem

    // Function to update the selected item when a tab is clicked
    fun onNavigationItemSelected(itemId: Int) {
        val newItem = when (itemId) {
            R.id.nav_ai_help -> NavigationItem.AI_HELP
            R.id.nav_dashboard -> NavigationItem.DRAFTS
            R.id.nav_write -> NavigationItem.WRITE
            R.id.nav_works -> NavigationItem.WORKS
            R.id.nav_explore -> NavigationItem.EXPLORE
            else -> return
        }
        // Use `.value` to update the LiveData properly
        _selectedItem.value = newItem
    }
}
