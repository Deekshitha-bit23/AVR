package com.deeksha.avr.repository

import android.util.Log
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.model.DeviceInfo
import com.deeksha.avr.model.NotificationPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<User> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user
            
            if (firebaseUser != null) {
                val phoneNumber = firebaseUser.phoneNumber?.replace("+91", "") ?: ""
                Log.d("AuthRepository", "🔍 Looking up user with phone: $phoneNumber")
                
                // First try to get user by phone number from Firebase
                val user = getUserByPhoneNumber(phoneNumber)
                if (user != null) {
                    Log.d("AuthRepository", "✅ Found existing user: ${user.name} with role: ${user.role}")
                    // Update the user with Firebase UID for future reference
                    val updatedUser = user.copy(uid = firebaseUser.uid)
                    updateUserUid(phoneNumber, firebaseUser.uid)
                    Result.success(updatedUser)
                } else {
                    Log.d("AuthRepository", "❌ User not found in database for phone: $phoneNumber")
                    Log.d("AuthRepository", "🚫 Access denied - user not authorized")
                    // Sign out the Firebase user since they're not authorized
                    auth.signOut()
                    Result.failure(Exception("User not authorized. Please contact administrator for access."))
                }
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Sign in error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getUserByPhoneNumber(phoneNumber: String): User? {
        return try {
            Log.d("AuthRepository", "🔍 Querying Firestore for phone: '$phoneNumber'")
            Log.d("AuthRepository", "🔍 Phone number length: ${phoneNumber.length}")
            
            // Query users collection by phone number
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()
            
            Log.d("AuthRepository", "📊 Query completed. Found ${querySnapshot.documents.size} documents")
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                Log.d("AuthRepository", "📄 Document ID: ${document.id}")
                Log.d("AuthRepository", "📄 Raw document data: ${document.data}")
                
                val userData = document.data ?: return null
                
                // Log each field individually for debugging
                Log.d("AuthRepository", "🔍 Parsing user data:")
                Log.d("AuthRepository", "   - uid: ${userData["uid"]}")
                Log.d("AuthRepository", "   - name: ${userData["name"]}")
                Log.d("AuthRepository", "   - email: ${userData["email"]}")
                Log.d("AuthRepository", "   - phoneNumber: ${userData["phoneNumber"]}")
                Log.d("AuthRepository", "   - role: ${userData["role"]} (type: ${userData["role"]?.javaClass?.simpleName})")
                Log.d("AuthRepository", "   - isActive: ${userData["isActive"]}")
                Log.d("AuthRepository", "   - assignedProjects: ${userData["assignedProjects"]}")
                
                // Manually parse the user data to handle role conversion
                val roleString = userData["role"] as? String
                Log.d("AuthRepository", "🔍 Raw role from database: '$roleString'")
                Log.d("AuthRepository", "🔍 Processed role: '${roleString?.uppercase()?.replace(" ", "_")}'")
                val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                    "APPROVER" -> {
                        Log.d("AuthRepository", "✅ Detected APPROVER role")
                        UserRole.APPROVER
                    }
                    "ADMIN" -> {
                        Log.d("AuthRepository", "✅ Detected ADMIN role")
                        UserRole.ADMIN
                    }
                    "USER" -> {
                        Log.d("AuthRepository", "✅ Detected USER role")
                        UserRole.USER
                    }
                    "PRODUCTION_HEAD" -> {
                        Log.d("AuthRepository", "✅ Detected PRODUCTION_HEAD role")
                        UserRole.PRODUCTION_HEAD
                    }
                    else -> {
                        Log.w("AuthRepository", "⚠️ Unknown role: '$roleString', defaulting to USER")
                        UserRole.USER
                    }
                }
                
                // Parse device info
                val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                val deviceInfo = DeviceInfo(
                    fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                    deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                    deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                    osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                    appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                    lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                    isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                )
                
                // Parse notification preferences
                val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                val notificationPreferences = NotificationPreferences(
                    pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                    expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                    expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                    expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                    projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                    pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                )
                
                val user = User(
                    uid = userData["uid"] as? String ?: "",
                    name = userData["name"] as? String ?: "",
                    email = userData["email"] as? String ?: "",
                    phone = userData["phoneNumber"] as? String ?: phoneNumber,
                    role = userRole,
                    createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                    isActive = userData["isActive"] as? Boolean ?: true,
                    assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    deviceInfo = deviceInfo,
                    notificationPreferences = notificationPreferences
                )
                
                Log.d("AuthRepository", "✅ Successfully parsed user:")
                Log.d("AuthRepository", "   - Final user: $user")
                return user
            } else {
                Log.d("AuthRepository", "❌ No documents found for phone: $phoneNumber")
                
                // Also try searching without country code for debugging
                val phoneWithoutPrefix = phoneNumber.replace("+91", "")
                if (phoneWithoutPrefix != phoneNumber) {
                    Log.d("AuthRepository", "🔄 Trying again without +91 prefix: $phoneWithoutPrefix")
                    return getUserByPhoneNumber(phoneWithoutPrefix)
                }
                
                // Also try searching with +91 prefix for debugging
                val phoneWithPrefix = "+91$phoneNumber"
                Log.d("AuthRepository", "🔄 Trying with +91 prefix: $phoneWithPrefix")
                val prefixQuerySnapshot = firestore.collection("users")
                    .whereEqualTo("phoneNumber", phoneWithPrefix)
                    .get()
                    .await()
                
                if (!prefixQuerySnapshot.isEmpty) {
                    Log.d("AuthRepository", "✅ Found user with +91 prefix!")
                    val document = prefixQuerySnapshot.documents.first()
                    Log.d("AuthRepository", "📄 Document data: ${document.data}")
                    
                    // Process this document the same way as above
                    val userData = document.data ?: return null
                    
                    // Log each field individually for debugging
                    Log.d("AuthRepository", "🔍 Parsing user data (fallback):")
                    Log.d("AuthRepository", "   - uid: ${userData["uid"]}")
                    Log.d("AuthRepository", "   - name: ${userData["name"]}")
                    Log.d("AuthRepository", "   - email: ${userData["email"]}")
                    Log.d("AuthRepository", "   - phoneNumber: ${userData["phoneNumber"]}")
                    Log.d("AuthRepository", "   - role: ${userData["role"]} (type: ${userData["role"]?.javaClass?.simpleName})")
                    Log.d("AuthRepository", "   - isActive: ${userData["isActive"]}")
                    Log.d("AuthRepository", "   - assignedProjects: ${userData["assignedProjects"]}")
                    
                    // Manually parse the user data to handle role conversion
                    val roleString = userData["role"] as? String
                    Log.d("AuthRepository", "🔍 Raw role from database (fallback): '$roleString'")
                    Log.d("AuthRepository", "🔍 Processed role (fallback): '${roleString?.uppercase()?.replace(" ", "_")}'")
                    val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                        "APPROVER" -> {
                            Log.d("AuthRepository", "✅ Detected APPROVER role (fallback)")
                            UserRole.APPROVER
                        }
                        "ADMIN" -> {
                            Log.d("AuthRepository", "✅ Detected ADMIN role (fallback)")
                            UserRole.ADMIN
                        }
                        "USER" -> {
                            Log.d("AuthRepository", "✅ Detected USER role (fallback)")
                            UserRole.USER
                        }
                        "PRODUCTION_HEAD" -> {
                            Log.d("AuthRepository", "✅ Detected PRODUCTION_HEAD role (fallback)")
                            UserRole.PRODUCTION_HEAD
                        }
                        else -> {
                            Log.w("AuthRepository", "⚠️ Unknown role (fallback): '$roleString', defaulting to USER")
                            UserRole.USER
                        }
                    }
                    
                    // Parse device info
                    val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                    val deviceInfo = DeviceInfo(
                        fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                        deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                        deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                        osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                        appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                        lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                        isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                    )
                    
                    // Parse notification preferences
                    val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                    val notificationPreferences = NotificationPreferences(
                        pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                        expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                        expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                        expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                        projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                        pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                    )
                    
                    val user = User(
                        uid = userData["uid"] as? String ?: "",
                        name = userData["name"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        phone = userData["phoneNumber"] as? String ?: phoneWithPrefix,
                        role = userRole,
                        createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                        isActive = userData["isActive"] as? Boolean ?: true,
                        assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        deviceInfo = deviceInfo,
                        notificationPreferences = notificationPreferences
                    )
                    
                    Log.d("AuthRepository", "✅ Successfully parsed user (fallback):")
                    Log.d("AuthRepository", "   - Final user: $user")
                    return user
                }
                
                return null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error querying user by phone: $phoneNumber", e)
            Log.e("AuthRepository", "Exception details: ${e.message}")
            null
        }
    }
    
    private suspend fun updateUserUid(phoneNumber: String, uid: String) {
        try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                firestore.collection("users")
                    .document(document.id)
                    .update("uid", uid)
                    .await()
                Log.d("AuthRepository", "✅ Updated user UID")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating user UID: ${e.message}")
        }
    }
    
    private suspend fun getUserFromFirestore(uid: String): User? {
        return try {
            val document = firestore.collection("users").document(uid).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun createUserByPhone(phoneNumber: String, user: User): Result<Unit> {
        return try {
            firestore.collection("users").add(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun createUser(user: User): Result<Unit> {
        return try {
            firestore.collection("users").document(user.uid).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUserFromFirebase(): User? {
        return try {
            val firebaseUser = auth.currentUser
            Log.d("AuthRepository", "🔍 getCurrentUserFromFirebase called")
            Log.d("AuthRepository", "🔍 Firebase user: ${firebaseUser?.uid}")
            Log.d("AuthRepository", "🔍 Firebase user phone: ${firebaseUser?.phoneNumber}")
            
            if (firebaseUser != null) {
                val phoneNumber = firebaseUser.phoneNumber?.replace("+91", "") ?: ""
                Log.d("AuthRepository", "🔍 Processing phone number: $phoneNumber")
                Log.d("AuthRepository", "🔍 Original phone number: ${firebaseUser.phoneNumber}")
                
                val user = getUserByPhoneNumber(phoneNumber)
                Log.d("AuthRepository", "🔍 getUserByPhoneNumber result: ${user?.name ?: "null"}")
                
                if (user == null) {
                    Log.d("AuthRepository", "⚠️ User not found with phone: $phoneNumber")
                    Log.d("AuthRepository", "⚠️ Trying with original phone number: ${firebaseUser.phoneNumber}")
                    // Try with the original phone number as well
                    val userWithOriginalPhone = getUserByPhoneNumber(firebaseUser.phoneNumber ?: "")
                    Log.d("AuthRepository", "🔍 getUserByPhoneNumber with original phone result: ${userWithOriginalPhone?.name ?: "null"}")
                    return userWithOriginalPhone
                }
                
                return user
            } else {
                Log.d("AuthRepository", "❌ No Firebase user found")
                return null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error in getCurrentUserFromFirebase: ${e.message}", e)
            return null
        }
    }
    
    // Direct phone lookup for development authentication
    suspend fun getUserByPhoneNumberDirect(phoneNumber: String): User? {
        return getUserByPhoneNumber(phoneNumber)
    }
    

    
    fun getCurrentUser(): User? {
        return auth.currentUser?.let { firebaseUser ->
            User(
                uid = firebaseUser.uid,
                phone = firebaseUser.phoneNumber ?: ""
            )
        }
    }
    
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    
    suspend fun signOut() {
        auth.signOut()
    }
    
    suspend fun getAllUsers(): List<User> {
        return try {
            val querySnapshot = firestore.collection("users")
                .get()
                .await()
            
            querySnapshot.documents.mapNotNull { document ->
                try {
                    val userData = document.data ?: return@mapNotNull null
                    
                    val roleString = userData["role"] as? String
                    val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                        "APPROVER" -> UserRole.APPROVER
                        "ADMIN" -> UserRole.ADMIN
                        "USER" -> UserRole.USER
                        "PRODUCTION_HEAD" -> UserRole.PRODUCTION_HEAD
                        else -> UserRole.USER
                    }
                    
                    // Parse device info
                    val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                    val deviceInfo = DeviceInfo(
                        fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                        deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                        deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                        osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                        appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                        lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                        isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                    )
                    
                    // Parse notification preferences
                    val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                    val notificationPreferences = NotificationPreferences(
                        pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                        expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                        expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                        expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                        projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                        pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                    )
                    
                    User(
                        uid = userData["uid"] as? String ?: "",
                        name = userData["name"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        phone = userData["phoneNumber"] as? String ?: "",
                        role = userRole,
                        createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                        isActive = userData["isActive"] as? Boolean ?: true,
                        assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        deviceInfo = deviceInfo,
                        notificationPreferences = notificationPreferences
                    )
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error parsing user document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching all users: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun createUserByAdmin(user: User): Result<Unit> {
        return try {
            // Check if user already exists
            val existingUser = getUserByPhoneNumber(user.phone)
            if (existingUser != null) {
                return Result.failure(Exception("User with this phone number already exists"))
            }
            
            // Create user document with proper field names for Firestore
            val userMap = mapOf(
                "uid" to user.phone,
                "name" to user.name,
                "email" to user.email,
                "phoneNumber" to user.phone,
                "role" to user.role.name,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "isActive" to user.isActive,
                "assignedProjects" to user.assignedProjects,
                "deviceInfo" to mapOf(
                    "fcmToken" to user.deviceInfo.fcmToken,
                    "deviceId" to user.deviceInfo.deviceId,
                    "deviceModel" to user.deviceInfo.deviceModel,
                    "osVersion" to user.deviceInfo.osVersion,
                    "appVersion" to user.deviceInfo.appVersion,
                    "lastLoginAt" to user.deviceInfo.lastLoginAt,
                    "isOnline" to user.deviceInfo.isOnline
                ),
                "notificationPreferences" to mapOf(
                    "pushNotifications" to user.notificationPreferences.pushNotifications,
                    "expenseSubmitted" to user.notificationPreferences.expenseSubmitted,
                    "expenseApproved" to user.notificationPreferences.expenseApproved,
                    "expenseRejected" to user.notificationPreferences.expenseRejected,
                    "projectAssignment" to user.notificationPreferences.projectAssignment,
                    "pendingApprovals" to user.notificationPreferences.pendingApprovals
                )
            )

            val await = firestore.collection("users")
                .document(user.phone) // Use phone number as document ID
                .set(userMap)         // Replaces `.add(...)`
                .await()
            Log.d("AuthRepository", "✅ Created user: ${user.name} with role: ${user.role}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error creating user: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getUsersByRole(role: UserRole): List<User> {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("role", role.name)
                .get()
                .await()
            
            querySnapshot.documents.mapNotNull { document ->
                try {
                    val userData = document.data ?: return@mapNotNull null
                    
                    val roleString = userData["role"] as? String
                    val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                        "APPROVER" -> UserRole.APPROVER
                        "ADMIN" -> UserRole.ADMIN
                        "USER" -> UserRole.USER
                        "PRODUCTION_HEAD" -> UserRole.PRODUCTION_HEAD
                        else -> UserRole.USER
                    }
                    
                    // Parse device info
                    val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                    val deviceInfo = DeviceInfo(
                        fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                        deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                        deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                        osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                        appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                        lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                        isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                    )
                    
                    // Parse notification preferences
                    val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                    val notificationPreferences = NotificationPreferences(
                        pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                        expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                        expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                        expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                        projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                        pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                    )
                    
                    User(
                        uid = userData["uid"] as? String ?: "",
                        name = userData["name"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        phone = userData["phoneNumber"] as? String ?: "",
                        role = userRole,
                        createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                        isActive = userData["isActive"] as? Boolean ?: true,
                        assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        deviceInfo = deviceInfo,
                        notificationPreferences = notificationPreferences
                    )
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error parsing user document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching users by role: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getUserById(userId: String): User? {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("uid", userId)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                val userData = document.data ?: return null
                
                val roleString = userData["role"] as? String
                val userRole = when (roleString?.uppercase()?.replace(" ", "_")) {
                    "APPROVER" -> UserRole.APPROVER
                    "ADMIN" -> UserRole.ADMIN
                    "USER" -> UserRole.USER
                    "PRODUCTION_HEAD" -> UserRole.PRODUCTION_HEAD
                    else -> UserRole.USER
                }
                
                // Parse device info
                val deviceInfoData = userData["deviceInfo"] as? Map<*, *>
                val deviceInfo = DeviceInfo(
                    fcmToken = deviceInfoData?.get("fcmToken") as? String ?: "",
                    deviceId = deviceInfoData?.get("deviceId") as? String ?: "",
                    deviceModel = deviceInfoData?.get("deviceModel") as? String ?: "",
                    osVersion = deviceInfoData?.get("osVersion") as? String ?: "",
                    appVersion = deviceInfoData?.get("appVersion") as? String ?: "",
                    lastLoginAt = deviceInfoData?.get("lastLoginAt") as? Long ?: System.currentTimeMillis(),
                    isOnline = deviceInfoData?.get("isOnline") as? Boolean ?: true
                )
                
                // Parse notification preferences
                val notificationPrefsData = userData["notificationPreferences"] as? Map<*, *>
                val notificationPreferences = NotificationPreferences(
                    pushNotifications = notificationPrefsData?.get("pushNotifications") as? Boolean ?: true,
                    expenseSubmitted = notificationPrefsData?.get("expenseSubmitted") as? Boolean ?: true,
                    expenseApproved = notificationPrefsData?.get("expenseApproved") as? Boolean ?: true,
                    expenseRejected = notificationPrefsData?.get("expenseRejected") as? Boolean ?: true,
                    projectAssignment = notificationPrefsData?.get("projectAssignment") as? Boolean ?: true,
                    pendingApprovals = notificationPrefsData?.get("pendingApprovals") as? Boolean ?: true
                )
                
                User(
                    uid = userData["uid"] as? String ?: "",
                    name = userData["name"] as? String ?: "",
                    email = userData["email"] as? String ?: "",
                    phone = userData["phoneNumber"] as? String ?: "",
                    role = userRole,
                    createdAt = (userData["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                    isActive = userData["isActive"] as? Boolean ?: true,
                    assignedProjects = (userData["assignedProjects"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    deviceInfo = deviceInfo,
                    notificationPreferences = notificationPreferences
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching user by ID: ${e.message}")
            null
        }
    }
    
    suspend fun updateUserFCMToken(userId: String, fcmToken: String): Result<Unit> {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("uid", userId)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                firestore.collection("users")
                    .document(document.id)
                    .update("deviceInfo.fcmToken", fcmToken)
                    .await()
                Log.d("AuthRepository", "✅ Updated FCM token for user: $userId")
                Result.success(Unit)
            } else {
                Log.e("AuthRepository", "❌ User not found for FCM token update: $userId")
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Error updating FCM token: ${e.message}")
            Result.failure(e)
        }
    }
} 