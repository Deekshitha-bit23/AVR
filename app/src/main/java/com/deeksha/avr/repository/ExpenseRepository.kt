package com.deeksha.avr.repository

import android.util.Log
import com.deeksha.avr.model.Expense
import com.deeksha.avr.model.ExpenseStatus
import com.deeksha.avr.model.ExpenseSummary
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    fun getProjectExpenses(projectId: String): Flow<List<Expense>> = callbackFlow {
        Log.d("ExpenseRepository", "🔥 Fetching expenses for project: $projectId")
        
        val listener = firestore.collection("projects")
            .document(projectId)
            .collection("expenses")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpenseRepository", "❌ Error fetching expenses: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                Log.d("ExpenseRepository", "📊 Received ${snapshot?.documents?.size ?: 0} expense documents")
                
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val expenseData = doc.data ?: emptyMap()
                        
                        Expense(
                            id = doc.id,
                            projectId = projectId, // Use the projectId parameter since it's stored in subcollection
                            userId = expenseData["userId"] as? String ?: "",
                            userName = expenseData["userName"] as? String ?: "",
                            date = expenseData["date"] as? Timestamp,
                            amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                            department = expenseData["department"] as? String ?: "",
                            category = expenseData["category"] as? String ?: "",
                            description = expenseData["description"] as? String ?: "",
                            modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                            tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                            gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                            netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                            attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                            attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                            status = when (expenseData["status"] as? String) {
                                "APPROVED" -> ExpenseStatus.APPROVED
                                "REJECTED" -> ExpenseStatus.REJECTED
                                "DRAFT" -> ExpenseStatus.DRAFT
                                else -> ExpenseStatus.PENDING
                            },
                            submittedAt = expenseData["submittedAt"] as? Timestamp,
                            reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                            reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                            reviewComments = expenseData["reviewComments"] as? String ?: "",
                            receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("ExpenseRepository", "❌ Error parsing expense document ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                Log.d("ExpenseRepository", "🎯 Sending ${expenses.size} expenses to UI")
                trySend(expenses)
            }
        
        awaitClose { 
            Log.d("ExpenseRepository", "🔚 Closing expenses listener")
            listener.remove() 
        }
    }
    
    fun getUserExpenses(userId: String): Flow<List<Expense>> = callbackFlow {
        Log.d("ExpenseRepository", "🔥 Setting up real-time listener for user expenses: $userId")
        
        try {
            // First get all projects
            val projectsSnapshot = firestore.collection("projects").get().await()
            val projectIds = projectsSnapshot.documents.map { it.id }
            
            if (projectIds.isEmpty()) {
                Log.d("ExpenseRepository", "📭 No projects found for user expenses")
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            
            // Create listeners for each project's user expenses
            val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
            val allUserExpenses = mutableMapOf<String, List<Expense>>()
            
            projectIds.forEach { projectId ->
                val listener = firestore.collection("projects")
                        .document(projectId)
                        .collection("expenses")
                        .whereEqualTo("userId", userId)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ExpenseRepository", "❌ Error listening to user expenses for project $projectId: ${error.message}")
                            return@addSnapshotListener
                        }
                        
                        val projectUserExpenses = snapshot?.documents?.mapNotNull { doc ->
                        try {
                                val expenseData = doc.data ?: return@mapNotNull null
                            Expense(
                                id = doc.id,
                                projectId = projectId,
                                userId = expenseData["userId"] as? String ?: "",
                                userName = expenseData["userName"] as? String ?: "",
                                date = expenseData["date"] as? Timestamp,
                                amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                                department = expenseData["department"] as? String ?: "",
                                category = expenseData["category"] as? String ?: "",
                                description = expenseData["description"] as? String ?: "",
                                modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                                tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                                gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                                netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                                attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                                attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                                status = when (expenseData["status"] as? String) {
                                    "APPROVED" -> ExpenseStatus.APPROVED
                                    "REJECTED" -> ExpenseStatus.REJECTED
                                    "DRAFT" -> ExpenseStatus.DRAFT
                                    else -> ExpenseStatus.PENDING
                                },
                                submittedAt = expenseData["submittedAt"] as? Timestamp,
                                reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                                reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                                reviewComments = expenseData["reviewComments"] as? String ?: "",
                                receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                            )
                        } catch (e: Exception) {
                                Log.w("ExpenseRepository", "⚠️ Error mapping user expense document ${doc.id}: ${e.message}")
                            null
                        }
                        } ?: emptyList()
                        
                        // Update the map for this project
                        allUserExpenses[projectId] = projectUserExpenses
                        
                        // Combine all user expenses and emit
                        val combinedExpenses = allUserExpenses.values.flatten()
                            .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
                        
                        Log.d("ExpenseRepository", "📊 Real-time update: ${combinedExpenses.size} user expenses total")
                        trySend(combinedExpenses)
                    }
                
                listeners.add(listener)
            }
            
            awaitClose { 
                Log.d("ExpenseRepository", "🔚 Cleaning up ${listeners.size} user expense listeners")
                listeners.forEach { it.remove() }
            }
            
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error setting up user expense listeners: ${e.message}")
            trySend(emptyList())
            awaitClose { }
        }
    }
    
    suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            // Don't store projectId since it's implicit in the subcollection path
            val expenseData = mapOf(
                "userId" to expense.userId,
                "userName" to expense.userName,
                "date" to expense.date,
                "amount" to expense.amount,
                "department" to expense.department,
                "category" to expense.category,
                "description" to expense.description,
                "modeOfPayment" to expense.modeOfPayment,
                "tds" to expense.tds,
                "gst" to expense.gst,
                "netAmount" to expense.netAmount,
                "attachmentUrl" to expense.attachmentUrl,
                "attachmentFileName" to expense.attachmentFileName,
                "status" to expense.status.name,
                "submittedAt" to Timestamp.now(),
                "reviewedAt" to expense.reviewedAt,
                "reviewedBy" to expense.reviewedBy,
                "reviewComments" to expense.reviewComments,
                "receiptNumber" to expense.receiptNumber
            )
            
            val docRef = firestore.collection("projects")
                .document(expense.projectId)
                .collection("expenses")
                .add(expenseData).await()
                
            Log.d("ExpenseRepository", "✅ Successfully added expense with ID: ${docRef.id} to project: ${expense.projectId}")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error adding expense: ${e.message}")
            Result.failure(e)
        }
    }
    

    
    suspend fun getExpenseSummary(projectId: String): ExpenseSummary {
        return try {
            val snapshot = firestore.collection("projects")
                .document(projectId)
                .collection("expenses")
                .get().await()
            
            val expenses = snapshot.documents.mapNotNull { doc ->
                try {
                    val expenseData = doc.data ?: return@mapNotNull null
                    val amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0
                    val status = when (expenseData["status"] as? String) {
                        "APPROVED" -> ExpenseStatus.APPROVED
                        "REJECTED" -> ExpenseStatus.REJECTED
                        "DRAFT" -> ExpenseStatus.DRAFT
                        else -> ExpenseStatus.PENDING
                    }
                    val category = expenseData["category"] as? String ?: ""
                    val department = expenseData["department"] as? String ?: ""
                    
                    Triple(amount, status, Pair(category, department))
                } catch (e: Exception) {
                    null
                }
            }
            
            val approvedExpensesList = expenses.filter { it.second == ExpenseStatus.APPROVED }
            val totalExpenses = approvedExpensesList.sumOf { it.first }
            val approvedExpenses = approvedExpensesList.sumOf { it.first }
            val approvedCount = approvedExpensesList.size
            val pendingExpenses = expenses.filter { it.second == ExpenseStatus.PENDING }.sumOf { it.first }
            
            val expensesByCategory = approvedExpensesList
                .filter { it.third.first.isNotEmpty() }
                .groupBy { it.third.first }
                .mapValues { entry -> entry.value.sumOf { it.first } }
            
            val expensesByDepartment = approvedExpensesList
                .filter { it.third.second.isNotEmpty() }
                .groupBy { it.third.second }
                .mapValues { entry -> entry.value.sumOf { it.first } }
            
            ExpenseSummary(
                totalExpenses = totalExpenses,
                totalApproved = approvedExpenses,
                totalPending = pendingExpenses,
                approvedCount = approvedCount,
                expensesByCategory = expensesByCategory,
                expensesByDepartment = expensesByDepartment
            )
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error getting expense summary: ${e.message}")
            ExpenseSummary()
        }
    }

    fun getPendingExpenses(): Flow<List<Expense>> = callbackFlow {
        Log.d("ExpenseRepository", "🔥 Setting up real-time listener for pending expenses across all projects...")
        
        try {
            // First get all projects with debugging
            Log.d("ExpenseRepository", "📋 Fetching all projects from Firebase...")
            val projectsSnapshot = firestore.collection("projects").get().await()
            Log.d("ExpenseRepository", "📊 Found ${projectsSnapshot.documents.size} projects in Firebase")
            
            val projectIds = projectsSnapshot.documents.map { it.id }
            Log.d("ExpenseRepository", "🎯 Project IDs: $projectIds")
            
            if (projectIds.isEmpty()) {
                Log.w("ExpenseRepository", "⚠️ No projects found in Firebase!")
                Log.d("ExpenseRepository", "🔍 Checking if projects collection exists...")
                
                // Try to check if projects collection exists at all
                try {
                    val collectionCheck = firestore.collection("projects").limit(1).get().await()
                    Log.d("ExpenseRepository", "📁 Projects collection query result: ${collectionCheck.documents.size} documents")
                    if (collectionCheck.isEmpty) {
                        Log.e("ExpenseRepository", "❌ Projects collection is completely empty!")
                    }
                } catch (e: Exception) {
                    Log.e("ExpenseRepository", "❌ Error checking projects collection: ${e.message}")
                    e.printStackTrace()
                }
                
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            
            // Create listeners for each project's pending expenses
            val listeners = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
            val allPendingExpenses = mutableMapOf<String, List<Expense>>()
            
            // Initialize with empty lists for all projects
            projectIds.forEach { projectId ->
                allPendingExpenses[projectId] = emptyList()
                Log.d("ExpenseRepository", "🎪 Initialized project $projectId with empty expense list")
            }
            
            projectIds.forEachIndexed { index, projectId ->
                Log.d("ExpenseRepository", "🔄 Setting up listener for project ${index + 1}/${projectIds.size}: $projectId")
                
                val listener = firestore.collection("projects")
                    .document(projectId)
                    .collection("expenses")
                    .whereEqualTo("status", ExpenseStatus.PENDING.name)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e("ExpenseRepository", "❌ Error listening to pending expenses for project $projectId: ${error.message}")
                            Log.e("ExpenseRepository", "❌ Error code: ${error.code}, Details: ${error.localizedMessage}")
                            error.printStackTrace()
                            // Don't close the flow, just log the error and continue with other projects
                            return@addSnapshotListener
                        }
                        
                        Log.d("ExpenseRepository", "📡 Received snapshot for project $projectId: ${snapshot?.documents?.size ?: 0} documents")
                        
                        val projectPendingExpenses = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                Log.d("ExpenseRepository", "📝 Processing document ${doc.id} in project $projectId")
                                val expenseData = doc.data ?: return@mapNotNull null
                                Log.d("ExpenseRepository", "📊 Document data: ${expenseData.keys}")
                                
                                val expense = Expense(
                                    id = doc.id,
                                    projectId = projectId,
                                    userId = expenseData["userId"] as? String ?: "",
                                    userName = expenseData["userName"] as? String ?: "",
                                    date = expenseData["date"] as? Timestamp,
                                    amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                                    department = expenseData["department"] as? String ?: "",
                                    category = expenseData["category"] as? String ?: "",
                                    description = expenseData["description"] as? String ?: "",
                                    modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                                    tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                                    gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                                    netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                                    attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                                    attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                                    status = when (expenseData["status"] as? String) {
                                        "APPROVED" -> ExpenseStatus.APPROVED
                                        "REJECTED" -> ExpenseStatus.REJECTED
                                        "DRAFT" -> ExpenseStatus.DRAFT
                                        else -> ExpenseStatus.PENDING
                                    },
                                    submittedAt = expenseData["submittedAt"] as? Timestamp,
                                    reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                                    reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                                    reviewComments = expenseData["reviewComments"] as? String ?: "",
                                    receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                                )
                                
                                Log.d("ExpenseRepository", "✅ Successfully mapped expense ${doc.id}: ${expense.department}/${expense.category} by ${expense.userName}")
                                expense
                            } catch (e: Exception) {
                                Log.w("ExpenseRepository", "⚠️ Error mapping expense document ${doc.id}: ${e.message}")
                                e.printStackTrace()
                                null
                            }
                        } ?: emptyList()
                        
                        Log.d("ExpenseRepository", "📦 Project $projectId has ${projectPendingExpenses.size} pending expenses")
                        
                        // Update the map for this project
                        allPendingExpenses[projectId] = projectPendingExpenses
                        
                        // Combine all pending expenses and emit
                        val combinedExpenses = allPendingExpenses.values.flatten()
                            .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
                        
                        Log.d("ExpenseRepository", "📊 Real-time update: ${combinedExpenses.size} pending expenses total (Project $projectId: ${projectPendingExpenses.size})")
                        
                        if (combinedExpenses.isNotEmpty()) {
                            Log.d("ExpenseRepository", "🎯 Sample expenses: ${combinedExpenses.take(3).map { "${it.id} - ${it.department}/${it.category}" }}")
                        }
                        
                        // Always emit the combined result, even if empty
                        val success = trySend(combinedExpenses)
                        Log.d("ExpenseRepository", "📤 Emitted ${combinedExpenses.size} expenses to UI, success: ${success.isSuccess}")
                    }
                
                listeners.add(listener)
                Log.d("ExpenseRepository", "✅ Listener ${index + 1} added for project $projectId")
            }
            
            Log.d("ExpenseRepository", "🎬 All ${listeners.size} listeners set up successfully")
            
            awaitClose { 
                Log.d("ExpenseRepository", "🔚 Cleaning up ${listeners.size} pending expense listeners")
                listeners.forEach { it.remove() }
            }
            
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error setting up pending expenses listeners: ${e.message}")
            Log.e("ExpenseRepository", "❌ Error type: ${e::class.simpleName}")
            e.printStackTrace()
            trySend(emptyList())
            awaitClose { }
        }
    }
    
    fun getPendingExpensesForProject(projectId: String): Flow<List<Expense>> = callbackFlow {
        try {
            Log.d("ExpenseRepository", "🔄 Loading pending expenses for project: $projectId")
            
            val listener = firestore.collection("projects")
                .document(projectId)
                .collection("expenses")
                .whereEqualTo("status", ExpenseStatus.PENDING.name)
                .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ExpenseRepository", "❌ Error loading pending expenses for project: ${error.message}")
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val expenses = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val expenseData = doc.data ?: emptyMap()
                            Expense(
                                id = doc.id,
                                projectId = projectId,
                                userId = expenseData["userId"] as? String ?: "",
                                userName = expenseData["userName"] as? String ?: "",
                                date = expenseData["date"] as? Timestamp,
                                amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                                department = expenseData["department"] as? String ?: "",
                                category = expenseData["category"] as? String ?: "",
                                description = expenseData["description"] as? String ?: "",
                                modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                                tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                                gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                                netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                                attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                                attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                                status = when (expenseData["status"] as? String) {
                                    "APPROVED" -> ExpenseStatus.APPROVED
                                    "REJECTED" -> ExpenseStatus.REJECTED
                                    "DRAFT" -> ExpenseStatus.DRAFT
                                    else -> ExpenseStatus.PENDING
                                },
                                submittedAt = expenseData["submittedAt"] as? Timestamp,
                                reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                                reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                                reviewComments = expenseData["reviewComments"] as? String ?: "",
                                receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                            )
                        } catch (e: Exception) {
                            Log.w("ExpenseRepository", "⚠️ Error mapping expense document ${doc.id}: ${e.message}")
                            null
                        }
                    } ?: emptyList()
                    
                    Log.d("ExpenseRepository", "✅ Loaded ${expenses.size} pending expenses for project $projectId")
                    trySend(expenses)
                }
                
            awaitClose { listener.remove() }
                
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error in getPendingExpensesForProject: ${e.message}")
            trySend(emptyList())
        }
    }
    
    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            // Don't store projectId since it's implicit in the subcollection path
            val expenseData = mapOf(
                "userId" to expense.userId,
                "userName" to expense.userName,
                "date" to expense.date,
                "amount" to expense.amount,
                "department" to expense.department,
                "category" to expense.category,
                "description" to expense.description,
                "modeOfPayment" to expense.modeOfPayment,
                "tds" to expense.tds,
                "gst" to expense.gst,
                "netAmount" to expense.netAmount,
                "attachmentUrl" to expense.attachmentUrl,
                "attachmentFileName" to expense.attachmentFileName,
                "status" to expense.status.name,
                "submittedAt" to expense.submittedAt,
                "reviewedAt" to expense.reviewedAt,
                "reviewedBy" to expense.reviewedBy,
                "reviewComments" to expense.reviewComments,
                "receiptNumber" to expense.receiptNumber
            )
            
            firestore.collection("projects")
                .document(expense.projectId)
                .collection("expenses")
                .document(expense.id)
                .set(expenseData).await()
            
            Log.d("ExpenseRepository", "✅ Successfully updated expense with ID: ${expense.id} in project: ${expense.projectId}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error updating expense: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateExpenseStatus(
        projectId: String,
        expenseId: String,
        status: ExpenseStatus,
        reviewedBy: String,
        reviewComments: String,
        reviewedAt: com.google.firebase.Timestamp
    ) {
        try {
            Log.d("ExpenseRepository", "========== UPDATING EXPENSE STATUS ==========")
            Log.d("ExpenseRepository", "🔄 Project ID: $projectId")
            Log.d("ExpenseRepository", "🔄 Expense ID: $expenseId")
            Log.d("ExpenseRepository", "🔄 New Status: $status")
            Log.d("ExpenseRepository", "🔄 Reviewed By: $reviewedBy")
            Log.d("ExpenseRepository", "🔄 Comments: $reviewComments")
            Log.d("ExpenseRepository", "🔄 Reviewed At: $reviewedAt")
            
            val updates = mapOf(
                "status" to status.name,
                "reviewedBy" to reviewedBy,
                "reviewComments" to reviewComments,
                "reviewedAt" to reviewedAt
            )
            
            Log.d("ExpenseRepository", "📝 Update data: $updates")
            Log.d("ExpenseRepository", "🎯 Firebase path: projects/$projectId/expenses/$expenseId")
            
            // Verify the document exists first
            val docRef = firestore.collection("projects")
                .document(projectId)
                .collection("expenses")
                .document(expenseId)
            
            Log.d("ExpenseRepository", "🔍 Checking if document exists...")
            val docSnapshot = docRef.get().await()
            
            if (!docSnapshot.exists()) {
                Log.e("ExpenseRepository", "❌ Document does not exist at path: projects/$projectId/expenses/$expenseId")
                throw Exception("Expense document not found")
            }
            
            Log.d("ExpenseRepository", "✅ Document exists, current data: ${docSnapshot.data}")
            
            Log.d("ExpenseRepository", "🔄 Performing Firebase update...")
            docRef.update(updates).await()
            
            Log.d("ExpenseRepository", "✅ Firebase update completed successfully")
            
            // Verify the update was applied
            Log.d("ExpenseRepository", "🔍 Verifying update...")
            val updatedDoc = docRef.get().await()
            if (updatedDoc.exists()) {
                val updatedData = updatedDoc.data
                Log.d("ExpenseRepository", "✅ Updated document data: $updatedData")
                Log.d("ExpenseRepository", "✅ Status in DB: ${updatedData?.get("status")}")
                Log.d("ExpenseRepository", "✅ Reviewed by in DB: ${updatedData?.get("reviewedBy")}")
            } else {
                Log.e("ExpenseRepository", "❌ Document disappeared after update")
            }
                
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error updating expense status: ${e.message}")
            Log.e("ExpenseRepository", "❌ Error type: ${e::class.simpleName}")
            Log.e("ExpenseRepository", "❌ Full error: $e")
            e.printStackTrace()
            throw e
        }
    }
    
    suspend fun getApprovedExpensesForProject(projectId: String): List<Expense> {
        return try {
            Log.d("ExpenseRepository", "🔄 Loading approved expenses for project: $projectId")
            
            // First verify the project exists
            val projectDoc = firestore.collection("projects")
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                Log.e("ExpenseRepository", "❌ Project $projectId does not exist")
                return emptyList()
            }
            
            // Query approved expenses with additional logging
            Log.d("ExpenseRepository", "🔍 Querying approved expenses for project: $projectId")
            val snapshot = firestore.collection("projects")
                .document(projectId)
                .collection("expenses")
                .whereEqualTo("status", "APPROVED") // Use string directly to match Firestore
                .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            Log.d("ExpenseRepository", "📊 Found ${snapshot.documents.size} approved expense documents")
            
            val expenses = snapshot.documents.mapNotNull { doc ->
                try {
                    val expenseData = doc.data ?: run {
                        Log.w("ExpenseRepository", "⚠️ Empty data for expense document ${doc.id}")
                        return@mapNotNull null
                    }
                    
                    // Log raw data for debugging
                    Log.d("ExpenseRepository", "📝 Raw expense data for ${doc.id}: $expenseData")
                    
                    Expense(
                        id = doc.id,
                        projectId = projectId,
                        userId = expenseData["userId"] as? String ?: "",
                        userName = expenseData["userName"] as? String ?: "",
                        date = expenseData["date"] as? Timestamp,
                        amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                        department = expenseData["department"] as? String ?: "",
                        category = expenseData["category"] as? String ?: "",
                        description = expenseData["description"] as? String ?: "",
                        modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                        tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                        gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                        netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                        attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                        attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                        status = ExpenseStatus.APPROVED, // We know it's approved from the query
                        submittedAt = expenseData["submittedAt"] as? Timestamp,
                        reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                        reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                        reviewComments = expenseData["reviewComments"] as? String ?: "",
                        receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                    ).also {
                        Log.d("ExpenseRepository", "✅ Successfully mapped expense: ${it.id} - ₹${it.amount} - ${it.category}")
                    }
                } catch (e: Exception) {
                    Log.e("ExpenseRepository", "❌ Error mapping expense document ${doc.id}: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
            
            Log.d("ExpenseRepository", "✅ Successfully loaded ${expenses.size} approved expenses for project $projectId")
            expenses
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error loading approved expenses for project $projectId: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getExpenseById(expenseId: String): Expense? {
        return try {
            Log.d("ExpenseRepository", "🔄 Loading expense by ID: $expenseId")
            
            // First get all projects
            val projectsSnapshot = firestore.collection("projects").get().await()
            val projectIds = projectsSnapshot.documents.map { it.id }
            
            // Search for the expense in each project
            for (projectId in projectIds) {
                try {
                    val expenseSnapshot = firestore.collection("projects")
                        .document(projectId)
                        .collection("expenses")
                        .document(expenseId)
                        .get()
                        .await()
                    
                    if (expenseSnapshot.exists()) {
                        val expenseData = expenseSnapshot.data ?: continue
                        val expense = Expense(
                            id = expenseSnapshot.id,
                            projectId = projectId,
                            userId = expenseData["userId"] as? String ?: "",
                            userName = expenseData["userName"] as? String ?: "",
                            date = expenseData["date"] as? Timestamp,
                            amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                            department = expenseData["department"] as? String ?: "",
                            category = expenseData["category"] as? String ?: "",
                            description = expenseData["description"] as? String ?: "",
                            modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                            tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                            gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                            netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                            attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                            attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                            status = when (expenseData["status"] as? String) {
                                "APPROVED" -> ExpenseStatus.APPROVED
                                "REJECTED" -> ExpenseStatus.REJECTED
                                "DRAFT" -> ExpenseStatus.DRAFT
                                else -> ExpenseStatus.PENDING
                            },
                            submittedAt = expenseData["submittedAt"] as? Timestamp,
                            reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                            reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                            reviewComments = expenseData["reviewComments"] as? String ?: "",
                            receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                        )
                        
                        Log.d("ExpenseRepository", "✅ Found expense $expenseId in project $projectId")
                        return expense
                    }
                } catch (e: Exception) {
                    Log.w("ExpenseRepository", "⚠️ Error checking project $projectId for expense $expenseId: ${e.message}")
                    continue
                }
            }
            
            Log.w("ExpenseRepository", "❌ Expense $expenseId not found in any project")
            null
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error loading expense by ID: ${e.message}")
            null
        }
    }
    
    fun getUserExpensesForProject(projectId: String, userId: String): Flow<List<Expense>> = callbackFlow {
        Log.d("ExpenseRepository", "🔥 Setting up real-time listener for user $userId expenses in project: $projectId")
        Log.d("ExpenseRepository", "🔍 Firebase query path: projects/$projectId/expenses where userId == $userId")
        
        // First, verify the project exists to avoid potential issues
        try {
            val projectDoc = firestore.collection("projects").document(projectId).get().await()
            if (!projectDoc.exists()) {
                Log.e("ExpenseRepository", "❌ Project $projectId does not exist")
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            Log.d("ExpenseRepository", "✅ Project $projectId exists, proceeding with listener setup")
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error checking project existence: ${e.message}")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val listener = firestore.collection("projects")
            .document(projectId)
            .collection("expenses")
            .whereEqualTo("userId", userId)
            .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpenseRepository", "❌ Error fetching user expenses for project: ${error.message}")
                    Log.e("ExpenseRepository", "❌ Error code: ${error.code}")
                    Log.e("ExpenseRepository", "❌ Error details: ${error.localizedMessage}")
                    error.printStackTrace()
                    
                    // Don't close the flow immediately, try to send empty list and continue
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                Log.d("ExpenseRepository", "📊 Received ${snapshot?.documents?.size ?: 0} user expense documents for project $projectId")
                Log.d("ExpenseRepository", "🔍 Snapshot isEmpty: ${snapshot?.isEmpty}")
                Log.d("ExpenseRepository", "🔍 Snapshot size: ${snapshot?.size()}")
                
                // Handle empty snapshot
                if (snapshot?.isEmpty == true) {
                    Log.d("ExpenseRepository", "📭 No expenses found for user $userId in project $projectId")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val expenseData = doc.data ?: run {
                            Log.w("ExpenseRepository", "⚠️ Empty data for expense document ${doc.id}")
                            return@mapNotNull null
                        }
                        
                        Log.d("ExpenseRepository", "📝 Processing expense ${doc.id}: userId=${expenseData["userId"]}, status=${expenseData["status"]}, amount=${expenseData["amount"]}")
                        
                        // Validate that this expense belongs to the requested user
                        val expenseUserId = expenseData["userId"] as? String ?: ""
                        if (expenseUserId != userId) {
                            Log.w("ExpenseRepository", "⚠️ Expense ${doc.id} userId mismatch: expected $userId, got $expenseUserId")
                            return@mapNotNull null
                        }
                        
                        val expense = Expense(
                            id = doc.id,
                            projectId = projectId,
                            userId = expenseUserId,
                            userName = expenseData["userName"] as? String ?: "",
                            date = expenseData["date"] as? Timestamp,
                            amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                            department = expenseData["department"] as? String ?: "",
                            category = expenseData["category"] as? String ?: "",
                            description = expenseData["description"] as? String ?: "",
                            modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                            tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                            gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                            netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                            attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                            attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                            status = when (expenseData["status"] as? String) {
                                "APPROVED" -> ExpenseStatus.APPROVED
                                "REJECTED" -> ExpenseStatus.REJECTED
                                "DRAFT" -> ExpenseStatus.DRAFT
                                else -> ExpenseStatus.PENDING
                            },
                            submittedAt = expenseData["submittedAt"] as? Timestamp,
                            reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                            reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                            reviewComments = expenseData["reviewComments"] as? String ?: "",
                            receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                        )
                        
                        Log.d("ExpenseRepository", "✅ Mapped expense: ${expense.id} - ${expense.status} - ₹${expense.amount} - ${expense.category}")
                        expense
                    } catch (e: Exception) {
                        Log.e("ExpenseRepository", "❌ Error parsing user expense document ${doc.id}: ${e.message}")
                        e.printStackTrace()
                        null
                    }
                } ?: emptyList()
                
                // Sort expenses by submittedAt in descending order (most recent first)
                val sortedExpenses = expenses.sortedByDescending { 
                    it.submittedAt?.toDate()?.time ?: 0L 
                }
                
                // Log detailed status breakdown
                val statusBreakdown = sortedExpenses.groupBy { it.status }
                Log.d("ExpenseRepository", "📊 Status breakdown for user $userId in project $projectId:")
                statusBreakdown.forEach { (status, expenseList) ->
                    Log.d("ExpenseRepository", "  📈 $status: ${expenseList.size} expenses")
                }
                
                // Log sample expenses for debugging
                if (sortedExpenses.isNotEmpty()) {
                    Log.d("ExpenseRepository", "🎯 Sample expenses (first 3):")
                    sortedExpenses.take(3).forEach { expense ->
                        Log.d("ExpenseRepository", "  💰 ${expense.id}: ${expense.category} - ${expense.status} - ₹${expense.amount}")
                    }
                }
                
                Log.d("ExpenseRepository", "🎯 Sending ${sortedExpenses.size} user expenses for project $projectId to UI")
                val sendResult = trySend(sortedExpenses)
                Log.d("ExpenseRepository", "📤 Send result: ${sendResult.isSuccess}, isFailure: ${sendResult.isFailure}")
                
                if (sendResult.isFailure) {
                    Log.e("ExpenseRepository", "❌ Failed to send expenses to UI: ${sendResult.exceptionOrNull()?.message}")
                }
            }
        
        awaitClose { 
            Log.d("ExpenseRepository", "🔚 Closing user expenses for project listener")
            listener.remove() 
        }
    }
    
    // Direct query method for debugging/fallback
    suspend fun getUserExpensesForProjectDirect(projectId: String, userId: String): List<Expense> {
        return try {
            Log.d("ExpenseRepository", "🔍 Direct query for user $userId expenses in project $projectId")
            
            val snapshot = firestore.collection("projects")
                .document(projectId)
                .collection("expenses")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            Log.d("ExpenseRepository", "📊 Direct query returned ${snapshot.documents.size} documents")
            
            val expenses = snapshot.documents.mapNotNull { doc ->
                try {
                    val expenseData = doc.data ?: return@mapNotNull null
                    Log.d("ExpenseRepository", "🔍 Direct expense ${doc.id}: status=${expenseData["status"]}, amount=${expenseData["amount"]}")
                    
                    Expense(
                        id = doc.id,
                        projectId = projectId,
                        userId = expenseData["userId"] as? String ?: "",
                        userName = expenseData["userName"] as? String ?: "",
                        date = expenseData["date"] as? Timestamp,
                        amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                        department = expenseData["department"] as? String ?: "",
                        category = expenseData["category"] as? String ?: "",
                        description = expenseData["description"] as? String ?: "",
                        modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                        tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                        gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                        netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                        attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                        attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                        status = when (expenseData["status"] as? String) {
                            "APPROVED" -> ExpenseStatus.APPROVED
                            "REJECTED" -> ExpenseStatus.REJECTED
                            "DRAFT" -> ExpenseStatus.DRAFT
                            else -> ExpenseStatus.PENDING
                        },
                        submittedAt = expenseData["submittedAt"] as? Timestamp,
                        reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                        reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                        reviewComments = expenseData["reviewComments"] as? String ?: "",
                        receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("ExpenseRepository", "❌ Error parsing direct expense ${doc.id}: ${e.message}")
                    null
                }
            }
            
            val sortedExpenses = expenses.sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
            Log.d("ExpenseRepository", "✅ Direct query result: ${sortedExpenses.size} expenses")
            
            sortedExpenses
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error in direct query: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // Fallback method to get pending expenses without real-time listeners
    suspend fun getPendingExpensesDirectly(): List<Expense> {
        return try {
            Log.d("ExpenseRepository", "🔄 Direct query for pending expenses (fallback method)")
            
            // First get all projects
            val projectsSnapshot = firestore.collection("projects").get().await()
            Log.d("ExpenseRepository", "📊 Direct query found ${projectsSnapshot.documents.size} projects")
            
            if (projectsSnapshot.isEmpty) {
                Log.w("ExpenseRepository", "⚠️ No projects found in direct query")
                return emptyList()
            }
            
            val allExpenses = mutableListOf<Expense>()
            
            for (projectDoc in projectsSnapshot.documents) {
                val projectId = projectDoc.id
                Log.d("ExpenseRepository", "🔍 Querying expenses for project: $projectId")
                
                try {
                    val expensesSnapshot = firestore.collection("projects")
                        .document(projectId)
                        .collection("expenses")
                        .whereEqualTo("status", ExpenseStatus.PENDING.name)
                        .get()
                        .await()
                    
                    Log.d("ExpenseRepository", "📦 Project $projectId has ${expensesSnapshot.documents.size} pending expenses")
                    
                    val projectExpenses = expensesSnapshot.documents.mapNotNull { doc ->
                        try {
                            val expenseData = doc.data ?: return@mapNotNull null
                            Expense(
                                id = doc.id,
                                projectId = projectId,
                                userId = expenseData["userId"] as? String ?: "",
                                userName = expenseData["userName"] as? String ?: "",
                                date = expenseData["date"] as? Timestamp,
                                amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
                                department = expenseData["department"] as? String ?: "",
                                category = expenseData["category"] as? String ?: "",
                                description = expenseData["description"] as? String ?: "",
                                modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
                                tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
                                gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
                                netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
                                attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
                                attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
                                status = when (expenseData["status"] as? String) {
                                    "APPROVED" -> ExpenseStatus.APPROVED
                                    "REJECTED" -> ExpenseStatus.REJECTED
                                    "DRAFT" -> ExpenseStatus.DRAFT
                                    else -> ExpenseStatus.PENDING
                                },
                                submittedAt = expenseData["submittedAt"] as? Timestamp,
                                reviewedAt = expenseData["reviewedAt"] as? Timestamp,
                                reviewedBy = expenseData["reviewedBy"] as? String ?: "",
                                reviewComments = expenseData["reviewComments"] as? String ?: "",
                                receiptNumber = expenseData["receiptNumber"] as? String ?: ""
                            )
                        } catch (e: Exception) {
                            Log.w("ExpenseRepository", "⚠️ Error mapping expense ${doc.id} in direct query: ${e.message}")
                            null
                        }
                    }
                    
                    allExpenses.addAll(projectExpenses)
                    Log.d("ExpenseRepository", "✅ Added ${projectExpenses.size} expenses from project $projectId")
                    
                } catch (e: Exception) {
                    Log.e("ExpenseRepository", "❌ Error querying project $projectId: ${e.message}")
                    e.printStackTrace()
                }
            }
            
            val sortedExpenses = allExpenses.sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
            Log.d("ExpenseRepository", "🎯 Direct query completed: ${sortedExpenses.size} total pending expenses")
            
            if (sortedExpenses.isNotEmpty()) {
                Log.d("ExpenseRepository", "📋 Sample expenses from direct query: ${sortedExpenses.take(3).map { "${it.id} - ${it.department}/${it.category}" }}")
            }
            
            sortedExpenses
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "❌ Error in direct pending expenses query: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
} 