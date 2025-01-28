package com.example.chattingapp.model

data class Chat(
    val chatId: String = "",
    val participants: Map<String, Boolean> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageSenderId: String = ""
)
