package com.example.verseloom.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "writings")
data class Writing(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Auto-generate unique ID
    val userId: String, // Associate with a user
    val content: String,
    val lastModified: Long
)