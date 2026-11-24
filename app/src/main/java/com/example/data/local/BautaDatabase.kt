package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LocaleEntity::class,
        ActorEntity::class,
        SettlementEntity::class,
        PaymentEventEntity::class,
        ManagerUserEntity::class,
        InvoiceEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class BautaDatabase : RoomDatabase() {
    abstract fun bautaDao(): BautaDao

    companion object {
        @Volatile
        private var INSTANCE: BautaDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS invoices (
                        id TEXT NOT NULL PRIMARY KEY,
                        invoiceNumber TEXT NOT NULL,
                        paymentEventId TEXT NOT NULL,
                        settlementId TEXT NOT NULL,
                        actorId TEXT NOT NULL,
                        actorName TEXT NOT NULL,
                        actorTaxId TEXT NOT NULL,
                        concept TEXT NOT NULL,
                        periodYear INTEGER NOT NULL,
                        periodMonth INTEGER NOT NULL,
                        profitShareCents INTEGER NOT NULL,
                        spaceRentCents INTEGER NOT NULL,
                        totalAmountCents INTEGER NOT NULL,
                        paymentChannel TEXT NOT NULL,
                        paymentReference TEXT NOT NULL,
                        debitAccountCode TEXT NOT NULL,
                        creditAccountCode TEXT NOT NULL,
                        costCenter TEXT NOT NULL,
                        issuedByUsername TEXT NOT NULL,
                        exportedToVersat INTEGER NOT NULL,
                        issueDateMillis INTEGER NOT NULL,
                        exportedAtMillis INTEGER
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE actors ADD COLUMN actorEmail TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): BautaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BautaDatabase::class.java,
                    "comercio_bauta.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
