package com.deeksha.avr.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class Chat(
    @PropertyName("id") val id: String = "",
    @PropertyName("members") val members: List<String> = emptyList(), // List of user IDs in this chat
    @PropertyName("lastMessage") val lastMessage: String = "",
    @PropertyName("lastMessageTime") val lastMessageTime: Timestamp? = null,
    @PropertyName("lastMessageSenderId") val lastMessageSenderId: String = "",
    @PropertyName("unreadCount") val unreadCount: Map<String, Int> = emptyMap(), // userId -> unread count
    @PropertyName("createdAt") val createdAt: Timestamp? = null,
    @PropertyName("updatedAt") val updatedAt: Timestamp? = null
)

data class Message(
    @PropertyName("id") val id: String = "",
    @PropertyName("chatId") val chatId: String = "",
    @PropertyName("senderId") val senderId: String = "",
    @PropertyName("messageType") val messageType: String = "Text", // Text or Media
    @PropertyName("message") val message: String = "",
    @PropertyName("mediaUrl") val mediaUrl: String? = null,
    @PropertyName("timestamp") val timestamp: Timestamp? = null,
    // Additional fields for UI
    @PropertyName("senderName") val senderName: String = "",
    @PropertyName("senderRole") val senderRole: String = "",
    @PropertyName("isRead") val isRead: Boolean = false,
    @PropertyName("readBy") val readBy: List<String> = emptyList()
)

data class ChatMember(
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.USER,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)
