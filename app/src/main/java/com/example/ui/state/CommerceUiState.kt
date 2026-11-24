package com.example.ui.state

import com.example.data.model.EconomicActor
import com.example.data.model.InvoiceRecord
import com.example.data.model.LegalType
import com.example.data.model.LocaleSpace
import com.example.data.model.LocaleStatus
import com.example.data.model.ManagerUser
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentStatus
import com.example.util.YearMonthKey

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class MonthlySummaryMetrics(
    val totalDueCents: Long = 0L,
    val totalPaidCents: Long = 0L,
    val totalPendingCents: Long = 0L,
    val totalProfitShareCents: Long = 0L,
    val totalSpaceRentCents: Long = 0L,
    val settlementsCount: Int = 0,
    val pendingCount: Int = 0,
    val activeActorsCount: Int = 0,
    val totalActorsCount: Int = 0,
    val leasedLocalesCount: Int = 0,
    val availableLocalesCount: Int = 0,
    val totalLocalesCount: Int = 0
)

data class CloudSyncState(
    val isAuthenticated: Boolean = false,
    val userEmail: String = "",
    val userUid: String = "",
    val workspaceId: String = "",
    val workspaceOwnerId: String = "",
    val memberCount: Int = 0,
    val isOnline: Boolean = true,
    val hasPendingWrites: Boolean = false,
    val isFromCache: Boolean = false,
    val syncedEventsCount: Int = 0,
    val cloudSettlementsCount: Int = 0,
    val isSettlementsFromCache: Boolean = false,
    val isSettlementsPendingWrites: Boolean = false,
    val isSyncing: Boolean = false,
    val syncError: String? = null
) {
    val isWorkspaceOwner: Boolean
        get() = isAuthenticated && userUid.isNotBlank() && workspaceOwnerId == userUid

    val isWorkspaceConnected: Boolean
        get() = isAuthenticated && workspaceId.isNotBlank()
}

data class CommerceUiState(
    val selectedMonth: YearMonthKey = YearMonthKey.current(),
    val paymentConfig: PaymentConfig = PaymentConfig(),
    val cloudSync: CloudSyncState = CloudSyncState(),
    val managerUsers: List<ManagerUser> = emptyList(),
    val activeManagerUser: ManagerUser? = null,
    val locales: List<LocaleSpace> = emptyList(),
    val filteredLocales: List<LocaleSpace> = emptyList(),
    val localeStatusFilter: LocaleStatus? = null,
    val actors: List<EconomicActor> = emptyList(),
    val filteredActors: List<EconomicActor> = emptyList(),
    val actorLegalTypeFilter: LegalType? = null,
    val actorSearchQuery: String = "",
    val allSettlements: List<MonthlySettlement> = emptyList(),
    val cloudSettlements: List<MonthlySettlement> = emptyList(),
    val monthSettlements: List<MonthlySettlement> = emptyList(),
    val filteredMonthSettlements: List<MonthlySettlement> = emptyList(),
    val settlementStatusFilter: PaymentStatus? = null,
    val pendingMonthSettlements: List<MonthlySettlement> = emptyList(),
    val actorsWithoutMonthSettlement: List<EconomicActor> = emptyList(),
    val paymentEvents: List<PaymentEvent> = emptyList(),
    val unverifiedPaymentsCount: Int = 0,
    val invoices: List<InvoiceRecord> = emptyList(),
    val pendingExportInvoicesCount: Int = 0,
    val metrics: MonthlySummaryMetrics = MonthlySummaryMetrics()
)
