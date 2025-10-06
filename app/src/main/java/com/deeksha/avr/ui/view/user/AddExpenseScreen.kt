package com.deeksha.avr.ui.view.user

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Project
import com.deeksha.avr.viewmodel.ExpenseViewModel
import com.deeksha.avr.viewmodel.NotificationViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.ui.common.NotificationBadgeComponent
import com.deeksha.avr.ui.common.NotificationPulseIndicator
import com.deeksha.avr.ui.components.BudgetWarningComponent
import com.deeksha.avr.ui.components.BudgetExceededDialog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    project: Project,
    userId: String,
    userName: String,
    onNavigateBack: () -> Unit,
    onExpenseAdded: () -> Unit,
    onNavigateToNotifications: (String) -> Unit = {},
    onNavigateToExpenseChat: (String) -> Unit = {},
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val formData by expenseViewModel.formData.collectAsState()
    val error by expenseViewModel.error.collectAsState()
    val isSubmitting by expenseViewModel.isSubmitting.collectAsState()
    val successMessage by expenseViewModel.successMessage.collectAsState()
    val budgetValidationResult by expenseViewModel.budgetValidationResult.collectAsState()
    val budgetWarning by expenseViewModel.budgetWarning.collectAsState()
    
    // Notification states
    val authState by authViewModel.authState.collectAsState()
    val notificationBadge by notificationViewModel.notificationBadge.collectAsState()
    val notifications by notificationViewModel.notifications.collectAsState()
    val isNotificationsLoading by notificationViewModel.isLoading.collectAsState()
    
    // Filter notifications for this project
    val projectNotifications = notifications.filter { it.projectId == project.id }
    val projectNotificationBadge = remember(projectNotifications) {
        val unreadCount = projectNotifications.count { !it.isRead }
        com.deeksha.avr.model.NotificationBadge(
            count = unreadCount,
            hasUnread = unreadCount > 0
        )
    }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showAttachmentDialog by remember { mutableStateOf(false) }
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachmentName by remember { mutableStateOf<String?>(null) }
    var showDepartmentDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showBudgetExceededDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Set the selected project in the view model so departments can be loaded
    LaunchedEffect(project) {
        expenseViewModel.setSelectedProject(project)
    }
    
    // Real-time budget validation when amount or department changes
    LaunchedEffect(formData.amount, formData.department) {
        if (formData.amount.isNotEmpty() && formData.department.isNotEmpty()) {
            val amount = formData.amount.toDoubleOrNull()
            if (amount != null && amount > 0) {
                expenseViewModel.validateBudget(project.id, formData.department, amount)
            }
        }
    }
    
    // Load notifications when screen opens
    LaunchedEffect(Unit) {
        authState.user?.uid?.let { userId ->
            notificationViewModel.loadNotifications(userId)
        }
    }
    
    // Refresh notifications when expense is successfully submitted
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            // Refresh notifications after successful expense submission
            authState.user?.uid?.let { userId ->
                delay(1000) // Wait for notification to be created
                notificationViewModel.onScreenVisible(userId)
            }
        }
    }
    
    // Activity result launchers for attachment selection
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedAttachmentUri?.let { uri ->
                selectedAttachmentName = "camera_${System.currentTimeMillis()}.jpg"
                expenseViewModel.updateFormField("attachmentUri", uri.toString())
            }
        }
    }
    
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedAttachmentUri = it
            selectedAttachmentName = "photo_${System.currentTimeMillis()}.jpg"
            expenseViewModel.updateFormField("attachmentUri", it.toString())
        }
    }
    
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedAttachmentUri = it
            selectedAttachmentName = "document_${System.currentTimeMillis()}.pdf"
            expenseViewModel.updateFormField("attachmentUri", it.toString())
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Create a temporary URI for the camera to save the image
            val tempUri = context.contentResolver.insert(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                android.content.ContentValues()
            )
            selectedAttachmentUri = tempUri
            tempUri?.let { cameraLauncher.launch(it) }
        }
    }
    
    LaunchedEffect(error) {
        if (error != null) {
            // Handle error display
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
                    "AVR Entertainment",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ) 
            },
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
                        if (isNotificationsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF4285F4)
                            )
                        } else {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Project Notifications",
                                tint = Color(0xFF4285F4)
                            )
                        }
                    }
                    
                    // Project-specific notification badge - only show when not loading
                    if (!isNotificationsLoading) {
                        NotificationBadgeComponent(
                            badge = projectNotificationBadge,
                            modifier = Modifier.align(Alignment.TopEnd)
                        )
                        
                        // Pulse indicator for better visibility
                        NotificationPulseIndicator(
                            hasUnread = projectNotificationBadge.hasUnread,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = (-4).dp)
                        )
                    }
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
            // Project Info
            item {
                SelectedProjectCard(project = project)
            }
            
            // Success Message
            successMessage?.let {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Green.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = it,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Error Message
            error?.let {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Budget Warning Component
            item {
                BudgetWarningComponent(
                    budgetValidationResult = budgetValidationResult,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            
            // Date Field
            item {
                OutlinedTextField(
                    value = formData.date,
                    onValueChange = { /* Handle via date picker */ },
                    label = { Text("Date") },
                    placeholder = { Text("DD/MM/YYYY") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
            }
            
            // Amount Field
            item {
                val budgetInfo = if (formData.department.isNotEmpty() && budgetValidationResult != null) {
                    val result = budgetValidationResult!!
                    "Budget: ₹${String.format("%.0f", result.departmentBudget)} | Spent: ₹${String.format("%.0f", result.currentSpent)} | Remaining: ₹${String.format("%.0f", result.remainingBudget)}"
                } else {
                    "Select department to see budget info"
                }
                
                OutlinedTextField(
                    value = formData.amount,
                    onValueChange = { expenseViewModel.updateFormField("amount", it) },
                    label = { Text("Amount") },
                    placeholder = { Text("Enter amount") },
                    supportingText = { 
                        Text(
                            text = budgetInfo,
                            fontSize = 11.sp,
                            color = if (budgetValidationResult?.isValid == false) Color(0xFFD32F2F) else Color(0xFF666666)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (budgetValidationResult?.isValid == false) Color(0xFFD32F2F) else Color(0xFF4285F4),
                        unfocusedBorderColor = if (budgetValidationResult?.isValid == false) Color(0xFFD32F2F) else Color.Gray.copy(alpha = 0.5f)
                    )
                )
            }
            
            // Department Dropdown
            item {
                val departmentBudgetInfo = if (formData.department.isNotEmpty() && budgetValidationResult != null) {
                    val result = budgetValidationResult!!
                    val progress = if (result.departmentBudget > 0) (result.currentSpent / result.departmentBudget * 100) else 0.0
                    "${String.format("%.0f", progress)}% used (₹${String.format("%.0f", result.remainingBudget)} remaining)"
                } else {
                    "Select department to see budget status"
                }
                
                ExposedDropdownMenuBox(
                    expanded = showDepartmentDropdown,
                    onExpandedChange = { showDepartmentDropdown = !showDepartmentDropdown }
                ) {
                    OutlinedTextField(
                        value = formData.department,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Department") },
                        placeholder = { Text("Select department") },
                        supportingText = {
                            Text(
                                text = departmentBudgetInfo,
                                fontSize = 11.sp,
                                color = if (budgetValidationResult?.isValid == false) Color(0xFFD32F2F) else Color(0xFF666666)
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (budgetValidationResult?.isValid == false) Color(0xFFD32F2F) else Color(0xFF4285F4),
                            unfocusedBorderColor = if (budgetValidationResult?.isValid == false) Color(0xFFD32F2F) else Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showDepartmentDropdown,
                        onDismissRequest = { showDepartmentDropdown = false }
                    ) {
                        if (expenseViewModel.departments.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No departments available") },
                                onClick = { }
                            )
                        } else {
                            expenseViewModel.departments.forEach { department ->
                                DropdownMenuItem(
                                    text = { Text(department) },
                                    onClick = {
                                        expenseViewModel.updateFormField("department", department)
                                        showDepartmentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Category Input with Dropdown Suggestions
            item {
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    OutlinedTextField(
                        value = formData.category,
                        onValueChange = { expenseViewModel.updateFormField("category", it) },
                        label = { Text("Category") },
                        placeholder = { 
                            Text(
                                if (expenseViewModel.categories.isEmpty()) 
                                    "No categories configured - contact project manager" 
                                else 
                                    "Type or select category..."
                            ) 
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        // Show available categories that match the input
                        val availableCategories = expenseViewModel.categories.filter { 
                            it.contains(formData.category, ignoreCase = true) || formData.category.isEmpty()
                        }
                        
                        if (expenseViewModel.categories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No categories configured for this project") },
                                onClick = { }
                            )
                        } else if (availableCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No matching categories") },
                                onClick = { }
                            )
                        } else {
                            availableCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        expenseViewModel.updateFormField("category", category)
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Description Field
            item {
                OutlinedTextField(
                    value = formData.description,
                    onValueChange = { expenseViewModel.updateFormField("description", it) },
                    label = { Text("Description") },
                    placeholder = { Text("Enter description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                    )
                )
            }
            
            // Mode of Payment
            item {
                Text(
                    text = "Mode of Payment",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    expenseViewModel.paymentModes.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = formData.modeOfPayment == value,
                                    onClick = { expenseViewModel.updateFormField("modeOfPayment", value) }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = formData.modeOfPayment == value,
                                onClick = { expenseViewModel.updateFormField("modeOfPayment", value) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF4285F4)
                                )
                            )
                            Text(
                                text = label,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
            
            // Add Attachment Button
            item {
                Column {
                    Button(
                        onClick = { showAttachmentDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(
                            text = "📎 Add Attachment",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Show selected attachment info
                    selectedAttachmentName?.let { name ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE8F5E8)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attachment: $name",
                                    fontSize = 14.sp,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    onClick = {
                                        selectedAttachmentUri = null
                                        selectedAttachmentName = null
                                        expenseViewModel.updateFormField("attachmentUri", "")
                                    }
                                ) {
                                    Text("Remove", color = Color(0xFFF44336))
                                }
                            }
                        }
                    }
                }
            }
            
            // Submit Button and Chat Icon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Submit Button
                    Button(
                        onClick = {
                            // Check if budget validation failed
                            if (budgetValidationResult?.isValid == false) {
                                showBudgetExceededDialog = true
                            } else {
                                expenseViewModel.submitExpense(
                                    projectId = project.id,
                                    userId = userId,
                                    userName = userName,
                                    onSuccess = onExpenseAdded
                                )
                            }
                        },
                        enabled = !isSubmitting && formData.amount.isNotEmpty() && formData.department.isNotEmpty() && formData.category.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (budgetValidationResult?.isValid == false) Color(0xFFD32F2F) else Color(0xFF4285F4)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = when {
                                    budgetValidationResult?.isValid == false -> "Budget Exceeded"
                                    else -> "Submit for Approval"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Chat Icon Button
                    Button(
                        onClick = {
                            onNavigateToExpenseChat(project.id)
                        },
                        modifier = Modifier
                            .size(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Chat with Approver",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = { dateString ->
                expenseViewModel.updateFormField("date", dateString)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
    
    // Attachment Selection Dialog
    if (showAttachmentDialog) {
        AttachmentSelectionDialog(
            onCameraSelected = {
                showAttachmentDialog = false
                // Check camera permission
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                        // Create a temporary URI for the camera to save the image
                        val tempUri = context.contentResolver.insert(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            android.content.ContentValues()
                        )
                        selectedAttachmentUri = tempUri
                        tempUri?.let { cameraLauncher.launch(it) }
                    }
                    else -> {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            },
            onPhotoSelected = {
                showAttachmentDialog = false
                photoPickerLauncher.launch("image/*")
            },
            onPdfSelected = {
                showAttachmentDialog = false
                documentPickerLauncher.launch("application/pdf")
            },
            onDismiss = { showAttachmentDialog = false }
        )
    }
    
    // Budget Exceeded Dialog
    if (showBudgetExceededDialog && budgetValidationResult != null) {
        val result = budgetValidationResult!!
        BudgetExceededDialog(
            budgetValidationResult = result,
            onDismiss = { 
                showBudgetExceededDialog = false
                expenseViewModel.clearBudgetWarning()
            },
            onProceed = {
                showBudgetExceededDialog = false
                expenseViewModel.submitExpense(
                    projectId = project.id,
                    userId = userId,
                    userName = userName,
                    onSuccess = onExpenseAdded
                )
            }
        )
    }
}

@Composable
fun SelectedProjectCard(project: Project) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Selected Project",
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = project.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Budget: ${java.text.NumberFormat.getCurrencyInstance(java.util.Locale("en", "IN")).format(project.budget)}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = calendar.timeInMillis
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val selectedCalendar = Calendar.getInstance()
                    selectedCalendar.timeInMillis = millis
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    onDateSelected(dateFormat.format(selectedCalendar.time))
                }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}



@Composable
fun AttachmentSelectionDialog(
    onCameraSelected: () -> Unit,
    onPhotoSelected: () -> Unit,
    onPdfSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Attachment",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Choose how you want to add an attachment:",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Camera Option
                AttachmentOptionItem(
                    icon = Icons.Default.Add,
                    title = "Take Photo",
                    description = "Use camera to capture receipt",
                    onClick = onCameraSelected
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Photo Gallery Option
                AttachmentOptionItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Choose Photo",
                    description = "Select from photo gallery",
                    onClick = onPhotoSelected
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // PDF Option
                AttachmentOptionItem(
                    icon = Icons.Default.Email,
                    title = "Add PDF",
                    description = "Select PDF document",
                    onClick = onPdfSelected
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AttachmentOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF4285F4),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
} 