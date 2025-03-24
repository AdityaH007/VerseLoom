package com.example.verseloom.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.verseloom.database.PublishedWork
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _publishedWorks = MutableLiveData<List<PublishedWork>>(emptyList())
    val publishedWorks: LiveData<List<PublishedWork>> get() = _publishedWorks

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    init {
        loadPublishedWorks()
    }

    fun loadPublishedWorks() {
        viewModelScope.launch {
            try {
                val documents = firestore.collection("published_works")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get().await()
                val works = documents.documents.mapNotNull { doc ->
                    val content = doc.getString("content") ?: return@mapNotNull null
                    val userName = doc.getString("userName") ?: "Anonymous"
                    val timestamp = doc.getLong("timestamp") ?: return@mapNotNull null
                    val comments = doc.get("comments") as? List<String> ?: emptyList()
                    val likes = doc.getLong("likes")?.toInt() ?: 0
                    val likedBy = doc.get("likedBy") as? List<String> ?: emptyList()
                    PublishedWork(
                        id = doc.id,
                        content = content,
                        userName = userName,
                        timestamp = timestamp,
                        comments = comments,
                        likes = likes,
                        likedBy = likedBy
                    )
                }
                _publishedWorks.postValue(works)
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to load published works: ${e.message}")
            }
        }
    }

    fun likeWork(workId: String, isLiked: Boolean) {
        val uid = auth.currentUser?.uid ?: run {
            _errorMessage.postValue("User not authenticated")
            return
        }
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("published_works").document(workId)
                if (isLiked) {
                    // Unlike: Decrement likes and remove user from likedBy
                    docRef.update(
                        mapOf(
                            "likes" to FieldValue.increment(-1),
                            "likedBy" to FieldValue.arrayRemove(uid)
                        )
                    ).await()
                } else {
                    // Like: Increment likes and add user to likedBy
                    docRef.update(
                        mapOf(
                            "likes" to FieldValue.increment(1),
                            "likedBy" to FieldValue.arrayUnion(uid)
                        )
                    ).await()
                }
                // Reload works to reflect the updated like count
                loadPublishedWorks()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to update like: ${e.message}")
            }
        }
    }

    fun addComment(workId: String, comment: String) {
        if (comment.isBlank()) {
            _errorMessage.postValue("Comment cannot be empty")
            return
        }
        val uid = auth.currentUser?.uid ?: run {
            _errorMessage.postValue("User not authenticated")
            return
        }
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("published_works").document(workId)
                val userName = auth.currentUser?.email?.split("@")?.get(0) ?: "Anonymous"
                val formattedComment = "$userName: $comment"
                docRef.update("comments", FieldValue.arrayUnion(formattedComment)).await()
                // Reload works to reflect the new comment
                loadPublishedWorks()
            } catch (e: Exception) {
                _errorMessage.postValue("Failed to add comment: ${e.message}")
            }
        }
    }
}