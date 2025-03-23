package com.example.verseloom

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

    // Current Writing ID (for Room) - No longer needed for saving
    private var currentWritingId: Long? = null

    init {
        // Start with a blank slate
    }

    fun loadDraftContent(draftContent: String) {
        _content.value = draftContent
        updateWordCount(draftContent)
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

    fun saveContent() {
        val uid = auth.currentUser?.uid ?: run {
            _errorMessage.postValue("User not authenticated")
            return
        }
        val contentToSave = _content.value?.takeIf { it.isNotBlank() } ?: run {
            _errorMessage.postValue("Content is empty")
            return
        }
        viewModelScope.launch {
            try {
                // Save to Room - Always create a new Writing instance
                val writing = Writing(
                    id = 0, // Let Room auto-generate a new ID
                    userId = uid,
                    content = contentToSave, // Store as HTML
                    lastModified = System.currentTimeMillis()
                )
                val insertedId = withContext(Dispatchers.IO) {
                    userDao.upsertWriting(writing)
                }
                // Update currentWritingId for reference (optional)
                currentWritingId = insertedId

                // Save to Firestore under users/{uid}/works
                val data = hashMapOf(
                    "content" to contentToSave, // Store as HTML
                    "timestamp" to System.currentTimeMillis()
                )
                firestore.collection("users").document(uid)
                    .collection("works").document(UUID.randomUUID().toString()).set(data).await()

                _errorMessage.postValue("Saved successfully")
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to save: ${e.message}")
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
                    "comments" to emptyList<String>()
                )
                firestore.collection("published_works").add(data).await()
                _errorMessage.postValue("Published successfully")
                updateContent("") // Clear the EditText after publishing
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to publish: ${e.message}")
            }
        }
    }
}