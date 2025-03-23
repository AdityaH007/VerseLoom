package com.example.verseloom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verseloom.database.Draft
import com.example.verseloom.database.UserDao
import com.example.verseloom.database.Writing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DraftsViewModel @Inject constructor(
    private val userDao: UserDao,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // List of drafts
    private val _drafts = MutableLiveData<List<Draft>>(emptyList())
    val drafts: LiveData<List<Draft>> get() = _drafts

    // Error messages
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    init {
        loadDrafts()
    }

    fun loadDrafts() {
        val uid = auth.currentUser?.uid ?: run {
            _errorMessage.postValue("User not authenticated")
            return
        }
        viewModelScope.launch {
            try {
                // Fetch from Room
                val localDrafts = withContext(Dispatchers.IO) {
                    userDao.getWritingsForUser(uid).map { writing ->
                        Draft(
                            id = writing.id.toString(),
                            content = writing.content,
                            timestamp = writing.lastModified,
                            source = "Room"
                        )
                    }
                }

                // Fetch from Firestore
                val firestoreDrafts = firestore.collection("users").document(uid)
                    .collection("works").get().await().documents.mapNotNull { doc ->
                        val content = doc.getString("content") ?: return@mapNotNull null
                        val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
                        Draft(
                            id = doc.id,
                            content = content,
                            timestamp = timestamp,
                            source = "Firestore"
                        )
                    }

                // Combine and deduplicate drafts (favoring Firestore if duplicates exist)
                val allDrafts = mutableListOf<Draft>()
                allDrafts.addAll(firestoreDrafts)

                // Add local drafts that aren't in Firestore (based on content and timestamp)
                localDrafts.forEach { localDraft ->
                    if (firestoreDrafts.none { it.content == localDraft.content && it.timestamp == localDraft.timestamp }) {
                        allDrafts.add(localDraft)
                    }
                }

                // Sort by timestamp (newest first) and convert to mutable list
                val sortedDrafts = allDrafts.sortedByDescending { it.timestamp }.toMutableList()
                _drafts.postValue(sortedDrafts)

                // Sync local drafts to Firestore if they don't exist
                localDrafts.forEach { localDraft ->
                    if (firestoreDrafts.none { it.content == localDraft.content && it.timestamp == localDraft.timestamp }) {
                        val data = hashMapOf(
                            "content" to localDraft.content,
                            "timestamp" to localDraft.timestamp
                        )
                        firestore.collection("users").document(uid)
                            .collection("works").document(localDraft.id).set(data).await()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to load drafts: ${e.message}")
            }
        }
    }
}