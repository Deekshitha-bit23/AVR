package com.deeksha.avr.ui.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationType
import com.deeksha.avr.utils.FormatUtils
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectNotificationScreen(
    projectId: String,
    projectName: String,
    onNavigateBack: () -> Unit,
    onNavigateToProject: (String) -> Unit,
    onNavigateToExpense: (String, String) -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    onNavigateToChat: (String, String, String) -> Unit = { _, _, _ -> }, // projectId, chatId, otherUserName
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val error by notificationViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // Load project-specific notifications
    LaunchedEffect(projectId) {
        val currentUserId = authState.user?.phone
        if (currentUserId != null) {
            notificationViewModel.loadProjectNotifications(currentUserId, projectId)
    }
    }
    
    // Filter notifications for this project
    val projectNotifications = notifications.filter { it.projectId == projectId }
    val unreadCount = projectNotifications.count { !it.isRead }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectName,
                            fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                        )
                    if (unreadCount > 0) {
                            Text(
                            text = "$unreadCount unread notifications",
                                fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                        )
                    }
                },
                actions = {
                if (unreadCount > 0) {
                    TextButton(
                            onClick = { 
                            val currentUserId = authState.user?.phone
                            if (currentUserId != null) {
                                notificationViewModel.markAllNotificationsAsRead(currentUserId)
                            }
                            }
                        ) {
                        Text(
                            text = "Mark All Read",
                            color = Color.White,
                            fontSize = 14.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF8B5FBF)
            )
        )
        
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                    CircularProgressIndicator(color = Color(0xFF8B5FBF))
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                            text = "Error Loading Notifications",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Text(
                            text = error ?: "Unknown error",
                                color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                            )
                            Button(
                                onClick = { 
                                notificationViewModel.clearError()
                                val currentUserId = authState.user?.phone
                                if (currentUserId != null) {
                                    notificationViewModel.refreshNotifications(currentUserId)
                                }
                                },
                                colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5FBF)
                            ),
                            modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                projectNotifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "No Project Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "You'll see notifications here when there are updates for this project",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                    )
                }
                }
            }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    items(projectNotifications) { notification ->
                        NotificationItem(
                            notification = notification,
                            onNotificationClick = {
                                handleNotificationClick(
                                    notification = notification,
                                    onNavigateToProject = onNavigateToProject,
                                    onNavigateToExpense = onNavigateToExpense,
                                    onNavigateToPendingApprovals = onNavigateToPendingApprovals,
                                    onNavigateToChat = onNavigateToChat,
                                    onMarkAsRead = {
                                        notificationViewModel.markNotificationAsRead(notification.id)
                                    }
                                )
                            }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
@Composable
private fun NotificationItem(
    notification: Notification,
    onNotificationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNotificationClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color.White else Color(0xFFF0F8FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Read/Unread indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = if (notification.isRead) Color.Gray else Color(0xFF8B5FBF),
                        shape = androidx.compose.foundation.shape.CircleShape
                    )
                    .padding(top = 2.dp)
            ) {
                if (notification.isRead) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Read",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Notification content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = notification.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = notification.message,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = FormatUtils.formatTimeAgo(notification.createdAt),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    if (notification.actionRequired && !notification.isRead) {
                        Badge(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "Action Required",
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun handleNotificationClick(
    notification: Notification,
    onNavigateToProject: (String) -> Unit,
    onNavigateToExpense: (String, String) -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    onNavigateToChat: (String, String, String) -> Unit,
    onMarkAsRead: () -> Unit
) {
    // Mark notification as read
    onMarkAsRead()
    
    // Navigate based on notification type and navigation target
    when (notification.type) {
        NotificationType.EXPENSE_SUBMITTED -> {
            if (notification.projectId.isNotEmpty()) {
                onNavigateToPendingApprovals(notification.projectId)
            }
        }
        NotificationType.EXPENSE_APPROVED, NotificationType.EXPENSE_REJECTED -> {
            if (notification.projectId.isNotEmpty() && notification.relatedId.isNotEmpty()) {
                onNavigateToExpense(notification.projectId, notification.relatedId)
            } else if (notification.projectId.isNotEmpty()) {
                onNavigateToProject(notification.projectId)
            }
        }
        NotificationType.PROJECT_ASSIGNMENT -> {
            if (notification.projectId.isNotEmpty()) {
                onNavigateToProject(notification.projectId)
            }
        }
        NotificationType.PENDING_APPROVAL -> {
            if (notification.projectId.isNotEmpty()) {
onNavigateToPendingApprovals(notification.projectId)
            }
        }
        NotificationType.CHAT_MESSAGE -> {
            // Navigate to specific chat
            if (notification.navigationTarget.startsWith("chat/")) {
                val parts = notification.navigationTarget.split("/")
                if (parts.size >= 4) {
                    val projectId = parts[1]
                    val chatId = parts[2]
                    val otherUserName = parts[3]
                    // Navigate to chat screen
                    onNavigateToChat(projectId, chatId, otherUserName)
                }
            } else if (notification.projectId.isNotEmpty()) {
                onNavigateToProject(notification.projectId)
            }
        }
        else -> {
            // Default navigation based on navigation target
            when {
                notification.navigationTarget.startsWith("pending_approvals/") -> {
                    val projectId = notification.navigationTarget.substringAfter("pending_approvals/")
                    onNavigateToPendingApprovals(projectId)
                }
                notification.navigationTarget.startsWith("expense_list/") -> {
                    val projectId = notification.navigationTarget.substringAfter("expense_list/")
                    onNavigateToProject(projectId)
                }
                notification.navigationTarget.startsWith("user_project_dashboard/") -> {
                    val projectId = notification.navigationTarget.substringAfter("user_project_dashboard/")
                    onNavigateToProject(projectId)
                }
                notification.navigationTarget.startsWith("approver_project_dashboard/") -> {
                    val projectId = notification.navigationTarget.substringAfter("approver_project_dashboard/")
                    onNavigateToProject(projectId)
                }
                notification.navigationTarget.startsWith("production_head_project_dashboard/") -> {
                    val projectId = notification.navigationTarget.substringAfter("production_head_project_dashboard/")
                    onNavigateToProject(projectId)
                }
                else -> {
                    // Default to project selection
                    if (notification.projectId.isNotEmpty()) {
                        onNavigateToProject(notification.projectId)
                    }
                }
            }
        }
    }
} 