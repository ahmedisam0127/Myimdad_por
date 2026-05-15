package com.myimdad_por.di

import com.myimdad_por.data.repository.AccountingRepositoryImpl
import com.myimdad_por.data.repository.AuditLogRepositoryImpl
import com.myimdad_por.data.repository.CustomerRepositoryImpl
import com.myimdad_por.data.repository.DashboardRepositoryImpl
import com.myimdad_por.data.repository.EmployeeRepositoryImpl
import com.myimdad_por.data.repository.ExpenseRepositoryImpl
import com.myimdad_por.data.repository.InvoiceRepositoryImpl
import com.myimdad_por.data.repository.PaymentRepositoryImpl
import com.myimdad_por.data.repository.ProductRepositoryImpl
import com.myimdad_por.data.repository.PurchaseRepositoryImpl
import com.myimdad_por.data.repository.ReportsRepositoryImpl
import com.myimdad_por.data.repository.ReturnRepositoryImpl
import com.myimdad_por.data.repository.SalesRepositoryImpl
import com.myimdad_por.data.repository.StockRepositoryImpl
import com.myimdad_por.data.repository.SubscriptionRepositoryImpl
import com.myimdad_por.data.repository.SupplierRepositoryImpl
import com.myimdad_por.domain.repository.AccountingRepository
import com.myimdad_por.domain.repository.AuditLogRepository
import com.myimdad_por.domain.repository.CustomerRepository
import com.myimdad_por.domain.repository.DashboardRepository
import com.myimdad_por.domain.repository.EmployeeRepository
import com.myimdad_por.domain.repository.ExpenseRepository
import com.myimdad_por.domain.repository.InvoiceRepository
import com.myimdad_por.domain.repository.PaymentRepository
import com.myimdad_por.domain.repository.ProductRepository
import com.myimdad_por.domain.repository.PurchaseRepository
import com.myimdad_por.domain.repository.ReportsRepository
import com.myimdad_por.domain.repository.ReturnRepository
import com.myimdad_por.domain.repository.SalesRepository
import com.myimdad_por.domain.repository.StockRepository
import com.myimdad_por.domain.repository.SubscriptionRepository
import com.myimdad_por.domain.repository.SupplierRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * RepositoryModule: المسار المركزي لربط الواجهات (Interfaces) بتطبيقاتها (Implementations).
 * يتم تثبيته في [SingletonComponent] لضمان توفر نسخة واحدة (Singleton) من كل Repository
 * على مستوى دورة حياة التطبيق بالكامل.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountingRepository(
        impl: AccountingRepositoryImpl
    ): AccountingRepository

    @Binds
    @Singleton
    abstract fun bindAuditLogRepository(
        impl: AuditLogRepositoryImpl
    ): AuditLogRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(
        impl: CustomerRepositoryImpl
    ): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        impl: DashboardRepositoryImpl
    ): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindEmployeeRepository(
        impl: EmployeeRepositoryImpl
    ): EmployeeRepository

    @Binds
    @Singleton
    abstract fun bindExpenseRepository(
        impl: ExpenseRepositoryImpl
    ): ExpenseRepository

    @Binds
    @Singleton
    abstract fun bindInvoiceRepository(
        impl: InvoiceRepositoryImpl
    ): InvoiceRepository

    @Binds
    @Singleton
    abstract fun bindPaymentRepository(
        impl: PaymentRepositoryImpl
    ): PaymentRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        impl: ProductRepositoryImpl
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindPurchaseRepository(
        impl: PurchaseRepositoryImpl
    ): PurchaseRepository

    @Binds
    @Singleton
    abstract fun bindReportsRepository(
        impl: ReportsRepositoryImpl
    ): ReportsRepository

    @Binds
    @Singleton
    abstract fun bindReturnRepository(
        impl: ReturnRepositoryImpl
    ): ReturnRepository

    @Binds
    @Singleton
    abstract fun bindSalesRepository(
        impl: SalesRepositoryImpl
    ): SalesRepository

    @Binds
    @Singleton
    abstract fun bindStockRepository(
        impl: StockRepositoryImpl
    ): StockRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        impl: SubscriptionRepositoryImpl
    ): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindSupplierRepository(
        impl: SupplierRepositoryImpl
    ): SupplierRepository

    // TODO: أضف RegisterRepositoryImpl عندما يصبح جاهزًا.
}