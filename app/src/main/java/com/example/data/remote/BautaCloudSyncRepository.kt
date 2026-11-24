package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.DeclarationSource
import com.example.data.model.EconomicActor
import com.example.data.model.LegalType
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentVerificationStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.security.SecureRandom

data class CloudWorkspaceSnapshot(
    val workspace: CloudWorkspace? = null,
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false
)

data class CloudPaymentEventsSnapshot(
    val events: List<CloudPaymentEvent> = emptyList(),
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false
)

data class CloudSettlementsSnapshot(
    val settlements: List<MonthlySettlement> = emptyList(),
    val createdByUidBySettlementId: Map<String, String> = emptyMap(),
    val actorUidBySettlementId: Map<String, String> = emptyMap(),
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false
)

data class CloudWorkspaceActorsSnapshot(
    val actors: List<EconomicActor> = emptyList(),
    val createdByUidByActorId: Map<String, String> = emptyMap(),
    val isFromCache: Boolean = false,
    val hasPendingWrites: Boolean = false
)

class BautaCloudSyncRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    private val auth: FirebaseAuth = Firebase.auth

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        check(!uid.isNullOrBlank()) {
            "Debes conectar tu cuenta de Google para sincronizar en la nube."
        }
        return uid
    }

    private fun isCurrentUserRootAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && isRootAdminEmail(user.email.orEmpty())
    }

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        trySend(auth.currentUser)
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    fun observeWorkspace(workspaceId: String, uid: String): Flow<CloudWorkspaceSnapshot> = flow {
        val cleanWorkspaceId = workspaceId.trim()
        require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
            "Código de empresa inválido."
        }
        require(uid.isNotBlank() && auth.currentUser?.uid == uid) {
            "Sesión no autenticada para observar el espacio."
        }
        val docRef = db.collection("workspaces").document(cleanWorkspaceId)
        val path = docRef.path
        emitAll(
            docRef.snapshots()
                .map { docSnap ->
                    val workspace = if (docSnap.exists()) {
                        docSnap.toObject(
                            CloudWorkspace::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                    CloudWorkspaceSnapshot(
                        workspace = workspace,
                        isFromCache = docSnap.metadata.isFromCache,
                        hasPendingWrites = docSnap.metadata.hasPendingWrites()
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeWorkspacePaymentEvents(
        workspaceId: String,
        uid: String,
        workspaceOwnerId: String,
        isManagerView: Boolean,
        isVerifiedRootAdmin: Boolean,
        userEmail: String = "",
        isEmailVerified: Boolean = false
    ): Flow<CloudPaymentEventsSnapshot> = flow {
        val cleanWorkspaceId = workspaceId.trim()
        require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
            "Código de empresa inválido."
        }
        require(uid.isNotBlank() && auth.currentUser?.uid == uid) {
            "Sesión no autenticada para observar pagos en vivo."
        }
        val colRef = db.collection("workspaces")
            .document(cleanWorkspaceId)
            .collection("payment_events")
        val path = colRef.path
        val cleanEmail = userEmail.trim().lowercase().take(120)

        val stream: Flow<CloudPaymentEventsSnapshot> = when {
            isManagerView && isVerifiedRootAdmin -> {
                colRef.snapshots().map { querySnapshot ->
                    toPaymentEventsSnapshot(
                        events = querySnapshot.toObjects(
                            CloudPaymentEvent::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ),
                        isFromCache = querySnapshot.metadata.isFromCache,
                        hasPendingWrites = querySnapshot.metadata.hasPendingWrites()
                    )
                }
            }
            isManagerView && workspaceOwnerId.isNotBlank() && uid == workspaceOwnerId -> {
                colRef.whereEqualTo("ownerId", workspaceOwnerId).snapshots().map { querySnapshot ->
                    toPaymentEventsSnapshot(
                        events = querySnapshot.toObjects(
                            CloudPaymentEvent::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ),
                        isFromCache = querySnapshot.metadata.isFromCache,
                        hasPendingWrites = querySnapshot.metadata.hasPendingWrites()
                    )
                }
            }
            else -> {
                val baseFlows = mutableListOf(
                    colRef.whereEqualTo("createdByUid", uid).snapshots(),
                    colRef.whereEqualTo("actorUid", uid).snapshots()
                )
                if (isEmailVerified && cleanEmail.isNotBlank()) {
                    baseFlows.add(colRef.whereEqualTo("actorEmail", cleanEmail).snapshots())
                }
                combine(baseFlows) { snapshots ->
                    val mergedById = linkedMapOf<String, CloudPaymentEvent>()
                    var fromCache = false
                    var pending = false
                    for (snap in snapshots) {
                        if (snap.metadata.isFromCache) fromCache = true
                        if (snap.metadata.hasPendingWrites()) pending = true
                        snap.toObjects(
                            CloudPaymentEvent::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).forEach { mergedById[it.id] = it }
                    }
                    toPaymentEventsSnapshot(
                        events = mergedById.values.toList(),
                        isFromCache = fromCache,
                        hasPendingWrites = pending
                    )
                }
            }
        }

        emitAll(
            stream.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
        )
    }

    private fun toPaymentEventsSnapshot(
        events: List<CloudPaymentEvent>,
        isFromCache: Boolean,
        hasPendingWrites: Boolean
    ): CloudPaymentEventsSnapshot {
        val sorted = events.sortedByDescending {
            it.clientCreatedAtMillis.takeIf { ts -> ts > 0L }
                ?: it.createdAt?.toDate()?.time
                ?: 0L
        }
        return CloudPaymentEventsSnapshot(
            events = sorted,
            isFromCache = isFromCache,
            hasPendingWrites = hasPendingWrites
        )
    }

    fun observeWorkspaceActors(
        workspaceId: String,
        uid: String,
        workspaceOwnerId: String,
        isManagerView: Boolean,
        isVerifiedRootAdmin: Boolean,
        userEmail: String = "",
        isEmailVerified: Boolean = false
    ): Flow<CloudWorkspaceActorsSnapshot> = flow {
        val cleanWorkspaceId = workspaceId.trim()
        require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
            "Código de empresa inválido."
        }
        require(uid.isNotBlank() && auth.currentUser?.uid == uid) {
            "Sesión no autenticada para observar actores."
        }
        val colRef = db.collection("workspaces")
            .document(cleanWorkspaceId)
            .collection("actors")
        val path = colRef.path
        val cleanEmail = userEmail.trim().lowercase().take(120)

        val stream: Flow<CloudWorkspaceActorsSnapshot> = when {
            isManagerView && isVerifiedRootAdmin -> {
                colRef.snapshots().map { querySnapshot ->
                    toWorkspaceActorsSnapshot(
                        cloudActors = querySnapshot.toObjects(
                            CloudWorkspaceActor::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ),
                        isFromCache = querySnapshot.metadata.isFromCache,
                        hasPendingWrites = querySnapshot.metadata.hasPendingWrites()
                    )
                }
            }
            isManagerView && workspaceOwnerId.isNotBlank() && uid == workspaceOwnerId -> {
                colRef.whereEqualTo("ownerId", workspaceOwnerId).snapshots().map { querySnapshot ->
                    toWorkspaceActorsSnapshot(
                        cloudActors = querySnapshot.toObjects(
                            CloudWorkspaceActor::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ),
                        isFromCache = querySnapshot.metadata.isFromCache,
                        hasPendingWrites = querySnapshot.metadata.hasPendingWrites()
                    )
                }
            }
            else -> {
                val baseFlows = mutableListOf(
                    colRef.whereEqualTo("createdByUid", uid).snapshots()
                )
                if (isEmailVerified && cleanEmail.isNotBlank()) {
                    baseFlows.add(colRef.whereEqualTo("actorEmail", cleanEmail).snapshots())
                }
                combine(baseFlows) { snapshots ->
                    val mergedById = linkedMapOf<String, CloudWorkspaceActor>()
                    var fromCache = false
                    var pending = false
                    for (snap in snapshots) {
                        if (snap.metadata.isFromCache) fromCache = true
                        if (snap.metadata.hasPendingWrites()) pending = true
                        snap.toObjects(
                            CloudWorkspaceActor::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).forEach { mergedById[it.id] = it }
                    }
                    toWorkspaceActorsSnapshot(
                        cloudActors = mergedById.values.toList(),
                        isFromCache = fromCache,
                        hasPendingWrites = pending
                    )
                }
            }
        }

        emitAll(
            stream.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
        )
    }

    private fun toWorkspaceActorsSnapshot(
        cloudActors: List<CloudWorkspaceActor>,
        isFromCache: Boolean,
        hasPendingWrites: Boolean
    ): CloudWorkspaceActorsSnapshot = CloudWorkspaceActorsSnapshot(
        actors = cloudActors.map { it.toDomain() },
        createdByUidByActorId = cloudActors.associate { it.id to it.createdByUid },
        isFromCache = isFromCache,
        hasPendingWrites = hasPendingWrites
    )

    fun observeWorkspaceSettlements(
        workspaceId: String,
        uid: String,
        workspaceOwnerId: String,
        isManagerView: Boolean,
        isVerifiedRootAdmin: Boolean,
        userEmail: String = "",
        isEmailVerified: Boolean = false
    ): Flow<CloudSettlementsSnapshot> = flow {
        val cleanWorkspaceId = workspaceId.trim()
        require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
            "Código de empresa inválido."
        }
        require(uid.isNotBlank() && auth.currentUser?.uid == uid) {
            "Sesión no autenticada para observar liquidaciones compartidas."
        }
        val colRef = db.collection("workspaces")
            .document(cleanWorkspaceId)
            .collection("settlements")
        val path = colRef.path
        val cleanEmail = userEmail.trim().lowercase().take(120)

        val stream: Flow<CloudSettlementsSnapshot> = when {
            isManagerView && isVerifiedRootAdmin -> {
                colRef.snapshots().map { querySnapshot ->
                    toWorkspaceSettlementsSnapshot(querySnapshot.toObjects(
                        CloudWorkspaceSettlement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ), querySnapshot.metadata.isFromCache, querySnapshot.metadata.hasPendingWrites())
                }
            }
            isManagerView && workspaceOwnerId.isNotBlank() && uid == workspaceOwnerId -> {
                colRef.whereEqualTo("ownerId", workspaceOwnerId).snapshots().map { querySnapshot ->
                    toWorkspaceSettlementsSnapshot(querySnapshot.toObjects(
                        CloudWorkspaceSettlement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ), querySnapshot.metadata.isFromCache, querySnapshot.metadata.hasPendingWrites())
                }
            }
            else -> {
                val baseFlows = mutableListOf(
                    colRef.whereEqualTo("createdByUid", uid).snapshots(),
                    colRef.whereEqualTo("actorUid", uid).snapshots()
                )
                if (isEmailVerified && cleanEmail.isNotBlank()) {
                    baseFlows.add(colRef.whereEqualTo("actorEmail", cleanEmail).snapshots())
                }
                combine(baseFlows) { snapshots ->
                    val mergedById = linkedMapOf<String, CloudWorkspaceSettlement>()
                    var fromCache = false
                    var pending = false
                    for (snap in snapshots) {
                        if (snap.metadata.isFromCache) fromCache = true
                        if (snap.metadata.hasPendingWrites()) pending = true
                        snap.toObjects(
                            CloudWorkspaceSettlement::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ).forEach { mergedById[it.id] = it }
                    }
                    toWorkspaceSettlementsSnapshot(
                        cloudList = mergedById.values.toList(),
                        isFromCache = fromCache,
                        hasPendingWrites = pending
                    )
                }
            }
        }

        emitAll(
            stream.catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
        )
    }

    private fun toWorkspaceSettlementsSnapshot(
        cloudList: List<CloudWorkspaceSettlement>,
        isFromCache: Boolean,
        hasPendingWrites: Boolean
    ): CloudSettlementsSnapshot {
        val domainList = cloudList.map { it.toDomain() }
            .sortedWith(
                compareByDescending<MonthlySettlement> { it.periodYear }
                    .thenByDescending { it.periodMonth }
                    .thenBy { it.actorName.lowercase() }
            )
        return CloudSettlementsSnapshot(
            settlements = domainList,
            createdByUidBySettlementId = cloudList.associate { it.id to it.createdByUid },
            actorUidBySettlementId = cloudList.associate { it.id to it.actorUid },
            isFromCache = isFromCache,
            hasPendingWrites = hasPendingWrites
        )
    }

    suspend fun ensureWorkspaceForManager(
        preferredWorkspaceId: String?,
        paymentConfig: PaymentConfig,
        forceCreateNew: Boolean = false
    ): Result<CloudWorkspace> {
        var opPath = "workspaces"
        return try {
            val uid = requireUserId()
            val isRootAdmin = isCurrentUserRootAdmin()

            if (!forceCreateNew) {
                val candidateIds = listOfNotNull(
                    preferredWorkspaceId?.trim()?.takeIf { it.isNotBlank() && VALID_ID_REGEX.matches(it) },
                    DEFAULT_MUNICIPAL_WORKSPACE_ID
                ).distinct()

                for (candidateId in candidateIds) {
                    val docRef = db.collection("workspaces").document(candidateId)
                    opPath = docRef.path
                    val snap = getDocumentWithCacheFallback(docRef)
                    if (snap != null && snap.exists()) {
                        val existing = snap.toObject(
                            CloudWorkspace::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (existing != null && (existing.ownerId == uid || isRootAdmin)) {
                            joinMemberBestEffort(docRef, existing, uid)
                            return Result.success(
                                if (uid in existing.memberUids) existing else existing.copy(
                                    memberUids = (existing.memberUids + uid).distinct()
                                )
                            )
                        }
                    } else if (candidateId == DEFAULT_MUNICIPAL_WORKSPACE_ID) {
                        // Claim the canonical municipal workspace "comercio-bauta" for the manager
                        return createWorkspaceDocument(docRef, uid, paymentConfig)
                    }
                }

                // Check if manager already owns another workspace
                opPath = "workspaces"
                val query = db.collection("workspaces")
                    .whereEqualTo("ownerId", uid)
                    .limit(1)
                val existingQuery = try {
                    withTimeoutOrNull(SERVER_READ_TIMEOUT_MS) { query.get().await() }
                        ?: query.get(Source.CACHE).await()
                } catch (_: Exception) {
                    runCatching { query.get(Source.CACHE).await() }.getOrNull()
                }
                val firstDoc = existingQuery?.documents?.firstOrNull()
                val ownedWorkspace = firstDoc?.toObject(
                    CloudWorkspace::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (ownedWorkspace != null) {
                    return Result.success(ownedWorkspace)
                }
            }

            val workspaceId = if (!forceCreateNew) {
                val canonicalRef = db.collection("workspaces").document(DEFAULT_MUNICIPAL_WORKSPACE_ID)
                val canonicalSnap = getDocumentWithCacheFallback(canonicalRef)
                if (canonicalSnap == null || !canonicalSnap.exists()) {
                    DEFAULT_MUNICIPAL_WORKSPACE_ID
                } else {
                    generateWorkspaceCode()
                }
            } else {
                generateWorkspaceCode()
            }
            val docRef = db.collection("workspaces").document(workspaceId)
            opPath = docRef.path
            createWorkspaceDocument(docRef, uid, paymentConfig)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, opPath)
            Result.failure(e)
        }
    }

    suspend fun connectActorToSharedWorkspace(
        preferredWorkspaceId: String?
    ): Result<CloudWorkspace?> {
        val candidateIds = listOfNotNull(
            preferredWorkspaceId?.trim()?.takeIf { it.isNotBlank() && VALID_ID_REGEX.matches(it) },
            DEFAULT_MUNICIPAL_WORKSPACE_ID
        ).distinct()
        var opPath = "workspaces/${candidateIds.firstOrNull() ?: DEFAULT_MUNICIPAL_WORKSPACE_ID}"
        return try {
            val uid = requireUserId()
            for (workspaceId in candidateIds) {
                val docRef = db.collection("workspaces").document(workspaceId)
                opPath = docRef.path
                val snap = getDocumentWithCacheFallback(docRef)
                if (snap != null && snap.exists()) {
                    val existing = snap.toObject(
                        CloudWorkspace::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (existing != null) {
                        joinMemberBestEffort(docRef, existing, uid)
                        return Result.success(
                            if (uid in existing.memberUids) existing else existing.copy(
                                memberUids = (existing.memberUids + uid).distinct()
                            )
                        )
                    }
                }
            }
            Result.success(null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, opPath)
            Result.failure(e)
        }
    }

    private suspend fun createWorkspaceDocument(
        docRef: com.google.firebase.firestore.DocumentReference,
        uid: String,
        paymentConfig: PaymentConfig
    ): Result<CloudWorkspace> {
        val safeHolder = paymentConfig.accountHolder.trim().ifBlank { "Empresa Municipal de Comercio Bauta" }.take(120)
        val safeAccount = paymentConfig.bankAccount.trim().ifBlank { PaymentConfig.DEFAULT_BAUTA_BANK_ACCOUNT }.take(80)
        val safeBankName = paymentConfig.bankName.trim().take(80)
        val safeQr = paymentConfig.qrPaymentUrl.trim().take(300)

        val payload = mapOf(
            "ownerId" to uid,
            "name" to "Comercio Municipal Bauta",
            "bankName" to safeBankName,
            "bankAccount" to safeAccount,
            "accountHolder" to safeHolder,
            "qrPaymentUrl" to safeQr,
            "memberUids" to listOf(uid),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        awaitMutationOrQueue(docRef.set(payload))
        return Result.success(
            CloudWorkspace(
                id = docRef.id,
                ownerId = uid,
                name = "Comercio Municipal Bauta",
                bankName = safeBankName,
                bankAccount = safeAccount,
                accountHolder = safeHolder,
                qrPaymentUrl = safeQr,
                memberUids = listOf(uid)
            )
        )
    }

    private suspend fun joinMemberBestEffort(
        docRef: com.google.firebase.firestore.DocumentReference,
        workspace: CloudWorkspace,
        uid: String
    ) {
        if (uid in workspace.memberUids || workspace.memberUids.size >= 100) return
        runCatching {
            awaitMutationOrQueue(
                docRef.update(
                    mapOf(
                        "memberUids" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
        }
    }

    suspend fun joinWorkspaceByCode(rawCode: String): Result<CloudWorkspace> {
        val cleanCode = rawCode.trim().lowercase().take(128)
        var docPath = "workspaces/$cleanCode"
        return try {
            val uid = requireUserId()
            require(cleanCode.isNotEmpty() && VALID_ID_REGEX.matches(cleanCode)) {
                "Ingresa un código de empresa válido (letras, números o guiones)."
            }
            val docRef = db.collection("workspaces").document(cleanCode)
            docPath = docRef.path
            val snapshot = try {
                withTimeoutOrNull(SERVER_READ_TIMEOUT_MS) { docRef.get().await() }
                    ?: runCatching { docRef.get(Source.CACHE).await() }.getOrNull()
            } catch (e: Exception) {
                val cached = runCatching { docRef.get(Source.CACHE).await() }.getOrNull()
                if (cached != null && cached.exists()) cached else throw e
            }
            if (snapshot == null) {
                return Result.failure(
                    IllegalStateException("Sin conexión suficiente para buscar el código '$cleanCode'. Verifica datos móviles o Wi-Fi.")
                )
            }
            if (!snapshot.exists()) {
                return Result.failure(
                    IllegalArgumentException("No se encontró ninguna empresa con el código '$cleanCode'.")
                )
            }
            val workspace = snapshot.toObject(
                CloudWorkspace::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(
                IllegalStateException("No se pudo leer la configuración de la empresa '$cleanCode'.")
            )

            joinMemberBestEffort(docRef, workspace, uid)
            Result.success(
                if (uid in workspace.memberUids) workspace else workspace.copy(
                    memberUids = (workspace.memberUids + uid).distinct()
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun syncWorkspacePaymentConfig(
        workspaceId: String,
        config: PaymentConfig,
        knownOwnerId: String? = null
    ): Result<Unit> {
        val cleanWorkspaceId = workspaceId.trim()
        var docPath = "workspaces/$cleanWorkspaceId"
        return try {
            val uid = requireUserId()
            val isRootAdmin = isCurrentUserRootAdmin()
            require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
                "Espacio de trabajo inválido."
            }
            val docRef = db.collection("workspaces").document(cleanWorkspaceId)
            docPath = docRef.path
            val ownerId = knownOwnerId?.takeIf { it.isNotBlank() } ?: run {
                val snap = getDocumentWithCacheFallback(docRef)
                snap?.getString("ownerId").orEmpty()
            }
            if (ownerId.isNotBlank() && ownerId != uid && !isRootAdmin) {
                return Result.failure(
                    IllegalStateException("Solo la cuenta administradora de la empresa ($cleanWorkspaceId) actualiza la cuenta bancaria en la nube.")
                )
            }
            awaitMutationOrQueue(
                docRef.update(
                    mapOf(
                        "bankName" to config.bankName.trim().take(80),
                        "bankAccount" to config.bankAccount.trim().take(80),
                        "accountHolder" to config.accountHolder.trim().take(120),
                        "qrPaymentUrl" to config.qrPaymentUrl.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushOrUpdateWorkspaceActor(
        workspaceId: String,
        actor: EconomicActor,
        knownWorkspace: CloudWorkspace? = null,
        knownCreatedByUid: String? = null
    ): Result<Unit> {
        val cleanWorkspaceId = workspaceId.trim()
        var docPath = "workspaces/$cleanWorkspaceId/actors/${actor.id}"
        return try {
            val uid = requireUserId()
            val isRootAdmin = isCurrentUserRootAdmin()
            require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
                "Espacio de trabajo inválido."
            }
            val cleanActorId = actor.id.trim()
            require(cleanActorId.isNotBlank() && VALID_ID_REGEX.matches(cleanActorId)) {
                "ID de actor inválido."
            }

            val workspace = resolveWorkspaceOrFail(cleanWorkspaceId, knownWorkspace)
                ?: return Result.failure(IllegalStateException("El espacio $cleanWorkspaceId aún no existe en la nube."))

            val docRef = db.collection("workspaces")
                .document(cleanWorkspaceId)
                .collection("actors")
                .document(cleanActorId)
            docPath = docRef.path

            val existingSnap = getDocumentWithCacheFallback(docRef)
            val exists = existingSnap != null && existingSnap.exists()
            val isManager = workspace.ownerId == uid || isRootAdmin

            val safeName = actor.name.trim().ifBlank { "Actor Bauta" }.take(100)
            val safeLegal = LegalType.fromRaw(actor.legalType).name
            val safeRep = actor.representativeName.trim().ifBlank { safeName }.take(100)
            val safePhone = actor.phone.trim().take(30)
            val safeTax = actor.taxId.trim().take(40)
            val currentVerifiedEmail = if (auth.currentUser?.isEmailVerified == true) {
                auth.currentUser?.email?.trim()?.lowercase()?.take(120).orEmpty()
            } else {
                ""
            }
            val normalizedInputEmail = actor.actorEmail.trim().lowercase().take(120)
            val resolvedEmail = normalizedInputEmail.ifBlank {
                if (!isManager) currentVerifiedEmail else ""
            }

            if (exists) {
                val createdByUid = knownCreatedByUid?.takeIf { it.isNotBlank() }
                    ?: existingSnap?.getString("createdByUid").orEmpty()
                val existingEmail = existingSnap?.getString("actorEmail").orEmpty().trim().lowercase()
                val isEmailMatchedActor = currentVerifiedEmail.isNotBlank() && existingEmail == currentVerifiedEmail
                if (isManager) {
                    val fields = mapOf(
                        "actorEmail" to normalizedInputEmail,
                        "name" to safeName,
                        "legalType" to safeLegal,
                        "representativeName" to safeRep,
                        "phone" to safePhone,
                        "taxId" to safeTax,
                        "localeId" to actor.localeId.trim().take(128),
                        "localeName" to actor.localeName.trim().take(100),
                        "monthlyRentCents" to actor.monthlyRentCents.coerceIn(0L, 100_000_000_000L),
                        "profitSharePercent" to actor.profitSharePercent.coerceIn(0L, 100L),
                        "active" to actor.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    awaitMutationOrQueue(docRef.update(fields))
                } else if (createdByUid == uid || isEmailMatchedActor) {
                    val fields = mapOf(
                        "actorEmail" to resolvedEmail.ifBlank { existingEmail },
                        "name" to safeName,
                        "legalType" to safeLegal,
                        "representativeName" to safeRep,
                        "phone" to safePhone,
                        "taxId" to safeTax,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    awaitMutationOrQueue(docRef.update(fields))
                }
            } else {
                val payload = mapOf(
                    "workspaceId" to cleanWorkspaceId,
                    "ownerId" to workspace.ownerId,
                    "createdByUid" to uid,
                    "actorEmail" to resolvedEmail,
                    "name" to safeName,
                    "legalType" to safeLegal,
                    "representativeName" to safeRep,
                    "phone" to safePhone,
                    "taxId" to safeTax,
                    "localeId" to actor.localeId.trim().take(128),
                    "localeName" to actor.localeName.trim().take(100),
                    "monthlyRentCents" to actor.monthlyRentCents.coerceIn(0L, 100_000_000_000L),
                    "profitSharePercent" to actor.profitSharePercent.coerceIn(0L, 100L),
                    "active" to actor.active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                awaitMutationOrQueue(docRef.set(payload))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushOrUpdateWorkspaceSettlement(
        workspaceId: String,
        settlement: MonthlySettlement,
        knownWorkspace: CloudWorkspace? = null,
        knownExistingInCloud: Boolean = false,
        knownActorUid: String? = null,
        knownActorEmail: String = ""
    ): Result<Unit> {
        val cleanWorkspaceId = workspaceId.trim()
        var docPath = "workspaces/$cleanWorkspaceId/settlements/${settlement.id}"
        return try {
            val uid = requireUserId()
            val isRootAdmin = isCurrentUserRootAdmin()
            require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
                "Espacio de trabajo inválido."
            }
            val cleanId = settlement.id.trim()
            require(cleanId.isNotBlank() && VALID_ID_REGEX.matches(cleanId)) {
                "ID de liquidación inválido."
            }

            val workspace = resolveWorkspaceOrFail(cleanWorkspaceId, knownWorkspace)
                ?: return Result.failure(IllegalStateException("El espacio $cleanWorkspaceId aún no existe en la nube."))

            val docRef = db.collection("workspaces")
                .document(cleanWorkspaceId)
                .collection("settlements")
                .document(cleanId)
            docPath = docRef.path

            val existingSnap = getDocumentWithCacheFallback(docRef)
            val exists = knownExistingInCloud || (existingSnap != null && existingSnap.exists())
            val isManager = workspace.ownerId == uid || isRootAdmin

            val safeDeclaredBy = if (isManager) {
                DeclarationSource.fromRaw(settlement.declaredBy).name
            } else {
                DeclarationSource.ACTOR.name
            }
            val safePaidCents = settlement.paidCents.coerceIn(0L, 200_000_000_000L)
            val safeActorUid = knownActorUid?.trim()?.take(128).orEmpty()
            val cleanActorEmail = knownActorEmail.trim().lowercase().take(120)
            val currentVerifiedEmail = if (auth.currentUser?.isEmailVerified == true) {
                auth.currentUser?.email?.trim()?.lowercase()?.take(120).orEmpty()
            } else {
                ""
            }

            val mutableFields = mutableMapOf<String, Any>(
                "actorId" to settlement.actorId.trim().ifBlank { "actor_${cleanId.take(12)}" }.take(128),
                "actorName" to settlement.actorName.trim().ifBlank { "Actor Bauta" }.take(100),
                "localeId" to settlement.localeId.trim().take(128),
                "localeName" to settlement.localeName.trim().take(100),
                "periodYear" to settlement.periodYear.coerceIn(2020L, 2100L),
                "periodMonth" to settlement.periodMonth.coerceIn(1L, 12L),
                "grossIncomeCents" to settlement.grossIncomeCents.coerceIn(0L, 100_000_000_000L),
                "operatingExpensesCents" to settlement.operatingExpensesCents.coerceIn(0L, 100_000_000_000L),
                "netProfitCents" to settlement.netProfitCents.coerceIn(-100_000_000_000L, 100_000_000_000L),
                "profitSharePercent" to settlement.profitSharePercent.coerceIn(0L, 100L),
                "profitShareCents" to settlement.profitShareCents.coerceIn(0L, 100_000_000_000L),
                "spaceRentCents" to settlement.spaceRentCents.coerceIn(0L, 100_000_000_000L),
                "totalDueCents" to settlement.totalDueCents.coerceIn(0L, 200_000_000_000L),
                "paidCents" to safePaidCents,
                "paymentStatus" to PaymentStatus.fromRaw(settlement.paymentStatus).name,
                "declaredBy" to safeDeclaredBy,
                "paymentChannel" to PaymentChannel.fromRaw(settlement.paymentChannel).name,
                "paymentReference" to settlement.paymentReference.trim().take(80),
                "notes" to settlement.notes.trim().take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (exists) {
                if (isManager && safeActorUid.isNotBlank()) {
                    mutableFields["actorUid"] = safeActorUid
                }
                if (isManager && cleanActorEmail.isNotBlank()) {
                    mutableFields["actorEmail"] = cleanActorEmail
                }
                val hasPaidAt = existingSnap?.getTimestamp("paidAt") != null
                if (safePaidCents > 0L && !hasPaidAt) {
                    mutableFields["paidAt"] = FieldValue.serverTimestamp()
                }
                awaitMutationOrQueue(docRef.update(mutableFields))
            } else {
                mutableFields["workspaceId"] = cleanWorkspaceId
                mutableFields["ownerId"] = workspace.ownerId
                mutableFields["createdByUid"] = uid
                if (safeActorUid.isNotBlank()) {
                    mutableFields["actorUid"] = safeActorUid
                } else if (!isManager) {
                    mutableFields["actorUid"] = uid
                }
                val resolvedCreateEmail = cleanActorEmail.ifBlank {
                    if (!isManager) currentVerifiedEmail else ""
                }
                if (resolvedCreateEmail.isNotBlank()) {
                    mutableFields["actorEmail"] = resolvedCreateEmail
                }
                mutableFields["createdAt"] = FieldValue.serverTimestamp()
                if (safePaidCents > 0L) {
                    mutableFields["paidAt"] = FieldValue.serverTimestamp()
                }
                awaitMutationOrQueue(docRef.set(mutableFields))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteWorkspaceSettlement(
        workspaceId: String,
        settlementId: String
    ): Result<Unit> {
        val cleanWorkspaceId = workspaceId.trim()
        var docPath = "workspaces/$cleanWorkspaceId/settlements/$settlementId"
        return try {
            requireUserId()
            require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
                "Espacio de trabajo inválido."
            }
            val cleanId = settlementId.trim()
            require(cleanId.isNotBlank() && VALID_ID_REGEX.matches(cleanId)) {
                "ID de liquidación inválido."
            }
            val docRef = db.collection("workspaces")
                .document(cleanWorkspaceId)
                .collection("settlements")
                .document(cleanId)
            docPath = docRef.path
            awaitMutationOrQueue(docRef.delete())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushPaymentEvent(
        workspaceId: String,
        event: PaymentEvent,
        knownWorkspace: CloudWorkspace? = null,
        knownActorUid: String = "",
        knownActorEmail: String = ""
    ): Result<Unit> {
        val cleanWorkspaceId = workspaceId.trim()
        var docPath = "workspaces/$cleanWorkspaceId/payment_events/${event.id}"
        return try {
            val uid = requireUserId()
            val isRootAdmin = isCurrentUserRootAdmin()
            require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
                "Espacio de trabajo inválido."
            }
            require(event.id.isNotBlank() && VALID_ID_REGEX.matches(event.id)) {
                "ID de evento inválido."
            }

            val workspaceRef = db.collection("workspaces").document(cleanWorkspaceId)
            val workspace = resolveWorkspaceOrFail(cleanWorkspaceId, knownWorkspace)
                ?: return Result.failure(
                    IllegalStateException("El espacio de empresa $cleanWorkspaceId aún no existe en la nube.")
                )

            val isManager = workspace.ownerId == uid || isRootAdmin
            val safeReportedBy = if (isManager) {
                DeclarationSource.fromRaw(event.reportedBy).name
            } else {
                DeclarationSource.ACTOR.name
            }
            val safeVerificationStatus = if (isManager) {
                PaymentVerificationStatus.fromRaw(event.verificationStatus).name
            } else {
                PaymentVerificationStatus.POR_VERIFICAR.name
            }
            val currentVerifiedEmail = if (auth.currentUser?.isEmailVerified == true) {
                auth.currentUser?.email?.trim()?.lowercase()?.take(120).orEmpty()
            } else {
                ""
            }
            val resolvedActorEmail = knownActorEmail.trim().lowercase().take(120)
                .ifBlank { if (!isManager) currentVerifiedEmail else "" }
                .takeIf { it.isNotEmpty() }
            val resolvedActorUid = knownActorUid.trim().take(128)
                .ifBlank { if (!isManager) uid else "" }
                .takeIf { it.isNotEmpty() }

            val eventRef = workspaceRef.collection("payment_events").document(event.id)
            docPath = eventRef.path

            val existingSnap = getDocumentWithCacheFallback(eventRef)
            if (existingSnap != null && existingSnap.exists()) {
                val existingEmail = existingSnap.getString("actorEmail").orEmpty().trim().lowercase()
                if (isManager && resolvedActorEmail != null && existingEmail != resolvedActorEmail) {
                    awaitMutationOrQueue(
                        eventRef.update(
                            mapOf(
                                "actorEmail" to resolvedActorEmail,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    )
                }
                return Result.success(Unit)
            }

            val memberList = listOf(workspace.ownerId, uid).distinct()
            val payload = mapOf<String, Any?>(
                "workspaceId" to cleanWorkspaceId,
                "ownerId" to workspace.ownerId,
                "memberUids" to memberList,
                "createdByUid" to uid,
                "settlementId" to event.settlementId.trim().take(128).takeIf { it.isNotEmpty() },
                "actorUid" to resolvedActorUid,
                "actorEmail" to resolvedActorEmail,
                "actorId" to event.actorId.trim().take(128).takeIf { it.isNotEmpty() },
                "actorName" to event.actorName.trim().ifBlank { "Actor Bauta" }.take(120),
                "periodYear" to event.periodYear.coerceIn(2020L, 2100L),
                "periodMonth" to event.periodMonth.coerceIn(1L, 12L),
                "amountCents" to event.amountCents.coerceIn(0L, 200_000_000_000L),
                "paymentChannel" to PaymentChannel.fromRaw(event.paymentChannel).name,
                "paymentReference" to event.paymentReference.trim().take(80).takeIf { it.isNotEmpty() },
                "reportedBy" to safeReportedBy,
                "verificationStatus" to safeVerificationStatus,
                "verifiedByUsername" to event.verifiedByUsername.trim().take(80).takeIf { it.isNotEmpty() },
                "note" to event.note.trim().take(300).takeIf { it.isNotEmpty() },
                "clientCreatedAtMillis" to event.createdAtMillis.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            awaitMutationOrQueue(eventRef.set(payload))
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun verifyCloudPaymentEvent(
        workspaceId: String,
        eventId: String,
        status: PaymentVerificationStatus,
        verifiedByUsername: String,
        note: String,
        knownOwnerId: String? = null,
        knownWorkspace: CloudWorkspace? = null,
        fallbackEvent: PaymentEvent? = null
    ): Result<Unit> {
        val cleanWorkspaceId = workspaceId.trim()
        val cleanEventId = eventId.trim()
        var docPath = "workspaces/$cleanWorkspaceId/payment_events/$cleanEventId"
        return try {
            val uid = requireUserId()
            val isRootAdmin = isCurrentUserRootAdmin()
            require(cleanWorkspaceId.isNotBlank() && VALID_ID_REGEX.matches(cleanWorkspaceId)) {
                "Espacio de trabajo inválido."
            }
            require(cleanEventId.isNotBlank() && VALID_ID_REGEX.matches(cleanEventId)) {
                "Evento inválido."
            }
            val eventRef = db.collection("workspaces")
                .document(cleanWorkspaceId)
                .collection("payment_events")
                .document(cleanEventId)
            docPath = eventRef.path
            if (!knownOwnerId.isNullOrBlank() && knownOwnerId != uid && !isRootAdmin) {
                return Result.failure(
                    IllegalStateException("Solo la cuenta propietaria puede confirmar pagos en la nube.")
                )
            }
            val existingSnap = getDocumentWithCacheFallback(eventRef)
            if (existingSnap == null || !existingSnap.exists()) {
                if (fallbackEvent != null && (knownOwnerId.isNullOrBlank() || knownOwnerId == uid || isRootAdmin)) {
                    return pushPaymentEvent(
                        workspaceId = cleanWorkspaceId,
                        event = fallbackEvent.copy(
                            verificationStatus = status.name,
                            verifiedByUsername = verifiedByUsername.trim().take(80),
                            note = note.trim().take(300)
                        ),
                        knownWorkspace = knownWorkspace
                    )
                }
                return Result.success(Unit)
            }
            awaitMutationOrQueue(
                eventRef.update(
                    mapOf(
                        "verificationStatus" to status.name,
                        "verifiedByUsername" to verifiedByUsername.trim().take(80),
                        "note" to note.trim().take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeCloudSettlements(uid: String): Flow<CloudSettlementsSnapshot> = flow {
        require(uid.isNotBlank() && auth.currentUser?.uid == uid) {
            "Sesión no autenticada para observar liquidaciones en Firestore."
        }
        val path = "users/$uid/settlements"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("settlements")
                .snapshots()
                .map { querySnapshot ->
                    val list = querySnapshot.toObjects(
                        CloudSettlement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.toDomain() }
                        .sortedWith(
                            compareByDescending<MonthlySettlement> { it.periodYear }
                                .thenByDescending { it.periodMonth }
                                .thenBy { it.actorName.lowercase() }
                        )
                    CloudSettlementsSnapshot(
                        settlements = list,
                        isFromCache = querySnapshot.metadata.isFromCache,
                        hasPendingWrites = querySnapshot.metadata.hasPendingWrites()
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun pushOrUpdateSettlement(
        settlement: MonthlySettlement,
        knownExistingInCloud: Boolean = false
    ): Result<Unit> {
        var docPath = "users/unknown/settlements/${settlement.id}"
        return try {
            val uid = requireUserId()
            val cleanId = settlement.id.trim()
            require(cleanId.isNotBlank() && VALID_ID_REGEX.matches(cleanId)) {
                "ID de liquidación inválido."
            }
            val docRef = db.collection("users")
                .document(uid)
                .collection("settlements")
                .document(cleanId)
            docPath = docRef.path

            val existingSnap = getDocumentWithCacheFallback(docRef)
            val exists = knownExistingInCloud || (existingSnap != null && existingSnap.exists())

            val safePaidCents = settlement.paidCents.coerceIn(0L, 200_000_000_000L)
            val fields = mutableMapOf<String, Any>(
                "actorId" to settlement.actorId.trim().ifBlank { "actor_${cleanId.take(12)}" }.take(128),
                "actorName" to settlement.actorName.trim().ifBlank { "Actor Bauta" }.take(100),
                "localeId" to settlement.localeId.trim().take(128),
                "localeName" to settlement.localeName.trim().take(100),
                "periodYear" to settlement.periodYear.coerceIn(2020L, 2100L),
                "periodMonth" to settlement.periodMonth.coerceIn(1L, 12L),
                "grossIncomeCents" to settlement.grossIncomeCents.coerceIn(0L, 100_000_000_000L),
                "operatingExpensesCents" to settlement.operatingExpensesCents.coerceIn(0L, 100_000_000_000L),
                "netProfitCents" to settlement.netProfitCents.coerceIn(-100_000_000_000L, 100_000_000_000L),
                "profitSharePercent" to settlement.profitSharePercent.coerceIn(0L, 100L),
                "profitShareCents" to settlement.profitShareCents.coerceIn(0L, 100_000_000_000L),
                "spaceRentCents" to settlement.spaceRentCents.coerceIn(0L, 100_000_000_000L),
                "totalDueCents" to settlement.totalDueCents.coerceIn(0L, 200_000_000_000L),
                "paidCents" to safePaidCents,
                "paymentStatus" to PaymentStatus.fromRaw(settlement.paymentStatus).name,
                "declaredBy" to DeclarationSource.fromRaw(settlement.declaredBy).name,
                "paymentChannel" to PaymentChannel.fromRaw(settlement.paymentChannel).name,
                "paymentReference" to settlement.paymentReference.trim().take(80),
                "notes" to settlement.notes.trim().take(300),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            if (exists) {
                val hasPaidAt = existingSnap?.getTimestamp("paidAt") != null
                if (safePaidCents > 0L && !hasPaidAt) {
                    fields["paidAt"] = FieldValue.serverTimestamp()
                }
                awaitMutationOrQueue(docRef.update(fields))
            } else {
                fields["createdAt"] = FieldValue.serverTimestamp()
                if (safePaidCents > 0L) {
                    fields["paidAt"] = FieldValue.serverTimestamp()
                }
                awaitMutationOrQueue(docRef.set(fields))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudSettlement(settlementId: String): Result<Unit> {
        var docPath = "users/unknown/settlements/$settlementId"
        return try {
            val uid = requireUserId()
            val cleanId = settlementId.trim()
            require(cleanId.isNotBlank() && VALID_ID_REGEX.matches(cleanId)) {
                "ID de liquidación inválido."
            }
            val docRef = db.collection("users")
                .document(uid)
                .collection("settlements")
                .document(cleanId)
            docPath = docRef.path
            awaitMutationOrQueue(docRef.delete())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun resolveWorkspaceOrFail(
        cleanWorkspaceId: String,
        knownWorkspace: CloudWorkspace?
    ): CloudWorkspace? {
        knownWorkspace?.takeIf { it.id == cleanWorkspaceId && it.ownerId.isNotBlank() }?.let { return it }
        val workspaceRef = db.collection("workspaces").document(cleanWorkspaceId)
        val snap = getDocumentWithCacheFallback(workspaceRef) ?: return null
        if (!snap.exists()) return null
        return snap.toObject(
            CloudWorkspace::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        )
    }

    private suspend fun getDocumentWithCacheFallback(
        docRef: com.google.firebase.firestore.DocumentReference
    ): DocumentSnapshot? {
        val cached = try {
            docRef.get(Source.CACHE).await()
        } catch (_: Exception) {
            null
        }
        if (cached != null && cached.exists()) return cached
        return try {
            withTimeoutOrNull(SERVER_READ_TIMEOUT_MS) { docRef.get().await() } ?: cached
        } catch (_: Exception) {
            cached
        }
    }

    private suspend fun awaitMutationOrQueue(task: com.google.android.gms.tasks.Task<Void>) {
        // Offline writes in Cuba stay safely in Firestore's 50 MB persistent disk queue;
        // do not hang UI spinners if the server ACK cannot arrive immediately.
        withTimeoutOrNull(MUTATION_ACK_TIMEOUT_MS) {
            task.await()
        }
    }

    companion object {
        const val DEFAULT_MUNICIPAL_WORKSPACE_ID = "comercio-bauta"
        const val ROOT_ADMIN_EMAIL = "amena4430@gmail.com"
        private const val TAG = "BautaCloudSyncRepo"
        private const val PERSISTENT_CACHE_SIZE_BYTES = 50L * 1024L * 1024L
        private const val SERVER_READ_TIMEOUT_MS = 5_000L
        private const val MUTATION_ACK_TIMEOUT_MS = 5_000L
        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
        private const val CODE_ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"
        private val random = SecureRandom()

        fun isRootAdminEmail(email: String): Boolean =
            email.trim().equals(ROOT_ADMIN_EMAIL, ignoreCase = true)

        fun createConfiguredFirestore(context: Context): FirebaseFirestore {
            val dbId = context.getString(R.string.firestore_database_id)
            val firestore = FirebaseFirestore.getInstance(dbId)
            runCatching {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(
                        PersistentCacheSettings.newBuilder()
                            .setSizeBytes(PERSISTENT_CACHE_SIZE_BYTES)
                            .build()
                    )
                    .build()
                firestore.firestoreSettings = settings
            }.onFailure { err ->
                Log.d(TAG, "Firestore settings already initialized: ${err.message}")
            }
            return firestore
        }

        private fun generateWorkspaceCode(): String {
            val suffix = buildString(6) {
                repeat(6) {
                    append(CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)])
                }
            }
            return "bauta-$suffix"
        }
    }
}
