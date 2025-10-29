package com.deeksha.avr.ui.view.approver

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Expense
import com.deeksha.avr.model.ExpenseStatus
import com.deeksha.avr.model.Project
import com.deeksha.avr.model.DetailedExpense
import com.deeksha.avr.utils.FormatUtils
import com.deeksha.avr.viewmodel.ReportsViewModel
import com.deeksha.avr.viewmodel.ProjectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailScreen(
    projectId: String,
    departmentName: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenseChat: (String) -> Unit = {},
    onNavigateToReview: (String) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    reportsViewModel: ReportsViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel()
) {
    val reportData by reportsViewModel.reportData.collectAsState()
    val isLoading by reportsViewModel.isLoading.collectAsState()
    val projects by projectViewModel.projects.collectAsState()
    val currentProject = projects.find { it.id == projectId }
    
    // Filter expenses for this specific department
    val departmentExpenses = remember(reportData.detailedExpenses, departmentName) {
        reportData.detailedExpenses.filter { expense ->
            expense.department.equals(departmentName, ignoreCase = true)
        }
    }
    
    // Calculate budget summary for this specific department
    // Only count APPROVED expenses for total spent
    val departmentBudget = currentProject?.departmentBudgets?.get(departmentName) ?: 0.0
    val totalSpent = departmentExpenses
        .filter { it.status == ExpenseStatus.APPROVED }
        .sumOf { it.amount }
    val remaining = departmentBudget - totalSpent
    val budgetUtilization = if (departmentBudget > 0) (totalSpent / departmentBudget * 100).toInt() else 0
    
    // Debug logging
    LaunchedEffect(departmentName, currentProject) {
        Log.d("DepartmentDetailScreen", "🏢 Department: $departmentName")
        Log.d("DepartmentDetailScreen", "💰 Department Budget: ₹$departmentBudget")
        Log.d("DepartmentDetailScreen", "💸 Total Spent: ₹$totalSpent")
        Log.d("DepartmentDetailScreen", "📊 Budget Utilization: $budgetUtilization%")
        Log.d("DepartmentDetailScreen", "📋 Available Department Budgets: ${currentProject?.departmentBudgets}")
    }
    
    // Filter state
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Filtered expenses based on status and search
    val filteredExpenses = remember(departmentExpenses, selectedStatus, searchQuery) {
        val filtered = departmentExpenses.filter { expense ->
            val statusMatch = when (selectedStatus) {
                null, "All" -> true
                "Pending" -> expense.status == ExpenseStatus.PENDING
                "Approved" -> expense.status == ExpenseStatus.APPROVED
                "Rejected" -> expense.status == ExpenseStatus.REJECTED
                else -> true
            }
            val searchMatch = searchQuery.isEmpty() || 
                expense.by.contains(searchQuery, ignoreCase = true) ||
                expense.invoice.contains(searchQuery, ignoreCase = true)
            statusMatch && searchMatch
        }
        
        // Debug logging
        Log.d("DepartmentDetailScreen", "🔍 Filtering expenses:")
        Log.d("DepartmentDetailScreen", "  Total department expenses: ${departmentExpenses.size}")
        Log.d("DepartmentDetailScreen", "  Selected status filter: $selectedStatus")
        Log.d("DepartmentDetailScreen", "  Search query: '$searchQuery'")
        Log.d("DepartmentDetailScreen", "  Filtered result: ${filtered.size} expenses")
        
        // Log each expense and its status for debugging
        departmentExpenses.forEach { expense ->
            Log.d("DepartmentDetailScreen", "  📋 Expense: ${expense.id} - Status: ${expense.status} - Amount: ₹${expense.amount}")
        }
        
        filtered
    }
    
    LaunchedEffect(projectId) {
        if (reportData.detailedExpenses.isEmpty()) {
            reportsViewModel.loadReportsForProject(projectId)
        }
        projectViewModel.loadProjects()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar - Center aligned to match reference image
        CenterAlignedTopAppBar(
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = departmentName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Department Expenses",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text(
                        text = "Done",
                        color = Color(0xFF4285F4),
                        fontSize = 16.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4285F4))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Budget Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Budget Row - Total Budget, Spent, Remaining
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Total Budget Column
                                Column {
                                    Text(
                                        text = "Total Budget",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(departmentBudget),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                
                                // Spent Column
                                Column {
                                    Text(
                                        text = "Spent",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(totalSpent),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                
                                // Remaining Column
                                Column {
                                    Text(
                                        text = "Remaining",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = FormatUtils.formatCurrency(remaining),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remaining < 0) Color(0xFFF44336) else Color(0xFF4285F4)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Budget Utilization Section
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Budget Utilization",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                LinearProgressIndicator(
                                    progress = minOf(budgetUtilization / 100f, 1f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp),
                                    color = Color(0xFF4CAF50),
                                    trackColor = Color(0xFFE0E0E0)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "$budgetUtilization%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search expenses...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                // Filter Tabs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            onClick = { selectedStatus = null },
                            label = { Text("All") },
                            selected = selectedStatus == null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4285F4),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            onClick = { selectedStatus = "Pending" },
                            label = { Text("Pending") },
                            selected = selectedStatus == "Pending",
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFF9800),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            onClick = { selectedStatus = "Approved" },
                            label = { Text("Approved") },
                            selected = selectedStatus == "Approved",
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF4CAF50),
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            onClick = { selectedStatus = "Rejected" },
                            label = { Text("Rejected") },
                            selected = selectedStatus == "Rejected",
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF44336),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                // Expenses List
                if (filteredExpenses.isNotEmpty()) {
                    items(filteredExpenses) { expense ->
                        ModernExpenseCard(
                            expense = expense,
                            onNavigateToExpenseChat = { expenseId ->
                                onNavigateToExpenseChat(expenseId)
                            },
                            onNavigateToReview = { expenseId ->
                                onNavigateToReview(expenseId)
                            },
                            onNavigateToDetail = { expenseId ->
                                onNavigateToDetail(expenseId)
                            }
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🔍 No Expenses Found",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No expenses found for this department",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernExpenseCard(
    expense: DetailedExpense,
    onNavigateToExpenseChat: (String) -> Unit = {},
    onNavigateToReview: (String) -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { base ->
                when (expense.status) {
                    ExpenseStatus.PENDING -> base.clickable { onNavigateToReview(expense.id) }
                    ExpenseStatus.APPROVED -> base.clickable { onNavigateToDetail(expense.id) }
                    else -> base
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: Amount (left) and date + status + chat (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Amount
                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                Text(
                        text = expense.date?.let { FormatUtils.formatDate(it) } ?: "No date",
                        fontSize = 12.sp,
                    color = Color.Gray
                )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Softer status pill with icon
                        val statusBg: Color
                        val statusFg: Color
                        val statusLabel: String
                        val statusIcon = when (expense.status) {
                            ExpenseStatus.APPROVED -> {
                                statusBg = Color(0xFFE8F5E9); statusFg = Color(0xFF4CAF50); statusLabel = "Approved"; Icons.Default.CheckCircle
                            }
                            ExpenseStatus.PENDING -> {
                                statusBg = Color(0xFFFFF3E0); statusFg = Color(0xFFFF9800); statusLabel = "Pending"; Icons.Default.Info
                            }
                            ExpenseStatus.REJECTED -> {
                                statusBg = Color(0xFFFFEBEE); statusFg = Color(0xFFD32F2F); statusLabel = "Rejected"; Icons.Default.Close
                            }
                            ExpenseStatus.DRAFT -> {
                                statusBg = Color(0xFFE0E0E0); statusFg = Color(0xFF616161); statusLabel = "Draft"; Icons.Default.Edit
                            }
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = statusBg),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(statusIcon, contentDescription = null, tint = statusFg, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(statusLabel, color = statusFg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Chat bubble
                        if (expense.status == ExpenseStatus.PENDING) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE3F2FD))
                                    .clickable { onNavigateToExpenseChat(expense.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Chat",
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Description - below the amount/date
            if (expense.description.isNotEmpty()) {
                Text(
                    text = expense.description,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Category - below description
            if (expense.category.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = "Category",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = expense.category,
                        fontSize = 13.sp,
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Bottom row: Submitted By (left) and Mode of Payment (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Submitted By - left side
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Submitted by",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Submitted by: ${expense.by}",
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
                
                // Mode of Payment - right side
                if (expense.modeOfPayment.isNotEmpty()) {
                    val paymentLabel = when (expense.modeOfPayment.lowercase()) {
                        "cash" -> "By cash"
                        "upi" -> "By UPI"
                        "check", "cheque" -> "By Cheque"
                        else -> "By ${expense.modeOfPayment}"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Payment,
                            contentDescription = "Payment mode",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = paymentLabel,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}