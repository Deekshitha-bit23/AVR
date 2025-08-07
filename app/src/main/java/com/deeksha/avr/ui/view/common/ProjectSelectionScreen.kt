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
import com.deeksha.avr.viewmodel.ProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.ui.common.ProjectNotificationSummaryCard
import com.deeksha.avr.utils.FormatUtils
import java.text.NumberFormat
import java.util.*
import com.deeksha.avr.viewmodel.AuthViewModel
import androidx.compose.material.icons.filled.ExitToApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSelectionScreen(
    onProjectSelected: (String) -> Unit,
    onNotificationClick: (String) -> Unit = {},
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
                    kotlinx.coroutines.delay(500)
                    authViewModel.forceCheckAuthState()
                    
                    // Wait a bit more for the state to update
                    kotlinx.coroutines.delay(500)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Status: ${if (authState.isLoading) "Checking..." else "Not authenticated"}",
                        fontSize = 12.sp,
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
            kotlinx.coroutines.delay(10000) // 10 seconds
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
                text = "Choose a project to continue:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
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
                                text = "Make sure you have projects in your Firebase Firestore database in the 'projects' collection.",
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
                            ProjectCard(
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
fun ProjectCard(
    project: Project,
    onProjectClick: () -> Unit,
    projectNotifications: List<com.deeksha.avr.model.Notification> = emptyList()
) {
    // Calculate project-specific notification counts
    val projectNotificationCount = projectNotifications.count { !it.isRead }
    val projectApprovedCount = projectNotifications.count { 
        it.type == com.deeksha.avr.model.NotificationType.EXPENSE_APPROVED && !it.isRead 
    }
    val projectRejectedCount = projectNotifications.count { 
        it.type == com.deeksha.avr.model.NotificationType.EXPENSE_REJECTED && !it.isRead 
    }
    
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
            // Project Code Circle with notification indicator
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
                
                // Notification badge for this project
                if (projectNotificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336)),
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                    
                    // Project notification status indicators
                    if (projectNotificationCount > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (projectApprovedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50)),
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
                            
                            if (projectRejectedCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF44336)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✗",
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
                
                // Show end date if available
                project.endDate?.let { endDate ->
                    val daysLeft = FormatUtils.calculateDaysLeft(endDate.toDate().time)
                    Spacer(modifier = Modifier.height(4.dp))
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
                        if (projectApprovedCount > 0) {
                            Text(
                                text = "✅ $projectApprovedCount approved",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        if (projectRejectedCount > 0) {
                            Text(
                                text = "❌ $projectRejectedCount rejected",
                                fontSize = 11.sp,
                                color = Color(0xFFF44336),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}



 