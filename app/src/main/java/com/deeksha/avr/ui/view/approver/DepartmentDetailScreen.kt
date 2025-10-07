package com.deeksha.avr.ui.view.approver

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Payment
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
    val departmentBudget = currentProject?.departmentBudgets?.get(departmentName) ?: 0.0
    val totalSpent = departmentExpenses.sumOf { it.amount }
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
        departmentExpenses.filter { expense ->
            val statusMatch = selectedStatus == null || selectedStatus == "All"
            val searchMatch = searchQuery.isEmpty() || 
                expense.by.contains(searchQuery, ignoreCase = true) ||
                expense.invoice.contains(searchQuery, ignoreCase = true)
            statusMatch && searchMatch
        }
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
        // Top Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = departmentName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Department Expenses",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text(
                        text = "Done",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF4285F4)
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
                            Text(
                                text = "Department Budget",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = FormatUtils.formatCurrency(departmentBudget),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
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
                                        color = Color(0xFF4285F4)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = budgetUtilization / 100f,
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
                        ModernExpenseCard(expense = expense)
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
private fun ModernExpenseCard(expense: DetailedExpense) {
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
            // Amount and Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = FormatUtils.formatCurrency(expense.amount),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Text(
                    text = expense.date?.let { FormatUtils.formatDateShort(it) } ?: "No date",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description (using invoice as description for DetailedExpense)
            if (expense.invoice.isNotEmpty() && expense.invoice != "N/A") {
                Text(
                    text = expense.invoice,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Department and Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Department
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = "Department",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = expense.department,
                        fontSize = 14.sp,
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Status Badge (assuming all DetailedExpense are approved since they come from reports)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Approved",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Submitted By and Payment Method Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Submitted By
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
                
                // Payment Method
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Payment,
                        contentDescription = "Payment method",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (expense.modeOfPayment) {
                            "cash" -> "By cash"
                            "upi" -> "By UPI"
                            "check" -> "By Cheque"
                            else -> expense.modeOfPayment.ifEmpty { "Not specified" }
                        },
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}