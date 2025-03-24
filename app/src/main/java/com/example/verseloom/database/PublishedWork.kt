package com.example.verseloom.database

import com.google.firebase.Timestamp

data class PublishedWork(

    val id: String,
    val content: String,
    val userName: String,
    val timestamp: Long,
    val comments: List<String>,
    val likes: Int,
    val likedBy: List<String>
)
