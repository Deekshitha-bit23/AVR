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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExitToApp
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
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.viewmodel.ProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.utils.FormatUtils
import java.text.NumberFormat
import java.util.*
import com.deeksha.avr.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectSelectionScreen(
    onProjectSelected: (String) -> Unit,
    onNotificationClick: (String) -> Unit = {},
    onNavigateToChat: (String, String) -> Unit = { _, _ -> }, // projectId, projectName
    onLogout: () -> Unit = {},
    currentUserId: String = "", // Make this optional with default empty string
    projectViewModel: ProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val projects by projectViewModel.projects.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()
    val error by projectViewModel.error.collectAsState()
    
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val isNotificationsLoading by notificationViewModel.isLoading.collectAsState()
    
    // Get current user from AuthViewModel
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
    
    // Use currentUser.uid if available, otherwise fall back to passed currentUserId
    val effectiveUserId = currentUser?.phone ?: "1234567891"
    
    // Track if we've attempted to refresh authentication
    var hasAttemptedAuthRefresh by remember { mutableStateOf(false) }
    
    // Load projects and notifications when user is authenticated
    LaunchedEffect(currentUser, authState.isAuthenticated) {
        android.util.Log.d("ProjectSelectionScreen", "🔄 Auth state changed - isAuthenticated: ${authState.isAuthenticated}, user: ${currentUser?.name}")
        
        if (authState.isAuthenticated && currentUser != null && effectiveUserId.isNotEmpty()) {
            android.util.Log.d("ProjectSelectionScreen", "✅ User authenticated, loading data for: $effectiveUserId")
            android.util.Log.d("ProjectSelectionScreen", "✅ User: ${currentUser.name}, Role: ${currentUser.role}")
            
            // Load notifications
            notificationViewModel.forceLoadNotifications(effectiveUserId)
            
            // Load projects
            projectViewModel.loadProjects(effectiveUserId)
        } else {
            android.util.Log.d("ProjectSelectionScreen", "⚠️ User not authenticated or missing data - isAuthenticated: ${authState.isAuthenticated}, user: ${currentUser?.name}")
        }
    }
    
    // Show loading state while checking authentication
    if (authState.isLoading) {
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
                    text = "Checking authentication...",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }
    
    // Show error if authentication failed
    if (authState.error != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "❌ Authentication Error",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = authState.error ?: "Unknown error occurred",
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        authViewModel.clearError()
                        authViewModel.forceCheckAuthState()
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
        return
    }
    
    // Show message if user is not authenticated
    if (!authState.isAuthenticated || currentUser == null) {
        // Also check if the user is authenticated using the AuthViewModel method
        val isUserAuthenticated = authViewModel.isUserAuthenticated()
        
        if (!isUserAuthenticated) {
            // Force check authentication state when this screen loads (only once)
            LaunchedEffect(Unit) {
                if (!hasAttemptedAuthRefresh) {
                    android.util.Log.d("ProjectSelectionScreen", "🔍 Authentication check triggered - isAuthenticated: ${authState.isAuthenticated}, user: ${currentUser?.name}")
                    android.util.Log.d("ProjectSelectionScreen", "🔍 Auth state details - isLoading: ${authState.isLoading}, error: ${authState.error}")
                    
                    hasAttemptedAuthRefresh = true
                    
                    // Add a small delay to allow for state synchronization
                    delay(500)
                    authViewModel.forceCheckAuthState()
                    
                    // Wait a bit more for the state to update
                    delay(500)
                }
            }
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🔐 Authentication Required",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please log in to access projects",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            android.util.Log.d("ProjectSelectionScreen", "🔄 Manual authentication refresh triggered")
                            authViewModel.forceCheckAuthState()
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
                        Text("Check Authentication")
                    }
                }
            }
            return
        } else {
            // User is authenticated, reset the refresh flag
            hasAttemptedAuthRefresh = false
        }
    } else {
        // User is authenticated, reset the refresh flag
        hasAttemptedAuthRefresh = false
    }
    
    LaunchedEffect(Unit) {
        println("🎬 ProjectSelectionScreen: Loading projects...")
        println("🎬 ProjectSelectionScreen: Current user ID: $effectiveUserId")
        println("🎬 ProjectSelectionScreen: Current user: ${currentUser?.name ?: "null"}")
        
        // Load projects for the current user
        if (effectiveUserId.isNotEmpty()) {
            projectViewModel.loadProjects(effectiveUserId)
        } else {
            println("⚠️ No valid user ID available for loading projects")
            projectViewModel.loadProjects() // Fallback to load all projects
        }
        
        // Force load notifications immediately if we have a valid user ID
        if (effectiveUserId.isNotEmpty()) {
            println("🔄 Force loading notifications for user: $effectiveUserId")
            notificationViewModel.forceLoadNotifications(effectiveUserId)
        } else {
            println("⚠️ No valid user ID available for loading notifications")
        }
    }
    
    // Auto-refresh notifications when user data changes
    LaunchedEffect(effectiveUserId) {
        if (effectiveUserId.isNotEmpty()) {
            println("🔄 Auto-refresh notifications for user: $effectiveUserId")
            notificationViewModel.forceLoadNotifications(effectiveUserId)
        }
    }
    
    // Set up periodic refresh for notifications (every 10 seconds for better responsiveness)
    LaunchedEffect(effectiveUserId) {
        while (true) {
            delay(10000) // 10 seconds
            if (effectiveUserId.isNotEmpty()) {
                println("🔄 Periodic refresh notifications for user: $effectiveUserId")
                notificationViewModel.refreshNotifications(effectiveUserId)
            }
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
                    if (effectiveUserId.isNotEmpty()) {
                        projectViewModel.loadProjects(effectiveUserId)
                    } else {
                        projectViewModel.loadProjects() 
                    }
                    notificationViewModel.forceLoadNotifications(effectiveUserId)
                    println("🔄 Refresh completed - projects and notifications updated")
                }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Projects",
                        tint = Color(0xFF4285F4)
                    )
                }
                
                Box {
                    IconButton(onClick = { onNotificationClick(effectiveUserId) }) {
                        if (isNotificationsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF4285F4)
                            )
                        } else {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "View Notifications",
                                tint = Color(0xFF4285F4)
                            )
                        }
                    }
                    
                    // Notification badge - only show when not loading
                    if (!isNotificationsLoading) {
                        NotificationBadgeComponent(
                            badge = notificationBadge,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                    }
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
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Projects",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray,
                modifier = Modifier.padding(bottom = 8.dp)
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
                                    if (effectiveUserId.isNotEmpty()) {
                                        projectViewModel.loadProjects(effectiveUserId)
                                    } else {
                                    projectViewModel.loadProjects() 
                                    }
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
                                text = "You don't have any projects assigned to you.",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { 
                                    if (effectiveUserId.isNotEmpty()) {
                                        projectViewModel.loadProjects(effectiveUserId)
                                    } else {
                                        projectViewModel.loadProjects()
                                    }
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
                                Text("Refresh")
                            }
                        }
                    }
                }
                else -> {
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
                            NewProjectCard(
                                project = project,
                                onProjectClick = { onProjectSelected(project.id) },
                                onChatClick = { onNavigateToChat(project.id, project.name) },
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
fun NewProjectCard(
    project: Project,
    onProjectClick: () -> Unit,
    onChatClick: () -> Unit = {},
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
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = project.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
                
                // Chat icon from first image
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(18.dp)
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
        }
    }
}
