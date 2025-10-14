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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
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
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationType
import com.deeksha.avr.utils.FormatUtils
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.ui.common.ProminentNotificationBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectNotificationListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProject: (String) -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    currentUserId: String,
    userRole: String,
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val error by notificationViewModel.error.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // Filter notifications for approvers/production heads
    val filteredNotifications = remember(notifications, userRole) {
        notifications.filter { notification ->
            when (userRole) {
                "APPROVER", "PRODUCTION_HEAD" -> {
                    // For approvers and production heads, show ONLY expense submitted and pending approval notifications
                    // NOT expense approved/rejected notifications (those go to users)
                    notification.type == NotificationType.EXPENSE_SUBMITTED ||
                    notification.type == NotificationType.PENDING_APPROVAL ||
                    notification.type == NotificationType.PROJECT_ASSIGNMENT
                }
                else -> {
                    // For other roles, show all notifications
                    true
                }
            }
        }
    }
    
    // Load notifications when screen opens and refresh when screen becomes visible
    LaunchedEffect(Unit) {
        if (currentUserId.isNotEmpty()) {
            notificationViewModel.loadNotifications(currentUserId)
        }
    }
    
    // Refresh notifications when screen becomes visible (for better UX)
    LaunchedEffect(Unit) {
        if (currentUserId.isNotEmpty()) {
            // Small delay to ensure screen is fully loaded
            delay(500)
            notificationViewModel.onScreenVisible(currentUserId)
        }
    }
    
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
                        text = "Project Notifications",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (userRole == "APPROVER") "Approver View" else "Production Head View",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
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
                // Show notification badge if there are unread notifications
                if (notificationBadge.count > 0) {
                    Box {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                        ProminentNotificationBadge(
                            badge = notificationBadge,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
                }
                
                // Refresh button
                IconButton(onClick = {
                    if (currentUserId.isNotEmpty()) {
                        notificationViewModel.forceLoadNotifications(currentUserId)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Notifications",
                        tint = Color.White
                    )
                }
                
                // Debug button
                IconButton(onClick = {
                    if (currentUserId.isNotEmpty()) {
                        notificationViewModel.debugNotificationState(currentUserId)
                    }
                }) {
                    Text(
                        text = "🔍",
                        fontSize = 12.sp,
                        color = Color.Red
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF4285F4)
            )
        )
        
        // Main Content
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading notifications...",
                            color = Color.Gray
                        )
                    }
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
                            text = "❌ Error Loading Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "Unknown error occurred",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                notificationViewModel.clearError()
                                if (currentUserId.isNotEmpty()) {
                                    notificationViewModel.forceLoadNotifications(currentUserId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            filteredNotifications.isEmpty() -> {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "No notifications",
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No Project Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "You're all caught up! No pending notifications for your projects.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Notifications will appear here when:\n• New expenses are submitted for approval\n• Projects require your attention\n• Important updates are available",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                // Notifications list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotifications) { notification ->
                        NotificationCard(
                            notification = notification,
                            onNotificationClick = { clickedNotification ->
                                // Navigate based on notification type
                                when (clickedNotification.type) {
                                    NotificationType.DELEGATION_REMOVED -> {
                                        // For delegation removed notifications, just mark as read and don't navigate
                                        // The notification will disappear from the list
                                        // No navigation needed - just mark as read
                                    }
                                    NotificationType.EXPENSE_SUBMITTED,
                                    NotificationType.PENDING_APPROVAL -> {
                                        onNavigateToPendingApprovals(clickedNotification.projectId)
                                    }
                                    NotificationType.EXPENSE_APPROVED,
                                    NotificationType.EXPENSE_REJECTED,
                                    NotificationType.PROJECT_ASSIGNMENT -> {
                                        onNavigateToProject(clickedNotification.projectId)
                                    }
                                    else -> {
                                        onNavigateToProject(clickedNotification.projectId)
                                    }
                                }
                                
                                // Mark notification as read
                                notificationViewModel.markNotificationAsRead(clickedNotification.id)
                            },
                            onMarkAsRead = { notificationId ->
                                notificationViewModel.markNotificationAsRead(notificationId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    onNotificationClick: (Notification) -> Unit,
    onMarkAsRead: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNotificationClick(notification) },
        colors = CardDefaults.cardColors(
            containerColor = if (!notification.isRead) Color(0xFFF3E5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Notification icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = getNotificationIconColor(notification.type),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getNotificationIcon(notification.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
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
                
                Text(
                    text = FormatUtils.formatTimeAgo(notification.createdAt),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Mark as read button (if unread)
            if (!notification.isRead) {
                IconButton(
                    onClick = { onMarkAsRead(notification.id) }
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Mark as read",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun getNotificationIcon(notificationType: NotificationType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (notificationType) {
        NotificationType.EXPENSE_APPROVED -> Icons.Default.CheckCircle
        NotificationType.EXPENSE_REJECTED -> Icons.Default.CheckCircle
        NotificationType.EXPENSE_SUBMITTED -> Icons.Default.Notifications
        NotificationType.PENDING_APPROVAL -> Icons.Default.Notifications
        NotificationType.PROJECT_ASSIGNMENT -> Icons.Default.Notifications
        else -> Icons.Default.Notifications
    }
}

@Composable
private fun getNotificationIconColor(notificationType: NotificationType): Color {
    return when (notificationType) {
        NotificationType.EXPENSE_APPROVED -> Color(0xFF4CAF50) // Green
        NotificationType.EXPENSE_REJECTED -> Color(0xFFF44336) // Red
        NotificationType.EXPENSE_SUBMITTED -> Color(0xFF2196F3) // Blue
        NotificationType.PENDING_APPROVAL -> Color(0xFFFF9800) // Orange
        NotificationType.PROJECT_ASSIGNMENT -> Color(0xFF9C27B0) // Purple
        else -> Color(0xFF9E9E9E) // Gray
    }
} 