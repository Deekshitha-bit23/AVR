package com.deeksha.avr.ui.view.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Project
import com.deeksha.avr.viewmodel.ExpenseViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.model.ExpenseNotificationSummary
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    project: Project,
    onNavigateBack: () -> Unit,
    onAddExpense: () -> Unit,
    onTrackSubmissions: () -> Unit = {},
    onNavigateToNotifications: (String) -> Unit = {},
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val expenseSummary by expenseViewModel.expenseSummary.collectAsState()
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
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar
        TopAppBar(
            title = { Text("Expenses", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                Box {
                    IconButton(
                        onClick = { 
                            authState.user?.uid?.let { userId ->
                                onNavigateToNotifications(project.id)
                            }
                        }
                    ) {
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
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project Info Header
            item {
                ProjectHeaderCard(project = project)
            }
            
            // Project-specific notifications banner
            if (projectExpenseStatusSummary.hasUpdates) {
                item {
                    ProjectNotificationBanner(
                        summary = projectExpenseStatusSummary,
                        onViewNotifications = {
                            authState.user?.uid?.let { userId ->
                                onNavigateToNotifications(project.id)
                            }
                        }
                    )
                }
            }
            
            // Total Expenses Card
            item {
                TotalExpensesCard(
                    totalAmount = expenseSummary.totalExpenses,
                    approvedCount = expenseSummary.approvedCount
                )
            }
            
            // Categories Section
            item {
                Text(
                    text = "Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Category Items
            if (expenseSummary.expensesByCategory.isNotEmpty()) {
                items(expenseSummary.expensesByCategory.toList()) { (category, amount) ->
                    CategoryItem(
                        categoryName = category,
                        amount = amount
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No expenses recorded yet",
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // Action Buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onTrackSubmissions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        text = "📊 Track Recent Submissions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onAddExpense,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4)
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Add Expenses",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ProjectHeaderCard(project: Project) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = project.name,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4285F4),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Budget: ${formatCurrencyExpenseList(project.budget)}",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun TotalExpensesCard(
    totalAmount: Double,
    approvedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Total Expenses",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = formatCurrencyExpenseList(totalAmount),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "$approvedCount approved expenses",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CategoryItem(
    categoryName: String,
    amount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
            
            Text(
                text = formatCurrencyExpenseList(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4)
            )
        }
    }
}

private fun formatCurrencyExpenseList(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
}

@Composable
fun ProjectNotificationBanner(
    summary: ExpenseNotificationSummary,
    onViewNotifications: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E8)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📝 Project Updates",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                
                Text(
                    text = "${summary.totalUnread} new",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier
                        .background(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (summary.approvedCount > 0) {
                    Text(
                        text = "✅ ${summary.approvedCount} Approved",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (summary.rejectedCount > 0) {
                    Text(
                        text = "❌ ${summary.rejectedCount} Rejected",
                        fontSize = 12.sp,
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onViewNotifications,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("View Project Updates")
            }
        }
    }
} 