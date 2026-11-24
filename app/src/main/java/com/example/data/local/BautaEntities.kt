package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.EconomicActor
import com.example.data.model.InvoiceRecord
import com.example.data.model.LocaleSpace
import com.example.data.model.ManagerUser
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentEvent

@Entity(tableName = "locales")
data class LocaleEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val address: String,
    val areaSqm: Double,
    val monthlyRentCents: Long,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): LocaleSpace = LocaleSpace(
        id = id,
        code = code,
        name = name,
        address = address,
        areaSqm = areaSqm,
        monthlyRentCents = monthlyRentCents,
        status = status,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "actors")
data class ActorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val legalType: String,
    val representativeName: String,
    val phone: String,
    val taxId: String,
    val actorEmail: String = "",
    val localeId: String,
    val localeName: String,
    val monthlyRentCents: Long,
    val profitSharePercent: Long,
    val active: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): EconomicActor = EconomicActor(
        id = id,
        name = name,
        legalType = legalType,
        representativeName = representativeName,
        phone = phone,
        taxId = taxId,
        actorEmail = actorEmail,
        localeId = localeId,
        localeName = localeName,
        monthlyRentCents = monthlyRentCents,
        profitSharePercent = profitSharePercent,
        active = active,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String,
    val actorId: String,
    val actorName: String,
    val localeId: String,
    val localeName: String,
    val periodYear: Long,
    val periodMonth: Long,
    val grossIncomeCents: Long,
    val operatingExpensesCents: Long,
    val netProfitCents: Long,
    val profitSharePercent: Long,
    val profitShareCents: Long,
    val spaceRentCents: Long,
    val totalDueCents: Long,
    val paidCents: Long,
    val paymentStatus: String,
    val declaredBy: String,
    val paymentChannel: String,
    val paymentReference: String,
    val notes: String,
    val paidAtMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toDomain(): MonthlySettlement = MonthlySettlement(
        id = id,
        actorId = actorId,
        actorName = actorName,
        localeId = localeId,
        localeName = localeName,
        periodYear = periodYear,
        periodMonth = periodMonth,
        grossIncomeCents = grossIncomeCents,
        operatingExpensesCents = operatingExpensesCents,
        netProfitCents = netProfitCents,
        profitSharePercent = profitSharePercent,
        profitShareCents = profitShareCents,
        spaceRentCents = spaceRentCents,
        totalDueCents = totalDueCents,
        paidCents = paidCents,
        paymentStatus = paymentStatus,
        declaredBy = declaredBy,
        paymentChannel = paymentChannel,
        paymentReference = paymentReference,
        notes = notes,
        paidAtMillis = paidAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

@Entity(tableName = "payment_events")
data class PaymentEventEntity(
    @PrimaryKey val id: String,
    val settlementId: String,
    val actorId: String,
    val actorName: String,
    val localeName: String,
    val periodYear: Long,
    val periodMonth: Long,
    val amountCents: Long,
    val totalPaidAfterCents: Long,
    val paymentChannel: String,
    val paymentReference: String,
    val reportedBy: String,
    val verificationStatus: String,
    val verifiedByUsername: String,
    val note: String,
    val createdAtMillis: Long,
    val verifiedAtMillis: Long?
) {
    fun toDomain(): PaymentEvent = PaymentEvent(
        id = id,
        settlementId = settlementId,
        actorId = actorId,
        actorName = actorName,
        localeName = localeName,
        periodYear = periodYear,
        periodMonth = periodMonth,
        amountCents = amountCents,
        totalPaidAfterCents = totalPaidAfterCents,
        paymentChannel = paymentChannel,
        paymentReference = paymentReference,
        reportedBy = reportedBy,
        verificationStatus = verificationStatus,
        verifiedByUsername = verifiedByUsername,
        note = note,
        createdAtMillis = createdAtMillis,
        verifiedAtMillis = verifiedAtMillis
    )
}

@Entity(
    tableName = "manager_users",
    indices = [Index(value = ["username"], unique = true)]
)
data class ManagerUserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val role: String,
    val passwordHash: String,
    val passwordSalt: String,
    val active: Boolean,
    val createdAtMillis: Long,
    val lastLoginAtMillis: Long?
) {
    fun toDomain(): ManagerUser = ManagerUser(
        id = id,
        username = username,
        fullName = fullName,
        role = role,
        active = active,
        createdAtMillis = createdAtMillis,
        lastLoginAtMillis = lastLoginAtMillis
    )
}

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val invoiceNumber: String,
    val paymentEventId: String,
    val settlementId: String,
    val actorId: String,
    val actorName: String,
    val actorTaxId: String,
    val concept: String,
    val periodYear: Long,
    val periodMonth: Long,
    val profitShareCents: Long,
    val spaceRentCents: Long,
    val totalAmountCents: Long,
    val paymentChannel: String,
    val paymentReference: String,
    val debitAccountCode: String,
    val creditAccountCode: String,
    val costCenter: String,
    val issuedByUsername: String,
    val exportedToVersat: Boolean,
    val issueDateMillis: Long,
    val exportedAtMillis: Long?
) {
    fun toDomain(): InvoiceRecord = InvoiceRecord(
        id = id,
        invoiceNumber = invoiceNumber,
        paymentEventId = paymentEventId,
        settlementId = settlementId,
        actorId = actorId,
        actorName = actorName,
        actorTaxId = actorTaxId,
        concept = concept,
        periodYear = periodYear,
        periodMonth = periodMonth,
        profitShareCents = profitShareCents,
        spaceRentCents = spaceRentCents,
        totalAmountCents = totalAmountCents,
        paymentChannel = paymentChannel,
        paymentReference = paymentReference,
        debitAccountCode = debitAccountCode,
        creditAccountCode = creditAccountCode,
        costCenter = costCenter,
        issuedByUsername = issuedByUsername,
        exportedToVersat = exportedToVersat,
        issueDateMillis = issueDateMillis,
        exportedAtMillis = exportedAtMillis
    )
}
