package com.deeksha.avr.di

import android.content.Context
import com.deeksha.avr.repository.ExpenseRepository
import com.deeksha.avr.repository.ExportRepository
import com.deeksha.avr.repository.ProjectRepository
import com.deeksha.avr.repository.NotificationRepository
import com.deeksha.avr.repository.AuthRepository
import com.deeksha.avr.service.NotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
    
    @Provides
    @Singleton
    fun provideExpenseRepository(firestore: FirebaseFirestore): ExpenseRepository {
        return ExpenseRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideProjectRepository(firestore: FirebaseFirestore): ProjectRepository {
        return ProjectRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideNotificationRepository(firestore: FirebaseFirestore): NotificationRepository {
        return NotificationRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideAuthRepository(firestore: FirebaseFirestore): AuthRepository {
        return AuthRepository(firestore)
    }
    
    @Provides
    @Singleton
    fun provideNotificationService(
        notificationRepository: NotificationRepository,
        projectRepository: ProjectRepository,
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ): NotificationService {
        return NotificationService(notificationRepository, projectRepository, authRepository, firestore)
    }
    
    @Provides
    @Singleton
    fun provideExportRepository(@ApplicationContext context: Context): ExportRepository {
        return ExportRepository(context)
    }
    
    // ViewModels are automatically injected by Hilt, but we can provide additional dependencies here if needed
} 