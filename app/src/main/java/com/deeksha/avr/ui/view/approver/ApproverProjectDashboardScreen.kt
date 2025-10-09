package com.deeksha.avr.ui.view.approver

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.viewmodel.ApproverProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.viewmodel.TemporaryApproverViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.utils.FormatUtils
import com.deeksha.avr.model.CategoryBudget
import com.deeksha.avr.model.DepartmentBudgetBreakdown
import com.deeksha.avr.model.ProjectBudgetSummary
import com.deeksha.avr.model.User
import com.deeksha.avr.model.TemporaryApprover
import com.deeksha.avr.repository.AuthRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.compose.runtime.remember
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DrawerState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverProjectDashboardScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    onNavigateToAddExpense: () -> Unit = {},
    onNavigateToReports: (String) -> Unit = {},
    onNavigateToAnalytics: (String) -> Unit = {},
    onNavigateToDepartmentDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToProjectNotifications: (String) -> Unit = {},
    onNavigateToDelegation: () -> Unit = {},
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    approverProjectViewModel: ApproverProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    temporaryApproverViewModel: TemporaryApproverViewModel = hiltViewModel(),
) {
    val projectBudgetSummary by approverProjectViewModel.projectBudgetSummary.collectAsState()
    val isLoading by approverProjectViewModel.isLoading.collectAsState()
    val error by approverProjectViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // State for temporary approver user data
    var temporaryApproverUser by remember { mutableStateOf<User?>(null) }
    var isLoadingTemporaryApprover by remember { mutableStateOf(false) }
    
    // State for temporary approver data
    val temporaryApprovers by temporaryApproverViewModel.temporaryApprovers.collectAsState()
    val currentTemporaryApprover = temporaryApprovers.firstOrNull()
    
    // Create AuthRepository instance
    val authRepository = remember { AuthRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance()) }
    
    // Project-specific notifications
    val notifications by notificationViewModel.notifications.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()

    // Filter notifications for this project
    val projectNotifications = notifications.filter { it.projectId == projectId }
    val projectNotificationBadge = remember(projectNotifications) {
        val unreadCount = projectNotifications.count { !it.isRead }
        com.deeksha.avr.model.NotificationBadge(
            count = unreadCount,
            hasUnread = unreadCount > 0
        )
    }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Track if the screen is resumed/visible for auto-refresh
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Load project data when screen starts
    LaunchedEffect(projectId) {
        approverProjectViewModel.loadProjectBudgetSummary(projectId)
        temporaryApproverViewModel.loadTemporaryApprovers(projectId)
        // Initialize notifications for this user
        authState.user?.uid?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
    }
    
    // Load temporary approver user data when temporary approver data is loaded
    LaunchedEffect(currentTemporaryApprover) {
        if (currentTemporaryApprover != null) {
            isLoadingTemporaryApprover = true
            try {
                val user = authRepository.getUserByPhoneNumber(currentTemporaryApprover.approverPhone)
                temporaryApproverUser = user
            } catch (e: Exception) {
                Log.e("ApproverProjectDashboard", "Error loading temporary approver user: ${e.message}")
            } finally {
                isLoadingTemporaryApprover = false
            }
        } else {
            temporaryApproverUser = null
        }
    }
    
    // Auto-refresh when screen becomes visible again (e.g., after returning from expense approval)
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0) {
            kotlinx.coroutines.delay(500) // Small delay to ensure smooth transition
            approverProjectViewModel.refreshProjectData()
            lastRefreshTime = System.currentTimeMillis()
        }
    }
    
    // Trigger refresh when the composable is recomposed (user returns to screen)
    DisposableEffect(projectId) {
        // Refresh when the user returns to this screen
        if (lastRefreshTime > 0 && System.currentTimeMillis() - lastRefreshTime > 1000) {
            refreshTrigger++
        }
        
        onDispose { 
            // Update timestamp when leaving the screen
            lastRefreshTime = System.currentTimeMillis()
        }
    }
    
    // Additional refresh trigger when screen regains focus (backup mechanism)
    LaunchedEffect(Unit) {
        // Periodic check to ensure data freshness when screen is active
        while (true) {
            kotlinx.coroutines.delay(5000) // Check every 5 seconds
            if (System.currentTimeMillis() - lastRefreshTime > 10000) { // If data is stale (10+ seconds)
                refreshTrigger++
                break // Exit loop after triggering refresh
            }
        }
    }
    
    // Listen for changes and refresh periodically when data might be stale
    LaunchedEffect(projectBudgetSummary) {
        // This ensures we refresh after any potential data changes
        lastRefreshTime = System.currentTimeMillis()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ApproverNavigationDrawer(
                projectId = projectId,
                onNavigateToDashboard = {
                    scope.launch { drawerState.close() }
                    // Already on dashboard
                },
                onNavigateToPendingApprovals = {
                    scope.launch { drawerState.close() }
                    onNavigateToPendingApprovals(projectId)
                },
                onNavigateToAddExpense = {
                    scope.launch { drawerState.close() }
                    onNavigateToAddExpense()
                },
                onNavigateToAnalytics = {
                    scope.launch { drawerState.close() }
                    onNavigateToAnalytics(projectId)
                },
                onNavigateToDelegation = {
                    scope.launch { drawerState.close() }
                    onNavigateToDelegation()
                },
                onNavigateToChat = { projectId, projectName ->
                    scope.launch { drawerState.close() }
                    onNavigateToChat(projectId, projectName)
                },
                projectName = projectBudgetSummary.project?.name ?: "Project",
                userRole = authState.user?.role?.name ?: "APPROVER"
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (projectBudgetSummary.project != null) {
                            Text(
                                text = projectBudgetSummary.project?.name ?: "",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "ACTIVE",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF4285F4)
                        )
                    }
                        Text(
                            text = "Back",
                            color = Color(0xFF4285F4),
                            fontSize = 14.sp
                        )
                    }
                },
                actions = {
                    // Notifications button
                    Box {
                        IconButton(onClick = { onNavigateToProjectNotifications(projectId) }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Project Notifications",
                                tint = Color.Black
                            )
                        }
                        
                        // Project-specific notification badge
                        NotificationBadgeComponent(
                            badge = projectNotificationBadge,
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
                                text = "Loading project dashboard...",
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
                                text = "❌ Error Loading Dashboard",
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
                                    approverProjectViewModel.loadProjectBudgetSummary(projectId)
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
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            // Project Overview Section
                            ProjectOverviewSection(
                                projectBudgetSummary = projectBudgetSummary,
                                temporaryApproverUser = temporaryApproverUser,
                                isLoadingTemporaryApprover = isLoadingTemporaryApprover,
                                currentTemporaryApprover = currentTemporaryApprover
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Department Budgets Section
                            DepartmentBudgetsSection(
                                departmentBreakdown = projectBudgetSummary.departmentBreakdown,
                                onNavigateToDepartmentDetail = onNavigateToDepartmentDetail,
                                projectId = projectId
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Department Distribution Section
                            DepartmentDistributionSection(
                                departmentBreakdown = projectBudgetSummary.departmentBreakdown,
                                totalBudget = projectBudgetSummary.totalBudget,
                                onNavigateToReports = { onNavigateToReports(projectId) },
                                scope = scope,
                                drawerState = drawerState
                            )
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        
                        // Floating Action Buttons - Left and Right corners
                        // Left FAB - Open Navigation Drawer
                        FloatingActionButton(
                            onClick = { 
                                scope.launch { drawerState.open() }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 16.dp)
                                .size(56.dp),
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(28.dp),
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Open Menu",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Right FAB - Project Report
                        FloatingActionButton(
                            onClick = { onNavigateToReports(projectId) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp)
                                .size(56.dp),
                            containerColor = Color(0xFF4285F4),
                            contentColor = Color.White,
                            shape = RoundedCornerShape(28.dp),
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 8.dp,
                                pressedElevation = 12.dp
                            )
                        ) {
                            Icon(
                                Icons.Default.Description,
                                contentDescription = "Project Report",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApproverNavigationDrawer(
    projectId: String,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPendingApprovals: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToDelegation: () -> Unit = {},
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    projectName: String = "Project",
    userRole: String = "APPROVER"
) {
    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color(0xFFFCE4EC)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "AVR ENTERTAINMENT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4),
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Menu Items with colorful circular backgrounds
            if (userRole == "PRODUCTION_HEAD") {
                ColorfulDrawerMenuItem(
                    icon = Icons.Default.Person,
                    iconColor = Color.White,
                    backgroundColor = Color(0xFF9C27B0),
                    title = "Delegate",
                    onClick = onNavigateToDelegation
                )
            }
            
            ColorfulDrawerMenuItem(
                icon = Icons.Default.CheckCircle,
                iconColor = Color.White,
                backgroundColor = Color(0xFF4285F4),
                title = "Dashboard",
                onClick = onNavigateToDashboard
            )
            
            ColorfulDrawerMenuItem(
                icon = Icons.Default.Notifications,
                iconColor = Color.White,
                backgroundColor = Color(0xFFFF9800),
                title = "Pending Approvals",
                onClick = onNavigateToPendingApprovals                              
            )
            
            ColorfulDrawerMenuItem(
                icon = Icons.Default.Add,
                iconColor = Color.White,
                backgroundColor = Color(0xFF4CAF50),
                title = "Add Expenses",
                onClick = onNavigateToAddExpense
            )
            
            // Analytics - Only show for Production Head users
            if (userRole == "PRODUCTION_HEAD") {
                ColorfulDrawerMenuItem(
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color.White,
                    backgroundColor = Color(0xFF9C27B0),
                    title = "Analytics",
                    onClick = onNavigateToAnalytics
                )
            }
            
            ColorfulDrawerMenuItem(
                icon = Icons.Default.Person,
                iconColor = Color.White,
                backgroundColor = Color(0xFF00BCD4),
                title = "Chats",
                onClick = { onNavigateToChat(projectId, projectName) }
            )
        }
    }
}

@Composable
private fun ColorfulDrawerMenuItem(
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(backgroundColor, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.Black
        )
    }
}

@Composable
private fun DepartmentCard(
    department: DepartmentBudgetBreakdown,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() }, // Fixed height, reduced width via modifier
        colors = CardDefaults.cardColors(
            containerColor = getAttractiveDepartmentColor(department.department)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = department.department,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Column {
                Text(
                    text = "Total: ${FormatUtils.formatCurrency(department.budgetAllocated)}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Spent: ${FormatUtils.formatCurrency(department.spent)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                
                Text(
                    text = "Remaining: ${FormatUtils.formatCurrency(department.remaining)}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    department: String,
    amount: Double
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = department,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = FormatUtils.formatCurrency(amount),
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun PieChart(
    data: List<DepartmentBudgetBreakdown>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.spent }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (total > 0) {
                drawPieChart(data, total)
            } else {
                // Draw empty circle
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f),
                    radius = size.minDimension / 2
                )
            }
        }
        
        // Show "No Data" text when there are no expenses
        if (total == 0.0) {
            Text(
                text = "No Data",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun DrawScope.drawPieChart(data: List<DepartmentBudgetBreakdown>, total: Double) {
    var startAngle = -90f
    val radius = size.minDimension / 2
    
    data.forEachIndexed { index, department ->
        val sweepAngle = ((department.spent / total) * 360).toFloat()
        if (sweepAngle > 0) {
            drawArc(
                color = getDepartmentColor(department.department),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }
    }
    
    // Draw inner circle to make it a donut chart
    drawCircle(
        color = Color.White,
        radius = radius * 0.6f
    )
}

// Dynamic color assignment for categories
private fun getCategoryColor(categoryName: String): Color {
    val colors = listOf(
        Color(0xFF4285F4), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFFF5722), // Red-Orange
        Color(0xFF607D8B), // Blue-Grey
        Color(0xFF795548), // Brown
        Color(0xFF3F51B5), // Indigo
        Color(0xFF009688), // Teal
        Color(0xFFE91E63)  // Pink
    )
    
    // Use category name hash to consistently assign the same color to the same category
    val colorIndex = kotlin.math.abs(categoryName.hashCode()) % colors.size
    return colors[colorIndex]
}

// More attractive gradient-like colors for category cards
private fun getAttractiveCategoryColor(categoryName: String): Color {
    val attractiveColors = listOf(
        Color(0xFF6366F1), // Modern Indigo
        Color(0xFF10B981), // Emerald Green
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
        Color(0xFFEC4899), // Pink
        Color(0xFF84CC16), // Lime
        Color(0xFFF97316), // Orange
        Color(0xFF3B82F6), // Blue
        Color(0xFF14B8A6), // Teal
        Color(0xFFA855F7)  // Purple
    )
    
    // Use category name hash to consistently assign the same color to the same category
    val colorIndex = kotlin.math.abs(categoryName.hashCode()) % attractiveColors.size
    return attractiveColors[colorIndex]
}

// Dynamic color assignment for departments with distinct colors
private fun getDepartmentColor(departmentName: String): Color {
    return when (departmentName.lowercase()) {
        "food" -> Color(0xFF2196F3) // Blue
        "marketing" -> Color(0xFFE91E63) // Pink/Magenta
        "art" -> Color(0xFFFF9800) // Orange  
        "location" -> Color(0xFF9C27B0) // Purple
        "production" -> Color(0xFF4CAF50) // Green
        "technology" -> Color(0xFF00BCD4) // Cyan
        "logistics" -> Color(0xFFFF5722) // Red-Orange
        "finance" -> Color(0xFF8BC34A) // Light Green
        "hr" -> Color(0xFF3F51B5) // Indigo
        "legal" -> Color(0xFFFFEB3B) // Yellow
        else -> {
            // Fallback to hash-based assignment for any other departments
    val colors = listOf(
                Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFFFF9800), 
                Color(0xFF9C27B0), Color(0xFF4CAF50), Color(0xFF00BCD4),
                Color(0xFFFF5722), Color(0xFF8BC34A), Color(0xFF3F51B5), Color(0xFFFFEB3B)
            )
    val colorIndex = kotlin.math.abs(departmentName.hashCode()) % colors.size
            colors[colorIndex]
        }
    }
}

// More attractive gradient-like colors for department cards
private fun getAttractiveDepartmentColor(departmentName: String): Color {
    val attractiveColors = listOf(
        Color(0xFF6366F1), // Modern Indigo
        Color(0xFF10B981), // Emerald Green
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFF8B5CF6), // Violet
        Color(0xFF06B6D4), // Cyan
        Color(0xFFEC4899), // Pink
        Color(0xFF84CC16), // Lime
        Color(0xFFF97316), // Orange
        Color(0xFF3B82F6), // Blue
        Color(0xFF14B8A6), // Teal
        Color(0xFFA855F7)  // Purple
    )
    
    // Use department name hash to consistently assign the same color to the same department
    val colorIndex = kotlin.math.abs(departmentName.hashCode()) % attractiveColors.size
    return attractiveColors[colorIndex]
}

@Composable
private fun ProjectOverviewSection(
    projectBudgetSummary: ProjectBudgetSummary,
    temporaryApproverUser: User?,
    isLoadingTemporaryApprover: Boolean,
    currentTemporaryApprover: TemporaryApprover?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Project Overview",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Project Details",
                fontSize = 14.sp,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Dynamic Overview Cards in 2x2 Grid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Temp Approver Card
        DynamicOverviewCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Person,
            iconColor = Color(0xFFFF9800),
            value = temporaryApproverUser?.name ?: "N/A",
            label = "Temp Approver",
            subtitle = if (currentTemporaryApprover?.expiringDate != null) {
                "Until: ${FormatUtils.formatDate(currentTemporaryApprover.expiringDate)}"
            } else if (temporaryApproverUser != null) {
                "Ongoing"
            } else null
        )
        
        // Total Budget Card  
        DynamicOverviewCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Home,
            iconColor = Color(0xFFFF9800),
            value = FormatUtils.formatCurrency(projectBudgetSummary.totalBudget),
            label = "Total Budget",
            subtitle = "Remaining: ${FormatUtils.formatCurrency(projectBudgetSummary.totalRemaining)}"
        )
    }
    
    Spacer(modifier = Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Team Members Card - Dynamic count
        var showTeamMembersDialog by remember { mutableStateOf(false) }
        
        DynamicOverviewCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Person,
            iconColor = Color(0xFF4285F4),
            value = projectBudgetSummary.project?.teamMembers?.size?.toString() ?: "0",
            label = "Team Members",
            subtitle = null,
            onClick = { showTeamMembersDialog = true }
        )
        
        // Team Members Dialog
        if (showTeamMembersDialog) {
            TeamMembersDialog(
                teamMemberIds = projectBudgetSummary.project?.teamMembers ?: emptyList(),
                onDismiss = { showTeamMembersDialog = false }
            )
        }
        
        // Departments Card - Dynamic count (only departments with allocated budgets)
        DynamicOverviewCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Settings,
            iconColor = Color(0xFF9C27B0),
            value = projectBudgetSummary.departmentBreakdown.count { it.budgetAllocated > 0 }.toString(),
            label = "Departments",
            subtitle = null
        )
    }
}

