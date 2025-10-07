package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
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
import com.deeksha.avr.viewmodel.ApprovalViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.utils.FormatUtils
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHeadPendingApprovals(
    projectId: String? = null, // Optional project ID for project-specific approvals
    onNavigateBack: () -> Unit,
    onReviewExpense: (String) -> Unit,
    onNavigateToExpenseChat: (String) -> Unit = {}, // Add chat navigation parameter
    approvalViewModel: ApprovalViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val pendingExpenses by approvalViewModel.pendingExpenses.collectAsState()
    val isProcessing by approvalViewModel.isProcessing.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val error by approvalViewModel.error.collectAsState()
    
    // State for filters and selections
    var selectedDate by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("") }
    var selectedExpenseIds by remember { mutableStateOf(setOf<String>()) }
    var showDeptModal by remember { mutableStateOf(false) }
    
    // Load data automatically when screen opens
    LaunchedEffect(projectId) {
        if (projectId != null) {
            // Load project-specific pending approvals
            approvalViewModel.loadPendingApprovalsForProject(projectId)
        } else {
            // Load all pending approvals
            approvalViewModel.loadPendingApprovals()
        }
    }
    
    // Auto-refresh after processing completes
    LaunchedEffect(isProcessing) {
        if (!isProcessing && selectedExpenseIds.isNotEmpty()) {
            delay(1000) // Wait for Firebase to update
            selectedExpenseIds = emptySet() // Clear selections
            // Auto-refresh data
            if (projectId != null) {
                approvalViewModel.loadPendingApprovalsForProject(projectId)
            } else {
                approvalViewModel.loadPendingApprovals()
            }
        }
    }
    
    // Filter expenses based on selected filters
    val filteredExpenses = pendingExpenses.filter { expense ->
        val dateMatch = selectedDate.isEmpty() || expense.date?.let {
            FormatUtils.formatDateShort(it)
        } == selectedDate
        val deptMatch = selectedDept.isEmpty() || expense.department == selectedDept
        dateMatch && deptMatch
    }
    
    // Dynamic filters - only show what exists in data
    val departments = pendingExpenses
        .map { it.department }
        .distinct()
        .filter { it.isNotEmpty() }
        .sorted()
    
    val dates = pendingExpenses
        .mapNotNull { expense ->
            expense.date?.let { FormatUtils.formatDateShort(it) }
        }
        .distinct()
        .sorted()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with close and search icons
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                    Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Black
                        )
                    }
                    
                    // Show selection count when items are selected
                    if (selectedExpenseIds.isNotEmpty()) {
                            Text(
                            text = "${selectedExpenseIds.size} selected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    IconButton(onClick = { 
                        if (projectId != null) {
                            approvalViewModel.loadPendingApprovalsForProject(projectId)
                        } else {
                            approvalViewModel.loadPendingApprovals()
                        }
                    }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Refresh",
                            tint = Color.Black
                        )
                    }
                }
            }
            
            // Filter pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterPill(
                    text = "All Dates",
                    isSelected = selectedDate.isEmpty(),
                    onClick = { selectedDate = "" }
                )
                
                FilterPill(
                    text = "All Depts",
                    isSelected = selectedDept.isEmpty(),
                    onClick = { showDeptModal = true }
                )
                
                FilterPill(
                    text = "Amount",
                    isSelected = false,
                    onClick = { /* TODO: Implement amount sorting */ }
                )
            }
            
            // Expenses list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        isSelected = selectedExpenseIds.contains(expense.id),
                        onSelectionChange = { isSelected ->
                            selectedExpenseIds = if (isSelected) {
                                selectedExpenseIds + expense.id
                            } else {
                                selectedExpenseIds - expense.id
                            }
                        },
                        onExpenseClick = { onReviewExpense(expense.id) },
                        onChatClick = { onNavigateToExpenseChat(expense.id) }
                    )
                }
                
                if (filteredExpenses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (pendingExpenses.isEmpty()) 
                                    "No pending approvals found! ✅" 
                                else 
                                    "No expenses match the selected filters",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
        
        // Department filter modal
        if (showDeptModal) {
            DepartmentFilterModal(
                departments = departments,
                selectedDepartment = selectedDept,
                onDepartmentSelected = { dept ->
                    selectedDept = dept
                    showDeptModal = false
                },
                onDismiss = { showDeptModal = false }
            )
        }
        
        // Floating Approve and Reject buttons
        if (selectedExpenseIds.isNotEmpty() && !isProcessing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Approve Button
                Button(
                    onClick = { 
                            val currentUser = authState.user
                            val reviewerName = currentUser?.name?.takeIf { it.isNotEmpty() } ?: "System Approver"
                            
                            approvalViewModel.approveSelectedExpenses(
                                expenseIds = selectedExpenseIds.toList(),
                                reviewerName = reviewerName,
                                comments = "Bulk approved by $reviewerName"
                            )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Green color
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Approve",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Approve",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Reject Button
                Button(
                    onClick = { 
                            val currentUser = authState.user
                        val reviewerName = currentUser?.name?.takeIf { it.isNotEmpty() } ?: "System Approver"
                            
                            approvalViewModel.rejectSelectedExpenses(
                                expenseIds = selectedExpenseIds.toList(),
                                reviewerName = reviewerName,
                                comments = "Bulk rejected by $reviewerName"
                            )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336) // Red color
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Reject",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reject",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Show processing indicator
        if (isProcessing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                colors = CardDefaults.cardColors(containerColor = Color.Blue.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Blue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Processing ${selectedExpenseIds.size} selected expenses...",
                        color = Color.Blue,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun FilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE0E0E0) else Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp,
            color = if (isSelected) Color.Black else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun ExpenseCard(
    expense: Expense,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onExpenseClick: () -> Unit,
    onChatClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpenseClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio button
            RadioButton(
                selected = isSelected,
                onClick = { onSelectionChange(!isSelected) },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFF1976D2)
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Main content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "₹${String.format("%.2f", expense.amount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
            Text(
                text = expense.category,
                    fontSize = 14.sp,
                    color = Color.Black
            )
            
                Spacer(modifier = Modifier.height(2.dp))
                
            Text(
                    text = "By: ${expense.userName}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            // Right side content
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expense.date?.let { 
                        FormatUtils.formatDateShort(it) 
                    } ?: "",
                    fontSize = 14.sp,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onChatClick() }
                            .padding(6.dp)
                    )
                    
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "View Details",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DepartmentFilterModal(
    departments: List<String>,
    selectedDepartment: String,
    onDepartmentSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable { /* Prevent click through */ },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Filter by Department",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // All Departments option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDepartmentSelected("") }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Departments",
                        fontSize = 16.sp,
                        color = if (selectedDepartment.isEmpty()) Color(0xFF1976D2) else Color.Black
                    )
                }
                
                // Department options
                departments.forEach { dept ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDepartmentSelected(dept) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dept,
                            fontSize = 16.sp,
                            color = if (selectedDepartment == dept) Color(0xFF1976D2) else Color.Black
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cancel button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF1976D2),
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
} 