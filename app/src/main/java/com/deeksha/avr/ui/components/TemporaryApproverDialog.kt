package com.deeksha.avr.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.User
import com.deeksha.avr.viewmodel.TemporaryApproverViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemporaryApproverDialog(
    projectId: String,
    currentUserId: String,
    currentUserName: String,
    onDismiss: () -> Unit,
    onApproverAdded: () -> Unit,
    viewModel: TemporaryApproverViewModel = hiltViewModel()
) {
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    val isAddingApprover by viewModel.isAddingApprover.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Dialog state
    var selectedApprover by remember { mutableStateOf<User?>(null) }
    var numberOfDays by remember { mutableStateOf("7") }
    var searchQuery by remember { mutableStateOf("") }
    
    // Calculate expiring date based on number of days
    val expiringDate = remember(numberOfDays) {
        try {
            val days = numberOfDays.toIntOrNull() ?: 7
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, days)
            calendar.time
        } catch (e: Exception) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 7)
            calendar.time
        }
    }
    
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // Load available approvers when dialog opens
    LaunchedEffect(Unit) {
        viewModel.loadAvailableApprovers()
    }
    
    // Clear any previous errors when dialog opens
    LaunchedEffect(Unit) {
        viewModel.clearError()
    }
    
    // Handle success
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            viewModel.clearSuccessMessage()
            onApproverAdded()
            onDismiss()
        }
    }
    
    // No need for date picker state handling since we're using days input
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Temporary Approver",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Description
                Text(
                    text = "Select an approver and set the expiration date for temporary approval access.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Error message
                error?.let { errorMsg ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Text(
                            text = errorMsg,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search approvers...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        focusedLabelColor = Color(0xFF4285F4)
                    )
                )
                
                // Selected approver display
                selectedApprover?.let { approver ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Selected: ${approver.name}",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1976D2)
                                )
                                Text(
                                    text = "Phone: ${approver.phone}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "Role: ${approver.role.name}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            TextButton(
                                onClick = { selectedApprover = null }
                            ) {
                                Text("Change", color = Color(0xFF1976D2))
                            }
                        }
                    }
                }
                
                // Approver list (show only if no approver selected)
                if (selectedApprover == null) {
                    Text(
                        text = "Select Approver:",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Filter approvers based on search
                    val filteredApprovers = availableApprovers.filter { approver ->
                        searchQuery.isBlank() || 
                        approver.name.contains(searchQuery, ignoreCase = true) ||
                        approver.phone.contains(searchQuery, ignoreCase = true)
                    }
                    
                    if (availableApprovers.isEmpty()) {
                        // Show message when no approvers are available
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No Approvers Available",
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No active users with APPROVER role were found. Please ensure approvers are properly configured in the system.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else if (filteredApprovers.isEmpty()) {
                        // Show message when no approvers match search
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "No Match Found",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No approvers match your search criteria. Try a different search term.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredApprovers) { approver ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedApprover = approver },
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = approver.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Phone: ${approver.phone}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Role: ${approver.role.name}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Number of days selection
                if (selectedApprover != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Temporary Duration:",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = numberOfDays,
                        onValueChange = { newValue ->
                            // Only allow numeric input and limit to reasonable range (1-365 days)
                            if (newValue.isEmpty()) {
                                numberOfDays = newValue
                            } else if (newValue.all { it.isDigit() }) {
                                val value = newValue.toIntOrNull()
                                if (value != null && value > 0 && value <= 365) {
                                    numberOfDays = newValue
                                }
                            }
                        },
                        label = { Text("Number of days (1-365)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            focusedLabelColor = Color(0xFF4285F4)
                        ),
                        supportingText = {
                            Text(
                                text = "Enter delegation period between 1 and 365 days",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Show calculated expiration date
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "Will expire on: ${dateFormatter.format(expiringDate)}",
                                fontSize = 14.sp,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "(${numberOfDays} days from now)",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                selectedApprover?.let { approver ->
                                    viewModel.addTemporaryApprover(
                                        projectId = projectId,
                                        approverId = approver.uid,
                                        approverName = approver.name,
                                        approverPhone = approver.phone,
                                        expiringDate = expiringDate,
                                        assignedBy = currentUserId,
                                        assignedByName = currentUserName
                                    )
                                }
                            },
                            enabled = selectedApprover != null && 
                                     numberOfDays.isNotEmpty() && 
                                     numberOfDays.toIntOrNull()?.let { it > 0 && it <= 365 } == true && 
                                     !isAddingApprover,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            if (isAddingApprover) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Add Approver")
                            }
                        }
                    }
                }
            }
        }
    }
}
