package com.myimdad_por.di

import android.content.Context
import com.myimdad_por.core.network.ConnectivityObserver
import com.myimdad_por.core.network.NetworkConnectivityObserver
import com.myimdad_por.core.security.SessionManager
import com.myimdad_por.data.worker.WorkerScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ملاحظة: تم حذف provideAppDispatchers من هنا لأنها موجودة في DispatcherModule
    // لضمان عدم تكرار التعريف (Duplicate Bindings) في Dagger Hilt.

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: Context
    ): ConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }

    @Provides
    @Singleton
    fun provideWorkerScheduler(
        @ApplicationContext context: Context
    ): WorkerScheduler {
        return WorkerScheduler(context)
    }

    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context
    ): SessionManager {
        SessionManager.init(context)
        return SessionManager
    }
}
