package com.deeksha.avr.ui.view.approver

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.viewmodel.ApproverProjectViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.utils.FormatUtils
import com.deeksha.avr.model.CategoryBudget
import com.deeksha.avr.model.DepartmentBudgetBreakdown
import com.deeksha.avr.model.ProjectBudgetSummary
import com.deeksha.avr.model.User
import com.deeksha.avr.repository.AuthRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.compose.runtime.remember
import java.text.NumberFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproverProjectDashboardScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPendingApprovals: (String) -> Unit,
    onNavigateToAddExpense: () -> Unit = {},
    onNavigateToReports: (String) -> Unit = {},
    onNavigateToDepartmentDetail: (String, String) -> Unit = { _, _ -> },
    onNavigateToProjectNotifications: (String) -> Unit = {},
    onNavigateToDelegation: () -> Unit = {},
    approverProjectViewModel: ApproverProjectViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val projectBudgetSummary by approverProjectViewModel.projectBudgetSummary.collectAsState()
    val isLoading by approverProjectViewModel.isLoading.collectAsState()
    val error by approverProjectViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // State for temporary approver user data
    var temporaryApproverUser by remember { mutableStateOf<User?>(null) }
    var isLoadingTemporaryApprover by remember { mutableStateOf(false) }
    
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
    
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Track if the screen is resumed/visible for auto-refresh
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // Load project data when screen starts
    LaunchedEffect(projectId) {
        approverProjectViewModel.loadProjectBudgetSummary(projectId)
        // Initialize notifications for this user
        authState.user?.uid?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
    }
    
    // Load temporary approver user data when project data is loaded
    LaunchedEffect(projectBudgetSummary.project?.temporaryApproverPhone) {
        val tempApproverPhone = projectBudgetSummary.project?.temporaryApproverPhone
        if (!tempApproverPhone.isNullOrEmpty()) {
            isLoadingTemporaryApprover = true
            try {
                val user = authRepository.getUserByPhoneNumber(tempApproverPhone)
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
                onNavigateToDelegation = {
                    scope.launch { drawerState.close() }
                    onNavigateToDelegation()
                },
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
                    Column {
                        Text(
                            text = "AVR ENTERTAINMENT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                        Text(
                            text = projectBudgetSummary.project?.name ?: "Project Dashboard",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF4285F4)
                        )
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { 
                            refreshTrigger++
                        }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Dashboard",
                            tint = Color(0xFF4285F4)
                        )
                    }
                    // Menu button
                    IconButton(
                        onClick = {
                            scope.launch { drawerState.open() }
                        }
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0xFF4285F4)
                        )
                    }
                    Box {
                        IconButton(onClick = { onNavigateToProjectNotifications(projectId) }) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Project Notifications",
                                tint = Color(0xFF4285F4)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // Project Header
                        Text(
                            text = "Dashboard",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        
                        Text(
                            text = projectBudgetSummary.project?.name ?: "Project",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF4285F4)
                        )
                        
                        Text(
                            text = "Budget: ${FormatUtils.formatCurrency(projectBudgetSummary.totalBudget)}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Temporary Approver Display
                        projectBudgetSummary.project?.let { project ->
                            if (project.temporaryApproverPhone.isNotEmpty()) {
                                if (isLoadingTemporaryApprover) {
                                    Row(
                                        modifier = Modifier.padding(bottom = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color(0xFF4285F4),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Loading temporary approver...",
                                            fontSize = 14.sp,
                                            color = Color(0xFF4285F4),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Temporary Approver: ${temporaryApproverUser?.name ?: project.temporaryApproverPhone}",
                                        fontSize = 14.sp,
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                }
                            }
                        }
                        
                        // Budget Summary Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "Budget",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Budget Summary",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Budget",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(projectBudgetSummary.totalBudget),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                    
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Spent",
                                            fontSize = 14.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = FormatUtils.formatCurrency(projectBudgetSummary.totalSpent),
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                TextButton(
                                    onClick = { onNavigateToReports(projectId) },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = "View Report",
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        // Department Cards - Show only if there are expenses or show "No expenses" message
                        if (projectBudgetSummary.departmentBreakdown.any { it.spent > 0 }) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                items(projectBudgetSummary.departmentBreakdown.filter { it.spent > 0 }) { department ->
                                    DepartmentCard(
                                        department = department,
                                        modifier = Modifier.width(160.dp),
                                        onClick = {
                                            Log.d("ApproverProjectDashboard", "🔍 Clicking department card: '${department.department}'")
                                            onNavigateToDepartmentDetail(projectId, department.department)
                                        }
                                    )
                                }
                            }
                        } else {
                            // No expenses message
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "✅ All Caught Up!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No expenses found for this project",
                                        fontSize = 14.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Department budgets will appear as expenses are added",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Department Budget Allocation Section
                        Text(
                            text = "Department Budget Allocation",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Pie Chart and Legend
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Pie Chart
                                    Box(
                                        modifier = Modifier.size(150.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PieChart(
                                            data = projectBudgetSummary.departmentBreakdown.filter { it.spent > 0 },
                                            modifier = Modifier.size(120.dp)
                                        )
                                    }
                                    
                                    // Legend - Only show departments with expenses
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val departmentsWithExpenses = projectBudgetSummary.departmentBreakdown.filter { it.spent > 0 }
                                        if (departmentsWithExpenses.isNotEmpty()) {
                                            departmentsWithExpenses.take(3).forEachIndexed { index, department ->
                                                LegendItem(
                                                    color = getDepartmentColor(department.department),
                                                    department = department.department,
                                                    amount = department.spent
                                                )
                                                if (index < departmentsWithExpenses.size - 1 && index < 2) Spacer(modifier = Modifier.height(8.dp))
                                            }
                                        } else {
                                            Text(
                                                text = "No expense data available",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
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
    onNavigateToDelegation: () -> Unit = {},
    userRole: String = "APPROVER"
) {
    ModalDrawerSheet(
        modifier = Modifier.width(280.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "AVR ENTERTAINMENT",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            // Menu Items
            DrawerMenuItem(
                icon = Icons.Default.CheckCircle,
                title = "Dashboard",
                onClick = onNavigateToDashboard
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Notifications,
                title = "Pending Approvals",
                onClick = onNavigateToPendingApprovals
            )
            
            DrawerMenuItem(
                icon = Icons.Default.Add,
                title = "Add Expenses",
                onClick = onNavigateToAddExpense
            )
            
            // Only show Delegation for Production Heads
            if (userRole == "PRODUCTION_HEAD") {
                DrawerMenuItem(
                    icon = Icons.Default.Person,
                    title = "Delegation",
                    onClick = onNavigateToDelegation
                )

            }
        }
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

// Dynamic color assignment for departments
private fun getDepartmentColor(departmentName: String): Color {
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
    
    // Use department name hash to consistently assign the same color to the same department
    val colorIndex = kotlin.math.abs(departmentName.hashCode()) % colors.size
    return colors[colorIndex]
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