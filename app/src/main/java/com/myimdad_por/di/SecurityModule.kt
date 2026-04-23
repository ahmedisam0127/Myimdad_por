package com.myimdad_por.di

import android.content.Context
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.myimdad_por.core.payment.security.PaymentTokenVault
import com.myimdad_por.core.security.AppSignatureVerifier
import com.myimdad_por.core.security.CryptoManager
import com.myimdad_por.core.security.IntegrityChecker
import com.myimdad_por.core.security.SecurePrefs
import com.myimdad_por.core.security.TamperProtection
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideSecurePrefs(
        @ApplicationContext context: Context
    ): SecurePrefs {
        initializeSecurity(context)
        return SecurePrefs
    }

    @Provides
    @Singleton
    fun providePaymentTokenVault(
        @ApplicationContext context: Context
    ): PaymentTokenVault {
        initializeSecurity(context)
        return PaymentTokenVault
    }

    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager = CryptoManager

    @Provides
    @Singleton
    fun provideTamperProtection(): TamperProtection = TamperProtection

    @Provides
    @Singleton
    fun provideAppSignatureVerifier(): AppSignatureVerifier = AppSignatureVerifier

    @Provides
    @Singleton
    fun provideIntegrityChecker(): IntegrityChecker = IntegrityChecker

    @Provides
    @Singleton
    fun provideIntegrityManager(
        @ApplicationContext context: Context
    ): IntegrityManager {
        return IntegrityManagerFactory.create(context.applicationContext)
    }

    private fun initializeSecurity(context: Context) {
        val appContext = context.applicationContext
        SecurePrefs.init(appContext)
        PaymentTokenVault.init(appContext)
    }
}