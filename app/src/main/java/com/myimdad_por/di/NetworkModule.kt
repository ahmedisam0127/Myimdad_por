package com.myimdad_por.di
import com.myimdad_por.data.remote.api.DashboardApiService

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.myimdad_por.core.network.CertificatePinningConfig
import com.myimdad_por.core.network.SecureApiClient
import com.myimdad_por.core.utils.Constants
import com.myimdad_por.data.remote.api.*
import com.myimdad_por.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val CLIENT_NAME = "myimdad-por-android"
    private const val CLIENT_VERSION = "1.0.0"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .serializeNulls()
        .setLenient()
        .create()

    @Provides
    @Singleton
    fun provideAuthInterceptor(): AuthInterceptor {
        return AuthInterceptor(
            clientName = CLIENT_NAME,
            clientVersion = CLIENT_VERSION,
            publicPaths = setOf(
                AuthApiService.Paths.LOGIN,
                AuthApiService.Paths.REGISTER,
                AuthApiService.Paths.FORGOT_PASSWORD,
                AuthApiService.Paths.VERIFY_OTP,
                AuthApiService.Paths.RESET_PASSWORD,
                AuthApiService.Paths.REFRESH_TOKEN
            )
        )
    }

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner? {
        return if (CertificatePinningConfig.isEnabled()) {
            CertificatePinningConfig.create()
        } else {
            null
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        authInterceptor: AuthInterceptor,
        certificatePinner: CertificatePinner?
    ): OkHttpClient {
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return SecureApiClient.create(
            certificatePinner = certificatePinner,
            interceptors = listOf(authInterceptor),
            isDebug = isDebug
        )
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.Network.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // --- حقن واجهات الـ API ---

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): AuthApiService = 
        retrofit.create(AuthApiService::class.java)

    /**
     * يوفر واجهة لوحة التحكم للتعامل مع البيانات والملخصات
     * (تمت إضافتها هنا لإصلاح خطأ MissingBinding)
     */
    @Provides
    @Singleton
    fun provideDashboardApiService(retrofit: Retrofit): DashboardApiService = 
        retrofit.create(DashboardApiService::class.java)

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = 
        retrofit.create(ApiService::class.java)

    @Provides
    @Singleton
    fun provideSubscriptionApiService(retrofit: Retrofit): SubscriptionApiService = 
        retrofit.create(SubscriptionApiService::class.java)

    @Provides
    @Singleton
    fun provideReportApiService(retrofit: Retrofit): ReportApiService = 
        retrofit.create(ReportApiService::class.java)

    @Provides
    @Singleton
    fun provideCustomerApiService(retrofit: Retrofit): CustomerApiService = 
        retrofit.create(CustomerApiService::class.java)

    @Provides
    @Singleton
    fun provideInvoiceApiService(retrofit: Retrofit): InvoiceApiService = 
        retrofit.create(InvoiceApiService::class.java)

    @Provides
    @Singleton
    fun provideStockApiService(retrofit: Retrofit): StockApiService = 
        retrofit.create(StockApiService::class.java)
}
