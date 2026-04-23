package com.myimdad_por.di

import android.content.Context
import androidx.room.Room
import com.myimdad_por.data.local.dao.AuditLogDao
import com.myimdad_por.data.local.dao.CustomerDao
import com.myimdad_por.data.local.dao.DashboardDao
import com.myimdad_por.data.local.dao.InvoiceDao
import com.myimdad_por.data.local.dao.PaymentDao
import com.myimdad_por.data.local.dao.PendingSyncDao
import com.myimdad_por.data.local.dao.ProductDao
import com.myimdad_por.data.local.dao.PurchaseDao
import com.myimdad_por.data.local.dao.ReportDao
import com.myimdad_por.data.local.dao.ReturnDao
import com.myimdad_por.data.local.dao.StockDao
import com.myimdad_por.data.local.database.AppDatabase
import com.myimdad_por.data.local.database.DatabaseMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "myimdad_por_database.db"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(*DatabaseMigrations.all())
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideInvoiceDao(database: AppDatabase): InvoiceDao = database.invoiceDao()

    @Provides
    fun providePaymentDao(database: AppDatabase): PaymentDao = database.paymentDao()

    @Provides
    fun providePendingSyncDao(database: AppDatabase): PendingSyncDao = database.pendingSyncDao()

    @Provides
    fun provideProductDao(database: AppDatabase): ProductDao = database.productDao()

    @Provides
    fun providePurchaseDao(database: AppDatabase): PurchaseDao = database.purchaseDao()

    @Provides
    fun provideReturnDao(database: AppDatabase): ReturnDao = database.returnDao()

    @Provides
    fun provideStockDao(database: AppDatabase): StockDao = database.stockDao()

    @Provides
    fun provideCustomerDao(database: AppDatabase): CustomerDao = database.customerDao()

    @Provides
    fun provideAuditLogDao(database: AppDatabase): AuditLogDao = database.auditLogDao()

    @Provides
    fun provideReportDao(database: AppDatabase): ReportDao = database.reportDao()

    @Provides
    fun provideDashboardDao(database: AppDatabase): DashboardDao = database.dashboardDao()
}