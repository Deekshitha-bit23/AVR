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
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
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
    
    // Development skip session tracking
    private val _isDevelopmentSkipUser = MutableStateFlow(false)
    val isDevelopmentSkipUser: StateFlow<Boolean> = _isDevelopmentSkipUser.asStateFlow()

    init {
        Log.d("AuthViewModel", "🚀 AuthViewModel initialized")
        initializeAuthState()
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
        _currentUser.value = user
        _isDevelopmentSkipUser.value = isDevelopmentSkip
        _hasCompletedVerification.value = true
        _authState.value = _authState.value.copy(
            isAuthenticated = true,
            user = user,
            isLoading = false,
            error = null
        )
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
    }
    
    // Send OTP to phone number
    fun sendOTP(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            _otpSent.value = false
            
            Log.d("AuthViewModel", "🔄 Sending OTP to: $phoneNumber")
            
            try {
                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(phoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(phoneAuthCallbacks)
                    .build()
                
                PhoneAuthProvider.verifyPhoneNumber(options)
                
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Error sending OTP: ${e.message}")
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Failed to send OTP: ${e.message}"
                )
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
                        .build()
                    
                    PhoneAuthProvider.verifyPhoneNumber(options)
                    
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "❌ Error resending OTP: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Failed to resend OTP: ${e.message}"
                    )
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
            initializeAuthState()
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
        return _authState.value.isAuthenticated && _authState.value.user != null
    }
    
    // Clear access restriction state
    fun clearAccessRestriction() {
        _isAccessRestricted.value = false
        _restrictedPhoneNumber.value = null
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
                
                // Small delay to ensure state is updated before navigation
                delay(100)
                
                // Trigger direct navigation
                onNavigationCallback(testUser.role)
                
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
