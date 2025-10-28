package com.deeksha.avr.ui.view.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.foundation.shape.CircleShape
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
import com.deeksha.avr.model.Expense
import com.deeksha.avr.model.ExpenseStatus
import com.deeksha.avr.viewmodel.ExpenseViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.model.ExpenseNotificationSummary
import java.text.NumberFormat
import java.util.*
import com.deeksha.avr.utils.FormatUtils
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    project: Project,
    onNavigateBack: () -> Unit,
    onAddExpense: () -> Unit,
    onTrackSubmissions: () -> Unit = {},
    onNavigateToNotifications: (String) -> Unit = {},
    onShowAllExpenses: () -> Unit = {},
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val expenseSummary by expenseViewModel.expenseSummary.collectAsState()
    val expenses by expenseViewModel.expenses.collectAsState()
    val isLoading by expenseViewModel.isLoading.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // Project-specific notifications
    val notifications by notificationViewModel.notifications.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    
    // Filter notifications for this project
    val projectNotifications = notifications.filter { it.projectId == project.id }
    val projectNotificationBadge = remember(projectNotifications) {
        val unreadCount = projectNotifications.count { !it.isRead }
        com.deeksha.avr.model.NotificationBadge(
            count = unreadCount,
            hasUnread = unreadCount > 0
        )
    }
    
    val projectExpenseStatusSummary = remember(projectNotifications) {
        val approvedCount = projectNotifications.count { 
            it.type == com.deeksha.avr.model.NotificationType.EXPENSE_APPROVED && !it.isRead 
        }
        val rejectedCount = projectNotifications.count { 
            it.type == com.deeksha.avr.model.NotificationType.EXPENSE_REJECTED && !it.isRead 
        }
        val submittedCount = projectNotifications.count { 
            it.type == com.deeksha.avr.model.NotificationType.EXPENSE_SUBMITTED && !it.isRead 
        }
        
        ExpenseNotificationSummary(
            approvedCount = approvedCount,
            rejectedCount = rejectedCount,
            submittedCount = submittedCount,
            totalUnread = projectNotifications.count { !it.isRead },
            hasUpdates = projectNotifications.any { !it.isRead }
        )
    }
    
    LaunchedEffect(project.id) {
        expenseViewModel.loadProjectExpenses(project.id)
        // Initialize notifications for this user
        authState.user?.uid?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
    ) {
        // Top Bar - iOS style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
                Text(
                    text = "Project Details",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            IconButton(onClick = { 
                authState.user?.uid?.let { userId ->
                    onNavigateToNotifications(project.id)
                }
            }) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Chat",
                    tint = Color.Black
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Project Header Card
            item {
                ProjectHeaderCard(project = project)
            }
            
            // Key Information Section
            item {
                KeyInformationCard(project = project, expenseSummary = expenseSummary)
            }
            
            // Project Manager and Team Size Card
            item {
                ManagerTeamCard(project = project)
            }
            
            // Department Budget Breakdown
            item {
                DepartmentBudgetCard(
                    project = project,
                    expenseSummary = expenseSummary,
                    expenses = expenses
                )
            }
            
            // Recent Expenses
            item {
                RecentExpensesCard(
                    expenses = expenses.take(4),
                    onShowAll = onShowAllExpenses
                )
            }
            
            // Action Button
            item {
                Button(
                    onClick = onAddExpense,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add New Expense",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProjectHeaderCard(project: Project) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                Text(
                    text = project.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
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
                                .background(Color(0xFF34C759), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF34C759)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "AVR Entertainment",
                fontSize = 15.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = project.description.ifEmpty { "Construction" },
                fontSize = 15.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun KeyInformationCard(
    project: Project,
    expenseSummary: com.deeksha.avr.model.ExpenseSummary
) {
    val approvedExpenses = expenseSummary.totalExpenses
    val remainingBudget = project.budget - approvedExpenses
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "KEY INFORMATION",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Total Budget
            InfoRow(
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF34C759),
                iconBgColor = Color(0xFF34C759).copy(alpha = 0.15f),
                label = "TOTAL BUDGET",
                value = FormatUtils.formatCurrency(project.budget)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Approved Expenses
            InfoRow(
                icon = Icons.Default.CheckCircle,
                iconColor = Color(0xFF007AFF),
                iconBgColor = Color(0xFF007AFF).copy(alpha = 0.15f),
                label = "APPROVED EXPENSES",
                value = FormatUtils.formatCurrency(approvedExpenses)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Remaining Budget
            InfoRow(
                icon = Icons.Default.Remove,
                iconColor = Color(0xFFFF9500),
                iconBgColor = Color(0xFFFF9500).copy(alpha = 0.15f),
                label = "REMAINING BUDGET",
                value = FormatUtils.formatCurrency(remainingBudget)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Project Timeline
            if (project.startDate != null && project.endDate != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFAF52DE).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFFAF52DE),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "PROJECT TIMELINE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${FormatUtils.formatDate(project.startDate)} - ${FormatUtils.formatDate(project.endDate)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    iconBgColor: Color,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun ManagerTeamCard(project: Project) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Project Manager
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFAF52DE).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFFAF52DE),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "PROJECT MANAGER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = project.managerId.ifEmpty { "N/A" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Team Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF007AFF).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "TEAM SIZE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${project.teamMembers.size} members",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun DepartmentBudgetCard(
    project: Project,
    expenseSummary: com.deeksha.avr.model.ExpenseSummary,
    expenses: List<Expense>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "DEPARTMENT BUDGET BREAKDOWN",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Show department budgets
            project.departmentBudgets.forEach { (deptName, budget) ->
                // Calculate total approved expenses for this department
                val spent = expenses
                    .filter { it.department == deptName && it.status == ExpenseStatus.APPROVED }
                    .sumOf { it.amount }
                
                val remaining = budget - spent
                val utilization = if (budget > 0) (spent / budget * 100).toInt() else 0
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = deptName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "ALLOCATED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatCurrency(budget),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "APPROVED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatCurrency(spent),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "REMAINING",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = FormatUtils.formatCurrency(remaining),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34C759)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "$utilization% utilized",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    // Utilization bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE0E0E0))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((utilization.coerceIn(0, 100)) / 100f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF2196F3))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
            
            // If no department budgets, show general budget
            if (project.departmentBudgets.isEmpty()) {
                val budget = project.budget
                // Calculate total approved expenses
                val spent = expenses
                    .filter { it.status == ExpenseStatus.APPROVED }
                    .sumOf { it.amount }
                val remaining = budget - spent
                val utilization = if (budget > 0) (spent / budget * 100).toInt() else 0
                
                Text(
                    text = "General",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "ALLOCATED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatCurrency(budget),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "APPROVED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatCurrency(spent),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF007AFF)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "REMAINING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = FormatUtils.formatCurrency(remaining),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34C759)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$utilization% utilized",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                // Utilization bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFFE0E0E0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((utilization.coerceIn(0, 100)) / 100f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF2196F3))
                    )
                }
            }
        }
    }
}

