package com.securercs.messenger.data.model

import androidx.compose.ui.graphics.Color

data class Conversation(
    val id: String,
    val contactName: String,
    val contactHandle: String,
    val lastMessage: String,
    val lastMessageTime: String,
    val unreadCount: Int,
    val service: String,
    val avatarInitials: String,
    val avatarColor: Long,
    val isOnline: Boolean = false,
)
