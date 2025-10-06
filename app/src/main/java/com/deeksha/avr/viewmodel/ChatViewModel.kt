package com.deeksha.avr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.Chat
import com.deeksha.avr.model.ChatMember
import com.deeksha.avr.model.Message
import com.deeksha.avr.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatState(
    val teamMembers: List<ChatMember> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentChatId: String = "",
    val currentChatUser: ChatMember? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    // Load team members for a project
    fun loadTeamMembers(projectId: String, currentUserId: String) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "Loading team members for project: $projectId, user: $currentUserId")
            _chatState.value = _chatState.value.copy(isLoading = true, error = null)
            try {
                val members = chatRepository.getProjectTeamMembers(projectId, currentUserId)
                android.util.Log.d("ChatViewModel", "Loaded ${members.size} team members")
                _chatState.value = _chatState.value.copy(
                    teamMembers = members,
                    isLoading = false
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Error loading team members", e)
                _chatState.value = _chatState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Start a chat with a user
    fun startChat(projectId: String, currentUserId: String, otherUserId: String, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isLoading = true)
            try {
                val chatId = chatRepository.getOrCreateChat(projectId, currentUserId, otherUserId)
                val user = chatRepository.getUserById(otherUserId)
                _chatState.value = _chatState.value.copy(
                    currentChatId = chatId,
                    currentChatUser = user,
                    isLoading = false
                )
                onChatCreated(chatId)
            } catch (e: Exception) {
                _chatState.value = _chatState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Load messages for a chat
    fun loadMessages(projectId: String, chatId: String) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "Starting to collect messages for project: $projectId, chat: $chatId")
            chatRepository.getChatMessages(projectId, chatId).collect { messages ->
                android.util.Log.d("ChatViewModel", "Received ${messages.size} messages from repository")
                messages.forEach { message ->
                    android.util.Log.d("ChatViewModel", "Message: ${message.senderName} - ${message.message}")
                }
                _chatState.value = _chatState.value.copy(messages = messages)
            }
        }
    }

    // Send a message
    fun sendMessage(projectId: String, chatId: String, senderId: String, senderName: String, senderRole: String, message: String, context: android.content.Context? = null) {
        viewModelScope.launch {
            chatRepository.sendMessage(projectId, chatId, senderId, senderName, senderRole, message, context = context)
        }
    }

    // Send an image message
    suspend fun sendImageMessage(projectId: String, chatId: String, senderId: String, senderName: String, senderRole: String, imageUri: android.net.Uri): Boolean {
        return try {
            android.util.Log.d("ChatViewModel", "Starting to send image message")
            val success = chatRepository.sendImageMessage(projectId, chatId, senderId, senderName, senderRole, imageUri)
            android.util.Log.d("ChatViewModel", "Image message send result: $success")
            if (!success) {
                android.util.Log.e("ChatViewModel", "Failed to send image message")
                _chatState.value = _chatState.value.copy(error = "Failed to send image message")
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Error sending image message: ${e.message}", e)
            _chatState.value = _chatState.value.copy(error = "Error sending image message: ${e.message}")
            false
        }
    }

    // Non-suspend wrapper for sending image message
    fun sendImageMessageAsync(projectId: String, chatId: String, senderId: String, senderName: String, senderRole: String, imageUri: android.net.Uri) {
        viewModelScope.launch {
            sendImageMessage(projectId, chatId, senderId, senderName, senderRole, imageUri)
        }
    }

    // Mark messages as read
    fun markMessagesAsRead(projectId: String, chatId: String, userId: String) {
        viewModelScope.launch {
            chatRepository.markMessagesAsRead(projectId, chatId, userId)
        }
    }

    // Load user chats
    fun loadUserChats(userId: String, projectId: String) {
        viewModelScope.launch {
            chatRepository.getUserChats(userId, projectId).collect { chats ->
                _chatState.value = _chatState.value.copy(chats = chats)
            }
        }
    }

    // Set current chat user
    fun setCurrentChatUser(user: ChatMember?) {
        _chatState.value = _chatState.value.copy(currentChatUser = user)
    }

    // Clear error
    fun clearError() {
        _chatState.value = _chatState.value.copy(error = null)
    }
}
