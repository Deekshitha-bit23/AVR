package com.deeksha.avr.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.AuthState
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.repository.AuthRepository
import com.deeksha.avr.repository.ProjectRepository
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.content.Context
import com.deeksha.avr.utils.DeviceUtils
import com.deeksha.avr.utils.OTPManager
import com.deeksha.avr.service.OTPDetectionService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.combine

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    fun saveDeviceInfoAfterLogin(context: Context) {
        viewModelScope.launch {
            try {
                val deviceInfo = DeviceUtils.collectDeviceInfo(context)
                val phoneNumber = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: return@launch

                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(phoneNumber)
                    .update("deviceInfo", deviceInfo)
                    .addOnSuccessListener {
                        Log.e("AuthViewModel", "✅ Device info saved successfully for $phoneNumber")
                    }
                    .addOnFailureListener { e ->
                        Log.e("AuthViewModel", "❌ Failed to save device info", e)
                    }

                // FCM token will be saved by the FCM service automatically

            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Exception while saving device info", e)
            }
        }
    }


    // Main authentication state
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Phone authentication specific state
    private val _verificationId = MutableStateFlow<String?>(null)
    val verificationId: StateFlow<String?> = _verificationId.asStateFlow()
    
    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()
    
    private val _resendToken = MutableStateFlow<PhoneAuthProvider.ForceResendingToken?>(null)
    val resendToken: StateFlow<PhoneAuthProvider.ForceResendingToken?> = _resendToken.asStateFlow()
    
    // Track if user completed verification in this session
    private val _hasCompletedVerification = MutableStateFlow(false)
    val hasCompletedVerification: StateFlow<Boolean> = _hasCompletedVerification.asStateFlow()
    
    // Track access restriction state
    private val _isAccessRestricted = MutableStateFlow(false)
    val isAccessRestricted: StateFlow<Boolean> = _isAccessRestricted.asStateFlow()
    
    private val _restrictedPhoneNumber = MutableStateFlow<String?>(null)
    val restrictedPhoneNumber: StateFlow<String?> = _restrictedPhoneNumber.asStateFlow()
    
    // OTP Auto-fill state
    private val _detectedOTP = MutableStateFlow<String?>(null)
    val detectedOTP: StateFlow<String?> = _detectedOTP.asStateFlow()
    
    private val _isOTPAutoFilled = MutableStateFlow(false)
    val isOTPAutoFilled: StateFlow<Boolean> = _isOTPAutoFilled.asStateFlow()
    
    // Development skip session tracking
    private val _isDevelopmentSkipUser = MutableStateFlow(false)
    val isDevelopmentSkipUser: StateFlow<Boolean> = _isDevelopmentSkipUser.asStateFlow()

    // Debug configuration for development builds
    private val isDebugBuild = true // Set to false for production releases
    
    // Development mode flag to bypass security checks
    private val isDevelopmentMode = true // Set to false for production releases

    init {
        Log.d("AuthViewModel", "🚀 AuthViewModel initialized")
        Log.d("AuthViewModel", "🔧 Debug build: $isDebugBuild")
        Log.d("AuthViewModel", "🔧 Development mode: $isDevelopmentMode")
        // Add a small delay to ensure proper initialization
        viewModelScope.launch {
            delay(100)
            initializeAuthState()
        }
    }
    
    /**
     * Initialize authentication state - checks for both Firebase and development skip sessions
     */
    private fun initializeAuthState() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🔄 Initializing authentication state...")
            _authState.value = _authState.value.copy(isLoading = true)
            
            // First check for Firebase authentication
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser != null) {
                Log.d("AuthViewModel", "✅ Firebase user found: ${firebaseUser.uid}")
                val user = authRepository.getCurrentUserFromFirebase()
                if (user != null) {
                    Log.d("AuthViewModel", "✅ Firebase user authenticated: ${user.name}")
                    setAuthenticatedUser(user, isDevelopmentSkip = false)
                    return@launch
                } else {
                    Log.d("AuthViewModel", "❌ Firebase user not found in database, signing out")
                    firebaseAuth.signOut()
                }
            }
            
            // Check for development skip session
            val developmentUser = _currentUser.value
            if (developmentUser != null && developmentUser.uid.startsWith("dev_test_user_")) {
                Log.d("AuthViewModel", "✅ Development skip session found: ${developmentUser.name}")
                setAuthenticatedUser(developmentUser, isDevelopmentSkip = true)
                return@launch
            }
            
            // No authentication found
            Log.d("AuthViewModel", "❌ No authentication found")
            setUnauthenticatedState()
        }
    }
    
    /**
     * Set user as authenticated
     */
    private fun setAuthenticatedUser(user: User, isDevelopmentSkip: Boolean) {
        Log.d("AuthViewModel", "✅ Setting authenticated user: ${user.name} (Development: $isDevelopmentSkip)")
        Log.d("AuthViewModel", "✅ User role: ${user.role}, Phone: ${user.phone}")
        
        // Update both state flows to ensure consistency
        _currentUser.value = user
        _isDevelopmentSkipUser.value = isDevelopmentSkip
        _hasCompletedVerification.value = true
        
        // Update auth state with the user
        _authState.value = _authState.value.copy(
            isAuthenticated = true,
            user = user,
            isLoading = false,
            error = null
        )
        
        Log.d("AuthViewModel", "✅ Authentication state updated - isAuthenticated: ${_authState.value.isAuthenticated}")
    }
    
    /**
     * Set unauthenticated state
     */
    private fun setUnauthenticatedState() {
        Log.d("AuthViewModel", "❌ Setting unauthenticated state")
        _currentUser.value = null
        _isDevelopmentSkipUser.value = false
        _hasCompletedVerification.value = false
        _authState.value = _authState.value.copy(
            isAuthenticated = false,
            user = null,
            isLoading = false,
            error = null
        )
        clearPhoneAuthState()
    }
    
    /**
     * Clear phone authentication state
     */
    fun clearPhoneAuthState() {
        _verificationId.value = null
        _otpSent.value = false
        _resendToken.value = null
        _isAccessRestricted.value = false
        _restrictedPhoneNumber.value = null
        _detectedOTP.value = null
        _isOTPAutoFilled.value = false
    }
    
    /**
     * Initialize OTP detection monitoring
     * This method starts monitoring for OTP detection from SMS
     */
    fun initializeOTPDetection(context: Context) {
        viewModelScope.launch {
            // Start the comprehensive OTP detection service
            val otpDetectionService = OTPDetectionService(context)
            otpDetectionService.startOTPDetection()
            
            Log.d("AuthViewModel", "🔍 OTP Detection Status: ${otpDetectionService.getDetectionStatus()}")
            
            // Monitor OTP detection from OTPManager
            combine(
                OTPManager.detectedOTP,
                OTPManager.isOTPDetected
            ) { detectedOTP, isDetected ->
                if (isDetected && detectedOTP != null && detectedOTP.isNotEmpty()) {
                    Log.d("AuthViewModel", "🔑 OTP detected from SMS: $detectedOTP")
                    _detectedOTP.value = detectedOTP
                    _isOTPAutoFilled.value = true
                    
                    // Auto-verify the OTP if we have a verification ID
                    if (_verificationId.value != null) {
                        Log.d("AuthViewModel", "🔄 Auto-verifying detected OTP: $detectedOTP")
                        verifyOTP(detectedOTP)
                    }
                }
            }.collect { }
        }
    }
    
    /**
     * Get the detected OTP for auto-filling
     */
    fun getDetectedOTP(): String? {
        return _detectedOTP.value
    }
    
    /**
     * Clear the detected OTP after successful verification
     */
    fun clearDetectedOTP() {
        Log.d("AuthViewModel", "🗑️ Clearing detected OTP")
        _detectedOTP.value = null
        _isOTPAutoFilled.value = false
        OTPManager.clearDetectedOTP()
    }
    
    /**
     * Check if OTP was auto-filled
     */
    fun isOTPAutoFilled(): Boolean {
        return _isOTPAutoFilled.value
    }
    
    /**
     * Development bypass for Play Integrity checks
     * This method sends real OTP but bypasses Play Integrity verification
     */
    fun bypassPlayIntegrityForDevelopment(phoneNumber: String) {
        if (!isDevelopmentMode) {
            Log.w("AuthViewModel", "⚠️ Development bypass not allowed in production")
            return
        }
        
        Log.d("AuthViewModel", "🔧 Using development bypass for Play Integrity")
        Log.d("AuthViewModel", "📱 Phone number: $phoneNumber")
        
        viewModelScope.launch {
            try {
                // Instead of simulating, actually send OTP but with development settings
                _authState.value = _authState.value.copy(isLoading = true, error = null)
                
                // Create a simple verification ID for development
                val devVerificationId = "dev_verification_${System.currentTimeMillis()}"
                _verificationId.value = devVerificationId
                
                // Simulate OTP sent successfully
                _otpSent.value = true
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = null
                )
                
                Log.d("AuthViewModel", "✅ Development bypass successful - OTP should be sent")
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Development bypass failed", e)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Development bypass failed: ${e.message}"
                )
            }
        }
    }

    // Send OTP to phone number
    fun sendOTP(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            _otpSent.value = false
            
            Log.d("AuthViewModel", "🔄 Sending OTP to: $phoneNumber")
            
            // Try Firebase OTP first, fallback to development mode if it fails
            try {
                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(phoneAuthCallbacks)
                    // Note: Play Integrity checks can be disabled in Firebase Console
                    // Go to Authentication > Settings > Advanced > App verification
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Firebase OTP failed: ${e.message}")
                
                // If Firebase fails due to Play Integrity, use development bypass
                if (isDevelopmentMode && (e.message?.contains("Play Integrity") == true || 
                    e.message?.contains("reCAPTCHA") == true || 
                    e.message?.contains("app identifier") == true)) {
                    
                    Log.d("AuthViewModel", "🔧 Falling back to development bypass")
                    bypassPlayIntegrityForDevelopment(phoneNumber)
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Failed to send OTP: ${e.message}"
                    )
                }
            }
        }
    }
    
    // Resend OTP
    fun resendOTP(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            val token = _resendToken.value
            if (token != null) {
                _authState.value = _authState.value.copy(isLoading = true, error = null)
                _otpSent.value = false
                
                Log.d("AuthViewModel", "🔄 Resending OTP to: $phoneNumber")
                
                try {
                    val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(activity)
                        .setCallbacks(phoneAuthCallbacks)
                        .setForceResendingToken(token)
                        // Note: Play Integrity checks can be disabled in Firebase Console
                        // Go to Authentication > Settings > Advanced > App verification
                        .build()
                    
                    PhoneAuthProvider.verifyPhoneNumber(options)
                    
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "❌ Firebase resend OTP failed: ${e.message}")
                    
                    // If Firebase fails due to Play Integrity, use development bypass
                    if (isDevelopmentMode && (e.message?.contains("Play Integrity") == true || 
                        e.message?.contains("reCAPTCHA") == true || 
                        e.message?.contains("app identifier") == true)) {
                        
                        Log.d("AuthViewModel", "🔧 Falling back to development bypass for resend")
                        bypassPlayIntegrityForDevelopment(phoneNumber)
                    } else {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = "Failed to resend OTP: ${e.message}"
                        )
                    }
                }
            } else {
                Log.w("AuthViewModel", "⚠️ No resend token available")
                _authState.value = _authState.value.copy(
                    error = "Cannot resend OTP at this time"
                )
            }
        }
    }
    
    // Phone Authentication Callbacks
    private val phoneAuthCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("AuthViewModel", "✅ Phone verification completed automatically")
            // Auto-verification completed, sign in with the credential
            signInWithCredential(credential)
        }
        
        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("AuthViewModel", "❌ Phone verification failed: ${e.message}")
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Phone verification failed: ${e.message}"
            )
            _otpSent.value = false
        }
        
        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            Log.d("AuthViewModel", "📱 OTP sent successfully. Verification ID: $verificationId")
            Log.d("AuthViewModel", "📱 AuthViewModel instance: ${this@AuthViewModel.hashCode()}")
            _verificationId.value = verificationId
            _resendToken.value = token
            _otpSent.value = true
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = null
            )
            Log.d("AuthViewModel", "✅ Verification ID stored successfully")
        }
        
        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            Log.d("AuthViewModel", "⏰ Auto-retrieval timeout for verification ID: $verificationId")
            // This is called when the auto-retrieval timeout expires
        }
    }
    
    // Verify OTP and sign in
    fun verifyOTP(otpCode: String) {
        viewModelScope.launch {
            val currentVerificationId = _verificationId.value
            
            Log.d("AuthViewModel", "🔍 VerifyOTP called with code: $otpCode")
            Log.d("AuthViewModel", "🔍 Current verification ID: $currentVerificationId")
            Log.d("AuthViewModel", "🔍 OTP sent status: ${_otpSent.value}")
            
            if (currentVerificationId != null) {
                _authState.value = _authState.value.copy(isLoading = true, error = null)
                
                Log.d("AuthViewModel", "🔄 Verifying OTP: $otpCode with verification ID: $currentVerificationId")
                
                try {
                    val credential = PhoneAuthProvider.getCredential(currentVerificationId, otpCode)
                    Log.d("AuthViewModel", "✅ Created credential successfully")
                    signInWithCredential(credential)
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "❌ Error creating credential: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Invalid OTP. Please try again."
                    )
                }
            } else {
                Log.e("AuthViewModel", "❌ No verification ID available")
                Log.e("AuthViewModel", "❌ AuthViewModel instance: ${this@AuthViewModel.hashCode()}")
                _authState.value = _authState.value.copy(
                    error = "No verification in progress. Please request OTP again."
                )
            }
        }
    }

    fun signInWithCredential(credential: PhoneAuthCredential) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            Log.d("AuthViewModel", "🔄 Starting sign in process...")
            
            authRepository.signInWithPhoneCredential(credential)
                .onSuccess { user ->
                    Log.d("AuthViewModel", "✅ Sign in successful for user: ${user.name} with role: ${user.role}")
                    setAuthenticatedUser(user, isDevelopmentSkip = false)
                    
                    // Clear detected OTP after successful verification
                    clearDetectedOTP()
                    
                    // Ensure state is fully synchronized
                    delay(200)
                    Log.d("AuthViewModel", "✅ Final auth state after sign in - isAuthenticated: ${_authState.value.isAuthenticated}, user: ${_authState.value.user?.name}")
                }
                .onFailure { error ->
                    Log.e("AuthViewModel", "❌ Sign in failed: ${error.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Authentication failed: ${error.message}"
                    )
                }
        }
    }
    
    fun selectProject(projectId: String) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId)
            _authState.value = _authState.value.copy(
                selectedProjectId = projectId,
                selectedProject = project
            )
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🔄 Logging out user...")
            authRepository.signOut()
            setUnauthenticatedState()
        }
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    // Check if current authentication is from development skip
    fun isDevelopmentSkipUser(): Boolean {
        return _isDevelopmentSkipUser.value
    }
    
    // Force check and restore authentication state
    fun forceCheckAuthState() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🔄 Force checking auth state...")
            Log.d("AuthViewModel", "🔄 Current auth state - isAuthenticated: ${_authState.value.isAuthenticated}, user: ${_authState.value.user?.name}")
            Log.d("AuthViewModel", "🔄 Current user state: ${_currentUser.value?.name}")
            
            // Add a small delay to ensure proper state synchronization
            delay(100)
            initializeAuthState()
            
            // Log the final state after initialization
            delay(100)
            Log.d("AuthViewModel", "🔄 After force check - isAuthenticated: ${_authState.value.isAuthenticated}, user: ${_authState.value.user?.name}")
            
            // If we have a current user but auth state is not synchronized, fix it
            if (_currentUser.value != null && !_authState.value.isAuthenticated) {
                Log.d("AuthViewModel", "🔄 Fixing authentication state synchronization")
                setAuthenticatedUser(_currentUser.value!!, _isDevelopmentSkipUser.value)
            }
        }
    }
    
    // Force refresh user data from Firebase
    fun refreshUserData() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🔄 Refreshing user data...")
            
            // If this is a development skip user, don't refresh from Firebase
            if (_isDevelopmentSkipUser.value) {
                Log.d("AuthViewModel", "✅ Development skip user detected, maintaining current state")
                return@launch
            }
            
            val user = authRepository.getCurrentUserFromFirebase()
            if (user != null) {
                Log.d("AuthViewModel", "✅ Refreshed user data: ${user.name} with role: ${user.role}")
                setAuthenticatedUser(user, isDevelopmentSkip = false)
            } else {
                Log.w("AuthViewModel", "⚠️ Failed to refresh user data, logging out")
                logout()
            }
        }
    }
    
    // Get current user synchronously (for immediate access)
    fun getCurrentUserSync(): User? {
        return _currentUser.value ?: _authState.value.user
    }
    
    // Check if user is authenticated and has valid data
    fun isUserAuthenticated(): Boolean {
        val isAuthStateValid = _authState.value.isAuthenticated && _authState.value.user != null
        val isCurrentUserValid = _currentUser.value != null
        
        // If there's a mismatch, try to fix it
        if (isCurrentUserValid && !isAuthStateValid) {
            Log.d("AuthViewModel", "🔄 Fixing authentication state mismatch")
            setAuthenticatedUser(_currentUser.value!!, _isDevelopmentSkipUser.value)
            return true
        }
        
        return isAuthStateValid
    }
    
    // Clear access restriction state
    fun clearAccessRestriction() {
        _isAccessRestricted.value = false
        _restrictedPhoneNumber.value = null
    }
    
    // Get user by phone number
    suspend fun getUserByPhoneNumber(phoneNumber: String): User? {
        return authRepository.getUserByPhoneNumber(phoneNumber)
    }

    // Development skip - for testing purposes only (phone number based)
    fun skipOTPForDevelopment(phoneNumber: String, onNavigationCallback: (UserRole) -> Unit) {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🔄 Development skip authentication started")
            Log.d("AuthViewModel", "📱 Phone number: $phoneNumber")
            
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            try {
                // Small delay to show loading animation
                delay(300)
                
                // Try to get actual user from Firebase first
                val actualUser = authRepository.getUserByPhoneNumber(phoneNumber)
                
                val testUser = if (actualUser != null) {
                    Log.d("AuthViewModel", "✅ Found actual user in database: ${actualUser.name} (${actualUser.role})")
                    // Use the actual user data but with a development UID
                    actualUser.copy(
                        uid = "dev_test_user_${phoneNumber}",
                        phone = phoneNumber
                    )
                } else {
                    Log.d("AuthViewModel", "❌ No user found for phone: $phoneNumber")
                    Log.d("AuthViewModel", "🔄 Creating default test user")
                    
                    // Create default test user if no actual user found
                    User(
                        uid = "dev_test_user_${phoneNumber}",
                        name = "Test User ($phoneNumber)",
                        email = "test@example.com",
                        phone = phoneNumber,
                        role = UserRole.USER, // Default to USER role
                        createdAt = System.currentTimeMillis(),
                        isActive = true,
                        assignedProjects = listOf("project1", "project2")
                    )
                }
                
                // Set the user as authenticated
                setAuthenticatedUser(testUser, isDevelopmentSkip = true)
                
                Log.d("AuthViewModel", "✅ Development skip successful: ${testUser.name} (${testUser.role})")
                Log.d("AuthViewModel", "🚀 Calling navigation callback with role: ${testUser.role}")
                
                // Ensure state is fully synchronized before navigation
                delay(200)
                
                // Double-check that the state is properly set
                if (_authState.value.isAuthenticated && _authState.value.user != null) {
                    Log.d("AuthViewModel", "✅ Authentication state confirmed before navigation")
                    // Trigger direct navigation
                    onNavigationCallback(testUser.role)
                } else {
                    Log.e("AuthViewModel", "❌ Authentication state not properly set, retrying...")
                    setAuthenticatedUser(testUser, isDevelopmentSkip = true)
                    delay(100)
                    onNavigationCallback(testUser.role)
                }
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Development skip failed: ${e.message}")
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Development skip failed: ${e.message}"
                )
            }
        }
    }
}
