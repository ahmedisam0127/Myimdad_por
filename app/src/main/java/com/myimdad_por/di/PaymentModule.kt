package com.myimdad_por.di

import android.content.Context
import com.myimdad_por.core.dispatchers.AppDispatchers
import com.myimdad_por.core.payment.contracts.IPaymentGateway
import com.myimdad_por.core.payment.contracts.IPaymentProcessor
import com.myimdad_por.core.payment.processor.PaymentProcessor
import com.myimdad_por.core.payment.processor.PaymentVerificationService
import com.myimdad_por.core.payment.processor.RefundService
import com.myimdad_por.data.local.dao.PaymentDao
import com.myimdad_por.data.remote.api.PaymentApiService
import com.myimdad_por.data.remote.datasource.PaymentRemoteDataSource
import com.myimdad_por.data.remote.datasource.PaymentVerificationRemoteDataSource
import com.myimdad_por.data.repository.PaymentRepositoryImpl
import com.myimdad_por.domain.repository.PaymentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PaymentModule {

    // --- API Services ---

    @Provides
    @Singleton
    fun providePaymentApiService(retrofit: Retrofit): PaymentApiService {
        return retrofit.create(PaymentApiService::class.java)
    }

    // --- Data Sources ---

    @Provides
    @Singleton
    fun providePaymentRemoteDataSource(
        apiService: PaymentApiService
    ): PaymentRemoteDataSource {
        return PaymentRemoteDataSource(apiService)
    }

    @Provides
    @Singleton
    fun providePaymentVerificationRemoteDataSource(
        apiService: PaymentApiService
    ): PaymentVerificationRemoteDataSource {
        return PaymentVerificationRemoteDataSource(apiService)
    }

    // --- Repository ---

    @Provides
    @Singleton
    fun providePaymentRepository(
        paymentDao: PaymentDao,
        dispatchers: AppDispatchers
    ): PaymentRepository {
        return PaymentRepositoryImpl(paymentDao, dispatchers)
    }

    // --- Payment Logic Services ---

    @Provides
    @Singleton
    fun providePaymentVerificationService(
        @ApplicationContext context: Context,
        gateway: IPaymentGateway // يجب توفير تنفيذ لـ IPaymentGateway في مكان آخر (مثل StripeModule أو FawryModule)
    ): PaymentVerificationService {
        return PaymentVerificationService(context, gateway)
    }

    @Provides
    @Singleton
    fun provideRefundService(
        @ApplicationContext context: Context,
        gateway: IPaymentGateway
    ): RefundService {
        return RefundService(context, gateway)
    }

    @Provides
    @Singleton
    fun providePaymentProcessor(
        @ApplicationContext context: Context,
        gateway: IPaymentGateway,
        verificationService: PaymentVerificationService,
        refundService: RefundService
    ): IPaymentProcessor {
        return PaymentProcessor(
            context = context,
            gateway = gateway,
            verificationService = verificationService,
            refundService = refundService
        )
    }
}
