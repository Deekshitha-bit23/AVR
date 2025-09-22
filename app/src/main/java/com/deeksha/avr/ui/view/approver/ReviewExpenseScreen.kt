package com.deeksha.avr.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
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
import com.deeksha.avr.viewmodel.ProjectViewModel
import com.deeksha.avr.utils.FormatUtils
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewExpenseScreen(
    expenseId: String,
    onNavigateBack: () -> Unit,
    approvalViewModel: ApprovalViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val selectedExpense by approvalViewModel.selectedExpense.collectAsState()
    val isLoading by approvalViewModel.isLoading.collectAsState()
    val error by approvalViewModel.error.collectAsState()
    val isProcessing by approvalViewModel.isProcessing.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    var reviewerNote by remember { mutableStateOf("") }
    
    // Track when processing was in progress to detect completion
    var wasProcessing by remember { mutableStateOf(false) }
    
    // Track edited amount for approval/rejection
    var editedAmount by remember { mutableStateOf(0.0) }
    
    // Update edited amount when expense changes
    LaunchedEffect(selectedExpense) {
        selectedExpense?.let { editedAmount = it.amount }
    }
    
    // Load the expense when screen opens
    LaunchedEffect(expenseId) {
        approvalViewModel.fetchExpenseById(expenseId)
    }
    
    // Navigate back automatically after successful approval/rejection
    LaunchedEffect(isProcessing) {
        if (wasProcessing && !isProcessing && error == null) {
            // Processing was ongoing and now completed without error
    
            kotlinx.coroutines.delay(250) // Reduced delay for faster navigation
            onNavigateBack()
        }
        wasProcessing = isProcessing
    }
    
    // No need to load project details separately for approval
    
    // Also add an alternative navigation trigger based on status change
    LaunchedEffect(selectedExpense?.status) {
        selectedExpense?.let { expense ->
            if (expense.status != com.deeksha.avr.model.ExpenseStatus.PENDING) {
        
                kotlinx.coroutines.delay(200) // Reduced delay for faster navigation
                onNavigateBack()
            }
        }
    }


    
    // Calculate budget summary for the department (simplified for approval view)
    val departmentBudget = 30000.0 // Default budget for demonstration
    val departmentSpent = 0.0 // Current spent amount
    val remaining = departmentBudget - departmentSpent
    val afterApproval = remaining - editedAmount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Header
        TopAppBar(
            title = {
                Text(
                    text = "AVR Entertainment",
                    fontSize = 22.sp,
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
                containerColor = Color(0xFF1976D2)
            )
        )

        if (selectedExpense == null) {
            // Show loading or expense not found
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading expense details...",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Expense not found",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This expense may have been already processed or removed.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        } else {
            val scrollState = rememberScrollState()
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Approval Model Heading
                Text(
                    text = "Approval Model",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // Expense Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ExpenseDetailRow(label = "Department", value = selectedExpense?.department ?: "")
                        ExpenseDetailRow(label = "Subcategory", value = selectedExpense?.category ?: "")
                        
                        // Editable Amount Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Amount",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Amount Input Field
                            OutlinedTextField(
                                value = editedAmount.toString(),
                                onValueChange = { newValue ->
                                    val amount = newValue.toDoubleOrNull() ?: 0.0
                                    editedAmount = amount
                                },
                                modifier = Modifier.width(120.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1976D2),
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = Color(0xFF1976D2)
                                ),
                                label = { Text("₹") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        }
                        
                        // Show amount change indicator and reset button
                        if (editedAmount != selectedExpense?.amount) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Original: ₹${selectedExpense?.amount} → New: ₹$editedAmount",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1976D2),
                                    fontWeight = FontWeight.Medium
                                )
                                
                                TextButton(
                                    onClick = { selectedExpense?.let { editedAmount = it.amount } },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = Color(0xFF1976D2)
                                    )
                                ) {
                                    Text(
                                        text = "Reset to Original",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                        
                        // Show validation errors
                        if (editedAmount < 0) {
                            Text(
                                text = "Amount cannot be negative",
                                fontSize = 12.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Show warning for very high amounts (more than 10x original)
                        if (selectedExpense?.let { editedAmount > it.amount * 10 } == true) {
                            Text(
                                text = "Warning: Amount is significantly higher than original",
                                fontSize = 12.sp,
                                color = Color(0xFFFF9800), // Orange warning color
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        ExpenseDetailRow(
                            label = "Date", 
                            value = selectedExpense?.date?.let { FormatUtils.formatDate(it) } ?: "N/A"
                        )
                        ExpenseDetailRow(
                            label = "Payment Mode",
                            value = when (selectedExpense?.modeOfPayment?.lowercase()) {
                                "cash" -> "By cash"
                                "upi" -> "By UPI"
                                "check" -> "By cheque"
                                else -> selectedExpense?.modeOfPayment?.ifEmpty { "Not specified" } ?: "Not specified"
                            }
                        )
                    }
                }

                // Notes from Submitter Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Create,
                            contentDescription = "Notes",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Notes from Submitter:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedExpense?.description?.ifEmpty { "No notes provided" } ?: "No notes provided",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Attachment Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Attachment",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attachment:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedExpense?.attachmentUrl?.isNotEmpty() == true || selectedExpense?.attachmentFileName?.isNotEmpty() == true) {
                                selectedExpense?.attachmentFileName?.ifEmpty { "File attached" } ?: "File attached"
                            } else {
                                "No attachment"
                            },
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                // Budget Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = "Budget",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Budget Summary:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        BudgetSummaryRow("Budget", departmentBudget, Color.Black)
                        BudgetSummaryRow("Spent", departmentSpent, Color.Black)
                        BudgetSummaryRow("Remaining", remaining, Color.Black)
                        BudgetSummaryRow(
                            "After Approval:",
                            afterApproval,
                            if (afterApproval >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                        
                        // Show amount change impact if amount was modified
                        if (editedAmount != selectedExpense?.amount) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.Gray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val originalAfterApproval = remaining - (selectedExpense?.amount ?: 0.0)
                            val amountDifference = (selectedExpense?.amount ?: 0.0) - editedAmount
                            val budgetImpact = if (amountDifference > 0) "Savings" else "Additional Cost"
                            
                            Text(
                                text = "Amount Change Impact:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1976D2)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Original After Approval:",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = FormatUtils.formatCurrency(originalAfterApproval),
                                    fontSize = 12.sp,
                                    color = if (originalAfterApproval >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Budget Impact:",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "${if (amountDifference > 0) "+" else ""}${FormatUtils.formatCurrency(kotlin.math.abs(amountDifference))} $budgetImpact",
                                    fontSize = 12.sp,
                                    color = if (amountDifference > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Reviewer Note Input with minimal padding
                OutlinedTextField(
                    value = reviewerNote,
                    onValueChange = { reviewerNote = it },
                    label = { Text("Add Reviewer Note (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1976D2),
                        unfocusedBorderColor = Color.Gray
                    ),
                    maxLines = 3
                )

                // Action Buttons - No gap with text field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val currentUser = authState.user
                            val reviewerName = currentUser?.name?.takeIf { it.isNotEmpty() } ?: "System Approver"
                            
                            // Check if amount was changed
                            if (editedAmount != selectedExpense?.amount) {
                                // Amount was changed, use the new method
                                selectedExpense?.let { expense ->
                                    approvalViewModel.approveExpenseWithAmount(
                                        expense = expense,
                                        newAmount = editedAmount,
                                        reviewerName = reviewerName,
                                        comments = reviewerNote.ifEmpty { "Approved with amount change from ₹${expense.amount} to ₹$editedAmount by $reviewerName" }
                                    )
                                }
                            } else {
                                // No amount change, use the original method
                                selectedExpense?.let { expense ->
                                    approvalViewModel.approveExpense(
                                        expense = expense,
                                        reviewerName = reviewerName,
                                        comments = reviewerNote.ifEmpty { "Approved by $reviewerName" }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp), // Slightly taller for better touch
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3), // Blue color matching design
                            disabledContainerColor = Color.Gray
                        ),
                        enabled = !isProcessing && editedAmount >= 0,
                        shape = RoundedCornerShape(25.dp), // More rounded corners
                        elevation = ButtonDefaults.buttonElevation(0.dp) // No elevation for flat look
                    ) {
                        if (isProcessing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Approving...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = if (editedAmount != selectedExpense?.amount) "Approve (₹$editedAmount)" else "Approve",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            val currentUser = authState.user
                            val reviewerName = currentUser?.name?.takeIf { it.isNotEmpty() } ?: "System Reviewer"
                            
                            // Check if amount was changed
                            if (editedAmount != selectedExpense?.amount) {
                                // Amount was changed, use the new method
                                selectedExpense?.let { expense ->
                                    approvalViewModel.rejectExpenseWithAmount(
                                        expense = expense,
                                        newAmount = editedAmount,
                                        reviewerName = reviewerName,
                                        comments = reviewerNote.ifEmpty { "Rejected with amount change from ₹${expense.amount} to ₹$editedAmount by $reviewerName" }
                                    )
                                }
                            } else {
                                // No amount change, use the original method
                                selectedExpense?.let { expense ->
                                    approvalViewModel.rejectExpense(
                                        expense = expense,
                                        reviewerName = reviewerName,
                                        comments = reviewerNote.ifEmpty { "Rejected by $reviewerName" }
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp), // Slightly taller for better touch
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722), // Orange color matching design
                            disabledContainerColor = Color.Gray
                        ),
                        enabled = !isProcessing && editedAmount >= 0,
                        shape = RoundedCornerShape(25.dp), // More rounded corners
                        elevation = ButtonDefaults.buttonElevation(0.dp) // No elevation for flat look
                    ) {
                        if (isProcessing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Rejecting...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = if (editedAmount != selectedExpense?.amount) "Reject (₹$editedAmount)" else "Reject",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
                
                // Minimal bottom spacing
                Spacer(modifier = Modifier.height(4.dp))

                // Show error if any
                error?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Minimal bottom spacing
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ExpenseDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun BudgetSummaryRow(
    label: String,
    amount: Double,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "• $label",
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = FormatUtils.formatCurrency(amount),
            fontSize = 14.sp,
            fontWeight = if (label.contains("After")) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
} 