package com.deeksha.avr.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.TemporaryApprover
import com.deeksha.avr.model.User
import com.deeksha.avr.repository.TemporaryApproverRepository
import com.deeksha.avr.repository.AuthRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TemporaryApproverViewModel @Inject constructor(
    private val temporaryApproverRepository: TemporaryApproverRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "TempApproverViewModel"
    }
    
    // State for temporary approvers list
    private val _temporaryApprovers = MutableStateFlow<List<TemporaryApprover>>(emptyList())
    val temporaryApprovers: StateFlow<List<TemporaryApprover>> = _temporaryApprovers.asStateFlow()
    
    // State for available approvers (users who can be assigned as temporary approvers)
    private val _availableApprovers = MutableStateFlow<List<User>>(emptyList())
    val availableApprovers: StateFlow<List<User>> = _availableApprovers.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isAddingApprover = MutableStateFlow(false)
    val isAddingApprover: StateFlow<Boolean> = _isAddingApprover.asStateFlow()
    
    // Error states
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Success message
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    /**
     * Load temporary approvers for a project
     */
    fun loadTemporaryApprovers(projectId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                Log.d(TAG, "🔄 Loading temporary approvers for project: $projectId")
                
                // First, deactivate any expired approvers
                temporaryApproverRepository.deactivateExpiredApprovers(projectId)
                
                // Then load all temporary approvers
                val result = temporaryApproverRepository.getTemporaryApprovers(projectId)
                
                if (result.isSuccess) {
                    _temporaryApprovers.value = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "✅ Loaded ${_temporaryApprovers.value.size} temporary approvers")
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to load temporary approvers: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to load temporary approvers", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error loading temporary approvers: ${e.message}"
                Log.e(TAG, "❌ Exception while loading temporary approvers", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load available approvers (users with APPROVER role who can be assigned)
     */
    fun loadAvailableApprovers() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Loading available approvers from database")
                
                // Clear any previous error
                _error.value = null
                
                // Get all users from the database (returns List<User> directly)
                val allUsers = authRepository.getAllUsers()
                
                // Safely filter users who can be approvers (only APPROVER role) and are active
                val approvers = allUsers.filter { user ->
                    try {
                        user.isActive && user.role.name == "APPROVER"
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Error checking user ${user.name}: ${e.message}")
                        false
                    }
                }
                
                _availableApprovers.value = approvers
                Log.d(TAG, "✅ Loaded ${approvers.size} available approvers from database")
                
                // Log each approver for debugging
                approvers.forEach { approver ->
                    Log.d(TAG, "📋 Available approver: ${approver.name} (${approver.phone}) - Role: ${approver.role.name}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while loading available approvers", e)
                _error.value = "Failed to load available approvers: ${e.message}"
                // Set empty list on error to prevent crash
                _availableApprovers.value = emptyList()
            }
        }
    }
    
    /**
     * Add a temporary approver to a project
     */
    fun addTemporaryApprover(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        expiringDate: Date,
        assignedBy: String,
        assignedByName: String
    ) {
        viewModelScope.launch {
            try {
                _isAddingApprover.value = true
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Adding temporary approver: $approverName to project: $projectId")
                
                val expiringTimestamp = Timestamp(expiringDate)
                
                val result = temporaryApproverRepository.addTemporaryApprover(
                    projectId = projectId,
                    approverId = approverId,
                    approverName = approverName,
                    approverPhone = approverPhone,
                    expiringDate = expiringTimestamp,
                    assignedBy = assignedBy,
                    assignedByName = assignedByName
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver added successfully"
                    Log.d(TAG, "✅ Temporary approver added successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to add temporary approver: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to add temporary approver", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error adding temporary approver: ${e.message}"
                Log.e(TAG, "❌ Exception while adding temporary approver", e)
            } finally {
                _isAddingApprover.value = false
            }
        }
    }
    
    /**
     * Deactivate a temporary approver
     */
    fun deactivateTemporaryApprover(projectId: String, tempApproverId: String) {
        viewModelScope.launch {
            try {
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Deactivating temporary approver: $tempApproverId")
                
                val result = temporaryApproverRepository.deactivateTemporaryApprover(projectId, tempApproverId)
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver deactivated successfully"
                    Log.d(TAG, "✅ Temporary approver deactivated successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to deactivate temporary approver: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to deactivate temporary approver", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error deactivating temporary approver: ${e.message}"
                Log.e(TAG, "❌ Exception while deactivating temporary approver", e)
            }
        }
    }
    
    /**
     * Update expiring date of a temporary approver
     */
    fun updateExpiringDate(projectId: String, tempApproverId: String, newExpiringDate: Date) {
        viewModelScope.launch {
            try {
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Updating expiring date for temporary approver: $tempApproverId")
                
                val newExpiringTimestamp = Timestamp(newExpiringDate)
                
                val result = temporaryApproverRepository.updateExpiringDate(
                    projectId, 
                    tempApproverId, 
                    newExpiringTimestamp
                )
                
                if (result.isSuccess) {
                    _successMessage.value = "Expiring date updated successfully"
                    Log.d(TAG, "✅ Expiring date updated successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to update expiring date: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to update expiring date", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error updating expiring date: ${e.message}"
                Log.e(TAG, "❌ Exception while updating expiring date", e)
            }
        }
    }
    
    /**
     * Delete a temporary approver
     */
    fun deleteTemporaryApprover(projectId: String, tempApproverId: String) {
        viewModelScope.launch {
            try {
                _error.value = null
                _successMessage.value = null
                
                Log.d(TAG, "🔄 Deleting temporary approver: $tempApproverId")
                
                val result = temporaryApproverRepository.deleteTemporaryApprover(projectId, tempApproverId)
                
                if (result.isSuccess) {
                    _successMessage.value = "Temporary approver deleted successfully"
                    Log.d(TAG, "✅ Temporary approver deleted successfully")
                    
                    // Reload the temporary approvers list
                    loadTemporaryApprovers(projectId)
                } else {
                    val exception = result.exceptionOrNull()
                    _error.value = "Failed to delete temporary approver: ${exception?.message}"
                    Log.e(TAG, "❌ Failed to delete temporary approver", exception)
                }
                
            } catch (e: Exception) {
                _error.value = "Error deleting temporary approver: ${e.message}"
                Log.e(TAG, "❌ Exception while deleting temporary approver", e)
            }
        }
    }
    
    /**
     * Check and deactivate expired approvers
     */
    fun checkAndDeactivateExpiredApprovers(projectId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Checking for expired temporary approvers")
                
                val result = temporaryApproverRepository.deactivateExpiredApprovers(projectId)
                
                if (result.isSuccess) {
                    val deactivatedCount = result.getOrNull() ?: 0
                    if (deactivatedCount > 0) {
                        _successMessage.value = "$deactivatedCount expired temporary approver(s) deactivated"
                        Log.d(TAG, "✅ Deactivated $deactivatedCount expired temporary approvers")
                        
                        // Reload the temporary approvers list
                        loadTemporaryApprovers(projectId)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "❌ Failed to check expired approvers", exception)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception while checking expired approvers", e)
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    /**
     * Get active temporary approvers only
     */
    fun getActiveTemporaryApprovers(): List<TemporaryApprover> {
        return _temporaryApprovers.value.filter { tempApprover ->
            tempApprover.isActive
        }
    }
    
    /**
     * Get expired temporary approvers
     */
    fun getExpiredTemporaryApprovers(): List<TemporaryApprover> {
        return _temporaryApprovers.value.filter { tempApprover ->
            !tempApprover.isActive
        }
    }
    
    /**
     * Get inactive temporary approvers
     */
    fun getInactiveTemporaryApprovers(): List<TemporaryApprover> {
        return _temporaryApprovers.value.filter { tempApprover ->
            !tempApprover.isActive
        }
    }
}
