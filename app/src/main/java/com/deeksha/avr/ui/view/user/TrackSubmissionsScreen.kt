/**
 * Track Expense Submissions Screen
 * 
 * Enhanced Features:
 * - Real-time status updates with Firebase Firestore listeners
 * - Dynamic status change notifications
 * - Pull-to-refresh functionality
 * - Time elapsed indicators for pending expenses
 * - Visual status indicators with icons
 * - Live activity tracking
 * - Status filtering (Approved, Pending, Rejected)
 * - Receipt number display
 * - Review comments for rejected expenses
 * - Automatic refresh and monitoring
 * - Dynamic status updates with visual feedback
 * 
 * The screen automatically updates when:
 * - New expenses are submitted
 * - Expense status changes (Pending → Approved/Rejected)
 * - User manually refreshes
 * - User switches between status filters
 * - Real-time Firebase updates occur
 */

package com.deeksha.avr.ui.view.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Expense
import com.deeksha.avr.model.ExpenseStatus
import com.deeksha.avr.model.Project
import com.deeksha.avr.viewmodel.ExpenseViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import android.util.Log
import java.util.*
import kotlinx.coroutines.launch
import com.deeksha.avr.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSubmissionsScreen(
    project: Project,
    onNavigateBack: () -> Unit,
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val statusCounts by expenseViewModel.statusCounts.collectAsState()
    val filteredExpenses by expenseViewModel.filteredExpenses.collectAsState()
    val selectedStatusFilter by expenseViewModel.selectedStatusFilter.collectAsState()
    val isLoading by expenseViewModel.isLoading.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val successMessage by expenseViewModel.successMessage.collectAsState()
    val errorMessage by expenseViewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Real-time update indicators
    var lastUpdateTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var isRealTimeConnected by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Coroutine scope for async operations
    val scope = rememberCoroutineScope()
    
    // Debug: Log status counts changes
    LaunchedEffect(statusCounts) {
        Log.d("TrackSubmissionsScreen", "📊 Status counts updated: A=${statusCounts.approved}, P=${statusCounts.pending}, R=${statusCounts.rejected}, T=${statusCounts.total}")
        lastUpdateTime = System.currentTimeMillis()
        isRealTimeConnected = true
    }
    
    // Debug: Log filtered expenses changes
    LaunchedEffect(filteredExpenses) {
        Log.d("TrackSubmissionsScreen", "📋 Filtered expenses updated: ${filteredExpenses.size} expenses")
        if (filteredExpenses.isNotEmpty()) {
            val statusBreakdown = filteredExpenses.groupBy { it.status }
            Log.d("TrackSubmissionsScreen", "📊 Current expenses status breakdown: $statusBreakdown")
        }
        lastUpdateTime = System.currentTimeMillis()
        isRealTimeConnected = true
    }
    
    // Track previous state for status change notifications
    var previousExpenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    var showStatusUpdateNotification by remember { mutableStateOf(false) }
    var statusUpdateMessage by remember { mutableStateOf("") }
    
    // Show Snackbar for success or error
    LaunchedEffect(successMessage, errorMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            expenseViewModel.clearSuccessMessage()
        }
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            expenseViewModel.clearExpenseError()
        }
    }

    // Load user's expenses for this project when screen starts
    LaunchedEffect(project.id, authState.user?.phone, refreshTrigger) {
        authState.user?.let { user ->
            Log.d("TrackSubmissionsScreen", "🚀 Loading expenses from subcollection: projects/${project.id}/expenses for user: nH6HRRTo2Xhhb5D8nR1iEzlrf7A2")
            
            // Clear any existing data first
            expenseViewModel.clearFilter()
            
            // Use direct loading method for immediate results
            expenseViewModel.loadUserExpensesForProjectDirect(project.id,user.phone)
            
            // Set up real-time status update listener
            expenseViewModel.listenForStatusUpdates(project.id, user.phone)
        }
    }
    
    // Debug: Monitor expenses state (raw data from Firestore)
    LaunchedEffect(Unit) {
        expenseViewModel.expenses.collect { expenses ->
            Log.d("TrackSubmissionsScreen", "📊 Raw expenses from Firestore: ${expenses.size} expenses")
            if (expenses.isNotEmpty()) {
                expenses.forEach { expense ->
                    Log.d("TrackSubmissionsScreen", "  💰 ${expense.id}: ${expense.category} - ${expense.status} - ₹${expense.amount} - User: ${expense.userId}")
                }
            } else {
                Log.d("TrackSubmissionsScreen", "📭 No expenses found in Firestore subcollection")
            }
        }
    }

    // Debug: Monitor filtered expenses state (what UI shows)
    LaunchedEffect(Unit) {
        expenseViewModel.filteredExpenses.collect { filteredExpenses ->
            Log.d("TrackSubmissionsScreen", "📋 Filtered expenses for UI: ${filteredExpenses.size} expenses")
            if (filteredExpenses.isNotEmpty()) {
                filteredExpenses.forEach { expense ->
                    Log.d("TrackSubmissionsScreen", "  🎯 ${expense.id}: ${expense.category} - ${expense.status} - ₹${expense.amount}")
                }
            } else {
                Log.d("TrackSubmissionsScreen", "📭 No filtered expenses - UI will show empty state")
            }
        }
    }

    // Debug: Monitor status counts
    LaunchedEffect(Unit) {
        expenseViewModel.statusCounts.collect { statusCounts ->
            Log.d("TrackSubmissionsScreen", "📊 Status counts: A=${statusCounts.approved}, P=${statusCounts.pending}, R=${statusCounts.rejected}, T=${statusCounts.total}")
        }
    }

    // Debug: Monitor selected filter
    LaunchedEffect(Unit) {
        expenseViewModel.selectedStatusFilter.collect { filter ->
            Log.d("TrackSubmissionsScreen", "🔍 Status filter: ${filter?.name ?: "None"}")
        }
    }

    // Debug: Monitor loading state
    LaunchedEffect(Unit) {
        expenseViewModel.isLoading.collect { loading ->
            Log.d("TrackSubmissionsScreen", "⏳ Loading state: $loading")
        }
    }
    
    // Monitor for status changes and show notifications
    LaunchedEffect(filteredExpenses) {
        if (previousExpenses.isNotEmpty() && filteredExpenses.isNotEmpty()) {
            val newExpenses = filteredExpenses.filter { newExpense ->
                val oldExpense = previousExpenses.find { it.id == newExpense.id }
                oldExpense?.status != newExpense.status
            }
            
            newExpenses.forEach { expense ->
                when (expense.status) {
                    ExpenseStatus.APPROVED -> {
                        statusUpdateMessage = "✅ ${expense.category} expense (₹${String.format("%,.2f", expense.amount)}) has been approved!"
                        showStatusUpdateNotification = true
                    }
                    ExpenseStatus.REJECTED -> {
                        statusUpdateMessage = "❌ ${expense.category} expense (₹${String.format("%,.2f", expense.amount)}) has been rejected"
                        showStatusUpdateNotification = true
                    }
                    else -> {}
                }
            }
        }
        previousExpenses = filteredExpenses
    }
    
    // Auto-hide notification after 3 seconds
    LaunchedEffect(showStatusUpdateNotification) {
        if (showStatusUpdateNotification) {
            kotlinx.coroutines.delay(3000)
            showStatusUpdateNotification = false
        }
    }
    
    // Check for real-time connection status
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(5000) // Check every 5 seconds
            val timeSinceLastUpdate = System.currentTimeMillis() - lastUpdateTime
            if (timeSinceLastUpdate > 30000) { // 30 seconds without updates
                isRealTimeConnected = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Snackbar Host
        SnackbarHost(hostState = snackbarHostState)
        
        // Top Bar with real-time status indicator
        TopAppBar(
            title = {
                Column {
                Text(
                    text = "Track Recent Submissions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                    // Real-time status indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isRealTimeConnected) Icons.Default.CheckCircle else Icons.Default.Close,
                            contentDescription = if (isRealTimeConnected) "Real-time connected" else "Real-time disconnected",
                            modifier = Modifier.size(12.dp),
                            tint = if (isRealTimeConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                        Text(
                            text = if (isRealTimeConnected) "Live Updates" else "Offline",
                            fontSize = 10.sp,
                            color = if (isRealTimeConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            },
            actions = {
                // Manual refresh button
                IconButton(
                    onClick = {
                        authState.user?.let { user ->
                            scope.launch {
                                isRefreshing = true
                                expenseViewModel.forceRefreshData(project.id, user.uid)
                                isRealTimeConnected = true
                                lastUpdateTime = System.currentTimeMillis()
                                // Stop refreshing after a delay
                                kotlinx.coroutines.delay(1000)
                                isRefreshing = false
                    }
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = if (isRefreshing) Color.Gray else Color(0xFF4285F4)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        // Status update notification
        if (showStatusUpdateNotification) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = statusUpdateMessage,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
        
        // Main content
        LazyColumn(
                modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
        Column(
                        modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = project.name,
                    fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                )
                        Spacer(modifier = Modifier.height(4.dp))
                Text(
                            text = "Budget: ₹${String.format("%,.2f", project.budget)}",
                            fontSize = 14.sp,
                    color = Color.Gray
                )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Last update time
                        val timeAgo = (System.currentTimeMillis() - lastUpdateTime) / 1000
                        val timeText = when {
                            timeAgo < 60 -> "Just now"
                            timeAgo < 3600 -> "${timeAgo / 60}m ago"
                            else -> "${timeAgo / 3600}h ago"
                        }
                        
                Text(
                            text = "Last updated: $timeText",
                    fontSize = 12.sp,
                            color = if (isRealTimeConnected) Color(0xFF4CAF50) else Color.Gray
                )
                
                        
                    }
                }
            }
            
            // Status Cards Row with dynamic counts
            item {
                Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                            .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Approved Card
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Approved",
                    count = statusCounts.approved,
                    icon = Icons.Default.CheckCircle,
                    backgroundColor = Color(0xFF4CAF50),
                    isSelected = selectedStatusFilter == ExpenseStatus.APPROVED,
                    onClick = { 
                        if (selectedStatusFilter == ExpenseStatus.APPROVED) {
                            expenseViewModel.clearFilter()
                        } else {
                            expenseViewModel.filterByStatus(ExpenseStatus.APPROVED)
                        }
                    }
                )
                // Pending Card
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending",
                    count = statusCounts.pending,
                    icon = Icons.Default.Notifications,
                    backgroundColor = Color(0xFFFF9800),
                    isSelected = selectedStatusFilter == ExpenseStatus.PENDING,
                    onClick = { 
                        if (selectedStatusFilter == ExpenseStatus.PENDING) {
                            expenseViewModel.clearFilter()
                        } else {
                            expenseViewModel.filterByStatus(ExpenseStatus.PENDING)
                        }
                    }
                )
                // Rejected Card
                StatusCard(
                    modifier = Modifier.weight(1f),
                    title = "Rejected",
                    count = statusCounts.rejected,
                    icon = Icons.Default.Close,
                    backgroundColor = Color(0xFFF44336),
                    isSelected = selectedStatusFilter == ExpenseStatus.REJECTED,
                    onClick = { 
                        if (selectedStatusFilter == ExpenseStatus.REJECTED) {
                            expenseViewModel.clearFilter()
                        } else {
                            expenseViewModel.filterByStatus(ExpenseStatus.REJECTED)
                        }
                    }
                )
            }
                    // Status update indicator
                    if (isRefreshing || isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF4285F4),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRefreshing) "Refreshing..." else "Updating...",
                                fontSize = 12.sp,
                                color = Color(0xFF4285F4),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    // Show All Button (only when filter is active)
            if (selectedStatusFilter != null) {
                Button(
                    onClick = { expenseViewModel.clearFilter() },
                    modifier = Modifier
                        .fillMaxWidth()
                                .padding(vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "📊 Show All (${statusCounts.total})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
                }
            }
            // Dynamic Expense List Below Cards
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4285F4))
                        }
                    }
            } else if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = when (selectedStatusFilter) {
                                    ExpenseStatus.APPROVED -> "No approved submissions found"
                                    ExpenseStatus.PENDING -> "No pending submissions found" 
                                    ExpenseStatus.REJECTED -> "No rejected submissions found"
                                    else -> "No submissions found"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Submit your first expense to see it here",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            
                            // Debug information
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                        text = "Debug Information:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "• Project ID: ${project.id}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "• User ID: ${authState.user?.uid ?: "Not logged in"}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "• Loading: $isLoading",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "• Real-time: $isRealTimeConnected",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "• Status Filter: ${selectedStatusFilter?.name ?: "None"}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                )
                            }
                        }
                    }
                }
                }
            } else {
                        items(filteredExpenses) { expense ->
                            ExpenseSubmissionCard(expense = expense)
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    icon: ImageVector,
    backgroundColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.padding(2.dp) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) backgroundColor.copy(alpha = 0.9f) else backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExpenseSubmissionCard(expense: Expense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row - Category and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = expense.category,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    // Show receipt number if available
                    if (expense.receiptNumber.isNotEmpty()) {
                        Text(
                            text = "Receipt: ${expense.receiptNumber}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Status Badge with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = when (expense.status) {
                            ExpenseStatus.APPROVED -> Icons.Default.CheckCircle
                            ExpenseStatus.PENDING -> Icons.Default.Notifications
                            ExpenseStatus.REJECTED -> Icons.Default.Close
                            ExpenseStatus.DRAFT -> Icons.Default.Create
                        },
                        contentDescription = null,
                        tint = when (expense.status) {
                            ExpenseStatus.APPROVED -> Color(0xFF4CAF50)
                            ExpenseStatus.PENDING -> Color(0xFFFF9800)
                            ExpenseStatus.REJECTED -> Color(0xFFF44336)
                            ExpenseStatus.DRAFT -> Color(0xFF9E9E9E)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (expense.status) {
                                ExpenseStatus.APPROVED -> Color(0xFF4CAF50)
                                ExpenseStatus.PENDING -> Color(0xFFFF9800)
                                ExpenseStatus.REJECTED -> Color(0xFFF44336)
                                ExpenseStatus.DRAFT -> Color(0xFF9E9E9E)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when (expense.status) {
                                ExpenseStatus.APPROVED -> "✅ Approved"
                                ExpenseStatus.PENDING -> "⏳ Pending"
                                ExpenseStatus.REJECTED -> "❌ Rejected"
                                ExpenseStatus.DRAFT -> "📝 Draft"
                            },
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Amount Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amount:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹${String.format("%,.2f", expense.amount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Details Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Date Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Date:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = expense.date?.let { 
                            FormatUtils.formatDateLong(it)
                        } ?: "N/A",
                        fontSize = 12.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Department Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Department:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = expense.department.ifEmpty { "Not specified" },
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                
                // Payment Mode Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Payment Mode:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when (expense.modeOfPayment) {
                            "cash" -> "By Cash"
                            "upi" -> "By UPI"
                            "check" -> "By Check"
                            else -> expense.modeOfPayment.ifEmpty { "Not specified" }
                        },
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                
                            // Submitted Date Row with time elapsed
            expense.submittedAt?.let { submittedAt ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Submitted:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = FormatUtils.formatDateLong(submittedAt),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        // Show time elapsed for pending expenses
                        if (expense.status == ExpenseStatus.PENDING) {
                            val timeElapsed = System.currentTimeMillis() - submittedAt.toDate().time
                            val hoursElapsed = timeElapsed / (1000 * 60 * 60)
                            val daysElapsed = hoursElapsed / 24L
                            
                            val timeText = when {
                                daysElapsed > 0L -> "$daysElapsed day${if (daysElapsed > 1L) "s" else ""} ago"
                                hoursElapsed > 0L -> "$hoursElapsed hour${if (hoursElapsed > 1L) "s" else ""} ago"
                                else -> "Just submitted"
                            }
                            
                            Text(
                                text = timeText,
                                fontSize = 12.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
                
                // Reviewed info for approved/rejected expenses
                if (expense.status != ExpenseStatus.PENDING && expense.status != ExpenseStatus.DRAFT) {
                    expense.reviewedAt?.let { reviewedAt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (expense.status == ExpenseStatus.APPROVED) "Approved:" else "Rejected:",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = FormatUtils.formatDateLong(reviewedAt),
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    if (expense.reviewedBy.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Reviewed by:",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = expense.reviewedBy,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
            
            // Description (if available)
            if (expense.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Description:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = expense.description,
                    fontSize = 14.sp,
                    color = Color.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Review Comments (if available)
            if (expense.reviewComments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Review Comments:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = expense.reviewComments,
                    fontSize = 14.sp,
                    color = if (expense.status == ExpenseStatus.REJECTED) Color.Red else Color.Black,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
} 