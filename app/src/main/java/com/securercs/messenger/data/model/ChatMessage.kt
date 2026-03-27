package com.securercs.messenger.data.model

enum class MessageStatus { SENDING, SENT, DELIVERED, READ }

data class ChatMessage(
    val id: String,
    val content: String,
    val timestamp: String,
    val isOutgoing: Boolean,
    val status: MessageStatus,
    val service: String,
)
