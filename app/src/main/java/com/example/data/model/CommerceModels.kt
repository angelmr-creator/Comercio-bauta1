package com.example.data.model

enum class LocaleStatus(val label: String) {
    DISPONIBLE("Disponible"),
    ARRENDADO("Arrendado"),
    MANTENIMIENTO("Mantenimiento");

    companion object {
        fun fromRaw(raw: String): LocaleStatus =
            entries.firstOrNull { it.name == raw } ?: DISPONIBLE
    }
}

enum class LegalType(val label: String, val fullName: String) {
    TCP("TCP", "Trabajador por Cuenta Propia"),
    MIPYME("MIPYME", "MIPYME"),
    CNA("CNA", "Cooperativa No Agropecuaria"),
    PDL("PDL", "Proyecto de Desarrollo Local"),
    OTRO("Otro", "Otra forma de gestión");

    companion object {
        fun fromRaw(raw: String): LegalType =
            entries.firstOrNull { it.name == raw } ?: TCP
    }
}

enum class PaymentStatus(val label: String) {
    PENDIENTE("Pendiente"),
    PARCIAL("Parcial"),
    PAGADO("Pagado");

    companion object {
        fun fromRaw(raw: String): PaymentStatus =
            entries.firstOrNull { it.name == raw } ?: PENDIENTE
    }
}

data class LocaleSpace(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val address: String = "",
    val areaSqm: Double = 0.0,
    val monthlyRentCents: Long = 0L,
    val status: String = LocaleStatus.DISPONIBLE.name,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun statusEnum(): LocaleStatus = LocaleStatus.fromRaw(status)
}

data class EconomicActor(
    val id: String = "",
    val name: String = "",
    val legalType: String = LegalType.TCP.name,
    val representativeName: String = "",
    val phone: String = "",
    val taxId: String = "",
    val actorEmail: String = "",
    val localeId: String = "",
    val localeName: String = "",
    val monthlyRentCents: Long = 0L,
    val profitSharePercent: Long = 30L,
    val active: Boolean = true,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun legalTypeEnum(): LegalType = LegalType.fromRaw(legalType)
}

enum class DeclarationSource(val label: String) {
    DIRECTOR("Dirección"),
    ACTOR("Portal Actor");

    companion object {
        fun fromRaw(raw: String): DeclarationSource =
            entries.firstOrNull { it.name == raw } ?: DIRECTOR
    }
}

enum class PaymentChannel(val label: String) {
    NO_DEFINIDO("Sin vía"),
    TRANSFERENCIA("Transferencia Transfermóvil"),
    QR_LINK("QR / Bulevar MiTransfer");

    companion object {
        fun fromRaw(raw: String): PaymentChannel =
            entries.firstOrNull { it.name == raw } ?: NO_DEFINIDO
    }
}

enum class PaymentVerificationStatus(val label: String) {
    POR_VERIFICAR("Por verificar"),
    CONFIRMADO("Confirmado"),
    OBSERVADO("Observado");

    companion object {
        fun fromRaw(raw: String): PaymentVerificationStatus =
            entries.firstOrNull { it.name == raw } ?: POR_VERIFICAR
    }
}

data class MonthlySettlement(
    val id: String = "",
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
    val paidAtMillis: Long? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    fun paymentStatusEnum(): PaymentStatus = PaymentStatus.fromRaw(paymentStatus)
    fun declaredByEnum(): DeclarationSource = DeclarationSource.fromRaw(declaredBy)
    fun paymentChannelEnum(): PaymentChannel = PaymentChannel.fromRaw(paymentChannel)
    val remainingCents: Long get() = (totalDueCents - paidCents).coerceAtLeast(0L)
}

data class PaymentEvent(
    val id: String = "",
    val settlementId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val localeName: String = "",
    val periodYear: Long = 2026L,
    val periodMonth: Long = 1L,
    val amountCents: Long = 0L,
    val totalPaidAfterCents: Long = 0L,
    val paymentChannel: String = PaymentChannel.TRANSFERENCIA.name,
    val paymentReference: String = "",
    val reportedBy: String = DeclarationSource.ACTOR.name,
    val verificationStatus: String = PaymentVerificationStatus.POR_VERIFICAR.name,
    val verifiedByUsername: String = "",
    val note: String = "",
    val createdAtMillis: Long = 0L,
    val verifiedAtMillis: Long? = null
) {
    fun channelEnum(): PaymentChannel = PaymentChannel.fromRaw(paymentChannel)
    fun reportedByEnum(): DeclarationSource = DeclarationSource.fromRaw(reportedBy)
    fun verificationStatusEnum(): PaymentVerificationStatus =
        PaymentVerificationStatus.fromRaw(verificationStatus)
}

enum class ManagerRole(val label: String) {
    ADMIN("Director Administrador"),
    ECONOMIST("Especialista Económico"),
    COMMERCIAL("Director Comercial");

    companion object {
        fun fromRaw(raw: String): ManagerRole =
            entries.firstOrNull { it.name == raw } ?: ADMIN
    }
}

data class ManagerUser(
    val id: String = "",
    val username: String = "",
    val fullName: String = "",
    val role: String = ManagerRole.ADMIN.name,
    val active: Boolean = true,
    val createdAtMillis: Long = 0L,
    val lastLoginAtMillis: Long? = null
) {
    fun roleEnum(): ManagerRole = ManagerRole.fromRaw(role)
}

data class PaymentConfig(
    val id: String = "payment",
    val bankName: String = "",
    val bankAccount: String = DEFAULT_BAUTA_BANK_ACCOUNT,
    val accountHolder: String = "Empresa Municipal de Comercio Bauta",
    val qrPaymentUrl: String = "",
    val updatedAtMillis: Long = 0L
) {
    val isConfigured: Boolean
        get() = bankAccount.isNotBlank() || qrPaymentUrl.isNotBlank()

    companion object {
        const val DEFAULT_BAUTA_BANK_ACCOUNT = "0618221336820012"
    }
}

data class InvoiceRecord(
    val id: String = "",
    val invoiceNumber: String = "",
    val paymentEventId: String = "",
    val settlementId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorTaxId: String = "",
    val concept: String = "",
    val periodYear: Long = 2026L,
    val periodMonth: Long = 1L,
    val profitShareCents: Long = 0L,
    val spaceRentCents: Long = 0L,
    val totalAmountCents: Long = 0L,
    val paymentChannel: String = PaymentChannel.TRANSFERENCIA.name,
    val paymentReference: String = "",
    val debitAccountCode: String = DEFAULT_VERSAT_DEBIT_ACCOUNT,
    val creditAccountCode: String = DEFAULT_VERSAT_CREDIT_ACCOUNT,
    val costCenter: String = DEFAULT_VERSAT_COST_CENTER,
    val issuedByUsername: String = "",
    val exportedToVersat: Boolean = false,
    val issueDateMillis: Long = 0L,
    val exportedAtMillis: Long? = null
) {
    fun channelEnum(): PaymentChannel = PaymentChannel.fromRaw(paymentChannel)

    companion object {
        const val DEFAULT_VERSAT_DEBIT_ACCOUNT = "111.01"
        const val DEFAULT_VERSAT_CREDIT_ACCOUNT = "900.01"
        const val DEFAULT_VERSAT_COST_CENTER = "COM-BAUTA"
    }
}
