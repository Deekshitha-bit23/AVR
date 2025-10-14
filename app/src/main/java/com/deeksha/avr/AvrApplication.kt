package com.deeksha.avr

import android.app.Application
import android.util.Log
import com.deeksha.avr.service.TemporaryApproverExpirationManager
import com.deeksha.avr.service.DelegationScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AvrApplication : Application() {
    
    @Inject
    lateinit var delegationScheduler: DelegationScheduler
    
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
        
        // Initialize delegation scheduler
        try {
            delegationScheduler.scheduleDelegationExpiryChecks()
            Log.d("AvrApplication", "✅ Delegation expiry scheduler initialized")
        } catch (e: Exception) {
            Log.e("AvrApplication", "❌ Failed to initialize delegation scheduler", e)
        }
        
        Log.d("AvrApplication", "✅ Application initialization completed")
    }
} 