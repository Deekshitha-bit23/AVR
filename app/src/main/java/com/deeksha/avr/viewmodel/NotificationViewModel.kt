package com.deeksha.avr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationBadge
import com.deeksha.avr.model.ProjectNotificationSummary
import com.deeksha.avr.model.ExpenseNotificationSummary
import com.deeksha.avr.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _notificationBadge = MutableStateFlow(NotificationBadge())
    val notificationBadge: StateFlow<NotificationBadge> = _notificationBadge.asStateFlow()

    private val _projectNotificationSummaries = MutableStateFlow<List<ProjectNotificationSummary>>(emptyList())
    val projectNotificationSummaries: StateFlow<List<ProjectNotificationSummary>> = _projectNotificationSummaries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentUserId: String = ""

    fun setUserId(userId: String) {
        currentUserId = userId
        refreshNotifications()
    }

    fun refreshNotifications() {
        if (currentUserId.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load notifications and badge count
                val notifications = notificationRepository.getNotificationsForUser(currentUserId)
                val badge = notificationRepository.getNotificationBadge(currentUserId)
                
                _notifications.value = notifications
                _notificationBadge.value = badge
                
                android.util.Log.d("NotificationViewModel", "📱 Loaded ${notifications.size} notifications, ${badge.count} unread")
            } catch (e: Exception) {
                _error.value = "Failed to load notifications: ${e.message}"
                android.util.Log.e("NotificationViewModel", "❌ Error loading notifications: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProjectNotificationSummaries(projectIds: List<String>) {
        if (currentUserId.isBlank()) return
        
        viewModelScope.launch {
            try {
                val summaries = notificationRepository.getProjectNotificationSummaries(currentUserId, projectIds)
                _projectNotificationSummaries.value = summaries
                
                android.util.Log.d("NotificationViewModel", "📊 Loaded notification summaries for ${summaries.size} projects")
            } catch (e: Exception) {
                _error.value = "Failed to load project summaries: ${e.message}"
                android.util.Log.e("NotificationViewModel", "❌ Error loading project summaries: ${e.message}")
            }
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                notificationRepository.markNotificationAsRead(notificationId)
                
                // Update local state
                _notifications.value = _notifications.value.map { notification ->
                    if (notification.id == notificationId) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
                
                // Update badge count
                val badge = notificationRepository.getNotificationBadge(currentUserId)
                _notificationBadge.value = badge
                
                android.util.Log.d("NotificationViewModel", "✅ Marked notification as read: $notificationId")
            } catch (e: Exception) {
                _error.value = "Failed to mark notification as read: ${e.message}"
                android.util.Log.e("NotificationViewModel", "❌ Error marking notification as read: ${e.message}")
            }
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                notificationRepository.markAllNotificationsAsRead(currentUserId)
                
                // Update local state
                _notifications.value = _notifications.value.map { notification ->
                    notification.copy(isRead = true)
                }
                
                // Update badge count
                _notificationBadge.value = NotificationBadge(count = 0, hasUnread = false)
                
                android.util.Log.d("NotificationViewModel", "✅ Marked all notifications as read")
            } catch (e: Exception) {
                _error.value = "Failed to mark all notifications as read: ${e.message}"
                android.util.Log.e("NotificationViewModel", "❌ Error marking all notifications as read: ${e.message}")
            }
        }
    }

    fun getUnreadNotificationsForProject(projectId: String): List<Notification> {
        return _notifications.value.filter { notification ->
            notification.projectId == projectId && !notification.isRead
        }
    }

    fun getNotificationCountForProject(projectId: String): Int {
        return _notifications.value.count { notification ->
            notification.projectId == projectId && !notification.isRead
        }
    }

    fun getPendingApprovalNotifications(): List<Notification> {
        return _notifications.value.filter { notification ->
            notification.actionRequired && !notification.isRead
        }
    }

    fun getExpenseUpdateNotifications(): List<Notification> {
        return _notifications.value.filter { notification ->
            (notification.type.name.contains("EXPENSE_") || 
             notification.type.name.contains("APPROVED") || 
             notification.type.name.contains("REJECTED")) && !notification.isRead
        }
    }

    // Get expense status notification summaries
    fun getExpenseStatusSummary(): ExpenseNotificationSummary {
        val notifications = _notifications.value
        val approvedCount = notifications.count { 
            it.type == com.deeksha.avr.model.NotificationType.EXPENSE_APPROVED && !it.isRead 
        }
        val rejectedCount = notifications.count { 
            it.type == com.deeksha.avr.model.NotificationType.EXPENSE_REJECTED && !it.isRead 
        }
        val totalUnread = notifications.count { !it.isRead }
        
        return ExpenseNotificationSummary(
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            totalUnread = totalUnread,
            hasUpdates = totalUnread > 0
        )
    }

    // Get latest expense notifications
    fun getLatestExpenseNotifications(limit: Int = 3): List<Notification> {
        return _notifications.value
            .filter { 
                it.type == com.deeksha.avr.model.NotificationType.EXPENSE_APPROVED || 
                it.type == com.deeksha.avr.model.NotificationType.EXPENSE_REJECTED 
            }
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    // Get project-specific notification badge
    fun getProjectNotificationBadge(projectId: String): StateFlow<NotificationBadge> {
        return _notifications.map { notifications ->
            val projectNotifications = notifications.filter { 
                it.projectId == projectId && !it.isRead 
            }
            NotificationBadge(
                count = projectNotifications.size,
                hasUnread = projectNotifications.isNotEmpty()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationBadge(count = 0, hasUnread = false)
        )
    }

    // Get project-specific expense status summary
    fun getProjectExpenseStatusSummary(projectId: String): StateFlow<ExpenseNotificationSummary> {
        return _notifications.map { notifications ->
            val projectNotifications = notifications.filter { it.projectId == projectId }
            val approvedCount = projectNotifications.count { 
                it.type == com.deeksha.avr.model.NotificationType.EXPENSE_APPROVED && !it.isRead 
            }
            val rejectedCount = projectNotifications.count { 
                it.type == com.deeksha.avr.model.NotificationType.EXPENSE_REJECTED && !it.isRead 
            }
            val submittedCount = projectNotifications.count { 
                it.type == com.deeksha.avr.model.NotificationType.EXPENSE_SUBMITTED && !it.isRead 
            }
            val totalUnread = projectNotifications.count { !it.isRead }
            
            ExpenseNotificationSummary(
                approvedCount = approvedCount,
                rejectedCount = rejectedCount,
                submittedCount = submittedCount,
                totalUnread = totalUnread,
                hasUpdates = totalUnread > 0,
                total = totalUnread
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExpenseNotificationSummary(
                approvedCount = 0,
                rejectedCount = 0,
                submittedCount = 0,
                totalUnread = 0,
                hasUpdates = false,
                total = 0
            )
        )
    }

    // Get project-specific notifications
    fun getProjectNotifications(projectId: String): StateFlow<List<Notification>> {
        return _notifications.map { notifications ->
            notifications
                .filter { it.projectId == projectId }
                .sortedByDescending { it.createdAt }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Mark project notifications as read
    fun markProjectNotificationsAsRead(projectId: String) {
        viewModelScope.launch {
            try {
                val projectNotifications = _notifications.value.filter { 
                    it.projectId == projectId && !it.isRead 
                }
                
                // Mark each notification as read
                projectNotifications.forEach { notification ->
                    notificationRepository.markNotificationAsRead(notification.id)
                }
                
                // Update local state
                _notifications.value = _notifications.value.map { notification ->
                    if (notification.projectId == projectId && !notification.isRead) {
                        notification.copy(isRead = true)
                    } else {
                        notification
                    }
                }
                
                // Update badge count
                val badge = notificationRepository.getNotificationBadge(currentUserId)
                _notificationBadge.value = badge
                
                android.util.Log.d("NotificationViewModel", "✅ Marked ${projectNotifications.size} project notifications as read")
            } catch (e: Exception) {
                _error.value = "Failed to mark project notifications as read: ${e.message}"
                android.util.Log.e("NotificationViewModel", "❌ Error marking project notifications as read: ${e.message}")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    // Helper function to get notification summary for a specific project
    fun getProjectNotificationSummary(projectId: String): ProjectNotificationSummary? {
        return _projectNotificationSummaries.value.find { it.projectId == projectId }
    }

    // Helper function to get total unread count across all projects
    fun getTotalUnreadCount(): Int {
        return _notificationBadge.value.count
    }

    // Helper function to check if there are any action-required notifications
    fun hasActionRequiredNotifications(): Boolean {
        return _notifications.value.any { it.actionRequired && !it.isRead }
    }
} 