package com.myimdad_por.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myimdad_por.data.local.dao.AuditLogDao
import com.myimdad_por.data.local.dao.CustomerDao
import com.myimdad_por.data.local.dao.InvoiceDao
import com.myimdad_por.data.local.dao.PaymentDao
import com.myimdad_por.data.local.dao.PendingSyncDao
import com.myimdad_por.data.local.dao.ProductDao
import com.myimdad_por.data.local.dao.PurchaseDao
import com.myimdad_por.data.local.dao.ReportDao
import com.myimdad_por.data.local.dao.ReturnDao
import com.myimdad_por.data.local.dao.StockDao
import com.myimdad_por.data.local.entity.AuditLogEntity
import com.myimdad_por.data.local.entity.CustomerEntity
import com.myimdad_por.data.local.entity.InvoiceEntity
import com.myimdad_por.data.local.entity.PaymentTransactionEntity
import com.myimdad_por.data.local.entity.PendingSyncEntity
import com.myimdad_por.data.local.entity.ProductEntity
import com.myimdad_por.data.local.entity.PurchaseEntity
import com.myimdad_por.data.local.entity.ReportEntity
import com.myimdad_por.data.local.entity.ReturnEntity
import com.myimdad_por.data.local.entity.StockEntity

@Database(
    entities = [
        InvoiceEntity::class,
        PaymentTransactionEntity::class,
        PendingSyncEntity::class,
        ProductEntity::class,
        PurchaseEntity::class,
        ReturnEntity::class,
        StockEntity::class,
        CustomerEntity::class,
        AuditLogEntity::class,
        ReportEntity::class
    ],
    version = DatabaseMigrations.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun invoiceDao(): InvoiceDao

    abstract fun paymentDao(): PaymentDao

    abstract fun pendingSyncDao(): PendingSyncDao

    abstract fun productDao(): ProductDao

    abstract fun purchaseDao(): PurchaseDao

    abstract fun returnDao(): ReturnDao

    abstract fun stockDao(): StockDao

    abstract fun customerDao(): CustomerDao

    abstract fun auditLogDao(): AuditLogDao

    abstract fun reportDao(): ReportDao
}