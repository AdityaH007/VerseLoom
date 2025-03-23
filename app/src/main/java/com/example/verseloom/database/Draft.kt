package com.example.verseloom.database

data class Draft(
    val id: String, // Unique ID (Room ID or Firestore document ID)
    val content: String,
    val timestamp: Long,
    val source: String // "Room" or "Firestore" for debugging
)