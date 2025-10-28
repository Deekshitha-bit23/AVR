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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
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
import com.deeksha.avr.ui.view.common.NewProjectCard
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
            .background(Color(0xFFF8F8F8))
    ) {
        // Top Bar - iOS style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Notification, Overall Reports icon and hamburger menu on the right
            var showMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification icon with badge
                Box {
                    IconButton(
                        onClick = onNavigateToNotifications
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.Black
                        )
                    }
                    // Notification badge
                    if (notificationBadge.hasUnread && notificationBadge.count > 0) {
                        Badge(
                        modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Text(
                                text = if (notificationBadge.count > 99) "99+" else notificationBadge.count.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                
                // Overall Reports icon
                IconButton(
                    onClick = onNavigateToOverallReports
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Overall Reports",
                        tint = Color.Black
                    )
                }
                
                // Hamburger menu
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Menu",
                        tint = Color.Black
                    )
                }
            }

            if (showMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showMenu = false },
                containerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Menu",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Settings & Account",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                            IconButton(onClick = { showMenu = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign Out
                        ApproverMenuItem(
                            icon = Icons.Default.ExitToApp,
                            iconColor = Color.White,
                            iconBackgroundColor = Color.Red,
                            title = "Sign Out",
                            subtitle = "Logout from your account",
                            onClick = {
                                showMenu = false
                                authViewModel.logout()
                                onLogout()
                            },
                            titleColor = Color.Red,
                            subtitleColor = Color.Red,
                            containerColor = Color(0xFFFFEBEE)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
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
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    // Title - iOS style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Projects",
                            fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                            color = Color.Black
                    )
                    }
                    
                    Text(
                        text = "${projects.size} ${if (projects.size == 1) "project" else "projects"}",
                        fontSize = 15.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(projects) { project ->
                            NewProjectCard(
                                project = project,
                                onProjectClick = { onProjectSelected(project.id) },
                                onChatClick = {},
                                projectNotifications = notifications.filter { it.projectId == project.id },
                                showChatIcon = false
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
    // Calculate days left
    val daysLeft = if (project.endDate != null) {
        FormatUtils.calculateDaysLeft(project.endDate.toDate().time)
    } else 0
    
    // Get project initials for badge
    val projectInitials = project.code.ifEmpty { project.name.split(" ").take(2).map { it.firstOrNull()?.uppercaseChar() ?: "" }.joinToString("").ifEmpty { project.name.take(2).uppercase() } }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Title row with project name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
                    // Project Initial Badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                                    .clip(CircleShape)
                            .background(Color(0xFF42A5F5).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = projectInitials,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF42A5F5)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                    Text(
                        text = project.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹" + FormatUtils.formatCurrency(project.budget),
                        fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Right column - Date, Days Left, Members
            if (project.startDate != null && project.endDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left column - Date range
                    Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF42A5F5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        Text(
                                text = "${FormatUtils.formatDateShort(project.startDate)} - ${FormatUtils.formatDateShort(project.endDate)}",
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }
                    
                    // Right column
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Days left
                        Text(
                            text = if (daysLeft >= 0) "$daysLeft days left" else "Overdue by ${-daysLeft} days",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (daysLeft >= 0) Color(0xFF4CAF50) else Color.Red
                        )
                        
                        // Members
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF42A5F5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${project.teamMembers.size} members",
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
            
            // Temporary approver indicator
                if (project.temporaryApproverPhone.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                    Row(
                    verticalAlignment = Alignment.CenterVertically
                    ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Temporary Assignment",
                        fontSize = 13.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                }
            }
        }
    }
}

@Composable
private fun ApproverMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBackgroundColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color = Color.Black,
    subtitleColor: Color = Color.Gray,
    containerColor: Color = Color.White
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = subtitleColor
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
} 