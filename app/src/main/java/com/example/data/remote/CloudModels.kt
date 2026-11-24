package com.example.data.remote

import androidx.annotation.Keep
import com.example.data.model.DeclarationSource
import com.example.data.model.EconomicActor
import com.example.data.model.LegalType
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentVerificationStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class CloudWorkspace(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "Empresa Municipal de Comercio Bauta",
    val bankName: String = "",
    val bankAccount: String = PaymentConfig.DEFAULT_BAUTA_BANK_ACCOUNT,
    val accountHolder: String = "Empresa Municipal de Comercio Bauta",
    val qrPaymentUrl: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudWorkspaceActor(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val ownerId: String = "",
    val createdByUid: String = "",
    val actorEmail: String = "",
    val name: String = "",
    val legalType: String = LegalType.TCP.name,
    val representativeName: String = "",
    val phone: String = "",
    val taxId: String = "",
    val localeId: String = "",
    val localeName: String = "",
    val monthlyRentCents: Long = 0L,
    val profitSharePercent: Long = 30L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(): EconomicActor = EconomicActor(
        id = id,
        name = name,
        legalType = LegalType.fromRaw(legalType).name,
        representativeName = representativeName,
        phone = phone,
        taxId = taxId,
        actorEmail = actorEmail.trim().lowercase(),
        localeId = localeId,
        localeName = localeName,
        monthlyRentCents = monthlyRentCents,
        profitSharePercent = profitSharePercent,
        active = active,
        createdAtMillis = createdAt?.toDate()?.time ?: 0L,
        updatedAtMillis = updatedAt?.toDate()?.time ?: 0L
    )
}

@Keep
@IgnoreExtraProperties
data class CloudPaymentEvent(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val createdByUid: String = "",
    val settlementId: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val periodYear: Long = 2026L,
    val periodMonth: Long = 1L,
    val amountCents: Long = 0L,
    val paymentChannel: String = PaymentChannel.TRANSFERENCIA.name,
    val paymentReference: String = "",
    val reportedBy: String = DeclarationSource.ACTOR.name,
    val verificationStatus: String = PaymentVerificationStatus.POR_VERIFICAR.name,
    val verifiedByUsername: String = "",
    val note: String = "",
    val clientCreatedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toDomain(
        existingLocaleName: String = "",
        existingTotalPaidAfterCents: Long? = null
    ): PaymentEvent {
        val safeChannel = PaymentChannel.fromRaw(paymentChannel).name
        val safeSource = DeclarationSource.fromRaw(reportedBy).name
        val safeVerification = PaymentVerificationStatus.fromRaw(verificationStatus).name
        val resolvedCreatedMillis = clientCreatedAtMillis.takeIf { it > 0L }
            ?: createdAt?.toDate()?.time
            ?: System.currentTimeMillis()
        val resolvedVerifiedMillis = if (safeVerification == PaymentVerificationStatus.POR_VERIFICAR.name) {
            null
        } else {
            updatedAt?.toDate()?.time ?: resolvedCreatedMillis
        }

        return PaymentEvent(
            id = id,
            settlementId = settlementId,
            actorId = actorId,
            actorName = actorName,
            localeName = existingLocaleName,
            periodYear = periodYear,
            periodMonth = periodMonth,
            amountCents = amountCents,
            totalPaidAfterCents = existingTotalPaidAfterCents ?: amountCents,
            paymentChannel = safeChannel,
            paymentReference = paymentReference,
            reportedBy = safeSource,
            verificationStatus = safeVerification,
            verifiedByUsername = verifiedByUsername,
            note = note,
            createdAtMillis = resolvedCreatedMillis,
            verifiedAtMillis = resolvedVerifiedMillis
        )
    }
}

@Keep
@IgnoreExtraProperties
data class CloudSettlement(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val localeId: String = "",
    val localeName: String = "",
    val periodYear: Long = 2026L,
    val periodMonth: Long = 1L,
    val grossIncomeCents: Long = 0L,
    val operatingExpensesCents: Long = 0L,
    val netProfitCents: Long = 0L,
    val profitSharePercent: Long = 30L,
    val profitShareCents: Long = 0L,
    val spaceRentCents: Long = 0L,
    val totalDueCents: Long = 0L,
    val paidCents: Long = 0L,
    val paymentStatus: String = PaymentStatus.PENDIENTE.name,
    val declaredBy: String = DeclarationSource.DIRECTOR.name,
    val paymentChannel: String = PaymentChannel.NO_DEFINIDO.name,
    val paymentReference: String = "",
    val notes: String = "",
    @ServerTimestamp val paidAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
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
        paymentStatus = PaymentStatus.fromRaw(paymentStatus).name,
        declaredBy = DeclarationSource.fromRaw(declaredBy).name,
        paymentChannel = PaymentChannel.fromRaw(paymentChannel).name,
        paymentReference = paymentReference,
        notes = notes,
        paidAtMillis = paidAt?.toDate()?.time,
        createdAtMillis = createdAt?.toDate()?.time ?: 0L,
        updatedAtMillis = updatedAt?.toDate()?.time ?: 0L
    )
}

@Keep
@IgnoreExtraProperties
data class CloudWorkspaceSettlement(
    @DocumentId val id: String = "",
    val workspaceId: String = "",
    val ownerId: String = "",
    val createdByUid: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val localeId: String = "",
    val localeName: String = "",
    val periodYear: Long = 2026L,
    val periodMonth: Long = 1L,
    val grossIncomeCents: Long = 0L,
    val operatingExpensesCents: Long = 0L,
    val netProfitCents: Long = 0L,
    val profitSharePercent: Long = 30L,
    val profitShareCents: Long = 0L,
    val spaceRentCents: Long = 0L,
    val totalDueCents: Long = 0L,
    val paidCents: Long = 0L,
    val paymentStatus: String = PaymentStatus.PENDIENTE.name,
    val declaredBy: String = DeclarationSource.DIRECTOR.name,
    val paymentChannel: String = PaymentChannel.NO_DEFINIDO.name,
    val paymentReference: String = "",
    val notes: String = "",
    @ServerTimestamp val paidAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
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
        paymentStatus = PaymentStatus.fromRaw(paymentStatus).name,
        declaredBy = DeclarationSource.fromRaw(declaredBy).name,
        paymentChannel = PaymentChannel.fromRaw(paymentChannel).name,
        paymentReference = paymentReference,
        notes = notes,
        paidAtMillis = paidAt?.toDate()?.time,
        createdAtMillis = createdAt?.toDate()?.time ?: 0L,
        updatedAtMillis = updatedAt?.toDate()?.time ?: 0L
    )
}
