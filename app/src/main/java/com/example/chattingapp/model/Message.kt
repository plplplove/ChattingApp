package com.example.chattingapp.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val seen: Boolean = false,
    val type: String = "text", // can be "text", "image", or "file"
    val fileName: String = "", // for file messages
    val fileUrl: String = "" // for both image and file messages
)
