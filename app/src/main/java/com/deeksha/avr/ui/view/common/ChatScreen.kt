package com.deeksha.avr.ui.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.Message
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import coil.compose.rememberAsyncImagePainter
import androidx.core.app.NotificationCompat
import com.deeksha.avr.R
import com.deeksha.avr.MainActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.deeksha.avr.utils.ImageUriHelper
import com.deeksha.avr.utils.FileProviderTest

// Simple notification function
fun showMessageNotification(context: Context, senderName: String, message: String) {
    try {
        val channelId = "message_notifications"
        
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Message Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new chat messages"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New message from $senderName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        
        android.util.Log.d("ChatScreen", "Notification shown for message from $senderName")
        
    } catch (e: Exception) {
        android.util.Log.e("ChatScreen", "Error showing notification: ${e.message}", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    projectId: String,
    chatId: String,
    otherUserName: String,
    onNavigateBack: () -> Unit,
    chatViewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val chatState by chatViewModel.chatState.collectAsState()
    val currentUser = authState.user

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showImagePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    
    // Test FileProvider configuration on first load
    LaunchedEffect(Unit) {
        android.util.Log.d("ChatScreen", "=== FileProvider Configuration Test ===")
        val cacheResult = FileProviderTest.testFileProvider(context)
        val externalResult = FileProviderTest.testExternalFiles(context)
        
        android.util.Log.d("ChatScreen", "FileProvider Test Results:")
        android.util.Log.d("ChatScreen", cacheResult)
        android.util.Log.d("ChatScreen", externalResult)
        android.util.Log.d("ChatScreen", "=== End FileProvider Test ===")
    }
    
    // Debug selectedImageUri changes
    LaunchedEffect(selectedImageUri) {
        android.util.Log.d("ChatScreen", "selectedImageUri changed: $selectedImageUri")
    }
    
    // Debug cameraImageUri changes
    LaunchedEffect(cameraImageUri) {
        android.util.Log.d("ChatScreen", "cameraImageUri changed: $cameraImageUri")
    }

    // Image picker launcher for gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        android.util.Log.d("ChatScreen", "Image picker result: $uri")
        uri?.let { imageUri ->
            // Store the selected image URI for preview
            selectedImageUri = imageUri
            android.util.Log.d("ChatScreen", "Image URI stored: $imageUri")
            
            // Clear any previous errors
            uploadError = null
            
            // Test if the URI is accessible
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                if (inputStream != null) {
                    android.util.Log.d("ChatScreen", "Image URI is accessible")
                    inputStream.close()
                } else {
                    android.util.Log.e("ChatScreen", "Image URI is not accessible")
                    uploadError = "Selected image is not accessible"
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Error accessing selected image: ${e.message}", e)
                uploadError = "Error accessing selected image: ${e.message}"
            }
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        android.util.Log.d("ChatScreen", "Camera result: $success")
        android.util.Log.d("ChatScreen", "Camera URI: $cameraImageUri")
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
            android.util.Log.d("ChatScreen", "Camera image captured and set as selected: $cameraImageUri")
            
            // Verify the file exists and is accessible
            try {
                val file = File(cameraImageUri!!.path ?: "")
                android.util.Log.d("ChatScreen", "Camera file exists: ${file.exists()}")
                android.util.Log.d("ChatScreen", "Camera file size: ${file.length()} bytes")
                android.util.Log.d("ChatScreen", "Camera file can read: ${file.canRead()}")
                android.util.Log.d("ChatScreen", "Camera file absolute path: ${file.absolutePath}")
                
                // Test if the URI is accessible for reading (don't recreate FileProvider URI)
                if (ImageUriHelper.isUriAccessible(context, cameraImageUri!!)) {
                    android.util.Log.d("ChatScreen", "Camera image URI is accessible for use")
                    // Clear any previous errors since the camera worked
                    uploadError = null
                } else {
                    android.util.Log.e("ChatScreen", "Camera image URI is not accessible")
                    uploadError = "Camera image is not accessible"
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Error checking camera file: ${e.message}", e)
                uploadError = "Error accessing camera file: ${e.message}"
            }
        } else {
            android.util.Log.e("ChatScreen", "Camera capture failed or URI is null")
            uploadError = "Camera capture failed"
        }
    }
    
    // Function to launch camera with permission check
    fun launchCamera() {
        try {
            // Create a unique filename with timestamp (no spaces or special characters)
            val timestamp = System.currentTimeMillis()
            val fileName = "camera_image_${timestamp}.jpg"
            
            android.util.Log.d("ChatScreen", "Creating camera image URI for file: $fileName")
            
            // Use the helper to create the URI
            val photoUri = ImageUriHelper.createCameraImageUri(context, fileName)
            
            if (photoUri != null) {
                android.util.Log.d("ChatScreen", "Camera image URI created: $photoUri")
                android.util.Log.d("ChatScreen", "URI scheme: ${photoUri.scheme}")
                android.util.Log.d("ChatScreen", "URI path: ${photoUri.path}")
                
                // Test if the URI is accessible
                if (ImageUriHelper.isUriAccessible(context, photoUri)) {
                    android.util.Log.d("ChatScreen", "Camera image URI is accessible")
                    cameraImageUri = photoUri
                    cameraLauncher.launch(photoUri)
                } else {
                    android.util.Log.e("ChatScreen", "Camera image URI is not accessible")
                    uploadError = "Cannot access camera image file"
                }
            } else {
                android.util.Log.e("ChatScreen", "Failed to create camera image URI")
                uploadError = "Failed to create camera image file"
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ChatScreen", "Error creating camera file: ${e.message}", e)
            android.util.Log.e("ChatScreen", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("ChatScreen", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            uploadError = "Error setting up camera: ${e.message}"
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        val oldStorageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        
        android.util.Log.d("ChatScreen", "Camera permission: $cameraGranted, Storage permission: $storageGranted, Old storage: $oldStorageGranted")
        
        if (cameraGranted) {
            // Launch camera after permission granted
            launchCamera()
        } else if (storageGranted || oldStorageGranted) {
            // Launch gallery after permission granted
            android.util.Log.d("ChatScreen", "Launching gallery picker after permission granted")
            imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    LaunchedEffect(chatId) {
        android.util.Log.d("ChatScreen", "Loading messages for project: $projectId, chat: $chatId")
        chatViewModel.loadMessages(projectId, chatId)
        currentUser?.phone?.let { userPhone ->
            // Use phone number since that's how user IDs are stored in Firestore
            android.util.Log.d("ChatScreen", "Marking messages as read for user: $userPhone")
            chatViewModel.markMessagesAsRead(projectId, chatId, userPhone)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(chatState.messages.size) {
        android.util.Log.d("ChatScreen", "Messages count changed: ${chatState.messages.size}")
        if (chatState.messages.isNotEmpty()) {
            android.util.Log.d("ChatScreen", "Auto-scrolling to bottom")
            listState.animateScrollToItem(chatState.messages.size - 1)
        }
    }
    
    // Debug messages
    LaunchedEffect(chatState.messages) {
        android.util.Log.d("ChatScreen", "Messages updated: ${chatState.messages.map { "${it.senderName}: ${it.message}" }}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherUserName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        chatState.currentChatUser?.let { user ->
                            Text(
                                text = if (user.isOnline) "Online" else "Offline",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Voice call */ }) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Call",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4285F4)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFECE5DD))
                .padding(paddingValues)
        ) {
            // Messages List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatState.messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUser?.phone
                    )
                }
            }

            // Message Input
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Plus button
                    IconButton(
                        onClick = { showImagePicker = true }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add attachment",
                            tint = if (selectedImageUri != null) Color(0xFF4285F4) else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Image Preview Thumbnail
                    selectedImageUri?.let { uri ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray.copy(alpha = 0.1f))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Selected image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Upload progress indicator
                            if (isUploadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f))
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                            
                            // Clear image button (only show when not uploading)
                            if (!isUploadingImage) {
                                IconButton(
                                    onClick = { 
                                        selectedImageUri = null
                                        cameraImageUri = null
                                        uploadError = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.7f),
                                            CircleShape
                                        )
                                        .padding(2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove image",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Message Input Field
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (currentUser != null && !isUploadingImage) {
                                if (selectedImageUri != null) {
                                    // Send image message
                                    android.util.Log.d("ChatScreen", "Sending image message with URI: $selectedImageUri")
                                    isUploadingImage = true
                                    uploadError = null
                                    
                                    // Use coroutine scope to handle async operation
                                    coroutineScope.launch {
                                        try {
                                            android.util.Log.d("ChatScreen", "Starting image upload process...")
                                            android.util.Log.d("ChatScreen", "Image URI: $selectedImageUri")
                                            android.util.Log.d("ChatScreen", "Image URI scheme: ${selectedImageUri?.scheme}")
                                            android.util.Log.d("ChatScreen", "Image URI path: ${selectedImageUri?.path}")
                                            android.util.Log.d("ChatScreen", "Project ID: $projectId, Chat ID: $chatId")
                                            android.util.Log.d("ChatScreen", "Sender: ${currentUser.name} (${currentUser.phone})")
                                            
                                            // Test if the URI is accessible before sending
                                            if (!ImageUriHelper.isUriAccessible(context, selectedImageUri!!)) {
                                                android.util.Log.e("ChatScreen", "Image URI is not accessible for upload")
                                                uploadError = "Selected image is not accessible"
                                                return@launch
                                            } else {
                                                android.util.Log.d("ChatScreen", "Image URI is accessible for upload")
                                            }
                                            
                                            val success = chatViewModel.sendImageMessage(
                                                projectId = projectId,
                                                chatId = chatId,
                                                senderId = currentUser.phone,
                                                senderName = currentUser.name,
                                                senderRole = currentUser.role.name,
                                                imageUri = selectedImageUri!!
                                            )
                                            
                                            android.util.Log.d("ChatScreen", "Image upload result: $success")
                                            
                                            if (success) {
                                                // Clear the image after successful sending
                                                selectedImageUri = null
                                                cameraImageUri = null
                                                uploadError = null
                                                android.util.Log.d("ChatScreen", "Image message sent successfully")
                                            } else {
                                                uploadError = "Failed to send image message"
                                                android.util.Log.e("ChatScreen", "Failed to send image message")
                                            }
                                        } catch (e: Exception) {
                                            uploadError = "Error sending image: ${e.message}"
                                            android.util.Log.e("ChatScreen", "Error sending image message: ${e.message}", e)
                                            android.util.Log.e("ChatScreen", "Exception type: ${e.javaClass.simpleName}")
                                            android.util.Log.e("ChatScreen", "Stack trace: ${e.stackTrace.joinToString("\n")}")
                                        } finally {
                                            isUploadingImage = false
                                        }
                                    }
                                } else if (messageText.isNotBlank()) {
                                    // Send text message
                                    android.util.Log.d("ChatScreen", "Sending text message: $messageText")
                                    chatViewModel.sendMessage(
                                        projectId = projectId,
                                        chatId = chatId,
                                        senderId = currentUser.phone,
                                        senderName = currentUser.name,
                                        senderRole = currentUser.role.name,
                                        message = messageText
                                    )
                                    messageText = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUploadingImage) 
                                    Color.Gray
                                else if (messageText.isNotBlank() || selectedImageUri != null) 
                                    Color(0xFF4285F4) 
                                else 
                                    Color.LightGray
                            )
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                // Error message display
                uploadError?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error,
                                fontSize = 12.sp,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { uploadError = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Image Picker Bottom Sheet
    if (showImagePicker) {
        ModalBottomSheet(
            onDismissRequest = { showImagePicker = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Select Image",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Camera option
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            android.util.Log.d("ChatScreen", "Camera option clicked")
                            showImagePicker = false
                            
                            // Check camera permission
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                launchCamera()
                            } else {
                                // Request camera permission
                                permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp),
                            tint = Color(0xFF4285F4)
                        )
                        Text(
                            text = "Camera",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    // Gallery option
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            android.util.Log.d("ChatScreen", "Gallery option clicked")
                            showImagePicker = false
                            
                            // Check storage permission for older Android versions
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                // Android 13+ uses READ_MEDIA_IMAGES
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                                    android.util.Log.d("ChatScreen", "Launching gallery picker")
                                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
                                }
                            } else {
                                // Older Android versions use READ_EXTERNAL_STORAGE
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                    android.util.Log.d("ChatScreen", "Launching gallery picker")
                                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                                }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            modifier = Modifier
                                .size(48.dp)
                                .padding(8.dp),
                            tint = Color(0xFF4285F4)
                        )
                        Text(
                            text = "Gallery",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) Color(0xFFDCF8C6) else Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isCurrentUser) {
                    Text(
                        text = message.senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (message.messageType == "Media" && message.mediaUrl != null) {
                    // Display image with better sizing and loading state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 150.dp, max = 300.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray.copy(alpha = 0.1f))
                    ) {
                        val painter = rememberAsyncImagePainter(
                            model = message.mediaUrl,
                            onLoading = { state ->
                                // Loading state handled by AsyncImagePainter
                            },
                            onError = { state ->
                                // Error state handled by AsyncImagePainter
                            }
                        )
                        
                        Image(
                            painter = painter,
                            contentDescription = "Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Text(
                        text = message.message,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatMessageTime(message.timestamp?.toDate()),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.isRead) Icons.Default.Done else Icons.Default.Check,
                            contentDescription = if (message.isRead) "Read" else "Sent",
                            tint = if (message.isRead) Color(0xFF4285F4) else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(date: Date?): String {
    if (date == null) return ""
    
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = date }
    
    return when {
        now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) -> {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        }
        now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 -> {
            "Yesterday ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)}"
        }
        else -> {
            SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(date)
        }
    }
}
