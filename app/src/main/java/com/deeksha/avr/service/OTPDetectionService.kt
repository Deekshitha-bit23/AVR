package com.deeksha.avr.service

import android.content.Context
import android.util.Log
import com.deeksha.avr.utils.OTPManager
import com.deeksha.avr.utils.PermissionUtils
import com.deeksha.avr.utils.SMSRetrieverHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Comprehensive OTP Detection Service
 * Combines both SMS permission-based detection and Google SMS Retriever API
 */
class OTPDetectionService(private val context: Context) {
    
    companion object {
        private const val TAG = "OTPDetectionService"
        private const val SMS_RETRIEVER_TIMEOUT = 30000L // 30 seconds
    }
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Start OTP detection using the best available method
     */
    fun startOTPDetection() {
        Log.d(TAG, "🚀 Starting OTP detection service...")
        
        // Check if SMS permissions are available
        val hasSMSPermissions = PermissionUtils.hasSMSPermissions(context)
        val hasGooglePlayServices = SMSRetrieverHelper.isGooglePlayServicesAvailable(context)
        
        Log.d(TAG, "📊 Detection capabilities:")
        Log.d(TAG, "  - SMS Permissions: $hasSMSPermissions")
        Log.d(TAG, "  - Google Play Services: $hasGooglePlayServices")
        
        when {
            hasSMSPermissions -> {
                Log.d(TAG, "✅ Using SMS permission-based detection")
                // SMS receiver will handle this automatically
            }
            hasGooglePlayServices -> {
                Log.d(TAG, "✅ Using Google SMS Retriever API")
                startSMSRetrieverDetection()
            }
            else -> {
                Log.w(TAG, "⚠️ No OTP detection method available - manual entry only")
                OTPManager.setDetectedOTP("", "Manual Entry Required")
            }
        }
    }
    
    /**
     * Start SMS Retriever detection (no permissions required)
     */
    private fun startSMSRetrieverDetection() {
        try {
            Log.d(TAG, "🔄 Starting Google SMS Retriever...")
            val task = SMSRetrieverHelper.startSMSRetrieverSync(context)
            
            task.addOnSuccessListener {
                Log.d(TAG, "✅ SMS Retriever started successfully")
                OTPManager.setDetectedOTP("", "SMS Retriever Active")
            }
            
            task.addOnFailureListener { exception ->
                Log.e(TAG, "❌ Failed to start SMS Retriever", exception)
                OTPManager.setDetectedOTP("", "SMS Retriever Failed")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error starting SMS Retriever", e)
        }
    }
    
    /**
     * Handle SMS message from Google SMS Retriever
     * This method should be called from a BroadcastReceiver
     */
    fun handleSMSRetrieverMessage(smsMessage: String) {
        Log.d(TAG, "📨 Handling SMS Retriever message: $smsMessage")
        
        val otp = SMSRetrieverHelper.parseSMSMessage(smsMessage)
        if (otp != null) {
            Log.d(TAG, "✅ OTP detected via SMS Retriever: $otp")
            OTPManager.setDetectedOTP(otp, "SMS Retriever")
        } else {
            Log.d(TAG, "❌ No OTP found in SMS Retriever message")
        }
    }
    
    /**
     * Handle SMS message from permission-based receiver
     * This method should be called from OTPSmsReceiver
     */
    fun handleSMSPermissionMessage(smsMessage: String) {
        Log.d(TAG, "📨 Handling SMS permission message: $smsMessage")
        
        val otp = SMSRetrieverHelper.parseSMSMessage(smsMessage)
        if (otp != null) {
            Log.d(TAG, "✅ OTP detected via SMS permission: $otp")
            OTPManager.setDetectedOTP(otp, "SMS Permission")
        } else {
            Log.d(TAG, "❌ No OTP found in SMS permission message")
        }
    }
    
    /**
     * Stop OTP detection
     */
    fun stopOTPDetection() {
        Log.d(TAG, "🛑 Stopping OTP detection service...")
        
        try {
            SMSRetrieverHelper.stopSMSRetriever(context)
            Log.d(TAG, "✅ SMS Retriever stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping SMS Retriever", e)
        }
    }
    
    /**
     * Get detection status
     */
    fun getDetectionStatus(): String {
        val hasSMSPermissions = PermissionUtils.hasSMSPermissions(context)
        val hasGooglePlayServices = SMSRetrieverHelper.isGooglePlayServicesAvailable(context)
        
        return when {
            hasSMSPermissions -> "SMS Permission Available"
            hasGooglePlayServices -> "SMS Retriever Available"
            else -> "Manual Entry Only"
        }
    }
}
