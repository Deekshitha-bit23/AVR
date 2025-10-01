package com.deeksha.avr.repository

import android.util.Log
import com.deeksha.avr.model.Chat
import com.deeksha.avr.model.ChatMember
import com.deeksha.avr.model.Message
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationType
import com.deeksha.avr.model.Project
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.repository.NotificationRepository
import com.deeksha.avr.service.MessageNotificationService
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository,
    private val messageNotificationService: MessageNotificationService
) {
    private val usersCollection = firestore.collection("users")
    private val projectsCollection = firestore.collection("projects")
    
    // Helper functions to get subcollections
    private fun getChatsCollection(projectId: String) = 
        projectsCollection.document(projectId).collection("chats")
    
    private fun getMessagesCollection(projectId: String, chatId: String) = 
        projectsCollection.document(projectId).collection("chats").document(chatId).collection("messages")

    // Get all team members for a specific project
    suspend fun getProjectTeamMembers(projectId: String, currentUserId: String): List<ChatMember> {
        return try {
            Log.d("ChatRepository", "Fetching team members for project: $projectId, currentUserId: $currentUserId")
            
            val project = projectsCollection.document(projectId).get().await()
            if (!project.exists()) {
                Log.e("ChatRepository", "Project document does not exist: $projectId")
                return emptyList()
            }
            
            val projectData = project.toObject(Project::class.java)
            if (projectData == null) {
                Log.e("ChatRepository", "Failed to parse project data")
                return emptyList()
            }
            
            Log.d("ChatRepository", "Project data - Approvers: ${projectData.approverIds.size}, " +
                    "Production Heads (from project): ${projectData.productionHeadIds.size}, " +
                    "Team Members: ${projectData.teamMembers.size}, " +
                    "Manager: ${projectData.managerId}")
            
            val allMemberIds = mutableSetOf<String>()
            
            // Add approvers
            allMemberIds.addAll(projectData.approverIds)
            
            // Add production heads from project data
            allMemberIds.addAll(projectData.productionHeadIds)
            
            // Also add any user with PRODUCTION_HEAD role (in case not assigned to project)
            // We'll fetch all users and filter by role later
            
            // Add team members (users)
            allMemberIds.addAll(projectData.teamMembers)
            
            // Add manager if exists
            if (projectData.managerId.isNotEmpty()) {
                allMemberIds.add(projectData.managerId)
            }
            
            Log.d("ChatRepository", "Total unique member IDs before filtering: ${allMemberIds.size}")
            Log.d("ChatRepository", "Member IDs: $allMemberIds")
            Log.d("ChatRepository", "Current user ID to remove: $currentUserId")
            
            // Remove current user from the list
            val removed = allMemberIds.remove(currentUserId)
            
            Log.d("ChatRepository", "Was current user removed? $removed")
            Log.d("ChatRepository", "Member IDs after removing current user: ${allMemberIds.size}")
            Log.d("ChatRepository", "Remaining member IDs: $allMemberIds")
            
            // Fetch user details
            val members = mutableListOf<ChatMember>()
            for (userId in allMemberIds) {
                try {
                    Log.d("ChatRepository", "Fetching user details for: $userId (currentUserId=$currentUserId, match=${userId == currentUserId})")
                    val userDoc = usersCollection.document(userId).get().await()
                    val userData = userDoc.data
                    
                    if (userData != null) {
                        val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                        val isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: false
                        val lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: 0L
                        
                        val roleString = userData["role"] as? String
                        val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                            "APPROVER" -> UserRole.APPROVER
                            "PRODUCTION_HEAD" -> UserRole.PRODUCTION_HEAD
                            "ADMIN" -> UserRole.ADMIN
                            else -> UserRole.USER
                        }
                        
                        val member = ChatMember(
                            userId = userId,
                            name = userData["name"] as? String ?: "Unknown",
                            phone = userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: "",
                            role = userRole,
                            isOnline = isOnline,
                            lastSeen = lastLoginAt
                        )
                        
                        // Double check - skip if this is somehow the current user
                        if (userId == currentUserId) {
                            Log.w("ChatRepository", "Skipping current user that wasn't filtered: ${member.name}")
                            continue
                        }
                        
                        members.add(member)
                        Log.d("ChatRepository", "Added member: ${member.name} (${member.role}) with ID: $userId")
                    } else {
                        Log.w("ChatRepository", "User data is null for userId: $userId")
                    }
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error fetching user $userId: ${e.message}", e)
                }
            }
            
            // Add Production Head if not already in the list
            val productionHead = getProductionHeadByRole()
            if (productionHead != null && !allMemberIds.contains(productionHead.userId)) {
                members.add(productionHead)
                Log.d("ChatRepository", "Added Production Head by role: ${productionHead.name} (${productionHead.role})")
            }
            
            Log.d("ChatRepository", "Total members fetched: ${members.size}")
            members.sortedBy { it.name }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting project team members: ${e.message}", e)
            emptyList()
        }
    }

    // Helper function to get the correct user ID for chat operations
    private suspend fun getChatUserId(userId: String): String {
        return try {
            // First try to get user by phone number
            val userByPhone = usersCollection.document(userId).get().await()
            if (userByPhone.exists()) {
                Log.d("ChatRepository", "Found user by phone number: $userId -> ${userByPhone.id}")
                return userByPhone.id
            }
            
            // If not found, try to find by phone number in the document
            val usersByPhone = usersCollection
                .whereEqualTo("phoneNumber", userId)
                .get()
                .await()
            
            if (!usersByPhone.isEmpty) {
                val docId = usersByPhone.documents.first().id
                Log.d("ChatRepository", "Found user by phoneNumber field: $userId -> $docId")
                return docId
            }
            
            // If still not found, try the phone field
            val usersByPhoneField = usersCollection
                .whereEqualTo("phone", userId)
                .get()
                .await()
            
            if (!usersByPhoneField.isEmpty) {
                val docId = usersByPhoneField.documents.first().id
                Log.d("ChatRepository", "Found user by phone field: $userId -> $docId")
                return docId
            }
            
            Log.w("ChatRepository", "Could not find user ID for: $userId")
            userId // Return original if not found
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting chat user ID: ${e.message}")
            userId
        }
    }

    // Get or create a chat between two users for a specific project
    suspend fun getOrCreateChat(projectId: String, currentUserId: String, otherUserId: String): String {
        return try {
            val chatsCollection = getChatsCollection(projectId)
            
            // Get the correct user IDs for chat operations
            val currentChatUserId = getChatUserId(currentUserId)
            val otherChatUserId = getChatUserId(otherUserId)
            
            Log.d("ChatRepository", "Looking for existing chat between $currentUserId ($currentChatUserId) and $otherUserId ($otherChatUserId)")
            
            // Check if chat already exists - look for any chat containing both users
            val existingChats = chatsCollection
                .whereArrayContains("members", currentChatUserId)
                .get()
                .await()
            
            Log.d("ChatRepository", "Found ${existingChats.documents.size} chats containing current user")
            
            val existingChat = existingChats.documents.firstOrNull { doc ->
                val members = doc.get("members") as? List<*>
                val containsOtherUser = members?.contains(otherChatUserId) == true
                Log.d("ChatRepository", "Chat ${doc.id} has members: $members, contains other user: $containsOtherUser")
                containsOtherUser
            }
            
            if (existingChat != null) {
                Log.d("ChatRepository", "Found existing chat: ${existingChat.id}")
                existingChat.id
            } else {
                Log.d("ChatRepository", "No existing chat found, creating new one")
                // Create new chat
                val chatData = hashMapOf(
                    "members" to listOf(currentChatUserId, otherChatUserId),
                    "lastMessage" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "lastMessageSenderId" to "",
                    "unreadCount" to mapOf(currentChatUserId to 0, otherChatUserId to 0),
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
                
                val chatRef = chatsCollection.add(chatData).await()
                Log.d("ChatRepository", "Created new chat: ${chatRef.id}")
                chatRef.id
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating chat: ${e.message}")
            ""
        }
    }

    // Send a message
    suspend fun sendMessage(
        projectId: String,
        chatId: String, 
        senderId: String, 
        senderName: String, 
        senderRole: String, 
        message: String,
        messageType: String = "Text",
        mediaUrl: String? = null,
        context: android.content.Context? = null
    ): Boolean {
        return try {
            val messagesCollection = getMessagesCollection(projectId, chatId)
            val chatsCollection = getChatsCollection(projectId)
            
            val messageData = hashMapOf(
                "chatId" to chatId,
                "senderId" to senderId,
                "messageType" to messageType,
                "message" to message,
                "mediaUrl" to mediaUrl,
                "timestamp" to Timestamp.now(),
                // Additional fields for UI
                "senderName" to senderName,
                "senderRole" to senderRole,
                "isRead" to false,
                "readBy" to listOf(senderId)
            )
            
            messagesCollection.add(messageData).await()
            
            // Update chat's last message
            chatsCollection.document(chatId).update(
                mapOf(
                    "lastMessage" to message,
                    "lastMessageTime" to Timestamp.now(),
                    "lastMessageSenderId" to senderId,
                    "updatedAt" to Timestamp.now()
                )
            ).await()
            
            // Increment unread count for other members
            val chat = chatsCollection.document(chatId).get().await()
            val members = (chat.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val unreadCount = (chat.get("unreadCount") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value } ?: emptyMap()
            val updatedUnreadCount = mutableMapOf<String, Any>()
            unreadCount.forEach { (k, v) -> updatedUnreadCount[k] = v ?: 0 }
            
            members.forEach { memberId ->
                if (memberId != senderId) {
                    val currentCount = (updatedUnreadCount[memberId] as? Number)?.toInt() ?: 0
                    updatedUnreadCount[memberId] = currentCount + 1
                }
            }
            
            chatsCollection.document(chatId).update("unreadCount", updatedUnreadCount).await()
            
            // Send notifications to other members
            val otherMembers = members.filter { it != senderId }
            if (otherMembers.isNotEmpty()) {
                Log.d("ChatRepository", "Creating notifications for ${otherMembers.size} members")
                
                // Get project name for notification
                val projectDoc = projectsCollection.document(projectId).get().await()
                val projectName = projectDoc.get("name") as? String ?: "Unknown Project"
                
                // Create notifications for each receiver
                for (receiverId in otherMembers) {
                    try {
                        // Get the normalized user ID for notification (same as chat creation)
                        val normalizedReceiverId = getChatUserId(receiverId)
                        Log.d("ChatRepository", "Creating notification for receiver: $receiverId -> $normalizedReceiverId")
                        
                        // Get receiver's role for notification
                        val receiverDoc = usersCollection.document(normalizedReceiverId).get().await()
                        val receiverRole = receiverDoc.get("role") as? String ?: "USER"
                        
                        val notification = Notification(
                            recipientId = normalizedReceiverId, // Use normalized ID
                            recipientRole = receiverRole,
                            title = "New message from $senderName",
                            message = message,
                            type = NotificationType.CHAT_MESSAGE,
                            projectId = projectId,
                            projectName = projectName,
                            relatedId = chatId,
                            isRead = false,
                            actionRequired = false,
                            navigationTarget = "chat/$projectId/$chatId/$senderName"
                        )
                        
                        notificationRepository.createNotification(notification)
                        Log.d("ChatRepository", "Created notification for receiver: $normalizedReceiverId (role: $receiverRole)")
                        
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error creating notification for $receiverId: ${e.message}")
                    }
                }
                
                // Send FCM push notifications if context is available
                if (context != null) {
                    try {
                        Log.d("ChatRepository", "Sending FCM notifications to ${otherMembers.size} recipients")
                        messageNotificationService.sendMessageNotification(
                            context = context,
                            projectId = projectId,
                            chatId = chatId,
                            senderId = senderId,
                            senderName = senderName,
                            message = message,
                            recipientIds = otherMembers
                        )
                        Log.d("ChatRepository", "FCM notifications sent successfully")
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error sending FCM notifications: ${e.message}")
                    }
                } else {
                    Log.w("ChatRepository", "Context not provided, skipping FCM notifications")
                }
            }
            
            true
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending message: ${e.message}")
            false
        }
    }

    // Send an image message
    suspend fun sendImageMessage(
        projectId: String,
        chatId: String, 
        senderId: String, 
        senderName: String, 
        senderRole: String, 
        imageUri: android.net.Uri
    ): Boolean {
        return try {
            Log.d("ChatRepository", "Starting image upload for project: $projectId, chat: $chatId")
            Log.d("ChatRepository", "Image URI: $imageUri")
            Log.d("ChatRepository", "Image URI scheme: ${imageUri.scheme}")
            Log.d("ChatRepository", "Image URI path: ${imageUri.path}")
            Log.d("ChatRepository", "Sender: $senderName ($senderId)")
            
            // Check if URI is valid
            if (imageUri.toString().isEmpty()) {
                Log.e("ChatRepository", "Image URI is empty")
                return false
            }
            
            // Test if the URI is accessible
            try {
                val context = FirebaseAuth.getInstance().app?.applicationContext
                if (context != null) {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    if (inputStream == null) {
                        Log.e("ChatRepository", "Cannot access image URI")
                        return false
                    }
                    inputStream.close()
                    Log.d("ChatRepository", "Image URI is accessible")
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error accessing image URI: ${e.message}", e)
                return false
            }
            
            // Upload image to Firebase Storage first
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("chat_images/${projectId}/${chatId}/${System.currentTimeMillis()}.jpg")
            
            Log.d("ChatRepository", "Uploading image to: chat_images/${projectId}/${chatId}/")
            
            // Upload the file
            val uploadTask = imageRef.putFile(imageUri)
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                Log.d("ChatRepository", "Upload progress: ${progress.toInt()}%")
            }
            
            // Wait for upload to complete
            val uploadResult = uploadTask.await()
            Log.d("ChatRepository", "Upload completed successfully")
            
            // Get download URL
            val downloadUrl = imageRef.downloadUrl.await()
            Log.d("ChatRepository", "Download URL obtained: $downloadUrl")
            
            if (downloadUrl.toString().isEmpty()) {
                Log.e("ChatRepository", "Download URL is empty")
                return false
            }
            
            // Send message with image URL
            val success = sendMessage(
                projectId = projectId,
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                senderRole = senderRole,
                message = "📷 Image",
                messageType = "Media",
                mediaUrl = downloadUrl.toString()
            )
            
            Log.d("ChatRepository", "Message sent with image URL: $success")
            success
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending image message: ${e.message}", e)
            Log.e("ChatRepository", "Exception type: ${e.javaClass.simpleName}")
            Log.e("ChatRepository", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            false
        }
    }

    // Get messages for a chat
    fun getChatMessages(projectId: String, chatId: String): Flow<List<Message>> = callbackFlow {
        val messagesCollection = getMessagesCollection(projectId, chatId)
        
        Log.d("ChatRepository", "Setting up message listener for project: $projectId, chat: $chatId")
        
        val listener = messagesCollection
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error listening to messages: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                Log.d("ChatRepository", "Received ${snapshot?.documents?.size ?: 0} messages from Firestore")
                
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Log.d("ChatRepository", "Processing message document: ${doc.id}")
                        Log.d("ChatRepository", "Message data: ${doc.data}")
                        
                        val message = doc.toObject(Message::class.java)?.copy(id = doc.id)
                        if (message != null) {
                            Log.d("ChatRepository", "Successfully parsed message: ${message.message} from ${message.senderName}")
                        } else {
                            Log.w("ChatRepository", "Failed to parse message document: ${doc.id}")
                        }
                        message
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error parsing message: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d("ChatRepository", "Sending ${messages.size} messages to UI")
                trySend(messages)
            }
        
        awaitClose { 
            Log.d("ChatRepository", "Removing message listener")
            listener.remove() 
        }
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(projectId: String, chatId: String, userId: String) {
        try {
            val messagesCollection = getMessagesCollection(projectId, chatId)
            val chatsCollection = getChatsCollection(projectId)
            
            val messages = messagesCollection
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()
            
            messages.documents.forEach { doc ->
                val readBy = (doc.get("readBy") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (!readBy.contains(userId)) {
                    messagesCollection.document(doc.id).update(
                        mapOf(
                            "readBy" to (readBy + userId),
                            "isRead" to true
                        )
                    ).await()
                }
            }
            
            // Reset unread count
            val chat = chatsCollection.document(chatId).get().await()
            val unreadCount = (chat.get("unreadCount") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value } ?: emptyMap()
            val updatedUnreadCount = mutableMapOf<String, Any>()
            unreadCount.forEach { (k, v) -> updatedUnreadCount[k] = v ?: 0 }
            updatedUnreadCount[userId] = 0
            
            chatsCollection.document(chatId).update("unreadCount", updatedUnreadCount).await()
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking messages as read: ${e.message}")
        }
    }

    // Get user chats
    fun getUserChats(userId: String, projectId: String): Flow<List<Chat>> = callbackFlow {
        val chatsCollection = getChatsCollection(projectId)
        
        val listener = chatsCollection
            .whereArrayContains("members", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error listening to chats: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Chat::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error parsing chat: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                trySend(chats)
            }
        
        awaitClose { listener.remove() }
    }

    // Get Production Head by role
    private suspend fun getProductionHeadByRole(): ChatMember? {
        return try {
            Log.d("ChatRepository", "Fetching Production Head by role...")
            // Try different role formats
            val roleVariations = listOf("PRODUCTION_HEAD", "Production Head", "production_head", "PRODUCTION HEAD")
            
            for (roleFormat in roleVariations) {
                Log.d("ChatRepository", "Trying role format: '$roleFormat'")
                val querySnapshot = usersCollection
                    .whereEqualTo("role", roleFormat)
                    .get()
                    .await()
            
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    val userData = document.data
                    
                    if (userData != null) {
                        val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                        val isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: false
                        val lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: 0L
                        
                        val member = ChatMember(
                            userId = document.id,
                            name = userData["name"] as? String ?: "Unknown",
                            phone = userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: "",
                            role = UserRole.PRODUCTION_HEAD,
                            isOnline = isOnline,
                            lastSeen = lastLoginAt
                        )
                        
                        Log.d("ChatRepository", "Found Production Head with role '$roleFormat': ${member.name} (${member.userId})")
                        return member
                    } else {
                        Log.w("ChatRepository", "Production Head user data is null for role '$roleFormat'")
                    }
                } else {
                    Log.d("ChatRepository", "No Production Head found with role '$roleFormat'")
                }
            }
            
            Log.w("ChatRepository", "No Production Head found in users collection with any role format")
            null
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching Production Head by role: ${e.message}", e)
            null
        }
    }

    // Get user details by ID
    suspend fun getUserById(userId: String): ChatMember? {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val userData = userDoc.data
            
            if (userData != null) {
                val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                val isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: false
                val lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: 0L
                
                val roleString = userData["role"] as? String
                val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                    "APPROVER" -> UserRole.APPROVER
                    "PRODUCTION_HEAD" -> UserRole.PRODUCTION_HEAD
                    "ADMIN" -> UserRole.ADMIN
                    else -> UserRole.USER
                }
                
                ChatMember(
                    userId = userId,
                    name = userData["name"] as? String ?: "Unknown",
                    phone = userData["phoneNumber"] as? String ?: userData["phone"] as? String ?: "",
                    role = userRole,
                    isOnline = isOnline,
                    lastSeen = lastLoginAt
                )
            } else null
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting user: ${e.message}")
            null
        }
    }
}
