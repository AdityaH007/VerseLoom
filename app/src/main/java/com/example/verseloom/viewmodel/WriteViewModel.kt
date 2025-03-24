package com.example.verseloom.viewmodel

import android.view.Gravity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verseloom.database.UserDao
import com.example.verseloom.database.Writing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import android.text.Html

@HiltViewModel
class WriteViewModel @Inject constructor(
    private val userDao: UserDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // Content (stored as HTML)
    private val _content = MutableLiveData<String>("")
    val content: LiveData<String> get() = _content

    // Word Count
    private val _wordCount = MutableLiveData<Int>(0)
    val wordCount: LiveData<Int> get() = _wordCount

    // Font Size
    private val _fontSize = MutableLiveData<Float>(16f) // Default: Medium (16sp)
    val fontSize: LiveData<Float> get() = _fontSize

    // Alignment
    private val _alignment = MutableLiveData<Int>(Gravity.START) // Default: Left
    val alignment: LiveData<Int> get() = _alignment

    // Background Color
    private val _backgroundColor = MutableLiveData<Int>()
    val backgroundColor: LiveData<Int> get() = _backgroundColor

    // Error Messages
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    // Current Writing ID (for Room)
    private var currentWritingId: Long? = null
    private var currentFirestoreDocId: String? = null // For Firestore document ID when editing

    // Debounce save operations
    private var lastSaveTime: Long = 0
    private val saveCooldown = 2000L // 2 seconds cooldown
    private var lastSavedContent: String? = null // Track the last saved content

    fun setCurrentWritingId(writingId: Long?, firestoreDocId: String?) {
        this.currentWritingId = writingId
        this.currentFirestoreDocId = firestoreDocId
    }

    fun loadDraftContent(draftContent: String) {
        _content.value = draftContent
        updateWordCount(draftContent)
        lastSavedContent = draftContent // Set as the last saved content
    }

    fun updateContent(newContent: String) {
        _content.value = newContent
        updateWordCount(newContent)
    }

    private fun updateWordCount(htmlText: String) {
        // Strip HTML tags to count words
        val plainText = Html.fromHtml(htmlText, Html.FROM_HTML_MODE_COMPACT).toString()
        val words = plainText.trim().split("\\s+".toRegex())
        _wordCount.value = if (plainText.isBlank()) 0 else words.size
    }

    fun setFontSize(position: Int) {
        _fontSize.value = when (position) {
            0 -> 14f // Small
            1 -> 16f // Medium
            2 -> 20f // Large
            else -> 16f
        }
    }

    fun setAlignment(position: Int) {
        _alignment.value = when (position) {
            0 -> Gravity.START // Left
            1 -> Gravity.CENTER // Center
            2 -> Gravity.END // Right
            else -> Gravity.START
        }
    }

    fun setBackgroundColor(colorRes: Int) {
        _backgroundColor.value = colorRes
    }

    fun saveContent(isAutoSave: Boolean = false) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSaveTime < saveCooldown) {
            // Skip save if within cooldown period
            return
        }

        val contentToSave = _content.value?.takeIf { it.isNotBlank() } ?: run {
            if (!isAutoSave) _errorMessage.postValue("Content is empty")
            return
        }

        // Skip auto-save if content hasn't changed
        if (isAutoSave && contentToSave == lastSavedContent) {
            return
        }

        val uid = auth.currentUser?.uid ?: run {
            if (!isAutoSave) _errorMessage.postValue("User not authenticated")
            return
        }

        viewModelScope.launch {
            try {
                // Save to Room
                val writing = Writing(
                    id = currentWritingId ?: 0, // Use existing ID if editing, otherwise 0 for new
                    userId = uid,
                    content = contentToSave, // Store as HTML
                    lastModified = System.currentTimeMillis()
                )
                val insertedId = withContext(Dispatchers.IO) {
                    userDao.upsertWriting(writing)
                }
                if (currentWritingId == null) {
                    currentWritingId = insertedId // Update ID only if creating a new entry
                }

                // Save to Firestore
                val data = hashMapOf(
                    "content" to contentToSave, // Store as HTML
                    "timestamp" to System.currentTimeMillis()
                )
                if (currentFirestoreDocId != null) {
                    // Update existing Firestore document
                    firestore.collection("users").document(uid)
                        .collection("works").document(currentFirestoreDocId!!).set(data).await()
                } else {
                    // Create new Firestore document
                    val docRef = firestore.collection("users").document(uid)
                        .collection("works").document()
                    docRef.set(data).await()
                    currentFirestoreDocId = docRef.id // Update Firestore document ID
                }

                lastSaveTime = System.currentTimeMillis()
                lastSavedContent = contentToSave
                if (!isAutoSave) _errorMessage.postValue("Saved successfully")
            } catch (e: Exception) {
                if (!isAutoSave) _errorMessage.postValue("Failed to save: ${e.message}")
            }
        }
    }

    fun publishContent(userName: String) {
        val uid = auth.currentUser?.uid ?: run {
            _errorMessage.postValue("User not authenticated")
            return
        }
        val contentToPublish = _content.value?.takeIf { it.isNotBlank() } ?: run {
            _errorMessage.postValue("No content to publish")
            return
        }
        viewModelScope.launch {
            try {
                val data = hashMapOf(
                    "content" to contentToPublish, // Store as HTML
                    "userName" to userName,
                    "timestamp" to System.currentTimeMillis(),
                    "comments" to emptyList<String>(),
                    "likes" to 0,
                    "likedBy" to emptyList<String>()
                )
                firestore.collection("published_works").add(data).await()
                _errorMessage.postValue("Published successfully")
                updateContent("") // Clear the EditText after publishing
                // Reset IDs after publishing
                currentWritingId = null
                currentFirestoreDocId = null
                lastSavedContent = null
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to publish: ${e.message}")
            }
        }
    }
}