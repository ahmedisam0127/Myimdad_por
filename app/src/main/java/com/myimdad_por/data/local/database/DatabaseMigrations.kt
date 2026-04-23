package com.myimdad_por.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    const val DATABASE_VERSION: Int = 5

    fun all(): Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5
    )

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            createInvoicesTable(database)
            createPaymentTransactionsTable(database)
            createPendingSyncTable(database)
            createProductsTable(database)
            createPurchasesTable(database)
            createReturnsTable(database)
            createStocksTable(database)
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            createCustomersTable(database)
            createAuditLogsTable(database)
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            createReportsTable(database)
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            createDashboardCacheTable(database)
        }
    }

    private fun createDashboardCacheTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dashboard_cache` (
                `id` INTEGER NOT NULL,
                `overviewTitle` TEXT NOT NULL DEFAULT 'لوحة التحكم',
                `overviewSubtitle` TEXT,
                `overviewGreeting` TEXT,
                `overviewTotalMetrics` INTEGER NOT NULL DEFAULT 0,
                `overviewPositiveMetrics` INTEGER NOT NULL DEFAULT 0,
                `overviewNegativeMetrics` INTEGER NOT NULL DEFAULT 0,
                `salesTodaySalesCount` INTEGER NOT NULL DEFAULT 0,
                `salesTodayRevenue` TEXT NOT NULL DEFAULT '0.00',
                `salesMonthSalesCount` INTEGER NOT NULL DEFAULT 0,
                `salesMonthRevenue` TEXT NOT NULL DEFAULT '0.00',
                `salesPendingInvoicesCount` INTEGER NOT NULL DEFAULT 0,
                `salesReturnsCount` INTEGER NOT NULL DEFAULT 0,
                `salesTopSellingProductName` TEXT,
                `salesGrowthRatePercent` TEXT,
                `inventoryProductsCount` INTEGER NOT NULL DEFAULT 0,
                `inventoryLowStockCount` INTEGER NOT NULL DEFAULT 0,
                `inventoryOutOfStockCount` INTEGER NOT NULL DEFAULT 0,
                `inventoryTotalStockValue` TEXT NOT NULL DEFAULT '0.00',
                `inventoryReservedItemsCount` INTEGER NOT NULL DEFAULT 0,
                `inventoryMostCriticalProductName` TEXT,
                `customersCount` INTEGER NOT NULL DEFAULT 0,
                `customersNewCustomersCount` INTEGER NOT NULL DEFAULT 0,
                `customersActiveCustomersCount` INTEGER NOT NULL DEFAULT 0,
                `customersDueCustomersCount` INTEGER NOT NULL DEFAULT 0,
                `customersTopCustomerName` TEXT,
                `customersAverageOrderValue` TEXT NOT NULL DEFAULT '0.00',
                `financialTotalCashIn` TEXT NOT NULL DEFAULT '0.00',
                `financialTotalCashOut` TEXT NOT NULL DEFAULT '0.00',
                `financialNetBalance` TEXT NOT NULL DEFAULT '0.00',
                `financialReceivables` TEXT NOT NULL DEFAULT '0.00',
                `financialPayables` TEXT NOT NULL DEFAULT '0.00',
                `financialProfitEstimate` TEXT,
                `financialCurrencyCode` TEXT NOT NULL DEFAULT 'SDG',
                `alertsJson` TEXT NOT NULL DEFAULT '[]',
                `quickActionsJson` TEXT NOT NULL DEFAULT '[]',
                `lastUpdatedAtEpochMillis` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }

    private fun createInvoicesTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `invoices` (
                `id` TEXT NOT NULL,
                `server_id` TEXT,
                `invoice_number` TEXT NOT NULL,
                `invoice_type` TEXT NOT NULL DEFAULT 'SALE',
                `party_id` TEXT,
                `party_name` TEXT,
                `party_tax_number` TEXT,
                `issued_by_employee_id` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'DRAFT',
                `payment_status` TEXT NOT NULL DEFAULT 'PENDING',
                `issue_date_millis` INTEGER NOT NULL,
                `due_date_millis` INTEGER,
                `notes` TEXT,
                `terms_and_conditions` TEXT,
                `subtotal_amount` TEXT NOT NULL DEFAULT '0.00',
                `tax_amount` TEXT NOT NULL DEFAULT '0.00',
                `discount_amount` TEXT NOT NULL DEFAULT '0.00',
                `total_amount` TEXT NOT NULL DEFAULT '0.00',
                `paid_amount` TEXT NOT NULL DEFAULT '0.00',
                `remaining_amount` TEXT NOT NULL DEFAULT '0.00',
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced_at_millis` INTEGER,
                `created_at_millis` INTEGER NOT NULL,
                `updated_at_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_server_id` ON `invoices` (`server_id`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_invoice_number` ON `invoices` (`invoice_number`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_invoice_type` ON `invoices` (`invoice_type`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_party_id` ON `invoices` (`party_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_party_name` ON `invoices` (`party_name`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_issued_by_employee_id` ON `invoices` (`issued_by_employee_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_status` ON `invoices` (`status`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_payment_status` ON `invoices` (`payment_status`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_issue_date_millis` ON `invoices` (`issue_date_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_due_date_millis` ON `invoices` (`due_date_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_invoices_sync_state` ON `invoices` (`sync_state`)""")
    }

    private fun createPaymentTransactionsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `payment_transactions` (
                `id` TEXT NOT NULL,
                `server_id` TEXT,
                `transaction_id` TEXT NOT NULL,
                `payment_intent_id` TEXT,
                `reference_number` TEXT,
                `payment_method_id` TEXT,
                `payment_method_name` TEXT,
                `payment_method_type` TEXT,
                `payment_method_requires_reference` INTEGER NOT NULL DEFAULT 0,
                `payment_method_extra_fees` TEXT NOT NULL DEFAULT '0.00',
                `payment_method_supported_currencies_json` TEXT NOT NULL DEFAULT '[]',
                `payment_method_is_active` INTEGER NOT NULL DEFAULT 1,
                `amount` TEXT NOT NULL,
                `currency_code` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'PENDING',
                `provider_name` TEXT,
                `provider_reference` TEXT,
                `receipt_number` TEXT,
                `authorized_at_millis` INTEGER,
                `captured_at_millis` INTEGER,
                `refunded_at_millis` INTEGER,
                `failure_reason` TEXT,
                `metadata_json` TEXT NOT NULL DEFAULT '{}',
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced_at_millis` INTEGER,
                `created_at_millis` INTEGER NOT NULL,
                `updated_at_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_transactions_server_id` ON `payment_transactions` (`server_id`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_payment_transactions_transaction_id` ON `payment_transactions` (`transaction_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_payment_transactions_payment_intent_id` ON `payment_transactions` (`payment_intent_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_payment_transactions_reference_number` ON `payment_transactions` (`reference_number`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_payment_transactions_payment_method_id` ON `payment_transactions` (`payment_method_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_payment_transactions_status` ON `payment_transactions` (`status`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_payment_transactions_currency_code` ON `payment_transactions` (`currency_code`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_payment_transactions_sync_state` ON `payment_transactions` (`sync_state`)""")
    }

    private fun createPendingSyncTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_sync` (
                `id` TEXT NOT NULL,
                `entity_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `operation` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'PENDING',
                `priority` INTEGER NOT NULL DEFAULT 0,
                `attempt_count` INTEGER NOT NULL DEFAULT 0,
                `max_retry_count` INTEGER NOT NULL DEFAULT 10,
                `payload_json` TEXT NOT NULL DEFAULT '{}',
                `last_error_message` TEXT,
                `next_attempt_at_millis` INTEGER,
                `locked_at_millis` INTEGER,
                `completed_at_millis` INTEGER,
                `metadata_json` TEXT NOT NULL DEFAULT '{}',
                `created_at_millis` INTEGER NOT NULL,
                `updated_at_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_pending_sync_entity_type` ON `pending_sync` (`entity_type`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_pending_sync_entity_id` ON `pending_sync` (`entity_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_pending_sync_operation` ON `pending_sync` (`operation`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_pending_sync_status` ON `pending_sync` (`status`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_pending_sync_next_attempt_at_millis` ON `pending_sync` (`next_attempt_at_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_pending_sync_priority` ON `pending_sync` (`priority`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_pending_sync_entity_type_entity_id_operation` ON `pending_sync` (`entity_type`, `entity_id`, `operation`)""")
    }

    private fun createProductsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `products` (
                `id` TEXT NOT NULL,
                `server_id` TEXT,
                `barcode` TEXT NOT NULL,
                `normalized_barcode` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `display_name` TEXT,
                `description` TEXT,
                `unit_of_measure` TEXT NOT NULL DEFAULT 'UNIT',
                `price` TEXT NOT NULL,
                `is_active` INTEGER NOT NULL DEFAULT 1,
                `search_tokens` TEXT NOT NULL DEFAULT '',
                `metadata_json` TEXT NOT NULL DEFAULT '{}',
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced_at_millis` INTEGER,
                `created_at_millis` INTEGER NOT NULL,
                `updated_at_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_products_server_id` ON `products` (`server_id`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_products_normalized_barcode` ON `products` (`normalized_barcode`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_products_name` ON `products` (`name`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_products_display_name` ON `products` (`display_name`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_products_unit_of_measure` ON `products` (`unit_of_measure`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_products_is_active` ON `products` (`is_active`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_products_sync_state` ON `products` (`sync_state`)""")
    }

    private fun createPurchasesTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `purchases` (
                `id` TEXT NOT NULL,
                `server_id` TEXT,
                `invoice_number` TEXT NOT NULL,
                `supplier_id` TEXT NOT NULL,
                `supplier_name` TEXT NOT NULL,
                `employee_id` TEXT NOT NULL,
                `items_json` TEXT NOT NULL,
                `subtotal_amount` TEXT NOT NULL,
                `tax_amount` TEXT NOT NULL,
                `discount_amount` TEXT NOT NULL,
                `total_amount` TEXT NOT NULL,
                `paid_amount` TEXT NOT NULL DEFAULT '0.00',
                `remaining_amount` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'DRAFT',
                `payment_status` TEXT NOT NULL DEFAULT 'PENDING',
                `created_at_millis` INTEGER NOT NULL,
                `due_date_millis` INTEGER,
                `note` TEXT,
                `metadata_json` TEXT NOT NULL DEFAULT '{}',
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced_at_millis` INTEGER,
                `updated_at_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_purchases_server_id` ON `purchases` (`server_id`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_purchases_invoice_number` ON `purchases` (`invoice_number`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_purchases_supplier_id` ON `purchases` (`supplier_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_purchases_supplier_name` ON `purchases` (`supplier_name`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_purchases_employee_id` ON `purchases` (`employee_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_purchases_status` ON `purchases` (`status`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_purchases_payment_status` ON `purchases` (`payment_status`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_purchases_created_at_millis` ON `purchases` (`created_at_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_purchases_due_date_millis` ON `purchases` (`due_date_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_purchases_sync_state` ON `purchases` (`sync_state`)""")
    }

    private fun createReturnsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `returns` (
                `id` TEXT NOT NULL,
                `server_id` TEXT,
                `return_number` TEXT NOT NULL,
                `return_type` TEXT NOT NULL,
                `original_document_id` TEXT,
                `original_document_number` TEXT,
                `party_id` TEXT,
                `party_name` TEXT,
                `processed_by_employee_id` TEXT NOT NULL,
                `items_json` TEXT NOT NULL,
                `return_date_millis` INTEGER NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'DRAFT',
                `refund_status` TEXT NOT NULL DEFAULT 'PENDING',
                `subtotal_amount` TEXT NOT NULL,
                `tax_amount` TEXT NOT NULL,
                `discount_amount` TEXT NOT NULL,
                `total_refund_amount` TEXT NOT NULL,
                `refunded_amount` TEXT NOT NULL DEFAULT '0.00',
                `remaining_refund_amount` TEXT NOT NULL,
                `reason` TEXT,
                `note` TEXT,
                `metadata_json` TEXT NOT NULL DEFAULT '{}',
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced_at_millis` INTEGER,
                `updated_at_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_returns_server_id` ON `returns` (`server_id`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_returns_return_number` ON `returns` (`return_number`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_returns_return_type` ON `returns` (`return_type`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_returns_status` ON `returns` (`status`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_returns_refund_status` ON `returns` (`refund_status`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_returns_party_id` ON `returns` (`party_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_returns_processed_by_employee_id` ON `returns` (`processed_by_employee_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_returns_return_date_millis` ON `returns` (`return_date_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_returns_sync_state` ON `returns` (`sync_state`)""")
    }

    private fun createStocksTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `stocks` (
                `id` TEXT NOT NULL,
                `server_id` TEXT,
                `product_barcode` TEXT NOT NULL,
                `normalized_barcode` TEXT NOT NULL,
                `product_name` TEXT NOT NULL,
                `display_name` TEXT,
                `location` TEXT NOT NULL,
                `normalized_location` TEXT NOT NULL,
                `unit_of_measure` TEXT NOT NULL DEFAULT 'UNIT',
                `quantity` REAL NOT NULL,
                `expiry_date_millis` INTEGER,
                `metadata_json` TEXT NOT NULL DEFAULT '{}',
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced_at_millis` INTEGER,
                `created_at_millis` INTEGER NOT NULL,
                `updated_at_millis` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_stocks_server_id` ON `stocks` (`server_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_stocks_normalized_barcode` ON `stocks` (`normalized_barcode`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_stocks_normalized_location` ON `stocks` (`normalized_location`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_stocks_product_name` ON `stocks` (`product_name`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_stocks_unit_of_measure` ON `stocks` (`unit_of_measure`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_stocks_expiry_date_millis` ON `stocks` (`expiry_date_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_stocks_sync_state` ON `stocks` (`sync_state`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_stocks_normalized_barcode_normalized_location` ON `stocks` (`normalized_barcode`, `normalized_location`)""")
    }

    private fun createCustomersTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `customers` (
                `id` TEXT NOT NULL,
                `server_id` TEXT,
                `code` TEXT,
                `full_name` TEXT NOT NULL,
                `trade_name` TEXT,
                `phone_number` TEXT,
                `email` TEXT,
                `address` TEXT,
                `city` TEXT,
                `country` TEXT,
                `tax_number` TEXT,
                `national_id` TEXT,
                `credit_limit` TEXT NOT NULL,
                `outstanding_balance` TEXT NOT NULL,
                `is_active` INTEGER NOT NULL DEFAULT 1,
                `created_at_millis` INTEGER NOT NULL,
                `updated_at_millis` INTEGER NOT NULL,
                `last_purchase_at_millis` INTEGER,
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `is_deleted` INTEGER NOT NULL DEFAULT 0,
                `synced_at_millis` INTEGER,
                `metadata_json` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_server_id` ON `customers` (`server_id`)""")
        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_customers_code` ON `customers` (`code`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_customers_full_name` ON `customers` (`full_name`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_customers_trade_name` ON `customers` (`trade_name`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_customers_phone_number` ON `customers` (`phone_number`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_customers_email` ON `customers` (`email`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_customers_national_id` ON `customers` (`national_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_customers_tax_number` ON `customers` (`tax_number`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_customers_is_active` ON `customers` (`is_active`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_customers_sync_state` ON `customers` (`sync_state`)""")
    }

    private fun createAuditLogsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `audit_logs` (
                `log_id` TEXT NOT NULL,
                `server_id` TEXT,
                `timestamp_millis` INTEGER NOT NULL,
                `action` TEXT NOT NULL,
                `severity` TEXT NOT NULL,
                `actor_type` TEXT NOT NULL,
                `actor_label` TEXT NOT NULL,
                `target_type` TEXT NOT NULL,
                `target_label` TEXT NOT NULL,
                `context_json` TEXT,
                `changes_json` TEXT,
                `note` TEXT,
                `correlation_id` TEXT,
                `metadata_json` TEXT,
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `created_at_millis` INTEGER NOT NULL,
                `synced_at_millis` INTEGER,
                PRIMARY KEY(`log_id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_audit_logs_server_id` ON `audit_logs` (`server_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_audit_logs_timestamp_millis` ON `audit_logs` (`timestamp_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_audit_logs_action` ON `audit_logs` (`action`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_audit_logs_severity` ON `audit_logs` (`severity`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_audit_logs_correlation_id` ON `audit_logs` (`correlation_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_audit_logs_actor_type` ON `audit_logs` (`actor_type`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_audit_logs_target_type` ON `audit_logs` (`target_type`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_audit_logs_sync_state` ON `audit_logs` (`sync_state`)""")
    }

    private fun createReportsTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reports` (
                `report_id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `generated_at_millis` INTEGER NOT NULL,
                `from_millis` INTEGER,
                `to_millis` INTEGER,
                `filters_json` TEXT NOT NULL DEFAULT '{}',
                `data_points_json` TEXT NOT NULL DEFAULT '[]',
                `summary` TEXT,
                `is_exported` INTEGER NOT NULL DEFAULT 0,
                `export_format` TEXT,
                `generated_by_user_id` TEXT,
                `metadata_json` TEXT NOT NULL DEFAULT '{}',
                `sync_state` TEXT NOT NULL DEFAULT 'PENDING',
                `synced_at_millis` INTEGER,
                PRIMARY KEY(`report_id`)
            )
            """.trimIndent()
        )

        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_reports_type` ON `reports` (`type`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_reports_generated_at_millis` ON `reports` (`generated_at_millis`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_reports_generated_by_user_id` ON `reports` (`generated_by_user_id`)""")
        database.execSQL("""CREATE INDEX IF NOT EXISTS `index_reports_sync_state` ON `reports` (`sync_state`)""")
    }
}