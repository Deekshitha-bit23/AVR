package com.deeksha.avr.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
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
import com.deeksha.avr.model.Project
import com.deeksha.avr.viewmodel.ApproverProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.ui.common.ProjectNotificationSummaryCard
import com.deeksha.avr.utils.FormatUtils
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverProjectSelectionScreen(
    onProjectSelected: (String) -> Unit,
    onNavigateToOverallReports: () -> Unit = {},
    onNavigateToAllPendingApprovals: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    currentUserId: String,
    approverProjectViewModel: ApproverProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val projects by approverProjectViewModel.projects.collectAsState()
    val isLoading by approverProjectViewModel.isLoading.collectAsState()
    val error by approverProjectViewModel.error.collectAsState()
    
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val projectNotificationSummaries by notificationViewModel.projectNotificationSummaries.collectAsState()
    
    // Load projects when screen starts
    LaunchedEffect(Unit) {
        approverProjectViewModel.loadProjects()
        notificationViewModel.setUserId(currentUserId)
    }
    
    LaunchedEffect(projects) {
        if (projects.isNotEmpty()) {
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
                        fontSize = 22.sp,
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
                IconButton(onClick = onNavigateToOverallReports) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Overall Reports",
                        tint = Color(0xFF4285F4)
                    )
                }
                Box {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
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
                            text = "Loading projects...",
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
                            text = "❌ Error Loading Projects",
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
                                approverProjectViewModel.clearError()
                                approverProjectViewModel.loadProjects() 
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
                            text = "📋 No Projects Available",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No projects found for review.",
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
                    Text(
                        text = "Choose a project to continue:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                                onProjectSelected(project.id) // Navigate to project dashboard
                                            }
                                            notification.navigationTarget.contains("expense_list") -> {
                                                onProjectSelected(project.id) // Navigate to project dashboard
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
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: Project,
    onProjectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Project Initial Circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = Color(0xFFE3F2FD),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getProjectInitials(project.name),
                    fontSize = 18.sp,
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Budget: ${FormatUtils.formatCurrency(project.budget)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                // Show project end date if available
                project.endDate?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📅 Ends: ${it}",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

private fun getProjectInitials(projectName: String): String {
    return projectName
        .split(" ")
        .take(2)
        .map { it.firstOrNull()?.uppercaseChar() ?: "" }
        .joinToString("")
        .ifEmpty { projectName.take(2).uppercase() }
} 