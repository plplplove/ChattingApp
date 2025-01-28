package com.example.chattingapp.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val status: String = "",
    val online: Boolean = false
)
