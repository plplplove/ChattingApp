package com.example.chattingapp.model

data class ChatPreview(
    val chatId: String = "",
    val otherUserId: String = "",
    val otherUserName: String = "",
    val otherUserImage: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageSenderId: String = ""
)
