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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationRepository: NotificationRepository
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

    // Get expense details from chat ID (for expense approval chats)
    private suspend fun getExpenseFromChatId(projectId: String, chatId: String): com.deeksha.avr.model.Expense? {
        return try {
            // Extract expense ID from chat ID (format: "expense_approval_{expenseId}")
            if (!chatId.startsWith("expense_approval_")) {
                Log.d("ChatRepository", "⚠️ Not an expense approval chat: $chatId")
                return null
            }
            
            val expenseId = chatId.removePrefix("expense_approval_")
            Log.d("ChatRepository", "🔍 Extracted expense ID: $expenseId from chat ID: $chatId")
            
            // Get expense from Firestore
            val expenseDoc = firestore.collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)
                .get()
                .await()
                
            if (!expenseDoc.exists()) {
                Log.e("ChatRepository", "❌ Expense not found: $expenseId")
                return null
            }
            
            // Map document to Expense object
            val expenseData = expenseDoc.data
            if (expenseData == null) {
                Log.e("ChatRepository", "❌ Expense data is null for ID: $expenseId")
                return null
            }
            
            val expense = com.deeksha.avr.model.Expense(
                id = expenseDoc.id,
                projectId = projectId,
                userId = expenseData["userId"] as? String ?: "",
                userName = expenseData["userName"] as? String ?: "",
                date = expenseData["date"] as? com.google.firebase.Timestamp,
                amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                department = expenseData["department"] as? String ?: "",
                category = expenseData["category"] as? String ?: "",
                description = expenseData["description"] as? String ?: "",
                modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                status = when (expenseData["status"] as? String) {
                    "APPROVED" -> com.deeksha.avr.model.ExpenseStatus.APPROVED
                    "REJECTED" -> com.deeksha.avr.model.ExpenseStatus.REJECTED
                    "DRAFT" -> com.deeksha.avr.model.ExpenseStatus.DRAFT
                    else -> com.deeksha.avr.model.ExpenseStatus.PENDING
                },
                submittedAt = expenseData["submittedAt"] as? com.google.firebase.Timestamp,
                reviewedAt = expenseData["reviewedAt"] as? com.google.firebase.Timestamp,
                reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                reviewComments = expenseData["reviewComments"] as? String ?: "",
                receiptNumber = expenseData["receiptNumber"] as? String ?: ""
            )
            
            Log.d("ChatRepository", "✅ Found expense: ${expense.id}, submitted by: ${expense.userId}")
            expense
            
        } catch (e: Exception) {
            Log.e("ChatRepository", "❌ Error getting expense from chat ID: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Get all approvers for a project (for expense approval chats)
    private suspend fun getApproversForExpenseChat(projectId: String, chatId: String): List<String> {
        return try {
            Log.d("ChatRepository", "📋 Getting approvers for project: $projectId, chatId: $chatId")
            
            val chatsCollection = getChatsCollection(projectId)
            val messagesCollection = getMessagesCollection(projectId, chatId)
            
            // First check if chat already exists and has messages from approvers
            val existingChatDoc = chatsCollection.document(chatId).get().await()
            if (existingChatDoc.exists()) {
                Log.d("ChatRepository", "📋 Chat exists, checking for approvers who have sent messages")
                
                // Get all messages in this chat
                val messages = messagesCollection.get().await()
                
                // Find unique sender IDs who are approvers (have sent messages in this chat)
                val approverSenders = mutableSetOf<String>()
                for (messageDoc in messages.documents) {
                    val senderId = messageDoc.get("senderId") as? String
                    if (senderId != null) {
                        try {
                            // Check if this sender is an approver
                            val userDoc = usersCollection.document(senderId).get().await()
                            val role = userDoc.get("role") as? String
                            if (role != null && (role.uppercase().contains("APPROVER") || role.uppercase().contains("PRODUCTION"))) {
                                approverSenders.add(senderId)
                                Log.d("ChatRepository", "📋 Found approver who has sent messages: $senderId (role: $role)")
                            }
                        } catch (e: Exception) {
                            Log.e("ChatRepository", "Error checking sender role: ${e.message}")
                        }
                    }
                }
                
                // If there are approvers who have actively participated, only notify them
                if (approverSenders.isNotEmpty()) {
                    Log.d("ChatRepository", "📋 Using ${approverSenders.size} approvers who have participated in chat")
                    return approverSenders.toList()
                }
                
                Log.d("ChatRepository", "📋 No approvers have sent messages yet, will notify all project approvers")
            }
            
            // If chat doesn't exist or no approvers have participated yet, get all project approvers
            Log.d("ChatRepository", "📋 Getting all approvers for project: $projectId")
            
            // Get project details
            val projectDoc = projectsCollection.document(projectId).get().await()
            if (!projectDoc.exists()) {
                Log.e("ChatRepository", "❌ Project not found: $projectId")
                return emptyList()
            }
            
            val approvers = mutableSetOf<String>()
            
            // Get regular approvers
            val approverIds = projectDoc.get("approverIds") as? List<*>
            approverIds?.filterIsInstance<String>()?.forEach { approverId ->
                approvers.add(approverId)
                Log.d("ChatRepository", "📋 Added regular approver: $approverId")
            }
            
            // Get production heads
            val productionHeadIds = projectDoc.get("productionHeadIds") as? List<*>
            productionHeadIds?.filterIsInstance<String>()?.forEach { prodHeadId ->
                approvers.add(prodHeadId)
                Log.d("ChatRepository", "📋 Added production head: $prodHeadId")
            }
            
            // Get temporary approver
            val temporaryApproverPhone = projectDoc.get("temporaryApproverPhone") as? String
            if (!temporaryApproverPhone.isNullOrEmpty()) {
                approvers.add(temporaryApproverPhone)
                Log.d("ChatRepository", "📋 Added temporary approver: $temporaryApproverPhone")
            }
            
            Log.d("ChatRepository", "📋 Total approvers found: ${approvers.size}")
            approvers.toList()
        } catch (e: Exception) {
            Log.e("ChatRepository", "❌ Error getting approvers for expense chat: ${e.message}")
            emptyList()
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
        // Wrap entire function in NonCancellable to ensure it completes
        return withContext(NonCancellable) {
            try {
                Log.d("ChatRepository", "🚀 ========== SEND MESSAGE START ==========")
                Log.d("ChatRepository", "🚀 ProjectId: $projectId")
                Log.d("ChatRepository", "🚀 ChatId: $chatId")
                Log.d("ChatRepository", "🚀 SenderId: $senderId")
                Log.d("ChatRepository", "🚀 SenderName: $senderName")
                Log.d("ChatRepository", "🚀 SenderRole: $senderRole")
                Log.d("ChatRepository", "🚀 Message: $message")
                Log.d("ChatRepository", "🚀 Is Expense Chat: ${chatId.startsWith("expense_approval_")}")
            
            val messagesCollection = getMessagesCollection(projectId, chatId)
            val chatsCollection = getChatsCollection(projectId)
            
            // Check if chat exists, if not create it
            val chatDoc = chatsCollection.document(chatId).get().await()
            if (!chatDoc.exists()) {
                Log.d("ChatRepository", "🆕 Chat $chatId does not exist, creating it")
                Log.d("ChatRepository", "🆕 ChatId starts with 'expense_approval_': ${chatId.startsWith("expense_approval_")}")
                
                // FOR EXPENSE APPROVAL CHATS: Include both user and approver in initial chat creation
                val initialMembers = if (chatId.startsWith("expense_approval_")) {
                    Log.d("ChatRepository", "📋 ========== NEW EXPENSE CHAT CREATION ==========")
                    Log.d("ChatRepository", "📋 Chat ID: $chatId")
                    Log.d("ChatRepository", "📋 Project ID: $projectId")
                    Log.d("ChatRepository", "📋 Sender ID: $senderId")
                    Log.d("ChatRepository", "📋 Sender Name: $senderName")
                    Log.d("ChatRepository", "📋 Sender Role: $senderRole")
                    
                    try {
                        val members = mutableSetOf<String>()
                        
                        // Add the current sender
                        members.add(senderId)
                        Log.d("ChatRepository", "📋 ✅ Added sender to members: $senderId")
                        
                        // Get expense details to find the user who submitted it
                        val expense = getExpenseFromChatId(projectId, chatId)
                        if (expense != null) {
                            Log.d("ChatRepository", "📋 Found expense: ${expense.id}")
                            Log.d("ChatRepository", "📋 Expense submitter: ${expense.userId}")
                            Log.d("ChatRepository", "📋 Expense status: ${expense.status}")
                            
                            // ALWAYS add expense submitter to ensure they receive notifications
                            if (expense.userId != senderId) {
                                members.add(expense.userId)
                                Log.d("ChatRepository", "📋 ✅ Added expense submitter to members: ${expense.userId}")
                            } else {
                                Log.d("ChatRepository", "📋 ✓ Sender is the expense submitter")
                            }
                            
                            // Get project details to find approvers/manager
                            val projectDoc = projectsCollection.document(projectId).get().await()
                            if (projectDoc.exists()) {
                                // Add manager if exists
                                val managerId = projectDoc.get("managerId") as? String
                                Log.d("ChatRepository", "📋 Project manager: ${managerId ?: "none"}")
                                if (!managerId.isNullOrEmpty()) {
                                    members.add(managerId)
                                    Log.d("ChatRepository", "📋 ✅ Added project manager to members: $managerId")
                                }
                                
                                // CRITICAL: Add ALL approvers to ensure notifications work
                                val approverIds = projectDoc.get("approverIds") as? List<*>
                                if (approverIds != null && approverIds.isNotEmpty()) {
                                    Log.d("ChatRepository", "📋 Found ${approverIds.size} approvers")
                                    approverIds.filterIsInstance<String>().forEach { approverId ->
                                        members.add(approverId)
                                        Log.d("ChatRepository", "📋 ✅ Added approver to members: $approverId")
                                    }
                                } else {
                                    Log.w("ChatRepository", "📋 ⚠️ No approvers found in approverIds")
                                }
                                
                                // Add production heads
                                val productionHeadIds = projectDoc.get("productionHeadIds") as? List<*>
                                if (productionHeadIds != null && productionHeadIds.isNotEmpty()) {
                                    Log.d("ChatRepository", "📋 Found ${productionHeadIds.size} production heads")
                                    productionHeadIds.filterIsInstance<String>().forEach { headId ->
                                        members.add(headId)
                                        Log.d("ChatRepository", "📋 ✅ Added production head to members: $headId")
                                    }
                                }
                                
                                // Add temporary approver if exists
                                val tempApprover = projectDoc.get("temporaryApproverPhone") as? String
                                if (!tempApprover.isNullOrEmpty()) {
                                    members.add(tempApprover)
                                    Log.d("ChatRepository", "📋 ✅ Added temporary approver to members: $tempApprover")
                                }
                            } else {
                                Log.w("ChatRepository", "📋 ⚠️ Project document not found: $projectId")
                            }
                        } else {
                            Log.w("ChatRepository", "📋 ⚠️ Expense not found for chat: $chatId")
                            // Fallback: Get all approvers if expense not found
                            val allApprovers = getApproversForExpenseChat(projectId, chatId)
                            Log.d("ChatRepository", "📋 Fallback: Found ${allApprovers.size} approvers")
                            members.addAll(allApprovers)
                        }
                        
                        Log.d("ChatRepository", "📋 ✅ Creating chat with ${members.size} members: $members")
                        Log.d("ChatRepository", "📋 ========== CHAT CREATION COMPLETE ==========")
                        members.toList()
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "📋 ❌ Error creating expense chat members: ${e.message}")
                        e.printStackTrace()
                        // Fallback to sender only
                        Log.w("ChatRepository", "📋 ⚠️ Falling back to sender only due to error")
                        listOf(senderId)
                    }
                } else {
                    Log.d("ChatRepository", "📋 Creating regular chat with sender only: $senderId")
                    listOf(senderId)
                }
                
                Log.d("ChatRepository", "📋 Final initial members list: $initialMembers (size: ${initialMembers.size})")
                
                // Create unread count map for all members
                val initialUnreadCount = initialMembers.associateWith { 0 }
                
                val chatData = hashMapOf(
                    "members" to initialMembers,
                    "lastMessage" to "",
                    "lastMessageTime" to Timestamp.now(),
                    "lastMessageSenderId" to "",
                    "unreadCount" to initialUnreadCount,
                    "createdAt" to Timestamp.now(),
                    "updatedAt" to Timestamp.now()
                )
                chatsCollection.document(chatId).set(chatData).await()
                Log.d("ChatRepository", "Created chat: $chatId with ${initialMembers.size} members")
            } else {
                Log.d("ChatRepository", "✅ Chat $chatId already exists")
                
                // Get current chat members for logging
                val existingMembers = (chatDoc.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                Log.d("ChatRepository", "✅ Existing chat members: $existingMembers (size: ${existingMembers.size})")
                
                // Chat exists - FOR EXPENSE APPROVAL CHATS: Ensure both user and approver are ALWAYS members
                if (chatId.startsWith("expense_approval_")) {
                    Log.d("ChatRepository", "📋 ========== EXPENSE CHAT MEMBER UPDATE ==========")
                    Log.d("ChatRepository", "📋 Chat ID: $chatId")
                    Log.d("ChatRepository", "📋 Project ID: $projectId")
                    Log.d("ChatRepository", "📋 Sender ID: $senderId")
                    Log.d("ChatRepository", "📋 Sender Name: $senderName")
                    Log.d("ChatRepository", "📋 Sender Role: $senderRole")
                    
                    try {
                        // Get current chat members
                        val currentMembers = (chatDoc.get("members") as? List<*>)?.filterIsInstance<String>()?.toMutableSet() ?: mutableSetOf()
                        Log.d("ChatRepository", "📋 Current chat members: $currentMembers (count: ${currentMembers.size})")
                        
                        var membersUpdated = false
                        val originalSize = currentMembers.size
                        
                        // Always add sender if not already in members
                        if (!currentMembers.contains(senderId)) {
                            currentMembers.add(senderId)
                            Log.d("ChatRepository", "📋 ✅ Added sender to chat members: $senderId")
                            membersUpdated = true
                        } else {
                            Log.d("ChatRepository", "📋 ✓ Sender already in members: $senderId")
                        }
                        
                        // CRITICAL: For expense chats, ensure BOTH submitter AND manager/approver are included
                        val expense = getExpenseFromChatId(projectId, chatId)
                        if (expense != null) {
                            Log.d("ChatRepository", "📋 Found expense: ${expense.id}")
                            Log.d("ChatRepository", "📋 Expense submitter: ${expense.userId}")
                            Log.d("ChatRepository", "📋 Expense status: ${expense.status}")
                            
                            // Add expense submitter if not already in members
                            if (!currentMembers.contains(expense.userId)) {
                                currentMembers.add(expense.userId)
                                Log.d("ChatRepository", "📋 ✅ Added expense submitter to members: ${expense.userId}")
                                membersUpdated = true
                            } else {
                                Log.d("ChatRepository", "📋 ✓ Expense submitter already in members: ${expense.userId}")
                            }
                            
                            // Get project details to find manager/approvers
                            val projectDoc = projectsCollection.document(projectId).get().await()
                            if (projectDoc.exists()) {
                                // Add manager if exists and not already in members
                                val managerId = projectDoc.get("managerId") as? String
                                Log.d("ChatRepository", "📋 Project manager: ${managerId ?: "none"}")
                                if (!managerId.isNullOrEmpty() && !currentMembers.contains(managerId)) {
                                    currentMembers.add(managerId)
                                    Log.d("ChatRepository", "📋 ✅ Added project manager to members: $managerId")
                                    membersUpdated = true
                                } else if (managerId != null && currentMembers.contains(managerId)) {
                                    Log.d("ChatRepository", "📋 ✓ Project manager already in members: $managerId")
                                }
                                
                                // IMPORTANT: Also add ALL approvers to ensure notifications reach the right people
                                val approverIds = projectDoc.get("approverIds") as? List<*>
                                if (approverIds != null && approverIds.isNotEmpty()) {
                                    approverIds.filterIsInstance<String>().forEach { approverId ->
                                        if (!currentMembers.contains(approverId)) {
                                            currentMembers.add(approverId)
                                            Log.d("ChatRepository", "📋 ✅ Added approver to members: $approverId")
                                            membersUpdated = true
                                        }
                                    }
                                }
                                
                                // Add production head if exists
                                val productionHeadIds = projectDoc.get("productionHeadIds") as? List<*>
                                if (productionHeadIds != null && productionHeadIds.isNotEmpty()) {
                                    productionHeadIds.filterIsInstance<String>().forEach { headId ->
                                        if (!currentMembers.contains(headId)) {
                                            currentMembers.add(headId)
                                            Log.d("ChatRepository", "📋 ✅ Added production head to members: $headId")
                                            membersUpdated = true
                                        }
                                    }
                                }
                                
                                // Add temporary approver if exists
                                val tempApprover = projectDoc.get("temporaryApproverPhone") as? String
                                if (!tempApprover.isNullOrEmpty() && !currentMembers.contains(tempApprover)) {
                                    currentMembers.add(tempApprover)
                                    Log.d("ChatRepository", "📋 ✅ Added temporary approver to members: $tempApprover")
                                    membersUpdated = true
                                }
                            } else {
                                Log.w("ChatRepository", "📋 ⚠️ Project document not found: $projectId")
                            }
                        } else {
                            Log.w("ChatRepository", "📋 ⚠️ Expense not found for chat: $chatId")
                            Log.w("ChatRepository", "📋 ⚠️ This may cause notification issues!")
                        }
                        
                        // Update chat members and unread counts if needed
                        if (membersUpdated) {
                            val unreadCount = (chatDoc.get("unreadCount") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value }?.toMutableMap() ?: mutableMapOf()
                            
                            // Initialize unread count for all members
                            currentMembers.forEach { memberId ->
                                if (!unreadCount.containsKey(memberId)) {
                                    unreadCount[memberId] = 0
                                    Log.d("ChatRepository", "📋 Initialized unread count for: $memberId")
                                }
                            }
                            
                            chatsCollection.document(chatId).update(
                                mapOf(
                                    "members" to currentMembers.toList(),
                                    "unreadCount" to unreadCount,
                                    "lastUpdated" to Timestamp.now()
                                )
                            ).await()
                            
                            val newSize = currentMembers.size
                            Log.d("ChatRepository", "📋 ✅ Updated chat members: $originalSize -> $newSize members")
                            Log.d("ChatRepository", "📋 ✅ Final members list: ${currentMembers.toList()}")
                        } else {
                            Log.d("ChatRepository", "📋 ✓ No member updates needed (all members already present)")
                            Log.d("ChatRepository", "📋 ✓ Current members (${currentMembers.size}): ${currentMembers.toList()}")
                        }
                        
                        Log.d("ChatRepository", "📋 ========== MEMBER UPDATE COMPLETE ==========")
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "📋 ❌ Error updating expense chat members: ${e.message}")
                        e.printStackTrace()
                        // Continue with sending message even if this fails
                    }
                } else {
                    Log.d("ChatRepository", "📋 Regular chat, no member updates needed")
                }
            }
            
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
            Log.d("ChatRepository", "📊 Fetching chat to update unread counts...")
            val chat = chatsCollection.document(chatId).get().await()
            val members = (chat.get("members") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            Log.d("ChatRepository", "📊 Retrieved members from chat: $members (size: ${members.size})")
            
            val unreadCount = (chat.get("unreadCount") as? Map<*, *>)?.mapKeys { it.key.toString() }?.mapValues { it.value } ?: emptyMap()
            val updatedUnreadCount = mutableMapOf<String, Any>()
            unreadCount.forEach { (k, v) -> updatedUnreadCount[k] = v ?: 0 }
            
            members.forEach { memberId ->
                if (memberId != senderId) {
                    val currentCount = (updatedUnreadCount[memberId] as? Number)?.toInt() ?: 0
                    updatedUnreadCount[memberId] = currentCount + 1
                    Log.d("ChatRepository", "📊 Incremented unread count for $memberId: $currentCount -> ${currentCount + 1}")
                }
            }
            
            chatsCollection.document(chatId).update("unreadCount", updatedUnreadCount).await()
            Log.d("ChatRepository", "📊 Updated unread counts in Firestore")
            
            // Send notifications to other members
            Log.d("ChatRepository", "💬 ========== NOTIFICATION PREPARATION ==========")
            Log.d("ChatRepository", "💬 Chat ID: $chatId")
            Log.d("ChatRepository", "💬 Is Expense Chat: ${chatId.startsWith("expense_approval_")}")
            Log.d("ChatRepository", "💬 All members: $members")
            Log.d("ChatRepository", "💬 All members count: ${members.size}")
            Log.d("ChatRepository", "💬 SenderId: $senderId")
            Log.d("ChatRepository", "💬 SenderName: $senderName")
            Log.d("ChatRepository", "💬 SenderRole: $senderRole")
            Log.d("ChatRepository", "💬 SenderId type: ${senderId.javaClass.simpleName}")
            Log.d("ChatRepository", "💬 Message: $message")
            
            val otherMembers = members.filter { it != senderId }
            Log.d("ChatRepository", "💬 Other members (after filtering sender): $otherMembers")
            Log.d("ChatRepository", "💬 Other members count: ${otherMembers.size}")
            
            // Check if senderId matches any member
            members.forEachIndexed { index, memberId ->
                Log.d("ChatRepository", "💬 Member[$index]: '$memberId' (equals senderId: ${memberId == senderId})")
            }
            
            // Additional check for expense chat
            if (chatId.startsWith("expense_approval_")) {
                Log.d("ChatRepository", "💬 📋 EXPENSE CHAT - Notification Recipients:")
                otherMembers.forEach { memberId ->
                    Log.d("ChatRepository", "💬 📋   - Will notify: $memberId")
                }
            }
            
            if (otherMembers.isNotEmpty()) {
                Log.d("ChatRepository", "✅ Creating notifications for ${otherMembers.size} members")
                
                // Get project name for notification
                val projectDoc = projectsCollection.document(projectId).get().await()
                val projectName = projectDoc.get("name") as? String ?: "Unknown Project"
                
                // Create notifications for each receiver
                // Note: Entire function is already wrapped in NonCancellable
                for (receiverId in otherMembers) {
                    try {
                            // Get the normalized user ID for notification (same as chat creation)
                            val normalizedReceiverId = getChatUserId(receiverId)
                            Log.d("ChatRepository", "Creating notification for receiver: $receiverId -> $normalizedReceiverId")
                            
                            // Get receiver's role for notification
                            val receiverDoc = usersCollection.document(normalizedReceiverId).get().await()
                            val receiverRole = receiverDoc.get("role") as? String ?: "USER"
                            
                            // Create notification with appropriate title based on chat type
                            val notificationTitle = if (chatId.startsWith("expense_approval_")) {
                                "New message about pending expense from $senderName"
                            } else {
                                "New message from $senderName"
                            }
                            
                            val notification = Notification(
                                recipientId = normalizedReceiverId, // Use normalized ID
                                recipientRole = receiverRole,
                                title = notificationTitle,
                                message = message,
                                type = NotificationType.CHAT_MESSAGE,
                                projectId = projectId,
                                projectName = projectName,
                                relatedId = chatId,
                                isRead = false,
                                actionRequired = if (chatId.startsWith("expense_approval_")) true else false,
                                navigationTarget = "chat/$projectId/$chatId/$senderName",
                                createdAt = Timestamp.now() // Ensure timestamp is set
                            )
                            
                            Log.d("ChatRepository", "📧 ========== CREATING NOTIFICATION ==========")
                            Log.d("ChatRepository", "📧 Notification Details:")
                            Log.d("ChatRepository", "   - recipientId: '$normalizedReceiverId'")
                            Log.d("ChatRepository", "   - recipientRole: '$receiverRole'")
                            Log.d("ChatRepository", "   - title: '${notification.title}'")
                            Log.d("ChatRepository", "   - message: '${notification.message}'")
                            Log.d("ChatRepository", "   - type: '${notification.type}'")
                            Log.d("ChatRepository", "   - projectId: '$projectId'")
                            Log.d("ChatRepository", "   - projectName: '$projectName'")
                            Log.d("ChatRepository", "   - relatedId (chatId): '$chatId'")
                            Log.d("ChatRepository", "   - isRead: ${notification.isRead}")
                            Log.d("ChatRepository", "   - actionRequired: ${notification.actionRequired}")
                            Log.d("ChatRepository", "   - navigationTarget: '${notification.navigationTarget}'")
                            Log.d("ChatRepository", "   - createdAt: ${notification.createdAt}")
                            
                            val notificationResult = notificationRepository.createNotification(notification)
                            
                            if (notificationResult.isSuccess) {
                                val notificationId = notificationResult.getOrNull()
                                Log.d("ChatRepository", "✅ ========== NOTIFICATION CREATED SUCCESSFULLY ==========")
                                Log.d("ChatRepository", "✅ Notification ID: $notificationId")
                                Log.d("ChatRepository", "✅ Recipient: $normalizedReceiverId ($receiverRole)")
                                Log.d("ChatRepository", "✅ Title: $notificationTitle")
                                Log.d("ChatRepository", "✅ Message: $message")
                                Log.d("ChatRepository", "✅ Chat ID: $chatId")
                                Log.d("ChatRepository", "✅ ========== ========== ========== ==========")
                            } else {
                                Log.e("ChatRepository", "❌ ========== NOTIFICATION CREATION FAILED ==========")
                                Log.e("ChatRepository", "❌ Failed to create notification for receiver: $normalizedReceiverId")
                                Log.e("ChatRepository", "❌ Recipient role: $receiverRole")
                                Log.e("ChatRepository", "❌ Error: ${notificationResult.exceptionOrNull()?.message}")
                                Log.e("ChatRepository", "❌ ========== ========== ========== ==========")
                                notificationResult.exceptionOrNull()?.printStackTrace()
                            }
                            
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "❌ Exception creating notification for $receiverId: ${e.message}")
                        e.printStackTrace()
                    }
                }
                
                Log.d("ChatRepository", "💬 ========== NOTIFICATION SUMMARY ==========")
                Log.d("ChatRepository", "💬 Total notifications attempted: ${otherMembers.size}")
                Log.d("ChatRepository", "💬 Chat ID: $chatId")
                Log.d("ChatRepository", "💬 Sender: $senderName ($senderId)")
                Log.d("ChatRepository", "💬 Is Expense Chat: ${chatId.startsWith("expense_approval_")}")
                Log.d("ChatRepository", "💬 ========== ========== ==========")
                
                // Send FCM push notifications if context is available
                if (context != null) {
                    try {
                        Log.d("ChatRepository", "Message sent successfully to ${otherMembers.size} recipients")
                        // FCM notifications are handled by the notification system
                        // when messages are stored in Firestore
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error processing message: ${e.message}")
                    }
                } else {
                    Log.w("ChatRepository", "Context not provided, message sent without notifications")
                }
            } else {
                Log.w("ChatRepository", "⚠️ ========== NO NOTIFICATIONS SENT ==========")
                Log.w("ChatRepository", "⚠️ No other members to notify! otherMembers is empty.")
                Log.w("ChatRepository", "⚠️ This means either:")
                Log.w("ChatRepository", "⚠️   1. Chat has no members")
                Log.w("ChatRepository", "⚠️   2. Sender ID doesn't match any member (ID format mismatch)")
                Log.w("ChatRepository", "⚠️   3. Chat only has 1 member (the sender)")
                Log.w("ChatRepository", "⚠️ Chat members: $members")
                Log.w("ChatRepository", "⚠️ SenderId: $senderId")
                Log.w("ChatRepository", "⚠️ Is Expense Chat: ${chatId.startsWith("expense_approval_")}")
                if (chatId.startsWith("expense_approval_")) {
                    Log.e("ChatRepository", "⚠️ ❌ CRITICAL: Expense chat has no recipients for notifications!")
                    Log.e("ChatRepository", "⚠️ ❌ This is a serious issue - expense chat notifications will not work!")
                }
            }
            
                Log.d("ChatRepository", "🎉 ========== SEND MESSAGE SUCCESS ==========")
                true
            } catch (e: Exception) {
                Log.e("ChatRepository", "❌ ========== SEND MESSAGE FAILED ==========")
                Log.e("ChatRepository", "❌ Error sending message: ${e.message}")
                e.printStackTrace()
                false
            }
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
