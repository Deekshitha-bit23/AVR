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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DateRange
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
import androidx.compose.ui.graphics.vector.ImageVector


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
            .background(Color(0xFFF8F8F8))
    ) {
        // Top Bar - iOS style
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Notifications icon and hamburger menu on the right
            var showMenu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification icon with badge
                Box {
                    IconButton(
                        onClick = { onNotificationClick(effectiveUserId) }
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
                
                // Hamburger menu
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        Icons.Default.Menu,
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

                        // About
                        UserMenuItem(
                            icon = Icons.Default.Info,
                            iconColor = Color(0xFF42A5F5),
                            iconBackgroundColor = Color(0xFFE3F2FD),
                            title = "About",
                            subtitle = "App information & version",
                            onClick = { /* reserved for About navigation */ }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Settings
                        UserMenuItem(
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFF42A5F5),
                            iconBackgroundColor = Color(0xFFE3F2FD),
                            title = "Settings",
                            subtitle = "Preferences & configuration",
                            onClick = { /* reserved for Settings navigation */ }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Sign Out
                        UserMenuItem(
                            icon = Icons.Default.ExitToApp,
                            iconColor = Color.White,
                            iconBackgroundColor = Color.Red,
                            title = "Sign Out",
                            subtitle = "Logout from your account",
                            onClick = {
                                showMenu = false
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
    projectNotifications: List<com.deeksha.avr.model.Notification> = emptyList(),
    showChatIcon: Boolean = true
) {
    // Calculate days left
    val daysLeft = if (project.endDate != null) {
        FormatUtils.calculateDaysLeft(project.endDate.toDate().time)
    } else 0
    
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
            // Title row with project name and category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = project.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    if (project.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = project.description,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            maxLines = 1
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status badge with green dot
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    
                    // Optional Chat button - circular with chat icon
                    if (showChatIcon) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onChatClick)
                                .background(Color(0xFFE3F2FD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = "Chat",
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bottom details row - Amount and Members in left column, Date and days in right column
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Amount
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 14.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = FormatUtils.formatCurrency(project.budget),
                            fontSize = 14.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Members
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${project.teamMembers.size} member${if (project.teamMembers.size != 1) "s" else ""}",
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                }
                
                // Right column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Date range
                    if (project.startDate != null && project.endDate != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
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
                        
                        // Days left
                        Text(
                            text = if (daysLeft >= 0) "$daysLeft days left" else "Overdue by ${-daysLeft} days",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (daysLeft >= 0) Color(0xFF4CAF50) else Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMenuItem(
    icon: ImageVector,
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
