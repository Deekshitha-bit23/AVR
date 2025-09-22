package com.deeksha.avr.ui.view.auth

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.utils.PermissionUtils
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    phoneNumber: String,
    onVerificationSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var otpCode by remember { mutableStateOf("") }
    val authState by authViewModel.authState.collectAsState()
    val verificationId by authViewModel.verificationId.collectAsState()
    val otpSent by authViewModel.otpSent.collectAsState()
    val detectedOTP by authViewModel.detectedOTP.collectAsState()
    val isOTPAutoFilled by authViewModel.isOTPAutoFilled.collectAsState()
    val context = LocalContext.current
    var isVerifying by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var resendCountdown by remember { mutableStateOf(60) }
    
    // SMS Permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            android.util.Log.d("OTPScreen", "✅ SMS permissions granted")
        } else {
            android.util.Log.d("OTPScreen", "❌ SMS permissions denied")
        }
    }
    
    // Debug logging and fallback mechanism
    LaunchedEffect(Unit) {
        android.util.Log.d("OTPScreen", "🔍 AuthViewModel instance: ${authViewModel.hashCode()}")
        android.util.Log.d("OTPScreen", "🔍 Initial verification ID: $verificationId")
        android.util.Log.d("OTPScreen", "🔍 Initial OTP sent status: $otpSent")
        
        // Request SMS permissions for OTP auto-fill
        if (!PermissionUtils.hasSMSPermissions(context)) {
            android.util.Log.d("OTPScreen", "🔐 Requesting SMS permissions for OTP auto-fill")
            smsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
        }
        
        // Initialize OTP detection monitoring
        authViewModel.initializeOTPDetection(context)
        
        // Fallback: If no verification ID is available, try to resend OTP
        if (verificationId == null && !otpSent) {
            android.util.Log.d("OTPScreen", "🔄 No verification ID found, triggering OTP resend")
            authViewModel.sendOTP(phoneNumber, context as Activity)
        }
    }
    
    // Auto-fill OTP when detected
    LaunchedEffect(detectedOTP, isOTPAutoFilled) {
        if (detectedOTP != null && isOTPAutoFilled && otpCode.isEmpty()) {
            android.util.Log.d("OTPScreen", "🔑 Auto-filling OTP: $detectedOTP")
            otpCode = detectedOTP!!
            showError = false
            authViewModel.clearError()
        }
    }
    
    // Navigate when authentication is complete AND user data is loaded
    LaunchedEffect(authState.isAuthenticated, authState.user) {
        if (authState.isAuthenticated && authState.user != null) {
            android.util.Log.d("OTPScreen", "✅ Authentication complete, user loaded: ${authState.user?.name} with role: ${authState.user?.role}")
            isVerifying = false
            onVerificationSuccess()
        }
    }
    
    // Handle authentication errors
    LaunchedEffect(authState.error) {
        if (authState.error != null) {
            isVerifying = false
            showError = true
            errorMessage = authState.error ?: "Authentication failed"
        }
    }
    
    // Resend countdown timer
    LaunchedEffect(otpSent) {
        if (otpSent) {
            resendCountdown = 60
            while (resendCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                resendCountdown--
            }
        }
    }
    
    // Clear phone auth state when navigating back
    DisposableEffect(Unit) {
        onDispose {
            authViewModel.clearPhoneAuthState()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Verify OTP") },
            navigationIcon = {
                IconButton(onClick = {
                    authViewModel.clearPhoneAuthState()
                    onNavigateBack()
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            Text(
                text = "Enter OTP",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "We've sent a verification code to\n$phoneNumber",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // OTP Input
            OutlinedTextField(
                value = otpCode,
                onValueChange = { 
                    if (it.length <= 6) {
                        otpCode = it
                        showError = false // Clear error when user types
                        authViewModel.clearError()
                    }
                },
                placeholder = { Text("Enter 6-digit OTP") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                isError = showError || authState.error != null,
                enabled = !isVerifying && !authState.isLoading,
                trailingIcon = {
                    if (isOTPAutoFilled && detectedOTP != null) {
                        Text(
                            text = "✓",
                            color = Color(0xFF4CAF50),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
            
            // Auto-fill indicator
            if (isOTPAutoFilled && detectedOTP != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "✓ OTP auto-filled from SMS",
                        color = Color(0xFF2E7D32),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // Detection status indicator
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        PermissionUtils.hasSMSPermissions(context) -> Color(0xFFE8F5E8)
                        else -> Color(0xFFFFF3E0)
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when {
                        PermissionUtils.hasSMSPermissions(context) -> 
                            "✅ SMS permissions granted - Auto-fill enabled"
                        else -> 
                            "⚠️ SMS permissions not granted - Using Google SMS Retriever or manual entry"
                    },
                    color = when {
                        PermissionUtils.hasSMSPermissions(context) -> Color(0xFF2E7D32)
                        else -> Color(0xFFE65100)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Verify Button
            Button(
                onClick = {
                    if (otpCode.length == 6) {
                        isVerifying = true
                        showError = false
                        android.util.Log.d("OTPScreen", "🔄 Verifying OTP: $otpCode")
                        
                        // Use the real Firebase OTP verification
                        authViewModel.verifyOTP(otpCode)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = otpCode.length == 6 && !isVerifying && !authState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4)
                ),
                shape = RoundedCornerShape(25.dp)
            ) {
                if (isVerifying || authState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "Verify OTP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Resend OTP Button
            TextButton(
                onClick = {
                    if (resendCountdown == 0) {
                        android.util.Log.d("OTPScreen", "🔄 Resending OTP to: $phoneNumber")
                        authViewModel.resendOTP(phoneNumber, context as Activity)
                    }
                },
                enabled = resendCountdown == 0 && !authState.isLoading
            ) {
                Text(
                    text = if (resendCountdown > 0) 
                        "Resend OTP in ${resendCountdown}s" 
                    else 
                        "Resend OTP",
                    color = if (resendCountdown > 0) Color.Gray else Color(0xFF4285F4)
                )
            }
            
            // Error message
            if (showError || authState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMessage.ifEmpty { authState.error ?: "An error occurred" },
                        color = Color(0xFFD32F2F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status indicator
            if (verificationId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✅ OTP Sent Successfully",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Check your SMS for the verification code",
                            color = Color(0xFF2E7D32),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Footer with enhanced instructions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Didn't receive the code? Check your network connection\nor try resending after ${resendCountdown}s",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when {
                        PermissionUtils.hasSMSPermissions(context) -> 
                            "💡 OTP will be auto-filled when SMS arrives"
                        else -> 
                            "💡 Copy OTP from SMS and paste it manually"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
} 