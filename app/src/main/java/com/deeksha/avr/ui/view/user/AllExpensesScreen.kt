package com.deeksha.avr.ui.view.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
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
import com.deeksha.avr.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllExpensesScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToExpenseChat: (String) -> Unit = {},
    expenseViewModel: ExpenseViewModel = hiltViewModel()
) {
    val expenses by expenseViewModel.expenses.collectAsState()
    val isLoading by expenseViewModel.isLoading.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    
    LaunchedEffect(projectId) {
        println("🔍 AllExpensesScreen: Loading expenses for project: $projectId")
        expenseViewModel.loadProjectExpenses(projectId)
        
        // Add a timeout to prevent infinite loading
        kotlinx.coroutines.delay(10000) // 10 seconds timeout
        if (isLoading) {
            println("⚠️ AllExpensesScreen: Loading timeout reached")
        }
    }
    
    // Debug logging
    LaunchedEffect(expenses.size, isLoading, error) {
        println("🔍 AllExpensesScreen: expenses.size=${expenses.size}, isLoading=$isLoading, error=$error")
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
                    text = "All Expenses",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        
        if (error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Error loading expenses",
                        fontSize = 16.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = error ?: "Unknown error",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { expenseViewModel.loadProjectExpenses(projectId) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF007AFF))
                    Text(
                        text = "Loading expenses...",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        } else if (expenses.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No expenses found",
                        fontSize = 18.sp,
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "This project doesn't have any expenses yet.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    // Debug button to create demo expenses
                    Button(
                        onClick = { 
                            expenseViewModel.addDemoExpenses(
                                projectId = projectId,
                                userId = "test_user", // You can get this from AuthViewModel
                                userName = "Test User"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("Create Demo Expenses (Debug)", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseRowAllExpenses(
                        expense = expense,
                        onNavigateToChat = { onNavigateToExpenseChat(expense.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseRowAllExpenses(
    expense: Expense,
    onNavigateToChat: () -> Unit = {}
) {
    val statusColor = when (expense.status) {
        ExpenseStatus.APPROVED -> Color(0xFF34C759)
        ExpenseStatus.PENDING -> Color(0xFFFFCC00)
        ExpenseStatus.REJECTED -> Color(0xFFFF3B30)
        else -> Color.Gray
    }
    
    val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedDate = expense.date?.toDate()?.let { dateFormatter.format(it) } ?: ""
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onNavigateToChat() },
                            tint = Color(0xFF007AFF)
                        )
                    }
                }
            }
        }
    }
}

