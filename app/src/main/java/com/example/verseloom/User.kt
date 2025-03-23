package com.example.verseloom

data class User(
    val uid: String? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val bio: String? = null
)
{
    constructor(): this("","","","")
}