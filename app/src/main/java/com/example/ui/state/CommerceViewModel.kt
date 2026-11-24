package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DeclarationSource
import com.example.data.model.EconomicActor
import com.example.data.model.InvoiceRecord
import com.example.data.model.LegalType
import com.example.data.model.LocaleStatus
import com.example.data.model.ManagerRole
import com.example.data.model.ManagerUser
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentVerificationStatus
import com.example.data.remote.BautaCloudSyncRepository
import com.example.data.remote.CloudPaymentEventsSnapshot
import com.example.data.remote.CloudSettlementsSnapshot
import com.example.data.remote.CloudWorkspace
import com.example.data.remote.CloudWorkspaceActorsSnapshot
import com.example.data.remote.CloudWorkspaceSnapshot
import com.example.data.repository.BautaCommerceRepository
import com.example.util.YearMonthKey
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
class CommerceViewModel(
    private val repository: BautaCommerceRepository,
    private val cloudSync: BautaCloudSyncRepository? = null,
    private val isOnlineFlow: Flow<Boolean> = flowOf(true)
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonthKey.current())
    private val localeStatusFilter = MutableStateFlow<LocaleStatus?>(null)
    private val actorLegalTypeFilter = MutableStateFlow<LegalType?>(null)
    private val actorSearchQuery = MutableStateFlow("")
    private val settlementStatusFilter = MutableStateFlow<PaymentStatus?>(null)
    private val activeManagerUserId = MutableStateFlow<String?>(null)
    private val isDirectorModeFlow = MutableStateFlow(false)
    private val isDirectorSessionFlow = combine(
        isDirectorModeFlow,
        activeManagerUserId
    ) { directorMode, managerId ->
        directorMode && !managerId.isNullOrBlank()
    }.distinctUntilChanged()

    private val cloudSyncState = MutableStateFlow(CloudSyncState())
    private val workspaceSettlementsFlow = MutableStateFlow<List<MonthlySettlement>>(emptyList())
    private val personalCloudSettlementsFlow = MutableStateFlow<List<MonthlySettlement>>(emptyList())
    private val cloudSettlementsFlow: Flow<List<MonthlySettlement>> = combine(
        workspaceSettlementsFlow,
        personalCloudSettlementsFlow
    ) { workspaceList, personalList ->
        val merged = linkedMapOf<String, MonthlySettlement>()
        personalList.forEach { merged[it.id] = it }
        workspaceList.forEach { merged[it.id] = it }
        merged.values.sortedWith(
            compareByDescending<MonthlySettlement> { it.periodYear }
                .thenByDescending { it.periodMonth }
                .thenBy { it.actorName.lowercase() }
        )
    }

    private val workspaceInitMutex = Mutex()
    private var latestCloudWorkspace: CloudWorkspace? = null
    private var latestCloudEventIds: Set<String> = emptySet()
    private var latestCloudWorkspaceActorIds: Set<String> = emptySet()
    private var latestCloudWorkspaceSettlementIds: Set<String> = emptySet()
    private var latestCloudSettlementIds: Set<String> = emptySet()
    private var cloudActorCreatedByUid: Map<String, String> = emptyMap()
    private var cloudSettlementActorUid: Map<String, String> = emptyMap()
    private var cloudEventsReadyForSessionKey: String? = null
    private var cloudActorsReadyForSessionKey: String? = null
    private var cloudWorkspaceSettlementsReadyForSessionKey: String? = null
    private var cloudSettlementsReadyForUid: String? = null
    private var backfilledSessionKey: String? = null
    private var backfilledSettlementsUid: String? = null
    private var autoRecoveredWorkspaceKey: String? = null

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        startNetworkMonitor()
        startCloudSyncObservation()
    }

    fun setDirectorMode(enabled: Boolean) {
        isDirectorModeFlow.value = enabled
    }

    private data class FilterSnapshot(
        val month: YearMonthKey,
        val localeStatus: LocaleStatus?,
        val actorType: LegalType?,
        val actorQuery: String,
        val settlementStatus: PaymentStatus?
    )

    private data class ExtraStreamsSnapshot(
        val paymentConfig: PaymentConfig,
        val paymentEvents: List<PaymentEvent>,
        val managerUsers: List<ManagerUser>,
        val activeUserId: String?,
        val cloudSync: CloudSyncState,
        val invoices: List<InvoiceRecord>,
        val cloudSettlements: List<MonthlySettlement>
    )

    private val filtersFlow = combine(
        selectedMonth,
        localeStatusFilter,
        actorLegalTypeFilter,
        actorSearchQuery,
        settlementStatusFilter
    ) { month, localeStatus, actorType, actorQuery, settlementStatus ->
        FilterSnapshot(month, localeStatus, actorType, actorQuery, settlementStatus)
    }

    private val coreExtraFlow = combine(
        repository.observePaymentConfig(),
        repository.observePaymentEvents(),
        repository.observeManagerUsers(),
        activeManagerUserId,
        cloudSyncState
    ) { paymentConfig, paymentEvents, managerUsers, activeUserId, cloudState ->
        ExtraStreamsSnapshot(paymentConfig, paymentEvents, managerUsers, activeUserId, cloudState, emptyList(), emptyList())
    }

    private val extraStreamsFlow = combine(
        coreExtraFlow,
        repository.observeInvoices(),
        cloudSettlementsFlow
    ) { snapshot, invoices, cloudSettlements ->
        snapshot.copy(invoices = invoices, cloudSettlements = cloudSettlements)
    }

    val uiState: StateFlow<UiState<CommerceUiState>> = combine(
        repository.observeLocales(),
        repository.observeActors(),
        repository.observeSettlements(),
        extraStreamsFlow,
        filtersFlow
    ) { locales, actors, settlements, extras, filters ->
        val monthSettlements = settlements.filter {
            it.periodYear.toInt() == filters.month.year &&
                it.periodMonth.toInt() == filters.month.month
        }

        val filteredLocales = if (filters.localeStatus == null) {
            locales
        } else {
            locales.filter { it.statusEnum() == filters.localeStatus }
        }

        val queryLower = filters.actorQuery.trim().lowercase()
        val filteredActors = actors.filter { actor ->
            val matchesType = filters.actorType == null || actor.legalTypeEnum() == filters.actorType
            val matchesQuery = queryLower.isEmpty() ||
                actor.name.lowercase().contains(queryLower) ||
                actor.representativeName.lowercase().contains(queryLower) ||
                actor.localeName.lowercase().contains(queryLower)
            matchesType && matchesQuery
        }

        val filteredSettlements = if (filters.settlementStatus == null) {
            monthSettlements
        } else {
            monthSettlements.filter { it.paymentStatusEnum() == filters.settlementStatus }
        }

        val pendingMonthSettlements = monthSettlements.filter {
            it.paymentStatusEnum() != PaymentStatus.PAGADO
        }

        val settledActorIds = monthSettlements.map { it.actorId }.toSet()
        val actorsWithoutSettlement = actors.filter { it.active && it.id !in settledActorIds }

        val totalDue = monthSettlements.sumOf { it.totalDueCents }
        val totalPaid = monthSettlements.sumOf { it.paidCents.coerceAtMost(it.totalDueCents) }
        val totalPending = monthSettlements.sumOf { it.remainingCents }
        val totalProfitShare = monthSettlements.sumOf { it.profitShareCents }
        val totalSpaceRent = monthSettlements.sumOf { it.spaceRentCents }

        val metrics = MonthlySummaryMetrics(
            totalDueCents = totalDue,
            totalPaidCents = totalPaid,
            totalPendingCents = totalPending,
            totalProfitShareCents = totalProfitShare,
            totalSpaceRentCents = totalSpaceRent,
            settlementsCount = monthSettlements.size,
            pendingCount = pendingMonthSettlements.size,
            activeActorsCount = actors.count { it.active },
            totalActorsCount = actors.size,
            leasedLocalesCount = locales.count { it.statusEnum() == LocaleStatus.ARRENDADO },
            availableLocalesCount = locales.count { it.statusEnum() == LocaleStatus.DISPONIBLE },
            totalLocalesCount = locales.size
        )

        val activeUser = extras.activeUserId?.let { id ->
            extras.managerUsers.firstOrNull { it.id == id && it.active }
        }
        val unverifiedCount = extras.paymentEvents.count {
            it.verificationStatusEnum() == PaymentVerificationStatus.POR_VERIFICAR
        }

        CommerceUiState(
            selectedMonth = filters.month,
            paymentConfig = extras.paymentConfig,
            cloudSync = extras.cloudSync,
            managerUsers = extras.managerUsers,
            activeManagerUser = activeUser,
            locales = locales,
            filteredLocales = filteredLocales,
            localeStatusFilter = filters.localeStatus,
            actors = actors,
            filteredActors = filteredActors,
            actorLegalTypeFilter = filters.actorType,
            actorSearchQuery = filters.actorQuery,
            allSettlements = settlements,
            cloudSettlements = extras.cloudSettlements,
            monthSettlements = monthSettlements,
            filteredMonthSettlements = filteredSettlements,
            settlementStatusFilter = filters.settlementStatus,
            pendingMonthSettlements = pendingMonthSettlements,
            actorsWithoutMonthSettlement = actorsWithoutSettlement,
            paymentEvents = extras.paymentEvents,
            unverifiedPaymentsCount = unverifiedCount,
            invoices = extras.invoices,
            pendingExportInvoicesCount = extras.invoices.count { !it.exportedToVersat },
            metrics = metrics
        )
    }
        .map<CommerceUiState, UiState<CommerceUiState>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing local Bauta commerce database", error)
            emit(UiState.Error(error.localizedMessage ?: "No se pudieron cargar los datos locales"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private fun startNetworkMonitor() {
        viewModelScope.launch {
            isOnlineFlow
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Network monitor warning: ${error.message}")
                }
                .collect { online ->
                    cloudSyncState.update { it.copy(isOnline = online) }
                }
        }
    }

    private data class AuthSessionInput(
        val user: com.google.firebase.auth.FirebaseUser?,
        val savedWorkspaceId: String,
        val isDirectorSession: Boolean
    )

    private fun startCloudSyncObservation() {
        val cloud = cloudSync ?: return

        viewModelScope.launch {
            combine(
                cloud.authStateFlow(),
                repository.observeCloudWorkspaceId(),
                isDirectorSessionFlow
            ) { user, workspaceId, isDirectorSession ->
                AuthSessionInput(user, workspaceId?.trim().orEmpty(), isDirectorSession)
            }
                .distinctUntilChanged { old, new ->
                    old.user?.uid == new.user?.uid &&
                        old.user?.email == new.user?.email &&
                        old.user?.isEmailVerified == new.user?.isEmailVerified &&
                        old.savedWorkspaceId == new.savedWorkspaceId &&
                        old.isDirectorSession == new.isDirectorSession
                }
                .flatMapLatest { (user, savedWorkspaceId, isDirectorSession) ->
                    if (user == null || user.uid.isBlank()) {
                        resetCloudTrackingState()
                        cloudSyncState.update {
                            it.copy(
                                isAuthenticated = false,
                                userEmail = "",
                                userUid = "",
                                workspaceId = savedWorkspaceId,
                                workspaceOwnerId = "",
                                memberCount = 0,
                                hasPendingWrites = false,
                                isFromCache = false,
                                syncedEventsCount = 0,
                                cloudSettlementsCount = 0,
                                isSettlementsFromCache = false,
                                isSettlementsPendingWrites = false,
                                isSyncing = false,
                                syncError = null
                            )
                        }
                        emptyFlow<Unit>()
                    } else {
                        cloudSyncState.update {
                            it.copy(
                                isAuthenticated = true,
                                userEmail = user.email.orEmpty(),
                                userUid = user.uid,
                                workspaceId = savedWorkspaceId,
                                syncError = null
                            )
                        }
                        if (savedWorkspaceId.isBlank()) {
                            ensureWorkspaceInitialized(
                                forceCreateNew = false,
                                isDirectorSession = isDirectorSession
                            )
                            emptyFlow()
                        } else {
                            val sessionKey = "${user.uid}:$savedWorkspaceId:${if (isDirectorSession) "M" else "A"}"
                            val isVerifiedRootAdmin = user.isEmailVerified &&
                                BautaCloudSyncRepository.isRootAdminEmail(user.email.orEmpty())

                            cloud.observeWorkspace(savedWorkspaceId, user.uid)
                                .onEach { wsSnapshot: CloudWorkspaceSnapshot ->
                                    val workspace = wsSnapshot.workspace
                                    latestCloudWorkspace = workspace
                                    cloudSyncState.update { current ->
                                        current.copy(
                                            isAuthenticated = true,
                                            userEmail = user.email.orEmpty(),
                                            userUid = user.uid,
                                            workspaceId = savedWorkspaceId,
                                            workspaceOwnerId = workspace?.ownerId.orEmpty(),
                                            memberCount = workspace?.memberUids?.size ?: 0,
                                            hasPendingWrites = wsSnapshot.hasPendingWrites,
                                            isFromCache = wsSnapshot.isFromCache,
                                            isSyncing = false,
                                            syncError = null
                                        )
                                    }

                                    if (workspace == null && !wsSnapshot.isFromCache && autoRecoveredWorkspaceKey != sessionKey) {
                                        autoRecoveredWorkspaceKey = sessionKey
                                        if (isDirectorSession) {
                                            viewModelScope.launch {
                                                ensureWorkspaceInitialized(
                                                    forceCreateNew = false,
                                                    isDirectorSession = true
                                                )
                                            }
                                        }
                                    }

                                    if (workspace != null &&
                                        !isDirectorSession &&
                                        workspace.ownerId != user.uid &&
                                        workspace.bankAccount.isNotBlank()
                                    ) {
                                        val localCfg = repository.observePaymentConfig().first()
                                        if (localCfg.bankAccount != workspace.bankAccount ||
                                            localCfg.bankName != workspace.bankName ||
                                            localCfg.accountHolder != workspace.accountHolder ||
                                            localCfg.qrPaymentUrl != workspace.qrPaymentUrl
                                        ) {
                                            repository.savePaymentConfig(
                                                bankName = workspace.bankName,
                                                bankAccount = workspace.bankAccount,
                                                accountHolder = workspace.accountHolder,
                                                qrPaymentUrl = workspace.qrPaymentUrl
                                            )
                                        }
                                    }
                                }
                                .map { wsSnapshot ->
                                    val ws = wsSnapshot.workspace
                                    Triple(ws != null, ws?.ownerId.orEmpty(), ws?.id.orEmpty())
                                }
                                .distinctUntilChanged()
                                .flatMapLatest { (wsExists, wsOwnerId, wsId) ->
                                    if (!wsExists || wsId.isBlank()) {
                                        emptyFlow()
                                    } else {
                                        combine(
                                            cloud.observeWorkspacePaymentEvents(
                                                workspaceId = wsId,
                                                uid = user.uid,
                                                workspaceOwnerId = wsOwnerId,
                                                isManagerView = isDirectorSession,
                                                isVerifiedRootAdmin = isVerifiedRootAdmin,
                                                userEmail = user.email.orEmpty(),
                                                isEmailVerified = user.isEmailVerified
                                            ).onEach { cloudEventsReadyForSessionKey = sessionKey },
                                            cloud.observeWorkspaceActors(
                                                workspaceId = wsId,
                                                uid = user.uid,
                                                workspaceOwnerId = wsOwnerId,
                                                isManagerView = isDirectorSession,
                                                isVerifiedRootAdmin = isVerifiedRootAdmin,
                                                userEmail = user.email.orEmpty(),
                                                isEmailVerified = user.isEmailVerified
                                            ).onEach { cloudActorsReadyForSessionKey = sessionKey },
                                            cloud.observeWorkspaceSettlements(
                                                workspaceId = wsId,
                                                uid = user.uid,
                                                workspaceOwnerId = wsOwnerId,
                                                isManagerView = isDirectorSession,
                                                isVerifiedRootAdmin = isVerifiedRootAdmin,
                                                userEmail = user.email.orEmpty(),
                                                isEmailVerified = user.isEmailVerified
                                            ).onEach { cloudWorkspaceSettlementsReadyForSessionKey = sessionKey }
                                        ) { eventsSnap: CloudPaymentEventsSnapshot,
                                            actorsSnap: CloudWorkspaceActorsSnapshot,
                                            wsSettlementsSnap: CloudSettlementsSnapshot ->
                                            Triple(eventsSnap, actorsSnap, wsSettlementsSnap)
                                        }.onEach { (eventsSnap, actorsSnap, wsSettlementsSnap) ->
                                            val workspace = latestCloudWorkspace
                                            latestCloudEventIds = eventsSnap.events.map { it.id }.toSet()
                                            latestCloudWorkspaceActorIds = actorsSnap.actors.map { it.id }.toSet()
                                            latestCloudWorkspaceSettlementIds = wsSettlementsSnap.settlements.map { it.id }.toSet()
                                            cloudActorCreatedByUid = actorsSnap.createdByUidByActorId
                                            cloudSettlementActorUid = wsSettlementsSnap.actorUidBySettlementId
                                            workspaceSettlementsFlow.value = wsSettlementsSnap.settlements

                                            cloudSyncState.update { current ->
                                                current.copy(
                                                    hasPendingWrites = current.hasPendingWrites ||
                                                        eventsSnap.hasPendingWrites ||
                                                        actorsSnap.hasPendingWrites ||
                                                        wsSettlementsSnap.hasPendingWrites,
                                                    isFromCache = eventsSnap.isFromCache ||
                                                        actorsSnap.isFromCache ||
                                                        wsSettlementsSnap.isFromCache,
                                                    syncedEventsCount = eventsSnap.events.size,
                                                    cloudSettlementsCount = maxOf(
                                                        wsSettlementsSnap.settlements.size,
                                                        personalCloudSettlementsFlow.value.size
                                                    ),
                                                    isSettlementsFromCache = wsSettlementsSnap.isFromCache,
                                                    isSettlementsPendingWrites = wsSettlementsSnap.hasPendingWrites,
                                                    isSyncing = false,
                                                    syncError = null
                                                )
                                            }

                                            if (actorsSnap.actors.isNotEmpty()) {
                                                repository.mergeCloudActors(actorsSnap.actors)
                                            }
                                            if (wsSettlementsSnap.settlements.isNotEmpty()) {
                                                repository.mergeCloudSettlements(wsSettlementsSnap.settlements)
                                            }
                                            if (eventsSnap.events.isNotEmpty()) {
                                                repository.mergeCloudPaymentEvents(eventsSnap.events)
                                            }

                                            if (workspace != null &&
                                                cloudEventsReadyForSessionKey == sessionKey &&
                                                cloudActorsReadyForSessionKey == sessionKey &&
                                                cloudWorkspaceSettlementsReadyForSessionKey == sessionKey &&
                                                backfilledSessionKey != sessionKey
                                            ) {
                                                backfilledSessionKey = sessionKey
                                                backfillWorkspaceDataToCloud(
                                                    workspaceId = wsId,
                                                    workspace = workspace,
                                                    uid = user.uid,
                                                    isManagerView = isDirectorSession || isVerifiedRootAdmin
                                                )
                                            }
                                        }.map { Unit }
                                    }
                                }
                                .catch { error ->
                                    val firestoreError = generateSequence(error) { it.cause }
                                        .filterIsInstance<FirebaseFirestoreException>()
                                        .firstOrNull()
                                    if (firestoreError == null && error is CancellationException) throw error
                                    val rootError = firestoreError ?: error
                                    Log.w(TAG, "Cloud sync stream error: ${rootError.message}", rootError)
                                    cloudSyncState.update {
                                        it.copy(
                                            isSyncing = false,
                                            syncError = friendlyCloudError(rootError)
                                        )
                                    }
                                }
                        }
                    }
                }
                .collect()
        }

        // Also keep legacy/personal /users/{uid}/settlements stream in sync as secondary source
        viewModelScope.launch {
            cloud.authStateFlow()
                .map { it?.uid?.trim().orEmpty() }
                .distinctUntilChanged()
                .flatMapLatest { uid ->
                    if (uid.isBlank()) {
                        emptyFlow()
                    } else {
                        cloud.observeCloudSettlements(uid)
                            .onEach { snap: CloudSettlementsSnapshot ->
                                cloudSettlementsReadyForUid = uid
                                latestCloudSettlementIds = snap.settlements.map { it.id }.toSet()
                                personalCloudSettlementsFlow.value = snap.settlements
                                cloudSyncState.update {
                                    it.copy(
                                        cloudSettlementsCount = maxOf(
                                            workspaceSettlementsFlow.value.size,
                                            snap.settlements.size
                                        )
                                    )
                                }
                                if (snap.settlements.isNotEmpty()) {
                                    repository.mergeCloudSettlements(snap.settlements)
                                }
                                if (cloudSettlementsReadyForUid == uid && backfilledSettlementsUid != uid) {
                                    backfilledSettlementsUid = uid
                                    backfillLocalSettlementsToCloud()
                                }
                            }
                            .catch { error ->
                                val firestoreError = generateSequence(error) { it.cause }
                                    .filterIsInstance<FirebaseFirestoreException>()
                                    .firstOrNull()
                                if (firestoreError == null && error is CancellationException) throw error
                                val rootError = firestoreError ?: error
                                Log.w(TAG, "Cloud settlements stream error: ${rootError.message}", rootError)
                            }
                    }
                }
                .collect()
        }
    }

    private fun resetCloudTrackingState() {
        latestCloudWorkspace = null
        latestCloudEventIds = emptySet()
        latestCloudWorkspaceActorIds = emptySet()
        latestCloudWorkspaceSettlementIds = emptySet()
        latestCloudSettlementIds = emptySet()
        cloudActorCreatedByUid = emptyMap()
        cloudSettlementActorUid = emptyMap()
        workspaceSettlementsFlow.value = emptyList()
        personalCloudSettlementsFlow.value = emptyList()
        cloudEventsReadyForSessionKey = null
        cloudActorsReadyForSessionKey = null
        cloudWorkspaceSettlementsReadyForSessionKey = null
        cloudSettlementsReadyForUid = null
        backfilledSessionKey = null
        backfilledSettlementsUid = null
        autoRecoveredWorkspaceKey = null
    }

    private suspend fun backfillLocalSettlementsToCloud() {
        val cloud = cloudSync ?: return
        val localSettlements = repository.observeSettlements().first()
        val missing = localSettlements.filter { it.id !in latestCloudSettlementIds }
        for (settlement in missing) {
            cloud.pushOrUpdateSettlement(settlement, knownExistingInCloud = false)
        }
    }

    private suspend fun backfillWorkspaceDataToCloud(
        workspaceId: String,
        workspace: CloudWorkspace,
        uid: String,
        isManagerView: Boolean
    ) {
        val cloud = cloudSync ?: return
        val localActors = repository.observeActors().first()
        val actorsById = localActors.associateBy { it.id }
        val missingActors = localActors.filter { it.id !in latestCloudWorkspaceActorIds }
        for (actor in missingActors) {
            cloud.pushOrUpdateWorkspaceActor(
                workspaceId = workspaceId,
                actor = actor,
                knownWorkspace = workspace,
                knownCreatedByUid = cloudActorCreatedByUid[actor.id]
            )
        }

        val localSettlements = repository.observeSettlements().first()
        val missingSettlements = localSettlements.filter { it.id !in latestCloudWorkspaceSettlementIds }
        for (settlement in missingSettlements) {
            cloud.pushOrUpdateWorkspaceSettlement(
                workspaceId = workspaceId,
                settlement = settlement,
                knownWorkspace = workspace,
                knownExistingInCloud = false,
                knownActorUid = cloudSettlementActorUid[settlement.id] ?: cloudActorCreatedByUid[settlement.actorId],
                knownActorEmail = actorsById[settlement.actorId]?.actorEmail.orEmpty()
            )
        }

        val localEvents = repository.observePaymentEvents().first()
        val missingEvents = localEvents.filter { event ->
            event.id !in latestCloudEventIds &&
                (isManagerView || workspace.ownerId == uid || event.reportedByEnum() == DeclarationSource.ACTOR)
        }
        for (event in missingEvents) {
            cloud.pushPaymentEvent(
                workspaceId = workspaceId,
                event = event,
                knownWorkspace = workspace,
                knownActorUid = cloudActorCreatedByUid[event.actorId].orEmpty(),
                knownActorEmail = actorsById[event.actorId]?.actorEmail.orEmpty()
            )
        }
    }

    fun onCloudAuthSuccess() {
        viewModelScope.launch {
            _messages.tryEmit("Google conectado · Sincronización En Vivo activa")
            ensureWorkspaceInitialized(
                forceCreateNew = false,
                isDirectorSession = isDirectorSessionFlow.first()
            )
        }
    }

    fun onCloudAuthError(message: String) {
        cloudSyncState.update { it.copy(syncError = message) }
        _messages.tryEmit(message)
    }

    fun onCloudSignOutComplete() {
        resetCloudTrackingState()
        _messages.tryEmit("Sesión en la nube desconectada. Modo local de Cuba activo.")
    }

    fun createNewCloudWorkspace() {
        viewModelScope.launch {
            ensureWorkspaceInitialized(
                forceCreateNew = true,
                isDirectorSession = true
            )
        }
    }

    private suspend fun ensureWorkspaceInitialized(
        forceCreateNew: Boolean,
        isDirectorSession: Boolean
    ) {
        val cloud = cloudSync ?: return
        workspaceInitMutex.withLock {
            val savedId = if (forceCreateNew) null else repository.observeCloudWorkspaceId().first()
            if (!forceCreateNew &&
                !savedId.isNullOrBlank() &&
                latestCloudWorkspace?.id == savedId
            ) {
                return
            }
            val currentConfig = repository.observePaymentConfig().first()
            cloudSyncState.update { it.copy(isSyncing = true, syncError = null) }

            if (isDirectorSession || forceCreateNew) {
                cloud.ensureWorkspaceForManager(
                    preferredWorkspaceId = savedId,
                    paymentConfig = currentConfig,
                    forceCreateNew = forceCreateNew
                ).onSuccess { workspace ->
                    latestCloudWorkspace = workspace
                    repository.saveCloudWorkspaceId(workspace.id)
                    cloudSyncState.update {
                        it.copy(
                            workspaceId = workspace.id,
                            workspaceOwnerId = workspace.ownerId,
                            memberCount = workspace.memberUids.size,
                            isSyncing = false,
                            syncError = null
                        )
                    }
                    if (forceCreateNew) {
                        _messages.tryEmit("Nuevo espacio creado: ${workspace.id}")
                    }
                }.onFailure { err ->
                    cloudSyncState.update {
                        it.copy(
                            isSyncing = false,
                            syncError = friendlyCloudError(err)
                        )
                    }
                }
            } else {
                cloud.connectActorToSharedWorkspace(preferredWorkspaceId = savedId)
                    .onSuccess { workspace ->
                        if (workspace != null) {
                            latestCloudWorkspace = workspace
                            repository.saveCloudWorkspaceId(workspace.id)
                            if (workspace.bankAccount.isNotBlank()) {
                                repository.savePaymentConfig(
                                    bankName = workspace.bankName,
                                    bankAccount = workspace.bankAccount,
                                    accountHolder = workspace.accountHolder,
                                    qrPaymentUrl = workspace.qrPaymentUrl
                                )
                            }
                            cloudSyncState.update {
                                it.copy(
                                    workspaceId = workspace.id,
                                    workspaceOwnerId = workspace.ownerId,
                                    memberCount = workspace.memberUids.size,
                                    isSyncing = false,
                                    syncError = null
                                )
                            }
                        } else {
                            cloudSyncState.update {
                                it.copy(
                                    isSyncing = false,
                                    syncError = null
                                )
                            }
                        }
                    }.onFailure { err ->
                        cloudSyncState.update {
                            it.copy(
                                isSyncing = false,
                                syncError = friendlyCloudError(err)
                            )
                        }
                    }
            }
        }
    }

    fun joinCloudWorkspace(rawCode: String) {
        val cloud = cloudSync ?: return
        viewModelScope.launch {
            cloudSyncState.update { it.copy(isSyncing = true, syncError = null) }
            cloud.joinWorkspaceByCode(rawCode)
                .onSuccess { workspace ->
                    latestCloudWorkspace = workspace
                    backfilledSessionKey = null
                    repository.saveCloudWorkspaceId(workspace.id)
                    if (workspace.bankAccount.isNotBlank()) {
                        repository.savePaymentConfig(
                            bankName = workspace.bankName,
                            bankAccount = workspace.bankAccount,
                            accountHolder = workspace.accountHolder,
                            qrPaymentUrl = workspace.qrPaymentUrl
                        )
                    }
                    cloudSyncState.update {
                        it.copy(
                            workspaceId = workspace.id,
                            workspaceOwnerId = workspace.ownerId,
                            memberCount = workspace.memberUids.size,
                            isSyncing = false,
                            syncError = null
                        )
                    }
                    _messages.tryEmit("Conectado a empresa ${workspace.id} en tiempo real")
                }
                .onFailure { err ->
                    val msg = friendlyCloudError(err)
                    cloudSyncState.update { it.copy(isSyncing = false, syncError = msg) }
                    _messages.tryEmit(msg)
                }
        }
    }

    fun forceSyncNow() {
        val cloud = cloudSync ?: return
        val state = cloudSyncState.value
        if (!state.isAuthenticated) {
            _messages.tryEmit("Conecta Google primero para sincronizar con la nube.")
            return
        }
        viewModelScope.launch {
            cloudSyncState.update { it.copy(isSyncing = true, syncError = null) }
            val ws = latestCloudWorkspace
            val workspaceId = state.workspaceId
            val uid = state.userUid
            val isDir = isDirectorSessionFlow.first()
            if (ws != null && workspaceId.isNotBlank()) {
                backfillWorkspaceDataToCloud(
                    workspaceId = workspaceId,
                    workspace = ws,
                    uid = uid,
                    isManagerView = isDir
                )
                if (isDir && (ws.ownerId == uid || BautaCloudSyncRepository.isRootAdminEmail(state.userEmail))) {
                    val cfg = repository.observePaymentConfig().first()
                    cloud.syncWorkspacePaymentConfig(workspaceId, cfg, knownOwnerId = ws.ownerId)
                }
            } else {
                ensureWorkspaceInitialized(
                    forceCreateNew = false,
                    isDirectorSession = isDir
                )
            }
            backfillLocalSettlementsToCloud()
            cloudSyncState.update { it.copy(isSyncing = false) }
            _messages.tryEmit(
                if (cloudSyncState.value.isOnline) {
                    "Sincronización En Vivo completada"
                } else {
                    "Sin señal: los cambios permanecen en cola local para enviarse al reconectar"
                }
            )
        }
    }

    fun syncSettlementsToFirestore() {
        val cloud = cloudSync ?: return
        val state = cloudSyncState.value
        if (!state.isAuthenticated) {
            _messages.tryEmit("Conecta tu cuenta Google para leer y sincronizar datos de Firestore.")
            return
        }
        viewModelScope.launch {
            cloudSyncState.update { it.copy(isSyncing = true, syncError = null) }
            val localSettlements = repository.observeSettlements().first()
            val actorsById = repository.observeActors().first().associateBy { it.id }
            val ws = latestCloudWorkspace
            val wsId = state.workspaceId
            var synced = 0
            for (settlement in localSettlements) {
                var ok = false
                if (ws != null && wsId.isNotBlank()) {
                    val wsRes = cloud.pushOrUpdateWorkspaceSettlement(
                        workspaceId = wsId,
                        settlement = settlement,
                        knownWorkspace = ws,
                        knownExistingInCloud = settlement.id in latestCloudWorkspaceSettlementIds,
                        knownActorUid = cloudSettlementActorUid[settlement.id] ?: cloudActorCreatedByUid[settlement.actorId],
                        knownActorEmail = actorsById[settlement.actorId]?.actorEmail.orEmpty()
                    )
                    if (wsRes.isSuccess) ok = true
                }
                val personalRes = cloud.pushOrUpdateSettlement(
                    settlement = settlement,
                    knownExistingInCloud = settlement.id in latestCloudSettlementIds
                )
                if (personalRes.isSuccess || ok) synced++
            }
            cloudSyncState.update { it.copy(isSyncing = false) }
            _messages.tryEmit("Firestore sincronizado ($synced liquidación(es))")
        }
    }

    fun previousMonth() {
        selectedMonth.value = selectedMonth.value.previous()
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.next()
    }

    fun setLocaleStatusFilter(status: LocaleStatus?) {
        localeStatusFilter.value = status
    }

    fun setActorLegalTypeFilter(type: LegalType?) {
        actorLegalTypeFilter.value = type
    }

    fun setActorSearchQuery(query: String) {
        actorSearchQuery.value = query
    }

    fun setSettlementStatusFilter(status: PaymentStatus?) {
        settlementStatusFilter.value = status
    }

    fun saveLocale(
        existingId: String?,
        code: String,
        name: String,
        address: String,
        areaSqm: Double,
        monthlyRentCents: Long,
        status: LocaleStatus
    ) {
        viewModelScope.launch {
            repository.saveLocale(
                existingId = existingId,
                code = code,
                name = name,
                address = address,
                areaSqm = areaSqm,
                monthlyRentCents = monthlyRentCents,
                status = status
            ).onSuccess {
                _messages.tryEmit(if (existingId == null) "Local registrado" else "Local actualizado")
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Error al guardar el local")
            }
        }
    }

    fun deleteLocale(localeId: String) {
        viewModelScope.launch {
            repository.deleteLocale(localeId)
                .onSuccess { _messages.tryEmit("Local eliminado") }
                .onFailure { err -> _messages.tryEmit(err.localizedMessage ?: "Error al eliminar el local") }
        }
    }

    fun saveActor(
        existingId: String?,
        previousLocaleId: String?,
        name: String,
        legalType: LegalType,
        representativeName: String,
        phone: String,
        taxId: String,
        actorEmail: String = "",
        localeId: String,
        localeName: String,
        monthlyRentCents: Long,
        profitSharePercent: Long,
        active: Boolean
    ) {
        viewModelScope.launch {
            val previousEmail = existingId?.takeIf { it.isNotBlank() }
                ?.let { repository.getActorById(it)?.actorEmail.orEmpty() }
                .orEmpty()
            repository.saveActor(
                existingId = existingId,
                previousLocaleId = previousLocaleId,
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
                active = active
            ).onSuccess { savedActor ->
                _messages.tryEmit(if (existingId == null) "Actor económico registrado" else "Actor económico actualizado")
                enqueueActorToCloud(savedActor)
                if (savedActor.actorEmail.isNotBlank() && savedActor.actorEmail != previousEmail) {
                    propagateActorEmailToCloudOperations(savedActor)
                }
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Error al guardar el actor")
            }
        }
    }

    private fun propagateActorEmailToCloudOperations(actor: EconomicActor) {
        val cloud = cloudSync ?: return
        val state = cloudSyncState.value
        val email = actor.actorEmail.trim().lowercase()
        if (!state.isWorkspaceConnected || email.isBlank()) return
        viewModelScope.launch {
            val actorSettlements = repository.observeSettlements().first().filter { it.actorId == actor.id }
            for (settlement in actorSettlements) {
                cloud.pushOrUpdateWorkspaceSettlement(
                    workspaceId = state.workspaceId,
                    settlement = settlement,
                    knownWorkspace = latestCloudWorkspace,
                    knownExistingInCloud = settlement.id in latestCloudWorkspaceSettlementIds,
                    knownActorUid = cloudSettlementActorUid[settlement.id] ?: cloudActorCreatedByUid[actor.id],
                    knownActorEmail = email
                )
            }
            val actorEvents = repository.observePaymentEvents().first().filter { it.actorId == actor.id }
            for (event in actorEvents) {
                cloud.pushPaymentEvent(
                    workspaceId = state.workspaceId,
                    event = event,
                    knownWorkspace = latestCloudWorkspace,
                    knownActorUid = cloudActorCreatedByUid[actor.id].orEmpty(),
                    knownActorEmail = email
                )
            }
        }
    }

    fun registerOrSelectActorForPortal(
        selectedActorId: String?,
        businessNameInput: String,
        legalType: LegalType,
        representativeName: String,
        phone: String,
        taxId: String,
        onResolved: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val signedInEmail = cloudSyncState.value
                .takeIf { it.isAuthenticated }
                ?.userEmail
                ?.trim()
                ?.lowercase()
                ?.take(120)
                .orEmpty()

            if (!selectedActorId.isNullOrBlank()) {
                val existing = repository.getActorById(selectedActorId)
                if (existing != null && existing.actorEmail.isBlank() && signedInEmail.isNotBlank()) {
                    repository.saveActor(
                        existingId = existing.id,
                        previousLocaleId = existing.localeId,
                        name = existing.name,
                        legalType = existing.legalTypeEnum(),
                        representativeName = existing.representativeName,
                        phone = existing.phone,
                        taxId = existing.taxId,
                        actorEmail = signedInEmail,
                        localeId = existing.localeId,
                        localeName = existing.localeName,
                        monthlyRentCents = existing.monthlyRentCents,
                        profitSharePercent = existing.profitSharePercent,
                        active = existing.active
                    ).onSuccess { updated ->
                        enqueueActorToCloud(updated)
                    }
                }
                onResolved(selectedActorId)
                return@launch
            }

            val cleanName = businessNameInput.trim()
            val cleanRep = representativeName.trim()
            val cleanPhone = phone.trim()
            val cleanTax = taxId.trim()
            if (cleanName.isBlank() || cleanRep.isBlank() || cleanPhone.isBlank() || cleanTax.isBlank()) {
                _messages.tryEmit("Completa todos los datos obligatorios del registro para continuar.")
                onResolved(null)
                return@launch
            }

            val existingActors = repository.observeActors().first()
            val matched = existingActors.firstOrNull { actor ->
                (actor.taxId.equals(cleanTax, ignoreCase = true)) ||
                    actor.name.equals(cleanName, ignoreCase = true) ||
                    (signedInEmail.isNotBlank() && actor.actorEmail.equals(signedInEmail, ignoreCase = true))
            }
            if (matched != null) {
                if (matched.phone.isBlank() || (matched.actorEmail.isBlank() && signedInEmail.isNotBlank())) {
                    repository.saveActor(
                        existingId = matched.id,
                        previousLocaleId = matched.localeId,
                        name = matched.name,
                        legalType = matched.legalTypeEnum(),
                        representativeName = matched.representativeName.ifBlank { cleanRep },
                        phone = matched.phone.ifBlank { cleanPhone },
                        taxId = matched.taxId.ifBlank { cleanTax },
                        actorEmail = matched.actorEmail.ifBlank { signedInEmail },
                        localeId = matched.localeId,
                        localeName = matched.localeName,
                        monthlyRentCents = matched.monthlyRentCents,
                        profitSharePercent = matched.profitSharePercent,
                        active = matched.active
                    ).onSuccess { updated ->
                        enqueueActorToCloud(updated)
                    }
                }
                onResolved(matched.id)
                return@launch
            }

            repository.saveActor(
                existingId = null,
                previousLocaleId = null,
                name = cleanName,
                legalType = legalType,
                representativeName = cleanRep,
                phone = cleanPhone,
                taxId = cleanTax,
                actorEmail = signedInEmail,
                localeId = "",
                localeName = "",
                monthlyRentCents = 0L,
                profitSharePercent = 30L,
                active = true
            ).onSuccess { created ->
                _messages.tryEmit("Negocio registrado en Comercio Bauta")
                enqueueActorToCloud(created)
                onResolved(created.id)
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "No se pudo registrar el negocio")
                onResolved(null)
            }
        }
    }

    fun updateActorProfileFromPortal(
        actor: EconomicActor,
        name: String,
        legalType: LegalType,
        representativeName: String,
        phone: String,
        taxId: String
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val cleanRep = representativeName.trim()
            val cleanPhone = phone.trim()
            val cleanTax = taxId.trim()
            if (cleanName.isBlank() || cleanRep.isBlank() || cleanPhone.isBlank() || cleanTax.isBlank()) {
                _messages.tryEmit("Completa nombre del negocio, titular, teléfono y NIT/Licencia.")
                return@launch
            }
            val signedInEmail = cloudSyncState.value
                .takeIf { it.isAuthenticated }
                ?.userEmail
                ?.trim()
                ?.lowercase()
                ?.take(120)
                .orEmpty()
            repository.saveActor(
                existingId = actor.id,
                previousLocaleId = actor.localeId,
                name = cleanName,
                legalType = legalType,
                representativeName = cleanRep,
                phone = cleanPhone,
                taxId = cleanTax,
                actorEmail = actor.actorEmail.ifBlank { signedInEmail },
                localeId = actor.localeId,
                localeName = actor.localeName,
                monthlyRentCents = actor.monthlyRentCents,
                profitSharePercent = actor.profitSharePercent,
                active = actor.active
            ).onSuccess { updated ->
                _messages.tryEmit("Datos del negocio actualizados")
                enqueueActorToCloud(updated)
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "No se pudieron actualizar tus datos")
            }
        }
    }

    fun deleteActor(actor: EconomicActor) {
        viewModelScope.launch {
            repository.deleteActor(actor)
                .onSuccess { _messages.tryEmit("Actor eliminado") }
                .onFailure { err -> _messages.tryEmit(err.localizedMessage ?: "Error al eliminar el actor") }
        }
    }

    fun saveSettlement(
        existingId: String?,
        actor: EconomicActor,
        periodYear: Int,
        periodMonth: Int,
        grossIncomeCents: Long,
        operatingExpensesCents: Long,
        netProfitCents: Long,
        profitSharePercent: Long,
        spaceRentCents: Long,
        paidCents: Long,
        notes: String,
        declaredBy: DeclarationSource = DeclarationSource.DIRECTOR,
        paymentChannel: PaymentChannel = PaymentChannel.NO_DEFINIDO,
        paymentReference: String = ""
    ) {
        viewModelScope.launch {
            val activeUsername = currentActiveUsername()
            repository.saveSettlement(
                existingId = existingId,
                actorId = actor.id,
                actorName = actor.name,
                localeId = actor.localeId,
                localeName = actor.localeName,
                periodYear = periodYear,
                periodMonth = periodMonth,
                grossIncomeCents = grossIncomeCents,
                operatingExpensesCents = operatingExpensesCents,
                netProfitCents = netProfitCents,
                profitSharePercent = profitSharePercent,
                spaceRentCents = spaceRentCents,
                paidCents = paidCents,
                notes = notes,
                declaredBy = declaredBy,
                paymentChannel = paymentChannel,
                paymentReference = paymentReference,
                directorUsername = activeUsername
            ).onSuccess { outcome ->
                _messages.tryEmit(
                    when {
                        existingId != null -> "Liquidación actualizada"
                        declaredBy == DeclarationSource.ACTOR -> "Declaración enviada a Comercio Bauta"
                        else -> "Liquidación registrada"
                    }
                )
                enqueueSettlementToCloud(outcome.settlement)
                if (outcome.createdEvent != null) {
                    enqueueEventToCloud(outcome.createdEvent)
                }
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Error al guardar la liquidación")
            }
        }
    }

    fun registerPayment(
        settlement: MonthlySettlement,
        newPaidCents: Long,
        paymentChannel: PaymentChannel,
        paymentReference: String,
        reportedBy: DeclarationSource
    ) {
        viewModelScope.launch {
            val activeUsername = currentActiveUsername()
            repository.registerPayment(
                settlement = settlement,
                newPaidCents = newPaidCents,
                paymentChannel = paymentChannel,
                paymentReference = paymentReference,
                reportedBy = reportedBy,
                directorUsername = activeUsername
            ).onSuccess { createdEvent ->
                val isCloudConnected = cloudSyncState.value.isWorkspaceConnected
                val isOnline = cloudSyncState.value.isOnline
                _messages.tryEmit(
                    when {
                        reportedBy == DeclarationSource.ACTOR && isCloudConnected && !isOnline ->
                            "Pago guardado offline · Se enviará a la nube al reconectar"
                        reportedBy == DeclarationSource.ACTOR && isCloudConnected ->
                            "Pago enviado en vivo a Comercio Bauta"
                        reportedBy == DeclarationSource.ACTOR ->
                            "Pago registrado en el equipo"
                        else ->
                            "Cobro digital confirmado"
                    }
                )
                repository.getSettlementById(settlement.id)?.let { updated ->
                    enqueueSettlementToCloud(updated)
                }
                if (createdEvent != null) {
                    enqueueEventToCloud(createdEvent)
                }
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Error al registrar cobro")
            }
        }
    }

    fun reportQuickActorPayment(
        actorName: String,
        periodYear: Int,
        periodMonth: Int,
        amountCents: Long,
        paymentChannel: PaymentChannel,
        paymentReference: String,
        note: String,
        actorId: String = ""
    ) {
        viewModelScope.launch {
            repository.reportQuickActorPayment(
                actorName = actorName,
                periodYear = periodYear,
                periodMonth = periodMonth,
                amountCents = amountCents,
                paymentChannel = paymentChannel,
                paymentReference = paymentReference,
                note = note,
                actorId = actorId
            ).onSuccess { createdEvent ->
                val isCloud = cloudSyncState.value.isWorkspaceConnected
                val isOnline = cloudSyncState.value.isOnline
                _messages.tryEmit(
                    when {
                        isCloud && !isOnline -> "Pago guardado en cola offline · Subirá al regresar la señal"
                        isCloud -> "Pago Transfermóvil enviado En Vivo a Comercio Bauta"
                        else -> "Pago registrado localmente (conecta la nube para enviarlo en línea)"
                    }
                )
                enqueueEventToCloud(createdEvent)
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Error al reportar pago")
            }
        }
    }

    fun verifyPaymentEvent(
        eventId: String,
        status: PaymentVerificationStatus,
        note: String = ""
    ) {
        viewModelScope.launch {
            val verifier = currentActiveUsername().ifBlank { "director" }
            repository.verifyPaymentEvent(
                eventId = eventId,
                status = status,
                verifiedByUsername = verifier,
                note = note
            ).onSuccess {
                _messages.tryEmit(
                    when (status) {
                        PaymentVerificationStatus.CONFIRMADO -> "Pago confirmado por @$verifier"
                        PaymentVerificationStatus.OBSERVADO -> "Pago marcado en revisión"
                        PaymentVerificationStatus.POR_VERIFICAR -> "Devuelto a pendiente de verificar"
                    }
                )
                val updatedEvent = repository.getPaymentEventById(eventId)
                enqueueVerificationToCloud(eventId, status, verifier, note, updatedEvent)
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Error al verificar pago")
            }
        }
    }

    fun savePaymentConfig(
        bankName: String,
        bankAccount: String,
        accountHolder: String,
        qrPaymentUrl: String
    ) {
        viewModelScope.launch {
            repository.savePaymentConfig(
                bankName = bankName,
                bankAccount = bankAccount,
                accountHolder = accountHolder,
                qrPaymentUrl = qrPaymentUrl
            )
                .onSuccess {
                    _messages.tryEmit("Cuenta bancaria y link QR guardados")
                    enqueuePaymentConfigToCloud(
                        PaymentConfig(
                            bankName = bankName,
                            bankAccount = bankAccount,
                            accountHolder = accountHolder,
                            qrPaymentUrl = qrPaymentUrl
                        )
                    )
                }
                .onFailure { err -> _messages.tryEmit(err.localizedMessage ?: "Error al guardar configuración") }
        }
    }

    private fun enqueueEventToCloud(event: PaymentEvent) {
        val cloud = cloudSync ?: return
        val state = cloudSyncState.value
        if (!state.isWorkspaceConnected) return
        viewModelScope.launch {
            val matchedActor = event.actorId.takeIf { it.isNotBlank() }
                ?.let { repository.getActorById(it) }
            cloud.pushPaymentEvent(
                workspaceId = state.workspaceId,
                event = event,
                knownWorkspace = latestCloudWorkspace,
                knownActorUid = cloudActorCreatedByUid[event.actorId].orEmpty(),
                knownActorEmail = matchedActor?.actorEmail.orEmpty()
            ).onFailure { err ->
                cloudSyncState.update { it.copy(syncError = friendlyCloudError(err)) }
            }
        }
    }

    private fun enqueueVerificationToCloud(
        eventId: String,
        status: PaymentVerificationStatus,
        verifier: String,
        note: String,
        updatedEvent: PaymentEvent?
    ) {
        val cloud = cloudSync ?: return
        val state = cloudSyncState.value
        if (!state.isWorkspaceConnected) return
        viewModelScope.launch {
            cloud.verifyCloudPaymentEvent(
                workspaceId = state.workspaceId,
                eventId = eventId,
                status = status,
                verifiedByUsername = verifier,
                note = note,
                knownOwnerId = state.workspaceOwnerId.ifBlank { null },
                knownWorkspace = latestCloudWorkspace,
                fallbackEvent = updatedEvent
            ).onFailure { err ->
                cloudSyncState.update { it.copy(syncError = friendlyCloudError(err)) }
            }
        }
    }

    private fun enqueueActorToCloud(actor: EconomicActor) {
        val cloud = cloudSync ?: return
        val state = cloudSyncState.value
        if (!state.isWorkspaceConnected) return
        viewModelScope.launch {
            cloud.pushOrUpdateWorkspaceActor(
                workspaceId = state.workspaceId,
                actor = actor,
                knownWorkspace = latestCloudWorkspace,
                knownCreatedByUid = cloudActorCreatedByUid[actor.id]
            ).onFailure { err ->
                cloudSyncState.update { it.copy(syncError = friendlyCloudError(err)) }
            }
        }
    }

    private fun enqueuePaymentConfigToCloud(config: PaymentConfig) {
        val cloud = cloudSync ?: return
        val state = cloudSyncState.value
        val isDirectorSession = isDirectorModeFlow.value && !activeManagerUserId.value.isNullOrBlank()
        val canManageWorkspace = state.isWorkspaceOwner ||
            (isDirectorSession && BautaCloudSyncRepository.isRootAdminEmail(state.userEmail))
        if (!state.isWorkspaceConnected || !canManageWorkspace) return
        viewModelScope.launch {
            cloud.syncWorkspacePaymentConfig(
                workspaceId = state.workspaceId,
                config = config,
                knownOwnerId = state.workspaceOwnerId
            ).onFailure { err ->
                cloudSyncState.update { it.copy(syncError = friendlyCloudError(err)) }
            }
        }
    }

    private fun enqueueSettlementToCloud(settlement: MonthlySettlement) {
        val cloud = cloudSync ?: return
        val state = cloudSyncState.value
        if (!state.isAuthenticated) return
        viewModelScope.launch {
            val matchedActor = settlement.actorId.takeIf { it.isNotBlank() }
                ?.let { repository.getActorById(it) }
            if (state.isWorkspaceConnected) {
                cloud.pushOrUpdateWorkspaceSettlement(
                    workspaceId = state.workspaceId,
                    settlement = settlement,
                    knownWorkspace = latestCloudWorkspace,
                    knownExistingInCloud = settlement.id in latestCloudWorkspaceSettlementIds,
                    knownActorUid = cloudSettlementActorUid[settlement.id]
                        ?: cloudActorCreatedByUid[settlement.actorId],
                    knownActorEmail = matchedActor?.actorEmail.orEmpty()
                ).onFailure { err ->
                    cloudSyncState.update { it.copy(syncError = friendlyCloudError(err)) }
                }
            }
            cloud.pushOrUpdateSettlement(
                settlement = settlement,
                knownExistingInCloud = settlement.id in latestCloudSettlementIds
            ).onFailure { err ->
                cloudSyncState.update { it.copy(syncError = friendlyCloudError(err)) }
            }
        }
    }

    fun deleteSettlement(settlementId: String) {
        viewModelScope.launch {
            repository.deleteSettlement(settlementId)
                .onSuccess {
                    _messages.tryEmit("Liquidación eliminada")
                    val state = cloudSyncState.value
                    if (state.isWorkspaceConnected) {
                        cloudSync?.deleteWorkspaceSettlement(state.workspaceId, settlementId)
                    }
                    if (state.isAuthenticated) {
                        cloudSync?.deleteCloudSettlement(settlementId)
                    }
                }
                .onFailure { err -> _messages.tryEmit(err.localizedMessage ?: "Error al eliminar liquidación") }
        }
    }

    // Local Manager Users
    suspend fun registerFirstManagerAndSignIn(
        username: String,
        fullName: String,
        role: ManagerRole,
        password: String
    ): Result<ManagerUser> {
        val result = repository.registerManagerUser(
            username = username,
            fullName = fullName,
            role = role,
            password = password
        )
        result.onSuccess { user ->
            activeManagerUserId.value = user.id
            _messages.tryEmit("Bienvenido, ${user.fullName}")
        }
        return result
    }

    suspend fun signInManager(username: String, password: String): Result<ManagerUser> {
        val result = repository.authenticateManagerUser(username = username, password = password)
        result.onSuccess { user ->
            activeManagerUserId.value = user.id
            _messages.tryEmit("Sesión iniciada: ${user.fullName}")
        }
        return result
    }

    fun signOutManager() {
        activeManagerUserId.value = null
    }

    fun createManagerUser(
        username: String,
        fullName: String,
        role: ManagerRole,
        password: String
    ) {
        viewModelScope.launch {
            repository.registerManagerUser(
                username = username,
                fullName = fullName,
                role = role,
                password = password
            ).onSuccess { user ->
                _messages.tryEmit("Usuario @${user.username} creado")
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Error al crear usuario")
            }
        }
    }

    fun toggleManagerUserActive(user: ManagerUser) {
        viewModelScope.launch {
            repository.setManagerUserActive(
                userId = user.id,
                active = !user.active,
                currentUserId = activeManagerUserId.value
            ).onSuccess {
                _messages.tryEmit(
                    if (!user.active) "Usuario @${user.username} activado" else "Usuario @${user.username} desactivado"
                )
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "No se pudo cambiar el estado del usuario")
            }
        }
    }

    fun resetManagerPassword(userId: String, newPassword: String) {
        viewModelScope.launch {
            repository.resetManagerPassword(userId, newPassword)
                .onSuccess { _messages.tryEmit("Contraseña actualizada") }
                .onFailure { err -> _messages.tryEmit(err.localizedMessage ?: "Error al cambiar contraseña") }
        }
    }

    fun saveInvoice(
        existingId: String?,
        invoiceNumber: String,
        paymentEventId: String,
        settlementId: String,
        actorId: String,
        actorName: String,
        actorTaxId: String,
        concept: String,
        periodYear: Int,
        periodMonth: Int,
        profitShareCents: Long,
        spaceRentCents: Long,
        totalAmountCents: Long,
        paymentChannel: PaymentChannel,
        paymentReference: String,
        debitAccountCode: String,
        creditAccountCode: String,
        costCenter: String
    ) {
        viewModelScope.launch {
            val issuer = currentActiveUsername().ifBlank { "director" }
            repository.saveInvoice(
                existingId = existingId,
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
                issuedByUsername = issuer
            ).onSuccess { saved ->
                _messages.tryEmit("Factura ${saved.invoiceNumber} registrada para Versat Sarasola")
            }.onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "Error al registrar la factura")
            }
        }
    }

    fun markInvoicesExported(invoiceIds: List<String>) {
        viewModelScope.launch {
            repository.markInvoicesExported(invoiceIds)
        }
    }

    fun deleteInvoice(invoiceId: String) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
                .onSuccess { _messages.tryEmit("Factura eliminada") }
                .onFailure { err -> _messages.tryEmit(err.localizedMessage ?: "Error al eliminar factura") }
        }
    }

    fun emitMessage(message: String) {
        _messages.tryEmit(message)
    }

    private fun currentActiveUsername(): String {
        val state = (uiState.value as? UiState.Success)?.data ?: return ""
        return state.activeManagerUser?.username ?: ""
    }

    private fun friendlyCloudError(error: Throwable): String {
        val msg = error.localizedMessage.orEmpty()
        return when {
            msg.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Sin permisos en este código de empresa. Usa Unirse o crea un espacio propio desde ahí."
            msg.contains("NOT_FOUND", ignoreCase = true) ->
                "Registro aún no sincronizado en la nube; reintentando en cola local."
            msg.contains("UNAVAILABLE", ignoreCase = true) ||
                msg.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                msg.contains("offline", ignoreCase = true) ->
                "Señal débil o sin conexión: los cambios quedan en cola local y subirán automáticamente."
            else -> msg.ifBlank { "Error de sincronización en la nube" }
        }
    }

    private companion object {
        const val TAG = "CommerceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
