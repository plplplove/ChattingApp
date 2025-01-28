package com.example.chattingapp.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val seen: Boolean = false
)
