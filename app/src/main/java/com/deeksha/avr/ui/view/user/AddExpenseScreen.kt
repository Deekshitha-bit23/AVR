package com.deeksha.avr.ui.view.user

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.AttachFile
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
        // Top Bar - iOS style with Cancel button and New Expense title
        TopAppBar(
            title = { 
                Text(
                    "New Expense",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                ) 
            },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text(
                        "Cancel",
                        color = Color(0xFF4285F4),
                        fontSize = 16.sp
                    )
                }
            },
            actions = {
                // No notification icon - removed as per requirement
                // Add empty box to balance the navigation icon for center alignment
                Spacer(modifier = Modifier.width(80.dp))
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Project Details Card (matching design)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                ProjectDetailsCard(project = project)
                Spacer(modifier = Modifier.height(16.dp))
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
                    Spacer(modifier = Modifier.height(16.dp))
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
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Budget Warning Component
            item {
                BudgetWarningComponent(
                    budgetValidationResult = budgetValidationResult,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // EXPENSE DETAILS Section
            item {
                Text(
                    text = "EXPENSE DETAILS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF999999),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Date Field - Pill-shaped button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Date",
                                fontSize = 16.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Normal
                            )
                            // Format date display
                            val displayDate = if (formData.date.isNotEmpty()) {
                                try {
                                    val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    val date = inputFormat.parse(formData.date)
                                    date?.let { outputFormat.format(it) } ?: formData.date
                                } catch (e: Exception) {
                                    formData.date
                                }
                            } else {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
                            }
                            
                            TextButton(
                                onClick = { showDatePicker = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .background(
                                        Color(0xFFF5F5F5),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                Text(
                                    text = displayDate,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        // Amount Field
                        Column {
                            Text(
                                text = "Amount",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = formData.amount,
                                onValueChange = { expenseViewModel.updateFormField("amount", it) },
                                placeholder = { Text("0", color = Color(0xFF999999)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4285F4),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }
                        
                        // Department Field
                        Column {
                            Text(
                                text = "Department",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ExposedDropdownMenuBox(
                                expanded = showDepartmentDropdown,
                                onExpandedChange = { showDepartmentDropdown = !showDepartmentDropdown }
                            ) {
                                OutlinedTextField(
                                    value = formData.department,
                                    onValueChange = { },
                                    readOnly = true,
                                    placeholder = { Text("Labour", color = Color.Black) },
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF999999))
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFFE0E0E0),
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp)
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
                        
                        // Category Field
                        Column {
                            Text(
                                text = "Category",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExposedDropdownMenuBox(
                                    expanded = showCategoryDropdown,
                                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = formData.category,
                                        onValueChange = { expenseViewModel.updateFormField("category", it) },
                                        placeholder = { 
                                            Text(
                                                "Enter category name",
                                                color = Color(0xFF999999)
                                            ) 
                                        },
                                        trailingIcon = {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF999999))
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF4285F4),
                                            unfocusedBorderColor = Color(0xFFE0E0E0),
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        singleLine = true
                                    )
                                    
                                    ExposedDropdownMenu(
                                        expanded = showCategoryDropdown,
                                        onDismissRequest = { showCategoryDropdown = false }
                                    ) {
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
                                
                                // Plus button for adding categories
                                IconButton(
                                    onClick = { 
                                        // Placeholder for adding category functionality
                                        // Currently just allows typing in the field
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF4285F4), RoundedCornerShape(20.dp))
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add category",
                                        tint = Color.White
                                    )
                                }
                            }
                            // Hint text for multiple categories
                            Text(
                                text = "Add multiple categories by tapping the + button",
                                fontSize = 12.sp,
                                color = Color(0xFF999999),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        // Description Field
                        Column {
                            Text(
                                text = "Description",
                                fontSize = 16.sp,
                                color = Color.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = formData.description,
                                onValueChange = { expenseViewModel.updateFormField("description", it) },
                                placeholder = { Text("Enter description", color = Color(0xFF999999)) },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4285F4),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // MODE OF PAYMENT Section
            item {
                Text(
                    text = "MODE OF PAYMENT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF999999),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        expenseViewModel.paymentModes.forEach { (value, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = formData.modeOfPayment == value,
                                        onClick = { expenseViewModel.updateFormField("modeOfPayment", value) }
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = formData.modeOfPayment == value,
                                    onClick = { expenseViewModel.updateFormField("modeOfPayment", value) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = Color(0xFF4285F4)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Simple icons - you can replace with custom icons if available
                                val iconText = when (value) {
                                    "cash" -> "💵"
                                    "upi" -> "📱"
                                    "check" -> "📄"
                                    else -> ""
                                }
                                Text(
                                    text = iconText,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // ATTACHMENT Section
            item {
                Text(
                    text = "ATTACHMENT (OPTIONAL)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF999999),
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedButton(
                    onClick = { showAttachmentDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF4285F4)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Add Attachment",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Attachment",
                        fontSize = 16.sp,
                        color = Color(0xFF4285F4)
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
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Submit Button
            item {
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
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSubmitting || (!formData.amount.isNotEmpty() || !formData.department.isNotEmpty() || !formData.category.isNotEmpty())) Color(0xFFCCCCCC) else Color(0xFF4285F4),
                        disabledContainerColor = Color(0xFFCCCCCC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Submit for Approval",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
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
fun ProjectDetailsCard(project: Project) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = project.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Business,
                    contentDescription = null,
                    tint = Color(0xFF999999),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AVR Entertainment",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            }
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