package com.deeksha.avr.di

import android.content.Context
import androidx.work.WorkManager
import com.deeksha.avr.repository.ExpenseRepository
import com.deeksha.avr.repository.ExportRepository
import com.deeksha.avr.repository.ProfessionalExportRepository
import com.deeksha.avr.repository.ProjectRepository
import com.deeksha.avr.repository.NotificationRepository
import com.deeksha.avr.repository.AuthRepository
import com.deeksha.avr.repository.TemporaryApproverRepository
import com.deeksha.avr.service.NotificationService
import com.deeksha.avr.service.DelegationExpiryService
import com.deeksha.avr.service.DelegationScheduler
import com.deeksha.avr.service.ProfessionalReportGenerator
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
    fun provideTemporaryApproverRepository(
        firestore: FirebaseFirestore,
        notificationService: NotificationService
    ): TemporaryApproverRepository {
        return TemporaryApproverRepository(firestore, notificationService)
    }
    
    @Provides
    @Singleton
    fun provideProjectRepository(
        firestore: FirebaseFirestore,
        temporaryApproverRepository: TemporaryApproverRepository
    ): ProjectRepository {
        return ProjectRepository(firestore, temporaryApproverRepository)
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
        authRepository: AuthRepository,
        firestore: FirebaseFirestore
    ): NotificationService {
        return NotificationService(notificationRepository, authRepository, firestore)
    }
    
    @Provides
    @Singleton
    fun provideExportRepository(@ApplicationContext context: Context): ExportRepository {
        return ExportRepository(context, provideProfessionalReportGenerator(context))
    }
    
    @Provides
    @Singleton
    fun provideProfessionalReportGenerator(@ApplicationContext context: Context): ProfessionalReportGenerator {
        return ProfessionalReportGenerator(context)
    }
    
    @Provides
    @Singleton
    fun provideProfessionalExportRepository(
        @ApplicationContext context: Context,
        professionalReportGenerator: ProfessionalReportGenerator
    ): ProfessionalExportRepository {
        return ProfessionalExportRepository(context, professionalReportGenerator)
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideDelegationExpiryService(
        firestore: FirebaseFirestore,
        projectRepository: ProjectRepository,
        temporaryApproverRepository: TemporaryApproverRepository,
        notificationService: NotificationService
    ): DelegationExpiryService {
        return DelegationExpiryService(firestore, projectRepository, temporaryApproverRepository, notificationService)
    }
    
    @Provides
    @Singleton
    fun provideDelegationScheduler(
        @ApplicationContext context: Context,
        workManager: WorkManager
    ): DelegationScheduler {
        return DelegationScheduler(context, workManager)
    }
    
    // ViewModels are automatically injected by Hilt, but we can provide additional dependencies here if needed
} 