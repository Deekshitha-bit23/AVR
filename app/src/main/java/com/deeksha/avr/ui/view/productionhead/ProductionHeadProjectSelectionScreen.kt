package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import com.deeksha.avr.viewmodel.ProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.ui.common.ProjectNotificationSummaryCard
import com.google.android.libraries.intelligence.acceleration.Analytics
import com.deeksha.avr.utils.FormatUtils
import java.util.*
import com.deeksha.avr.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHeadProjectSelectionScreen(
    onProjectSelected: (String) -> Unit,
    onCreateUser: () -> Unit,
    onNewProject: () -> Unit,
    onEditProject: (String) -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToOverallReports: () -> Unit = {},
    onNavigateToAllPendingApprovals: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onLogout: () -> Unit = {},
    currentUserId: String = "", // Make this optional with default empty string
    projectViewModel: ProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel
) {
    val projects by projectViewModel.projects.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()
    
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    
    // Get current user from AuthViewModel
    val authState by authViewModel.authState.collectAsState()
    val currentUser = authState.user
//
//    // Use currentUser.uid if available, otherwise fall back to passed currentUserId
//    val effectiveUserId = currentUser?.uid ?: currentUserId
    
    // Refresh user data when screen opens to ensure current user is loaded
    LaunchedEffect(Unit) {
        println("🎬 ProductionHeadProjectSelectionScreen: Refreshing user data...")
        authViewModel.refreshUserData()
    }
    
    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
    }
    
    Scaffold(
        topBar = {
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
                    // Analytics/Reports icon
                    IconButton(onClick = onNavigateToOverallReports) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Analysis",
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
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Create User FAB
                FloatingActionButton(
                    onClick = onCreateUser,
                    containerColor = Color(0xFF4285F4),
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Create User",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // New Project FAB
                FloatingActionButton(
                    onClick = onNewProject,
                    containerColor = Color(0xFF4285F4),
                    contentColor = Color.White,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New project",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
            
            if (isLoading) {
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
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projects) { project ->
                        ProductionHeadProjectCard(
                            project = project,
                            onClick = { onProjectSelected(project.id) },
                            onEditClick = { onEditProject(project.id) }
                        )
                    }
                    
                    // Add bottom spacing for FABs
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ProductionHeadProjectCard(
    project: Project,
    onClick: () -> Unit,
    onEditClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                
                // Edit Button
                IconButton(
                    onClick = { onEditClick() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Project",
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