package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.deeksha.avr.viewmodel.ProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.ui.common.ProjectNotificationSummaryCard
import com.google.android.libraries.intelligence.acceleration.Analytics
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHeadProjectSelectionScreen(
    onProjectSelected: (String) -> Unit,
    onCreateUser: () -> Unit,
    onNewProject: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToOverallReports: () -> Unit = {},
    onNavigateToAllPendingApprovals: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    currentUserId: String,
    projectViewModel: ProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val projects by projectViewModel.projects.collectAsState()
    val isLoading by projectViewModel.isLoading.collectAsState()
    
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    
    LaunchedEffect(Unit) {
        projectViewModel.loadProjects()
        notificationViewModel.loadNotifications(currentUserId)
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
                            contentDescription = "Analytics",
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
                        contentDescription = "New Project",
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
                text = "Choose a project to continue:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(projects) { project ->
                            ProjectCard(
                                project = project,
                                onClick = { onProjectSelected(project.id) }
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
fun ProjectCard(
    project: Project,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = project.code.ifEmpty { 
                        project.name.take(2).uppercase() 
                    },
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                formatter.currency = Currency.getInstance("INR")
                val budgetText = formatter.format(project.budget)
                
                Text(
                    text = "Budget: $budgetText",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                // Show end date if available
                project.endDate?.let { endDate ->
                    val calendar = Calendar.getInstance()
                    calendar.time = endDate.toDate()
                    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val year = calendar.get(Calendar.YEAR)
                    
                    Text(
                        text = "Ends: $dayOfMonth/${String.format("%02d", month)}/$year",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
} 