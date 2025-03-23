package com.example.verseloom.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserData")
data class UserData(
    @PrimaryKey
    val uid: Int,
    val name: String,
    val imageUrl: String?,
    val bio: String?
)