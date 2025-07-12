package com.deeksha.avr.ui.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
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
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.ui.common.NotificationCard
import com.deeksha.avr.ui.common.EmptyNotificationsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectNotificationScreen(
    projectId: String,
    projectName: String,
    onBackClick: () -> Unit,
    onNotificationClick: (Notification) -> Unit,
    currentUserId: String,
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val projectNotifications by notificationViewModel.getProjectNotifications(projectId).collectAsState(initial = emptyList())
    val projectBadge by notificationViewModel.getProjectNotificationBadge(projectId).collectAsState()
    val expenseStatusSummary by notificationViewModel.getProjectExpenseStatusSummary(projectId).collectAsState()
    val isLoading by notificationViewModel.isLoading.collectAsState()
    val error by notificationViewModel.error.collectAsState()
    
    LaunchedEffect(currentUserId) {
        notificationViewModel.setUserId(currentUserId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = projectName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (projectBadge.hasUnread) {
                            Text(
                                text = "${projectBadge.count} unread notifications",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (projectBadge.hasUnread) {
                        IconButton(
                            onClick = { 
                                notificationViewModel.markProjectNotificationsAsRead(projectId)
                            }
                        ) {
                            Icon(
                                Icons.Default.Clear,
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
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
                                    notificationViewModel.refreshNotifications()
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
                    EmptyNotificationsState(
                        title = "No Project Updates",
                        subtitle = "You'll be notified when there are updates for this project"
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Project expense summary header
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFE3F2FD)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "💰 $projectName Updates",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1976D2)
                                    )
                                    
                                    if (expenseStatusSummary.hasUpdates) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            if (expenseStatusSummary.approvedCount > 0) {
                                                Text(
                                                    text = "✅ ${expenseStatusSummary.approvedCount} Approved",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF4CAF50),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            
                                            if (expenseStatusSummary.rejectedCount > 0) {
                                                Text(
                                                    text = "❌ ${expenseStatusSummary.rejectedCount} Rejected",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFE53935),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            
                                            if (expenseStatusSummary.submittedCount > 0) {
                                                Text(
                                                    text = "📝 ${expenseStatusSummary.submittedCount} Submitted",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFFFF9800),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Notifications list
                        items(projectNotifications) { notification ->
                            NotificationCard(
                                notification = notification,
                                onNotificationClick = onNotificationClick,
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
} 