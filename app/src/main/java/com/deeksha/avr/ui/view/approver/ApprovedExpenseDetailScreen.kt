package com.deeksha.avr.ui.view.approver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.ExpenseStatus
import com.deeksha.avr.utils.FormatUtils
import com.deeksha.avr.viewmodel.ApprovalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovedExpenseDetailScreen(
    expenseId: String,
    onNavigateBack: () -> Unit,
    approvalViewModel: ApprovalViewModel = hiltViewModel()
) {
    val selectedExpense by approvalViewModel.selectedExpense.collectAsState()
    val isLoading by approvalViewModel.isLoading.collectAsState()

    LaunchedEffect(expenseId) {
        approvalViewModel.fetchExpenseById(expenseId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Expense Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "Done", color = Color(0xFF4285F4))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        if (isLoading || selectedExpense == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4285F4))
            }
            return@Scaffold
        }

        val expense = selectedExpense!!

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount + Status
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Amount", color = Color.Gray)
                        Text(
                            text = FormatUtils.formatCurrency(expense.amount),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Department:  ${expense.department}", color = Color.Gray)
                        Text(text = "Categories:  ${expense.category}", color = Color.Gray)
                    }

                    // Softer green badge with tick icon
                    if (expense.status == ExpenseStatus.APPROVED) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(24.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Approved",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = expense.status.name.replaceFirstChar { it.titlecase() },
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Expense Details
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Expense Details", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(label = "Date", value = FormatUtils.formatDate(expense.date))
                    val submittedByVal = expense.userName.ifBlank { expense.userId }
                    val descriptionVal = expense.description.ifBlank { "—" }
                    DetailRow(label = "Submitted By", value = submittedByVal)
                    DetailRow(label = "Description", value = descriptionVal)
                }
            }

            // Payment Information
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Payment Information", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    val mode = when (expense.modeOfPayment) {
                        "cash" -> "By cash"
                        "upi" -> "By UPI"
                        "check" -> "By Cheque"
                        else -> expense.modeOfPayment.ifEmpty { "Not specified" }
                    }
                    DetailRow(label = "Payment Mode", value = mode)
                }
            }

            // Approval Information
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(text = "Approval Information", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailRow(label = "Last Updated", value = FormatUtils.formatDateTime(expense.reviewedAt))
                    DetailRow(label = "Submitted By", value = expense.reviewedBy ?: "—")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "This expense has been approved and processed",
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = Color.Gray)
        Text(text = value, color = Color.Black)
    }
}

private fun defaultApprovalMessage(expense: com.deeksha.avr.model.Expense): String =
    if (expense.status == ExpenseStatus.APPROVED) "This expense has been approved and processed" else "—"


