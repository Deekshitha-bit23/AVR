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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ExitToApp
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
import com.deeksha.avr.viewmodel.ApproverProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.ui.common.ProjectNotificationSummaryCard
import com.deeksha.avr.utils.FormatUtils
import android.util.Log
import java.util.*
import com.deeksha.avr.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverProjectSelectionScreen(
    onProjectSelected: (String) -> Unit,
    onNavigateToOverallReports: () -> Unit = {},
    onNavigateToAllPendingApprovals: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onLogout: () -> Unit = {},
    currentUserId: String = "", // Make this optional with default empty string
    approverProjectViewModel: ApproverProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val projects by approverProjectViewModel.projects.collectAsState()
    val isLoading by approverProjectViewModel.isLoading.collectAsState()
    val error by approverProjectViewModel.error.collectAsState()
    
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    
    // Get current user from AuthViewModel
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    
    // Use currentUser.uid if available, otherwise fall back to passed currentUserId
    val effectiveUserId = currentUser?.phone ?: "1234567891"
    
    // Track if we've attempted to load projects
    var hasAttemptedProjectLoad by remember { mutableStateOf(false) }
    
    // Function to refresh projects
    fun refreshProjects() {
        hasAttemptedProjectLoad = false
        if (effectiveUserId.isNotEmpty() && effectiveUserId != "1234567891") {
            approverProjectViewModel.loadProjects(effectiveUserId)
        } else {
            approverProjectViewModel.loadProjects()
        }
        if (effectiveUserId.isNotEmpty()) {
            notificationViewModel.loadNotifications(effectiveUserId)
        }
    }
    
    // Refresh user data when screen opens to ensure current user is loaded
    LaunchedEffect(Unit) {
        println("🎬 ApproverProjectSelectionScreen: Refreshing user data...")
        authViewModel.refreshUserData()
    }
    
    // Load projects when user data is available and authenticated
    LaunchedEffect(currentUser, authState.isAuthenticated, effectiveUserId) {
        println("🎬 ApproverProjectSelectionScreen: Auth state changed - isAuthenticated: ${authState.isAuthenticated}, user: ${currentUser?.name}")
        println("🎬 ApproverProjectSelectionScreen: Effective User ID: $effectiveUserId")
        
        if (authState.isAuthenticated && currentUser != null && effectiveUserId.isNotEmpty() && !hasAttemptedProjectLoad) {
            println("✅ User authenticated, loading projects for: $effectiveUserId")
            println("✅ User: ${currentUser.name}, Role: ${currentUser.role}")
            
            hasAttemptedProjectLoad = true
            
            // Load projects for the approver
            approverProjectViewModel.loadProjects(effectiveUserId)
            
            // Load notifications
            notificationViewModel.loadNotifications(effectiveUserId)
        } else if (!authState.isAuthenticated || currentUser == null) {
            println("⚠️ User not authenticated or missing data - isAuthenticated: ${authState.isAuthenticated}, user: ${currentUser?.name}")
            hasAttemptedProjectLoad = false
        }
    }
    
    // Retry loading projects if user data becomes available later
    LaunchedEffect(effectiveUserId) {
        if (effectiveUserId.isNotEmpty() && effectiveUserId != "1234567891" && !hasAttemptedProjectLoad) {
            println("🔄 Retrying project load for user: $effectiveUserId")
            hasAttemptedProjectLoad = true
            approverProjectViewModel.loadProjects(effectiveUserId)
            notificationViewModel.loadNotifications(effectiveUserId)
        }
    }
    
    // Auto-retry on error with a delay
    LaunchedEffect(error) {
        if (error != null && effectiveUserId.isNotEmpty() && effectiveUserId != "1234567891") {
            println("🔄 Auto-retrying project load due to error: $error")
            delay(2000) // Wait 2 seconds before retry
            approverProjectViewModel.clearError()
            approverProjectViewModel.loadProjects(effectiveUserId)
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
                IconButton(onClick = { refreshProjects() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Projects",
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
                
                // Logout button
                IconButton(onClick = { 
                    println("🚪 Logout button clicked")
                    authViewModel.logout()
                    onLogout()
                }) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFF4285F4)
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
            !authState.isAuthenticated || currentUser == null -> {
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
                            text = "Loading user data...",
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please wait while we verify your account",
                            fontSize = 12.sp,
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
                                approverProjectViewModel.loadProjects(effectiveUserId) 
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "User ID: $effectiveUserId",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                hasAttemptedProjectLoad = false
                                if (effectiveUserId.isNotEmpty() && effectiveUserId != "1234567891") {
                                    approverProjectViewModel.loadProjects(effectiveUserId)
                                } else {
                                    approverProjectViewModel.loadProjects()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Retry Loading Projects")
                        }
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
                        text = "Your Projects",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "${projects.size} projects",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(projects) { project ->
                            ApproverProjectCard(
                                project = project,
                                onProjectClick = { onProjectSelected(project.id) },
                                projectNotifications = notifications.filter { it.projectId == project.id }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApproverProjectCard(
    project: Project,
    onProjectClick: () -> Unit,
    projectNotifications: List<com.deeksha.avr.model.Notification> = emptyList()
) {
    // Calculate project-specific notification counts
    val projectNotificationCount = projectNotifications.count { !it.isRead }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Project name and code
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
                    // Project Initial Circle
            Box(
                modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4285F4).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                            text = project.code.ifEmpty { project.name.take(2).uppercase() },
                            fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
                        // Notification badge
                if (projectNotificationCount > 0) {
                    Box(
                        modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                                    text = projectNotificationCount.toString(),
                                    color = Color.White,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = project.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
            }
            
            // Project description
            if (project.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = project.description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Budget
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹",
                    fontSize = 14.sp,
                                            color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 2.dp)
                )
                Text(
                    text = FormatUtils.formatCurrency(project.budget),
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Date range and team members in same row
            if (project.startDate != null && project.endDate != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Date range
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📅",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                        Text(
                            text = "${FormatUtils.formatDate(project.startDate)} - ${FormatUtils.formatDate(project.endDate)}",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                    
                    // Days left and team members
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Days left
                        val daysLeft = FormatUtils.calculateDaysLeft(project.endDate.toDate().time)
                        Text(
                            text = "${daysLeft} days left",
                            fontSize = 12.sp,
                            color = if (daysLeft > 10) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        
                        // Team members count
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👥",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                            Text(
                                text = "${project.teamMembers.size} members",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            } else {
                // If no date range, show only team members
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Text(
                        text = "${project.teamMembers.size} members",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
            
            // Temporary approver indicator
                if (project.temporaryApproverPhone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                    Row(
                    verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏰",
                            fontSize = 12.sp,
                        modifier = Modifier.padding(end = 2.dp)
                        )
                        Text(
                            text = "Temporary Assignment",
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
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