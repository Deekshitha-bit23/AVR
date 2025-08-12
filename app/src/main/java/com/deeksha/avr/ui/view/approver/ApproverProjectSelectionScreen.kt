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
                        text = "Choose a project to continue:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "Found ${projects.size} projects:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(projects) { project ->
                            ProjectCard(
                                project = project,
                                onProjectClick = { onProjectSelected(project.id) },
                                projectNotifications = notifications.filter { it.projectId == project.id }
                            )
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
    onProjectClick: () -> Unit,
    projectNotifications: List<com.deeksha.avr.model.Notification> = emptyList()
) {
    // Calculate project-specific notification counts
    val projectNotificationCount = projectNotifications.count { !it.isRead }
    val projectChangedCount = projectNotifications.count { 
        it.type == com.deeksha.avr.model.NotificationType.PROJECT_CHANGED && !it.isRead 
    }
    val projectAssignmentCount = projectNotifications.count { 
        it.type == com.deeksha.avr.model.NotificationType.PROJECT_ASSIGNMENT && !it.isRead 
    }
    
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
            // Project Initial Circle with notification indicator
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
                    text = project.code.ifEmpty { getProjectInitials(project.name) },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
                
                // Notification badge for this project
                if (projectNotificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .background(
                                color = Color(0xFFF44336),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (projectNotificationCount > 9) "9+" else projectNotificationCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Project Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = project.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    // Project notification status indicators
                    if (projectNotificationCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (projectChangedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = Color(0xFF9C27B0),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✏",
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            if (projectAssignmentCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = Color(0xFF4CAF50),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓",
                                        fontSize = 8.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Budget: ${FormatUtils.formatCurrency(project.budget)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                // Show project end date if available
                project.endDate?.let { endDate ->
                    Spacer(modifier = Modifier.height(2.dp))
                    val daysLeft = FormatUtils.calculateDaysLeft(endDate.toDate().time)
                    val formattedDate = FormatUtils.formatDate(endDate)
                    val daysText = when {
                        daysLeft > 0 -> "(${daysLeft} days left)"
                        daysLeft == 0L -> "(Today)"
                        else -> "(${kotlin.math.abs(daysLeft)} days overdue)"
                    }
                    Text(
                        text = "📅 Ends: $formattedDate $daysText",
                        fontSize = 12.sp,
                        color = if (daysLeft >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
                
                // Show project-specific notification summary
                if (projectNotificationCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (projectChangedCount > 0) {
                            Text(
                                text = "✏ $projectChangedCount changes",
                                fontSize = 11.sp,
                                color = Color(0xFF9C27B0),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (projectAssignmentCount > 0) {
                            Text(
                                text = "✓ $projectAssignmentCount assignments",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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