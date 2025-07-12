package com.deeksha.avr.ui.view.auth

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.model.UserRole

@Composable
fun LoginScreen(
    onNavigateToOtp: (String) -> Unit,
    onSkipForDevelopment: () -> Unit,
    onNavigateToRole: (com.deeksha.avr.model.UserRole) -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val otpSent by authViewModel.otpSent.collectAsState()
    val isAccessRestricted by authViewModel.isAccessRestricted.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    
    // Development skip now uses phone number lookup
    
    // Navigate to OTP screen when OTP is sent
    LaunchedEffect(otpSent) {
        if (otpSent) {
            android.util.Log.d("LoginScreen", "🔍 OTP sent, navigating to verification screen")
            android.util.Log.d("LoginScreen", "🔍 AuthViewModel instance: ${authViewModel.hashCode()}")
            onNavigateToOtp("+91$phoneNumber")
        }
    }
    
    // Handle access restriction navigation
    LaunchedEffect(isAccessRestricted) {
        if (isAccessRestricted) {
            android.util.Log.d("LoginScreen", "🚫 Access restricted, navigating to access restricted screen")
            authViewModel.clearAccessRestriction() // Clear the state to prevent re-triggering
            // Handle access restriction in navigation
        }
    }
    
    // Development skip now uses direct navigation callback
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        
        // App Logo/Title
        Text(
            text = "AVR",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4285F4)
        )
        
        Text(
            text = "Expense Tracker",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(80.dp))
        
        // Login Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Enter Phone Number",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4285F4)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Phone Number Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "+91",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { 
                            if (it.length <= 10) {
                                phoneNumber = it
                                authViewModel.clearError() // Clear any previous errors
                            }
                        },
                        placeholder = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        enabled = !authState.isLoading,
                        isError = authState.error != null
                    )
                }
                
                // Error message
                if (authState.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Send OTP Button
        Button(
            onClick = {
                if (phoneNumber.length == 10) {
                    val fullPhoneNumber = "+91$phoneNumber"
                    authViewModel.sendOTP(fullPhoneNumber, context as android.app.Activity)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = phoneNumber.length == 10 && !authState.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (phoneNumber.length == 10 && !authState.isLoading) Color(0xFF4285F4) else Color.Gray
            ),
            shape = RoundedCornerShape(25.dp)
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White
                )
            } else {
                Text(
                    text = "Send OTP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer Note
        Text(
            text = "By continuing, you agree to receive SMS messages\nfrom our service for verification purposes",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Development info
        Text(
            text = "Add users in Firebase Console:\nFirestore Database > users collection",
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Development Skip Button (for testing purposes only)
        TextButton(
            onClick = {
                android.util.Log.d("LoginScreen", "🔄 Development skip button clicked")
                android.util.Log.d("LoginScreen", "📱 Phone number: $phoneNumber")
                
                if (phoneNumber.isNotEmpty()) {
                    authViewModel.skipOTPForDevelopment(phoneNumber) { role ->
                        android.util.Log.d("LoginScreen", "🎯 Navigating to role: $role")
                        onNavigateToRole(role)
                    }
                } else {
                    android.util.Log.w("LoginScreen", "⚠️ No phone number entered")
                    // You could show a toast or error message here
                }
            },
            modifier = Modifier.padding(8.dp),
            enabled = !authState.isLoading && phoneNumber.isNotEmpty()
        ) {
            if (authState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFF4285F4)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Logging in...",
                        fontSize = 12.sp,
                        color = Color(0xFF4285F4)
                    )
                }
            } else {
                Text(
                    text = if (phoneNumber.isEmpty()) "Enter phone number first" else "Development Login (Skip OTP)",
                    fontSize = 12.sp,
                    color = if (phoneNumber.isEmpty()) Color.Gray else Color(0xFF4285F4),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onNavigateToOtp = {},
        onSkipForDevelopment = {},
        authViewModel = hiltViewModel()
    )
} 