@Composable
fun RecentExpensesCard(
    expenses: List<Expense>,
    onShowAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                Text(
                    text = "Recent Expenses",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                if (expenses.size >= 4) {
                    TextButton(onClick = onShowAll) {
                        Text(
                            text = "Show All",
                            fontSize = 15.sp,
                            color = Color(0xFF007AFF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (expenses.isEmpty()) {
                Text(
                    text = "No expenses yet",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                expenses.forEach { expense ->
                    ExpenseRow(expense = expense)
                    if (expense != expenses.last()) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseRow(expense: Expense) {
    val statusColor = when (expense.status) {
        ExpenseStatus.APPROVED -> Color(0xFF34C759)
        ExpenseStatus.PENDING -> Color(0xFFFFCC00)
        ExpenseStatus.REJECTED -> Color(0xFFFF3B30)
        else -> Color.Gray
    }
    
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedDate = expense.date?.toDate()?.let { dateFormatter.format(it) } ?: ""
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f)) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = expense.category,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (expense.modeOfPayment.isNotEmpty()) {
                    Text(
                        text = "By ${expense.modeOfPayment.lowercase()}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = FormatUtils.formatCurrency(expense.amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                
                // Chat icon for pending expenses
                if (expense.status == ExpenseStatus.PENDING) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Chat",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF007AFF)
                    )
                }
            }
        }
    }
} 