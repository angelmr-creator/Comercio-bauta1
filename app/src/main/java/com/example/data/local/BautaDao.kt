package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BautaDao {
    @Query("SELECT * FROM locales ORDER BY code COLLATE NOCASE ASC")
    fun observeLocales(): Flow<List<LocaleEntity>>

    @Query("SELECT * FROM locales WHERE id = :id LIMIT 1")
    suspend fun getLocaleById(id: String): LocaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocale(entity: LocaleEntity)

    @Query("UPDATE locales SET status = :status, updatedAtMillis = :updatedAt WHERE id = :id")
    suspend fun updateLocaleStatus(id: String, status: String, updatedAt: Long)

    @Query("DELETE FROM locales WHERE id = :id")
    suspend fun deleteLocaleById(id: String)

    @Query("SELECT * FROM actors ORDER BY name COLLATE NOCASE ASC")
    fun observeActors(): Flow<List<ActorEntity>>

    @Query("SELECT * FROM actors WHERE id = :id LIMIT 1")
    suspend fun getActorById(id: String): ActorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActor(entity: ActorEntity)

    @Query("DELETE FROM actors WHERE id = :id")
    suspend fun deleteActorById(id: String)

    @Query("SELECT * FROM settlements ORDER BY periodYear DESC, periodMonth DESC, actorName COLLATE NOCASE ASC")
    fun observeSettlements(): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE id = :id LIMIT 1")
    suspend fun getSettlementById(id: String): SettlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettlement(entity: SettlementEntity)

    @Query(
        "UPDATE settlements SET paidCents = :paidCents, paymentStatus = :status, " +
            "paymentChannel = :channel, paymentReference = :reference, paidAtMillis = :paidAt, " +
            "updatedAtMillis = :updatedAt WHERE id = :id"
    )
    suspend fun updateSettlementPayment(
        id: String,
        paidCents: Long,
        status: String,
        channel: String,
        reference: String,
        paidAt: Long?,
        updatedAt: Long
    )

    @Query("DELETE FROM settlements WHERE id = :id")
    suspend fun deleteSettlementById(id: String)

    // Real-time payment feed
    @Query("SELECT * FROM payment_events ORDER BY createdAtMillis DESC")
    fun observePaymentEvents(): Flow<List<PaymentEventEntity>>

    @Query("SELECT * FROM payment_events WHERE id = :id LIMIT 1")
    suspend fun getPaymentEventById(id: String): PaymentEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPaymentEvent(entity: PaymentEventEntity)

    @Query(
        "UPDATE payment_events SET verificationStatus = :status, verifiedByUsername = :verifiedBy, " +
            "verifiedAtMillis = :verifiedAt, note = :note WHERE id = :id"
    )
    suspend fun updatePaymentEventVerification(
        id: String,
        status: String,
        verifiedBy: String,
        verifiedAt: Long?,
        note: String
    )

    // Local Manager user accounts
    @Query("SELECT * FROM manager_users ORDER BY createdAtMillis ASC")
    fun observeManagerUsers(): Flow<List<ManagerUserEntity>>

    @Query("SELECT * FROM manager_users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getManagerUserByUsername(username: String): ManagerUserEntity?

    @Query("SELECT * FROM manager_users WHERE id = :id LIMIT 1")
    suspend fun getManagerUserById(id: String): ManagerUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertManagerUser(entity: ManagerUserEntity)

    @Query("UPDATE manager_users SET lastLoginAtMillis = :loginAt WHERE id = :id")
    suspend fun updateManagerLastLogin(id: String, loginAt: Long)

    @Query("UPDATE manager_users SET active = :active WHERE id = :id")
    suspend fun updateManagerUserActive(id: String, active: Boolean)

    @Query("UPDATE manager_users SET passwordHash = :hash, passwordSalt = :salt WHERE id = :id")
    suspend fun updateManagerPassword(id: String, hash: String, salt: String)

    @Query("SELECT COUNT(*) FROM manager_users WHERE active = 1")
    suspend fun countActiveManagers(): Int

    // Invoices for Versat Sarasola export
    @Query("SELECT * FROM invoices ORDER BY issueDateMillis DESC")
    fun observeInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: String): InvoiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(entity: InvoiceEntity)

    @Query("UPDATE invoices SET exportedToVersat = 1, exportedAtMillis = :exportedAt WHERE id IN (:ids)")
    suspend fun markInvoicesExported(ids: List<String>, exportedAt: Long)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: String)
}
