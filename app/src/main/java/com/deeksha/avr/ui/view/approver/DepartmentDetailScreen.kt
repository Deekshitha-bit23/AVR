package com.deeksha.avr.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.deeksha.avr.model.DetailedExpense

import com.deeksha.avr.utils.FormatUtils
import com.deeksha.avr.viewmodel.ReportsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartmentDetailScreen(
    projectId: String,
    departmentName: String,
    onNavigateBack: () -> Unit,
    reportsViewModel: ReportsViewModel = hiltViewModel()
) {
    val reportData by reportsViewModel.reportData.collectAsState()
    val isLoading by reportsViewModel.isLoading.collectAsState()
    
    // Filter expenses for this specific department
    val departmentExpenses = remember(reportData.detailedExpenses, departmentName) {
        reportData.detailedExpenses.filter { expense ->
            expense.department.equals(departmentName, ignoreCase = true)
        }
    }
    
    val departmentAmount = reportData.expensesByDepartment[departmentName] ?: 0.0
    
    LaunchedEffect(projectId) {
        if (reportData.detailedExpenses.isEmpty()) {
            reportsViewModel.loadReportsForProject(projectId)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Department Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
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
                // Department Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4285F4)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = departmentName,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Total Spent",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            
                            Text(
                                text = FormatUtils.formatCurrency(departmentAmount),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "${departmentExpenses.size} expenses",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                
                // Expenses List Header
                item {
                    Text(
                        text = "Expense Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                if (departmentExpenses.isNotEmpty()) {
                    items(departmentExpenses) { expense ->
                        ExpenseDetailCard(expense = expense)
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
private fun ExpenseDetailCard(expense: DetailedExpense) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = FormatUtils.formatCurrency(expense.amount),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Text(
                        text = expense.by.ifEmpty { "Deeksha" },
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = expense.date?.let { FormatUtils.formatDate(it) } ?: "No date",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    
                    Text(
                        text = expense.department.ifEmpty { "No Department" },
                        fontSize = 12.sp,
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (expense.invoice.isNotEmpty() && expense.invoice != "N/A") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Invoice: ${expense.invoice}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            if (expense.modeOfPayment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Payment: ${expense.modeOfPayment}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
} 