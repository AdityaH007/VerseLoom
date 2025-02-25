package com.example.verseloom

data class User (
    val userID: String = "",
    val name: String = "",
    val imageUrl: String = ""
) {
    constructor(): this("","","")
}