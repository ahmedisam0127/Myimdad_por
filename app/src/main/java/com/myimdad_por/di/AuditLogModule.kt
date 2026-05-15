package com.myimdad_por.di

import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.data.local.dao.AuditLogDao
import com.myimdad_por.data.remote.api.ApiService
import com.myimdad_por.data.remote.datasource.AuditLogRemoteDataSource
import com.myimdad_por.data.repository.AuditLogRepositoryImpl
import com.myimdad_por.domain.repository.AuditLogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuditLogModule {

    // تم حذف provideAppDispatchers من هنا لمنع تكرار التعريف مع DispatcherModule

    @Provides
    @Singleton
    fun provideAuditLogRemoteDataSource(
        apiService: ApiService
    ): AuditLogRemoteDataSource {
        return AuditLogRemoteDataSource(apiService)
    }

    @Provides
    @Singleton
    fun provideAuditLogRepository(
        auditLogDao: AuditLogDao,
        remoteDataSource: AuditLogRemoteDataSource,
        dispatchers: AppDispatchers // سيقوم Hilt بجلبها تلقائياً من DispatcherModule
    ): AuditLogRepository {
        return AuditLogRepositoryImpl(
            auditLogDao = auditLogDao,
            remoteDataSource = remoteDataSource,
            dispatchers = dispatchers
        )
    }
}