@Composable
private fun DynamicOverviewCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    value: String,
    label: String,
    subtitle: String?,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Icon and Label
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            iconColor.copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
            
            // Bottom section - Value and Subtitle
            Column {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    bottomText: String?
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            iconColor.copy(alpha = 0.15f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray
                )
            }
            
            Column {
                Text(
                    text = subtitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (bottomText != null) {
                    Text(
                        text = bottomText,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun DepartmentBudgetsSection(
    departmentBreakdown: List<DepartmentBudgetBreakdown>,
    onNavigateToDepartmentDetail: (String, String) -> Unit,
    projectId: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Department Budgets",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Across All Projects",
                fontSize = 14.sp,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Department Budget Cards - Show all departments with allocated budgets
    val departmentsWithBudgets = departmentBreakdown.filter { it.budgetAllocated > 0 }
    
    if (departmentsWithBudgets.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(departmentsWithBudgets) { department ->
                DepartmentBudgetCard(
                    department = department,
                    onClick = { onNavigateToDepartmentDetail(projectId, department.department) }
                )
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No department budgets allocated",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DepartmentBudgetCard(
    department: DepartmentBudgetBreakdown,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .width(170.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = department.department,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Budget:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(department.budgetAllocated),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spent:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(department.spent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Remaining:",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(department.remaining),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar
            val progress = if (department.budgetAllocated > 0) {
                (department.spent / department.budgetAllocated).toFloat()
            } else 0f
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = Color(0xFF4285F4),
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun DepartmentDistributionSection(
    departmentBreakdown: List<DepartmentBudgetBreakdown>,
    totalBudget: Double,
    onNavigateToReports: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    drawerState: DrawerState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Department Distribution",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Budget Allocation",
                fontSize = 14.sp,
                color = Color(0xFF2E7D32),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Chart area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Donut Chart
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.Center)
                    ) {
                        DonutChart(
                            data = departmentBreakdown.filter { it.spent > 0 },
                            totalSpent = departmentBreakdown.sumOf { it.spent },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        // Center content
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Spent",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = FormatUtils.formatCurrency(departmentBreakdown.sumOf { it.spent }),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "₹",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                
                // Legend
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val departmentsWithSpending = departmentBreakdown.filter { it.spent > 0 }
                    val totalSpent = departmentsWithSpending.sumOf { it.spent }
                    departmentsWithSpending.forEach { department ->
                        LegendItem(
                            color = getDepartmentColor(department.department),
                            department = department.department,
                            amount = department.spent,
                            percentage = if (totalSpent > 0) ((department.spent / totalSpent) * 100).toInt() else 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    data: List<DepartmentBudgetBreakdown>,
    totalSpent: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (totalSpent > 0) {
                drawDonutChart(data, totalSpent)
            } else {
                // Draw empty circle
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.3f),
                    radius = size.minDimension / 2
                )
            }
        }
    }
}

private fun DrawScope.drawDonutChart(data: List<DepartmentBudgetBreakdown>, totalSpent: Double) {
    var startAngle = -90f
    val radius = size.minDimension / 2
    val innerRadius = radius * 0.6f
    
    data.forEachIndexed { index, department ->
        val sweepAngle = ((department.spent / totalSpent) * 360).toFloat()
        if (sweepAngle > 0) {
            val departmentColor = getDepartmentColor(department.department)
            drawArc(
                color = departmentColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                ),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            startAngle += sweepAngle
        }
    }
    
    // Draw inner circle to make it a donut chart
    drawCircle(
        color = Color.White,
        radius = innerRadius,
        center = androidx.compose.ui.geometry.Offset(
            size.width / 2,
            size.height / 2
        )
    )
}

@Composable
private fun LegendItem(
    color: Color,
    department: String,
    amount: Double,
    percentage: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color, RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = department,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${FormatUtils.formatCurrency(amount)} · $percentage%",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Card(
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// Team Members Dialog
@Composable
private fun TeamMembersDialog(
    teamMemberIds: List<String>,
    onDismiss: () -> Unit
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    var teamMembers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(teamMemberIds) {
        isLoading = true
        val members = mutableListOf<User>()
        
        teamMemberIds.forEach { phoneNumber ->
            try {
                val user = authViewModel.getUserByPhoneNumber(phoneNumber)
                if (user != null) {
                    members.add(user)
                }
            } catch (e: Exception) {
                // If user not found, create a placeholder with phone number
                members.add(User(uid = phoneNumber, name = "Unknown User", phone = phoneNumber))
            }
        }
        
        teamMembers = members
        isLoading = false
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Team Members",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5FBF)
            )
        },
        text = {
            if (teamMemberIds.isEmpty()) {
                Text(
                    text = "No team members assigned to this project.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF8B5FBF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(teamMembers) { member ->
                        TeamMemberItem(user = member)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Close",
                    color = Color(0xFF8B5FBF),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

// Team Member Item Component
@Composable
private fun TeamMemberItem(
    user: User
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF4285F4)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Member Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name.ifEmpty { "Unknown User" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            Text(
                text = user.role.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 14.sp,
                color = Color(0xFF4285F4)
            )
        }
        
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (user.isActive) Color(0xFF4CAF50) else Color(0xFF9E9E9E))
        )
    }
}
