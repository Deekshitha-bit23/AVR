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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Notification
import com.deeksha.avr.viewmodel.ProjectNotificationViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.ui.common.NotificationCard
import com.deeksha.avr.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSpecificNotificationScreen(
    projectId: String,
    projectName: String,
    onNavigateBack: () -> Unit,
    onNavigateToProject: (String) -> Unit,
    onNavigateToExpense: (String, String) -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    projectNotificationViewModel: ProjectNotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val projectNotifications by projectNotificationViewModel.projectNotifications.collectAsState()
    val isLoading by projectNotificationViewModel.isLoading.collectAsState()
    val error by projectNotificationViewModel.error.collectAsState()
    
    // Load project notifications when screen starts
    LaunchedEffect(projectId) {
        // Ensure user data is up to date first
        authViewModel.refreshUserData()
        
        // Get current user ID and role from AuthViewModel
        val currentUserId = authViewModel.authState.value.user?.uid ?: ""
        val currentUserRole = authViewModel.authState.value.user?.role?.name ?: "USER"
        
        if (currentUserId.isNotEmpty()) {
            projectNotificationViewModel.loadProjectNotificationsRealtime(
                userId = currentUserId,
                projectId = projectId,
                userRole = currentUserRole
            )
        }
    }
    
    // Additional effect to handle user data changes
    LaunchedEffect(authState.user) {
        val currentUserId = authState.user?.uid ?: ""
        val currentUserRole = authState.user?.role?.name ?: "USER"
        
        if (currentUserId.isNotEmpty() && currentUserId != projectNotificationViewModel.getCurrentUserId()) {
            projectNotificationViewModel.loadProjectNotificationsRealtime(
                userId = currentUserId,
                projectId = projectId,
                userRole = currentUserRole
            )
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
                        color = Color(0xFF4285F4)
                    )
                    Text(
                        text = projectName,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF4285F4)
                    )
                }
            },
            actions = {
                // Refresh button
                IconButton(
                    onClick = { projectNotificationViewModel.refreshProjectNotifications() }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF4285F4)
                    )
                }
                
                // Mark all as read button
                if (projectNotifications.any { !it.isRead }) {
                    IconButton(
                        onClick = { projectNotificationViewModel.markAllProjectNotificationsAsRead() }
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Mark all as read",
                            tint = Color(0xFF4285F4)
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
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
                            text = "Loading project notifications...",
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
                                projectNotificationViewModel.clearError()
                                projectNotificationViewModel.refreshProjectNotifications()
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
                            text = "📋 No Project Notifications",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No notifications found for this project.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Notification count header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Project Notifications (${projectNotifications.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        
                        if (projectNotifications.any { !it.isRead }) {
                            Text(
                                text = "${projectNotifications.count { !it.isRead }} unread",
                                fontSize = 14.sp,
                                color = Color(0xFF4285F4),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Notifications list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(projectNotifications) { notification ->
                            NotificationCard(
                                notification = notification,
                                onNotificationClick = { clickedNotification ->
                                    // Mark as read
                                    projectNotificationViewModel.markProjectNotificationAsRead(clickedNotification.id)
                                    
                                    // Navigate based on notification type
                                    when (clickedNotification.type) {
                                        com.deeksha.avr.model.NotificationType.EXPENSE_APPROVED,
                                        com.deeksha.avr.model.NotificationType.EXPENSE_REJECTED -> {
                                            if (clickedNotification.projectId.isNotEmpty() && clickedNotification.relatedId.isNotEmpty()) {
                                                onNavigateToExpense(clickedNotification.projectId, clickedNotification.relatedId)
                                            } else if (clickedNotification.projectId.isNotEmpty()) {
                                                onNavigateToProject(clickedNotification.projectId)
                                            }
                                        }
                                        com.deeksha.avr.model.NotificationType.PROJECT_ASSIGNMENT -> {
                                            if (clickedNotification.projectId.isNotEmpty()) {
                                                onNavigateToProject(clickedNotification.projectId)
                                            }
                                        }
                                        com.deeksha.avr.model.NotificationType.PENDING_APPROVAL -> {
                                            if (clickedNotification.projectId.isNotEmpty()) {
                                                onNavigateToPendingApprovals(clickedNotification.projectId)
                                            }
                                        }
                                        else -> {
                                            // Default navigation based on navigation target
                                            when {
                                                clickedNotification.navigationTarget.startsWith("expense_list/") -> {
                                                    val projectId = clickedNotification.navigationTarget.substringAfter("expense_list/")
                                                    onNavigateToProject(projectId)
                                                }
                                                clickedNotification.navigationTarget.startsWith("pending_approvals/") -> {
                                                    val projectId = clickedNotification.navigationTarget.substringAfter("pending_approvals/")
                                                    onNavigateToPendingApprovals(projectId)
                                                }
                                                else -> {
                                                    if (clickedNotification.projectId.isNotEmpty()) {
                                                        onNavigateToProject(clickedNotification.projectId)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                onMarkAsRead = { notificationId ->
                                    projectNotificationViewModel.markProjectNotificationAsRead(notificationId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
