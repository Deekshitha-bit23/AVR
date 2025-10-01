package com.deeksha.avr.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationBadge
import com.deeksha.avr.model.NotificationType
import com.deeksha.avr.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _notificationBadge = MutableStateFlow(NotificationBadge())
    val notificationBadge: StateFlow<NotificationBadge> = _notificationBadge.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentUserId: String? = null
    private var notificationListener: kotlinx.coroutines.Job? = null

    fun loadNotifications(userId: String) {
        // Cancel any existing listener
        notificationListener?.cancel()
        
        currentUserId = userId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("NotificationViewModel", "🔄 Loading notifications for user: $userId")
                
                // Set up real-time listener for notifications
                notificationListener = viewModelScope.launch {
                    notificationRepository.getNotificationsForUserRealtime(userId)
                        .collect { notifications ->
                            Log.d("NotificationViewModel", "📡 Real-time notification update: ${notifications.size} notifications")
                            
                            // Log each notification for debugging
                            notifications.forEach { notification ->
                                Log.d("NotificationViewModel", "📋 Notification: ${notification.title} - Recipient: ${notification.recipientId} - Project: ${notification.projectName} - Read: ${notification.isRead}")
                            }
                            
                            _notifications.value = notifications
                            
                            // Update badge immediately after loading notifications
                            updateNotificationBadge(userId)
                            
                            Log.d("NotificationViewModel", "✅ Updated ${notifications.size} notifications in real-time")
                        }
                }
                
                Log.d("NotificationViewModel", "✅ Set up real-time notification listener")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error loading notifications: ${e.message}")
                _error.value = "Failed to load notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Load all notifications (both read and unread) - for approver notification screen
    fun loadAllNotifications(userId: String) {
        // Cancel any existing listener
        notificationListener?.cancel()
        
        currentUserId = userId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("NotificationViewModel", "🔄 Loading ALL notifications for user: $userId")
                
                // Set up real-time listener for all notifications
                notificationListener = viewModelScope.launch {
                    notificationRepository.getAllNotificationsForUserRealtime(userId)
                        .collect { notifications ->
                            Log.d("NotificationViewModel", "📡 Real-time ALL notification update: ${notifications.size} notifications")
                            
                            // Log each notification for debugging
                            notifications.forEach { notification ->
                                Log.d("NotificationViewModel", "📋 Notification: ${notification.title} - Recipient: ${notification.recipientId} - Project: ${notification.projectName} - Read: ${notification.isRead}")
                            }
                            
                            _notifications.value = notifications
                            
                            // Update badge immediately after loading notifications
                            updateNotificationBadge(userId)
                            
                            Log.d("NotificationViewModel", "✅ Updated ${notifications.size} ALL notifications in real-time")
                        }
                }
                
                Log.d("NotificationViewModel", "✅ Set up real-time ALL notification listener")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error loading all notifications: ${e.message}")
                _error.value = "Failed to load notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProjectNotifications(userId: String, projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("NotificationViewModel", "🔄 Loading project notifications for user: $userId, project: $projectId")
                
                val notifications = notificationRepository.getNotificationsForProject(projectId, userId)
                _notifications.value = notifications
                
                Log.d("NotificationViewModel", "✅ Loaded ${notifications.size} project notifications")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error loading project notifications: ${e.message}")
                _error.value = "Failed to load project notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "🔄 Marking notification as read: $notificationId")
                
                notificationRepository.markNotificationAsRead(notificationId)
                
                // Update local state
                _notifications.value = _notifications.value.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
                
                // Update badge
                currentUserId?.let { updateNotificationBadge(it) }
                
                Log.d("NotificationViewModel", "✅ Marked notification as read")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error marking notification as read: ${e.message}")
                _error.value = "Failed to mark notification as read: ${e.message}"
            }
        }
    }

    fun markAllNotificationsAsRead(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "🔄 Marking all notifications as read for user: $userId")
                
                notificationRepository.markAllNotificationsAsRead(userId)
                
                // Update local state
                _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                
                // Update badge
                updateNotificationBadge(userId)
                
                Log.d("NotificationViewModel", "✅ Marked all notifications as read")
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error marking all notifications as read: ${e.message}")
                _error.value = "Failed to mark all notifications as read: ${e.message}"
            }
        }
    }
    
    private suspend fun updateNotificationBadge(userId: String) {
        try {
            Log.d("NotificationViewModel", "🔄 Updating notification badge for user: $userId")
            
            // Calculate badge from current notifications first
            val currentNotifications = _notifications.value
            val unreadCount = currentNotifications.count { !it.isRead }
            val hasUnread = unreadCount > 0
            
            Log.d("NotificationViewModel", "📊 Calculated from local notifications: unread=$unreadCount, hasUnread=$hasUnread")
            
            // Also get badge from repository to ensure consistency
            val repositoryBadge = notificationRepository.getNotificationBadge(userId)
            
            // Use the higher count to ensure we don't miss any notifications
            val finalCount = maxOf(unreadCount, repositoryBadge.count)
            val finalHasUnread = finalCount > 0
            
            val finalBadge = NotificationBadge(
                count = finalCount,
                hasUnread = finalHasUnread
            )
            
            _notificationBadge.value = finalBadge
            
            Log.d("NotificationViewModel", "📊 Final notification badge: count=$finalCount, hasUnread=$finalHasUnread")
            Log.d("NotificationViewModel", "📊 Repository badge: count=${repositoryBadge.count}, hasUnread=${repositoryBadge.hasUnread}")
            
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "❌ Error updating notification badge: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun debugNotificationState(userId: String) {
        Log.d("NotificationViewModel", "🔍 === DEBUG NOTIFICATION STATE ===")
        Log.d("NotificationViewModel", "🔍 Current user ID: $userId")
        Log.d("NotificationViewModel", "🔍 Current notifications count: ${_notifications.value.size}")
        Log.d("NotificationViewModel", "🔍 Current badge: count=${_notificationBadge.value.count}, hasUnread=${_notificationBadge.value.hasUnread}")
        
        _notifications.value.forEach { notification ->
            Log.d("NotificationViewModel", "🔍 Notification: ${notification.title} - Read: ${notification.isRead} - Recipient: ${notification.recipientId}")
        }
        
        // Force reload to check repository state
        viewModelScope.launch {
            try {
                val repositoryNotifications = notificationRepository.getNotificationsForUser(userId)
                val repositoryBadge = notificationRepository.getNotificationBadge(userId)
                
                Log.d("NotificationViewModel", "🔍 Repository notifications count: ${repositoryNotifications.size}")
                Log.d("NotificationViewModel", "🔍 Repository badge: count=${repositoryBadge.count}, hasUnread=${repositoryBadge.hasUnread}")
                
                repositoryNotifications.forEach { notification ->
                    Log.d("NotificationViewModel", "🔍 Repository notification: ${notification.title} - Read: ${notification.isRead} - Recipient: ${notification.recipientId}")
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "🔍 Error in debug: ${e.message}")
            }
        }
    }

    fun forceLoadNotifications(userId: String) {
        Log.d("NotificationViewModel", "🔄 Force loading notifications for user: $userId")
        
        // Clear everything and reload
        _notifications.value = emptyList()
        _notificationBadge.value = NotificationBadge()
        currentUserId = null
        
        // Load notifications
        loadNotifications(userId)
    }

    fun refreshNotifications(userId: String) {
        Log.d("NotificationViewModel", "🔄 Force refreshing notifications for user: $userId")
        
        // Clear current notifications to force reload
        _notifications.value = emptyList()
        currentUserId = null
        
        // Force reload notifications
        loadNotifications(userId)
    }
    
    fun onScreenVisible(userId: String) {
        Log.d("NotificationViewModel", "👁️ Screen became visible, refreshing notifications for user: $userId")
        
        // Refresh notifications when screen becomes visible
        viewModelScope.launch {
            try {
                // Small delay to ensure smooth transition
                delay(100)
                refreshNotifications(userId)
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error refreshing on screen visible: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up the notification listener when ViewModel is cleared
        notificationListener?.cancel()
        Log.d("NotificationViewModel", "🧹 Cleaned up notification listener")
    }
    
    fun getUnreadCount(): Int = _notificationBadge.value.count
    
    fun hasUnreadNotifications(): Boolean = _notificationBadge.value.hasUnread
    
    fun getNotificationsByType(type: NotificationType): List<Notification> {
        return _notifications.value.filter { it.type == type }
    }
    
    fun getProjectNotifications(projectId: String): List<Notification> {
        return _notifications.value.filter { it.projectId == projectId }
    }
    
    fun getUnreadNotifications(): List<Notification> {
        return _notifications.value.filter { !it.isRead }
    }
    
    fun getActionRequiredNotifications(): List<Notification> {
        return _notifications.value.filter { it.actionRequired && !it.isRead }
    }

    fun loadAllNotificationsForDebug() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("NotificationViewModel", "🔄 Loading ALL notifications for debugging")
                
                val allNotifications = notificationRepository.getAllNotifications()
                Log.d("NotificationViewModel", "📋 Found ${allNotifications.size} total notifications")
                
                // Show notifications with empty recipientId
                val emptyRecipientNotifications = allNotifications.filter { it.recipientId.isEmpty() }
                if (emptyRecipientNotifications.isNotEmpty()) {
                    Log.w("NotificationViewModel", "⚠️ Found ${emptyRecipientNotifications.size} notifications with empty recipientId:")
                    emptyRecipientNotifications.forEach { notification ->
                        Log.w("NotificationViewModel", "⚠️ Empty recipient notification: ${notification.title} - Project: ${notification.projectName}")
                    }
                }
                
                _notifications.value = allNotifications
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error loading all notifications: ${e.message}")
                _error.value = "Failed to load all notifications: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fixEmptyRecipientNotifications(userId: String) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "🔄 Fixing notifications with empty recipientId for user: $userId")
                
                val allNotifications = notificationRepository.getAllNotifications()
                val emptyRecipientNotifications = allNotifications.filter { it.recipientId.isEmpty() }
                
                if (emptyRecipientNotifications.isNotEmpty()) {
                    Log.d("NotificationViewModel", "📋 Found ${emptyRecipientNotifications.size} notifications with empty recipientId, fixing...")
                    
                    // Update each notification to assign to current user
                    emptyRecipientNotifications.forEach { notification ->
                        try {
                            // Update the notification in Firestore
                            notificationRepository.updateNotificationRecipient(notification.id, userId)
                            Log.d("NotificationViewModel", "✅ Fixed notification: ${notification.title}")
                        } catch (e: Exception) {
                            Log.e("NotificationViewModel", "❌ Failed to fix notification ${notification.id}: ${e.message}")
                        }
                    }
                    
                    // Reload notifications
                    loadNotifications(userId)
                } else {
                    Log.d("NotificationViewModel", "📋 No notifications with empty recipientId found")
                }
                
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "❌ Error fixing empty recipient notifications: ${e.message}")
                _error.value = "Failed to fix notifications: ${e.message}"
            }
        }
    }
} 