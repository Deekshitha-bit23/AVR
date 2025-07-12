package com.deeksha.avr.ui.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Project
import com.deeksha.avr.model.ExpenseNotificationSummary
import com.deeksha.avr.viewmodel.ProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.ui.common.ProjectNotificationSummaryCard
import com.deeksha.avr.utils.FormatUtils
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelectionScreen(
    onProjectSelected: (String) -> Unit,
    onNotificationClick: (String) -> Unit = {},
    currentUserId: String,
    projectViewModel: ProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val projects by projectViewModel.projects.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()
    val error by projectViewModel.error.collectAsState()
    
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val projectNotificationSummaries by notificationViewModel.projectNotificationSummaries.collectAsState()
    
    // Get expense status summary
    val expenseStatusSummary: ExpenseNotificationSummary = remember(notificationBadge) {
        notificationViewModel.getExpenseStatusSummary()
    }
    val latestExpenseNotifications: List<com.deeksha.avr.model.Notification> = remember(notificationBadge) {
        notificationViewModel.getLatestExpenseNotifications(3)
    }
    
    LaunchedEffect(Unit) {
        println("🎬 ProjectSelectionScreen: Loading projects...")
        projectViewModel.loadProjects()
        
        // Initialize notifications
        notificationViewModel.setUserId(currentUserId)
    }
    
    LaunchedEffect(projects) {
        if (projects.isNotEmpty()) {
            // Load notification summaries for all projects
            val projectIds = projects.map { it.id }
            notificationViewModel.loadProjectNotificationSummaries(projectIds)
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
                        text = "AVR ENTERTAINMENT",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4)
                    )
                    Text(
                        text = "Select Project",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            actions = {
                // Refresh button
                IconButton(onClick = { 
                    println("🔄 Refresh button clicked")
                    projectViewModel.loadProjects() 
                    notificationViewModel.refreshNotifications()
                }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Projects",
                        tint = Color(0xFF4285F4)
                    )
                }
                
                Box {
                    IconButton(onClick = { onNotificationClick(currentUserId) }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "View Notifications",
                            tint = Color(0xFF4285F4)
                        )
                    }
                    
                    // Notification badge
                    NotificationBadgeComponent(
                        badge = notificationBadge,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Choose a project to continue:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Dynamic expense status notifications
            if (expenseStatusSummary.hasUpdates) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E8)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💰 Expense Updates",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            
                            NotificationBadgeComponent(
                                badge = notificationBadge,
                                modifier = Modifier
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Status breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (expenseStatusSummary.approvedCount > 0) {
                                StatusChip(
                                    text = "✅ ${expenseStatusSummary.approvedCount} Approved",
                                    backgroundColor = Color(0xFFE8F5E8),
                                    textColor = Color(0xFF4CAF50)
                                )
                            }
                            
                            if (expenseStatusSummary.rejectedCount > 0) {
                                StatusChip(
                                    text = "❌ ${expenseStatusSummary.rejectedCount} Rejected",
                                    backgroundColor = Color(0xFFFFEBEE),
                                    textColor = Color(0xFFF44336)
                                )
                            }
                        }
                        
                        // Latest notification preview - clickable to navigate to project
                        if (latestExpenseNotifications.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Latest: ${latestExpenseNotifications.first().message}",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable {
                                    // Navigate to the specific project from the latest notification
                                    val latestNotification = latestExpenseNotifications.first()
                                    if (latestNotification.projectId.isNotEmpty()) {
                                        onProjectSelected(latestNotification.projectId)
                                        // Mark as read when navigating
                                        notificationViewModel.markNotificationAsRead(latestNotification.id)
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { onNotificationClick(currentUserId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("View All Updates")
                        }
                    }
                }
            }
            
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
                                text = "Loading projects from Firebase...",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
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
                                text = "❌ Error Loading Projects",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red,
                                textAlign = TextAlign.Center
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
                                    projectViewModel.clearError()
                                    projectViewModel.loadProjects() 
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4285F4)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                projects.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "📋 No Projects Found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Make sure you have projects in your Firebase Firestore database in the 'projects' collection.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { projectViewModel.loadProjects() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4285F4)
                                )
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "Found ${projects.size} project${if (projects.size != 1) "s" else ""}:",
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(projects) { project ->
                            val notificationSummary = projectNotificationSummaries.find { it.projectId == project.id }
                            if (notificationSummary != null) {
                                ProjectNotificationSummaryCard(
                                    summary = notificationSummary,
                                    onProjectClick = { onProjectSelected(project.id) },
                                    onNotificationClick = { notification ->
                                        // Navigate to specific notification target
                                        when {
                                            notification.navigationTarget.contains("pending_approvals") -> {
                                                onProjectSelected(project.id) // Navigate to project, will handle approval navigation
                                            }
                                            notification.navigationTarget.contains("expense_list") -> {
                                                onProjectSelected(project.id) // Navigate to project, will handle expense navigation
                                            }
                                            else -> {
                                                onProjectSelected(project.id)
                                            }
                                        }
                                        notificationViewModel.markNotificationAsRead(notification.id)
                                    }
                                )
                            } else {
                                ProjectCard(
                                    project = project,
                                    onProjectClick = { onProjectSelected(project.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onProjectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project Code Circle
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4285F4).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = project.code.ifEmpty { project.name.take(2).uppercase() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Project Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Budget: ${FormatUtils.formatCurrency(project.budget)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                // Show end date if available
                project.endDate?.let { endDate ->
                    val daysLeft = calculateDaysLeft(endDate.toDate().time)
                    if (daysLeft > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📅 Ends: ${formatDate(endDate)} ($daysLeft days left)",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }
    }
}

private fun calculateDaysLeft(endDate: Long): Long {
    val currentTime = System.currentTimeMillis()
    val diffInMillis = endDate - currentTime
    return diffInMillis / (1000 * 60 * 60 * 24)
}

private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
    val calendar = Calendar.getInstance()
    calendar.time = timestamp.toDate()
    return "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
}

private fun formatDate(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    return "${calendar.get(Calendar.DAY_OF_MONTH)}/${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.YEAR)}"
}

@Composable
fun StatusChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
} 