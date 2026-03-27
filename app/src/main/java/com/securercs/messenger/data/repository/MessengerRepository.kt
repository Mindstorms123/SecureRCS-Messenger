package com.securercs.messenger.data.repository

import com.securercs.messenger.data.model.ChatMessage
import com.securercs.messenger.data.model.Conversation
import com.securercs.messenger.data.model.MessageStatus
import com.securercs.messenger.data.model.ServiceAccount
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object MessengerRepository {
    private val formatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    // Demo Konversationen
    private val demoConversations = listOf(
        Conversation(
            id = "conv_1",
            contactName = "Alice",
            contactHandle = "@alice:matrix.org",
            lastMessage = "Hey! How are you doing?",
            lastMessageTime = "14:23",
            unreadCount = 2,
            service = "matrix",
            avatarInitials = "A",
            avatarColor = 0xFF7C3AED,
            isOnline = true,
        ),
        Conversation(
            id = "conv_2",
            contactName = "Bob",
            contactHandle = "+49151123456",
            lastMessage = "Thanks for the update! 👍",
            lastMessageTime = "12:15",
            unreadCount = 0,
            service = "whatsapp",
            avatarInitials = "B",
            avatarColor = 0xFF059669,
            isOnline = false,
        ),
        Conversation(
            id = "conv_3",
            contactName = "Charlie",
            contactHandle = "@charlie",
            lastMessage = "See you tomorrow!",
            lastMessageTime = "09:42",
            unreadCount = 0,
            service = "telegram",
            avatarInitials = "C",
            avatarColor = 0xFF0EA5E9,
            isOnline = true,
        ),
        Conversation(
            id = "conv_4",
            contactName = "Diana",
            contactHandle = "diana@example.com",
            lastMessage = "Looking forward to the meeting",
            lastMessageTime = "Gestern",
            unreadCount = 1,
            service = "email",
            avatarInitials = "D",
            avatarColor = 0xFFF59E0B,
            isOnline = false,
        ),
        Conversation(
            id = "conv_5",
            contactName = "RCS Gateway",
            contactHandle = "+49170999999",
            lastMessage = "Message via RCS",
            lastMessageTime = "Montag",
            unreadCount = 0,
            service = "rcs",
            avatarInitials = "R",
            avatarColor = 0xFFEC4899,
            isOnline = false,
        ),
    )

    // Demo Nachrichten für Konversation 1 (Alice)
    private val demoMessages = listOf(
        ChatMessage(
            id = "msg_1",
            content = "Hi Alice! How's it going?",
            timestamp = "10:30",
            isOutgoing = true,
            status = MessageStatus.READ,
            service = "matrix",
        ),
        ChatMessage(
            id = "msg_2",
            content = "Hey! All good here. Just finished the project 🎉",
            timestamp = "10:35",
            isOutgoing = false,
            status = MessageStatus.READ,
            service = "matrix",
        ),
        ChatMessage(
            id = "msg_3",
            content = "That's awesome! Want to grab coffee this week?",
            timestamp = "10:36",
            isOutgoing = true,
            status = MessageStatus.DELIVERED,
            service = "matrix",
        ),
        ChatMessage(
            id = "msg_4",
            content = "Sounds good to me! How about Thursday?",
            timestamp = "10:42",
            isOutgoing = false,
            status = MessageStatus.READ,
            service = "matrix",
        ),
        ChatMessage(
            id = "msg_5",
            content = "Thursday works perfectly! ☕",
            timestamp = "10:43",
            isOutgoing = true,
            status = MessageStatus.READ,
            service = "matrix",
        ),
        ChatMessage(
            id = "msg_6",
            content = "Hey! How are you doing?",
            timestamp = "14:23",
            isOutgoing = false,
            status = MessageStatus.READ,
            service = "matrix",
        ),
    )

    // Verfügbare Dienste
    private val availableServices = mutableListOf(
        ServiceAccount(
            service = "matrix",
            displayName = "Matrix",
            handle = "@myself:matrix.org",
            isConnected = true,
        ),
        ServiceAccount(
            service = "whatsapp",
            displayName = "WhatsApp",
            handle = "+49151999999",
            isConnected = true,
        ),
        ServiceAccount(
            service = "telegram",
            displayName = "Telegram",
            handle = "@myusername",
            isConnected = false,
        ),
        ServiceAccount(
            service = "rcs",
            displayName = "RCS",
            handle = "+49151999999",
            isConnected = true,
        ),
        ServiceAccount(
            service = "email",
            displayName = "E-Mail",
            handle = "user@example.com",
            isConnected = false,
        ),
        ServiceAccount(
            service = "discord",
            displayName = "Discord",
            handle = "MyDiscordUser#1234",
            isConnected = false,
        ),
        ServiceAccount(
            service = "slack",
            displayName = "Slack",
            handle = "@username",
            isConnected = false,
        ),
    )

    fun getConversations(): List<Conversation> = demoConversations

    fun getConversationById(id: String): Conversation? =
        demoConversations.find { it.id == id }

    fun getMessagesForConversation(conversationId: String): List<ChatMessage> {
        return if (conversationId == "conv_1") demoMessages else emptyList()
    }

    fun sendMessage(conversationId: String, content: String): ChatMessage {
        val now = LocalDateTime.now().format(formatter)
        return ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            content = content,
            timestamp = now,
            isOutgoing = true,
            status = MessageStatus.SENT,
            service = getConversationById(conversationId)?.service ?: "unknown",
        )
    }

    fun getAvailableServices(): List<ServiceAccount> = availableServices

    fun connectService(service: String) {
        val index = availableServices.indexOfFirst { it.service == service }
        if (index >= 0) {
            val account = availableServices[index]
            availableServices.removeAt(index)
            availableServices.add(index, account.copy(isConnected = true))
        }
    }

    fun getConnectedServices(): List<ServiceAccount> =
        availableServices.filter { it.isConnected }

    fun getServiceColor(service: String): Long = when (service.lowercase()) {
        "matrix" -> 0xFF7C3AED      // Purple
        "whatsapp" -> 0xFF25D366    // Green
        "telegram" -> 0xFF0EA5E9    // Blue
        "rcs" -> 0xFFEC4899         // Pink
        "email" -> 0xFFF59E0B       // Amber
        "discord" -> 0xFF6366F1     // Indigo
        "slack" -> 0xFFE11D48       // Rose
        else -> 0xFF6B7280          // Gray
    }
}
