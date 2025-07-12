package com.deeksha.avr.ui.view.approver

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingApprovalsScreen(
    projectId: String? = null, // Optional project ID for project-specific approvals
    onNavigateBack: () -> Unit,
    onReviewExpense: (String) -> Unit,
    approvalViewModel: ApprovalViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val pendingExpenses by approvalViewModel.pendingExpenses.collectAsState()
    val isProcessing by approvalViewModel.isProcessing.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val error by approvalViewModel.error.collectAsState()
    val currentProject by approvalViewModel.currentProject.collectAsState()
    
    // State for filters and selections
    var selectedDate by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("") }
    var selectedExpenseIds by remember { mutableStateOf(setOf<String>()) }
    var showDateDropdown by remember { mutableStateOf(false) }
    var showDeptDropdown by remember { mutableStateOf(false) }
    
    // Load data automatically when screen opens
    LaunchedEffect(projectId) {
        if (projectId != null) {
            approvalViewModel.loadPendingApprovalsForProject(projectId)
        } else {
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
    
    // Dynamic filters
    val departments = pendingExpenses.map { it.department }.distinct().filter { it.isNotEmpty() }.sorted()
    val dates = pendingExpenses.mapNotNull { expense ->
        expense.date?.let { FormatUtils.formatDateShort(it) }
    }.distinct().sorted()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    text = "AVR ENTERTAINMENT",
                    fontSize = 18.sp,
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
            actions = {
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
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF1976D2)
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title section
            val project = currentProject
            Text(
                text = if (projectId != null && project != null) {
                    "Pending Approvals - ${project.name}"
                } else {
                    "Pending Approvals"
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Show error if any
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = { approvalViewModel.clearError() }) {
                            Text("Dismiss", color = Color.Red)
                        }
                    }
                }
            }
            
            // Filters Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Filter
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDateDropdown = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDate.isEmpty()) "Date" else selectedDate,
                            color = if (selectedDate.isEmpty()) Color.Gray else Color.Black,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Date",
                            tint = Color.Gray
                        )
                    }
                    
                    // Date Dropdown
                    if (showDateDropdown) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column {
                                // Clear option
                                Text(
                                    text = "All Dates",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                selectedDate = ""
                                showDateDropdown = false
                            }
                                        .padding(16.dp),
                                    color = Color.Black
                        )
                                
                                // Date options
                        dates.forEach { date ->
                                    Text(
                                        text = date,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                    selectedDate = date
                                    showDateDropdown = false
                                            }
                                            .padding(16.dp),
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Department Filter
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDeptDropdown = true },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedDept.isEmpty()) "Dept" else selectedDept,
                            color = if (selectedDept.isEmpty()) Color.Gray else Color.Black,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select Department",
                            tint = Color.Gray
                        )
                    }
                    
                    // Department Dropdown
                    if (showDeptDropdown) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column {
                                // Clear option
                                Text(
                                    text = "All Departments",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                selectedDept = ""
                                showDeptDropdown = false
                            }
                                        .padding(16.dp),
                                    color = Color.Black
                        )
                                
                                // Department options
                        departments.forEach { dept ->
                                    Text(
                                        text = dept,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                    selectedDept = dept
                                    showDeptDropdown = false
                                            }
                                            .padding(16.dp),
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Table Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF333333),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Dept",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF333333),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Subcategory",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF333333),
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "Submitted By",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF333333),
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.End
                    )
                    Box(
                        modifier = Modifier.width(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Empty header for checkbox column
                    }
                }
            }
            
            // Table Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseTableRow(
                        expense = expense,
                        isSelected = selectedExpenseIds.contains(expense.id),
                        onSelectionChange = { isSelected ->
                            selectedExpenseIds = if (isSelected) {
                                selectedExpenseIds + expense.id
                            } else {
                                selectedExpenseIds - expense.id
                            }
                        },
                        onExpenseClick = { onReviewExpense(expense.id) }
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
            
            // Show processing indicator
            if (isProcessing) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
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
            
            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { 
                        if (selectedExpenseIds.isNotEmpty() && !isProcessing) {
                            val currentUser = authState.user
                            val reviewerName = currentUser?.name?.takeIf { it.isNotEmpty() } ?: "System Approver"
                            
                            approvalViewModel.approveSelectedExpenses(
                                expenseIds = selectedExpenseIds.toList(),
                                reviewerName = reviewerName,
                                comments = "Bulk approved by $reviewerName"
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3),
                        disabledContainerColor = Color.Gray
                    ),
                    enabled = selectedExpenseIds.isNotEmpty() && !isProcessing,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Approve",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedExpenseIds.isNotEmpty()) 
                                "Approve Selected (${selectedExpenseIds.size})" 
                            else 
                                "Approve Selected",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Button(
                    onClick = { 
                        if (selectedExpenseIds.isNotEmpty() && !isProcessing) {
                            val currentUser = authState.user
                            val reviewerName = currentUser?.name?.takeIf { it.isNotEmpty() } ?: "System Reviewer"
                            
                            approvalViewModel.rejectSelectedExpenses(
                                expenseIds = selectedExpenseIds.toList(),
                                reviewerName = reviewerName,
                                comments = "Bulk rejected by $reviewerName"
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C757D),
                        disabledContainerColor = Color.Gray
                    ),
                    enabled = selectedExpenseIds.isNotEmpty() && !isProcessing,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedExpenseIds.isNotEmpty()) 
                                "Reject Selected (${selectedExpenseIds.size})" 
                            else 
                                "Reject Selected",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseTableRow(
    expense: Expense,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onExpenseClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpenseClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date
            Text(
                text = expense.date?.let { FormatUtils.formatDateShort(it) } ?: "",
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            
            // Department
            Text(
                text = expense.department,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            
            // Category
            Text(
                text = expense.category,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.weight(1.5f)
            )
            
            // User Name
            Text(
                text = expense.userName,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.End
            )
            
            // Checkbox
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF2196F3),
                        uncheckedColor = Color.Gray
                    )
                )
            }
        }
    }
} 