package com.deeksha.avr

import android.app.Application
import android.util.Log
import com.deeksha.avr.service.TemporaryApproverExpirationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AvrApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("AvrApplication", "🚀 Application starting...")
        
        // Initialize temporary approver expiration service
        try {
            TemporaryApproverExpirationManager.initialize(this)
            Log.d("AvrApplication", "✅ Temporary approver expiration service initialized")
        } catch (e: Exception) {
            Log.e("AvrApplication", "❌ Failed to initialize expiration service", e)
        }
        
        Log.d("AvrApplication", "✅ Application initialization completed")
    }
} 