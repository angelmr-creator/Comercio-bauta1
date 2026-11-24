package com.example.data.repository

import android.content.Context
import com.example.data.local.ActorEntity
import com.example.data.local.BautaDao
import com.example.data.local.BautaDatabase
import com.example.data.local.InvoiceEntity
import com.example.data.local.LocalPreferencesStore
import com.example.data.local.LocaleEntity
import com.example.data.local.ManagerUserEntity
import com.example.data.local.PaymentEventEntity
import com.example.data.local.SettlementEntity
import com.example.data.model.DeclarationSource
import com.example.data.model.EconomicActor
import com.example.data.model.InvoiceRecord
import com.example.data.model.LegalType
import com.example.data.model.LocaleSpace
import com.example.data.model.LocaleStatus
import com.example.data.model.ManagerRole
import com.example.data.model.ManagerUser
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentChannel
import com.example.data.model.PaymentConfig
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentStatus
import com.example.data.model.PaymentVerificationStatus
import com.example.data.remote.CloudPaymentEvent
import com.example.util.PasswordHasher
import com.example.util.SettlementCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

data class SaveSettlementResult(
    val settlement: MonthlySettlement,
    val createdEvent: PaymentEvent?
)

class BautaCommerceRepository(
    private val dao: BautaDao,
    private val prefs: LocalPreferencesStore
) {
    constructor(context: Context) : this(
        dao = BautaDatabase.getInstance(context).bautaDao(),
        prefs = LocalPreferencesStore(context.applicationContext)
    )

    fun observePaymentConfig(): Flow<PaymentConfig> = prefs.paymentConfigFlow

    fun observeCloudWorkspaceId(): Flow<String?> = prefs.cloudWorkspaceIdFlow

    suspend fun saveCloudWorkspaceId(workspaceId: String) = prefs.saveCloudWorkspaceId(workspaceId)

    suspend fun clearCloudWorkspaceId() = prefs.clearCloudWorkspaceId()

    fun observeLocales(): Flow<List<LocaleSpace>> =
        dao.observeLocales().map { entities -> entities.map { it.toDomain() } }

    fun observeActors(): Flow<List<EconomicActor>> =
        dao.observeActors().map { entities -> entities.map { it.toDomain() } }

    fun observeSettlements(): Flow<List<MonthlySettlement>> =
        dao.observeSettlements().map { entities -> entities.map { it.toDomain() } }

    fun observePaymentEvents(): Flow<List<PaymentEvent>> =
        dao.observePaymentEvents().map { entities -> entities.map { it.toDomain() } }

    fun observeManagerUsers(): Flow<List<ManagerUser>> =
        dao.observeManagerUsers().map { entities -> entities.map { it.toDomain() } }

    fun observeInvoices(): Flow<List<InvoiceRecord>> =
        dao.observeInvoices().map { entities -> entities.map { it.toDomain() } }

    suspend fun saveLocale(
        existingId: String?,
        code: String,
        name: String,
        address: String,
        areaSqm: Double,
        monthlyRentCents: Long,
        status: LocaleStatus
    ): Result<Unit> {
        val cleanCode = code.trim().take(30)
        val cleanName = name.trim().take(80)
        val cleanAddress = address.trim().take(160)
        val safeArea = areaSqm.coerceIn(0.0, 100000.0)
        val safeRent = monthlyRentCents.coerceIn(0L, 100_000_000_000L)

        return try {
            require(cleanCode.isNotEmpty()) { "El código del local es obligatorio." }
            require(cleanName.isNotEmpty()) { "El nombre del local es obligatorio." }
            val now = System.currentTimeMillis()
            val existing = existingId?.takeIf { it.isNotBlank() }?.let { dao.getLocaleById(it) }
            val id = existing?.id ?: UUID.randomUUID().toString()
            val entity = LocaleEntity(
                id = id,
                code = cleanCode,
                name = cleanName,
                address = cleanAddress,
                areaSqm = safeArea,
                monthlyRentCents = safeRent,
                status = status.name,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            dao.upsertLocale(entity)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteLocale(localeId: String): Result<Unit> {
        return try {
            require(localeId.isNotBlank()) { "ID de local inválido." }
            dao.deleteLocaleById(localeId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveActor(
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
    ): Result<EconomicActor> {
        val cleanName = name.trim().take(100)
        val cleanRep = representativeName.trim().take(100)
        val cleanPhone = phone.trim().take(30)
        val cleanTaxId = taxId.trim().take(40)
        val cleanEmail = actorEmail.trim().lowercase().take(120)
        val cleanLocaleId = localeId.trim().take(128)
        val cleanLocaleName = localeName.trim().take(100)
        val safeRent = monthlyRentCents.coerceIn(0L, 100_000_000_000L)
        val safePercent = profitSharePercent.coerceIn(0L, 100L)

        return try {
            require(cleanName.isNotEmpty()) { "El nombre del actor económico es obligatorio." }
            require(cleanRep.isNotEmpty()) { "El representante es obligatorio." }
            val now = System.currentTimeMillis()
            val existing = existingId?.takeIf { it.isNotBlank() }?.let { dao.getActorById(it) }
            val id = existing?.id ?: UUID.randomUUID().toString()

            val entity = ActorEntity(
                id = id,
                name = cleanName,
                legalType = legalType.name,
                representativeName = cleanRep,
                phone = cleanPhone,
                taxId = cleanTaxId,
                actorEmail = cleanEmail,
                localeId = cleanLocaleId,
                localeName = cleanLocaleName,
                monthlyRentCents = safeRent,
                profitSharePercent = safePercent,
                active = active,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            dao.upsertActor(entity)

            if (!previousLocaleId.isNullOrBlank() && previousLocaleId != cleanLocaleId) {
                dao.updateLocaleStatus(previousLocaleId, LocaleStatus.DISPONIBLE.name, now)
            }
            if (cleanLocaleId.isNotBlank() && active) {
                dao.updateLocaleStatus(cleanLocaleId, LocaleStatus.ARRENDADO.name, now)
            }

            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActorById(actorId: String): EconomicActor? =
        dao.getActorById(actorId)?.toDomain()

    suspend fun mergeCloudActors(cloudActors: List<EconomicActor>) {
        for (cloud in cloudActors) {
            if (cloud.id.isBlank()) continue
            val existing = dao.getActorById(cloud.id)
            val resolvedEmail = cloud.actorEmail.trim().lowercase().take(120)
            if (existing == null ||
                cloud.updatedAtMillis > existing.updatedAtMillis ||
                (resolvedEmail.isNotBlank() && existing.actorEmail != resolvedEmail)
            ) {
                val mergedEmail = if (existing == null || cloud.updatedAtMillis > existing.updatedAtMillis) {
                    resolvedEmail
                } else {
                    resolvedEmail.ifBlank { existing.actorEmail }
                }
                dao.upsertActor(
                    ActorEntity(
                        id = cloud.id,
                        name = cloud.name,
                        legalType = cloud.legalType,
                        representativeName = cloud.representativeName,
                        phone = cloud.phone,
                        taxId = cloud.taxId,
                        actorEmail = mergedEmail,
                        localeId = cloud.localeId,
                        localeName = cloud.localeName,
                        monthlyRentCents = cloud.monthlyRentCents,
                        profitSharePercent = cloud.profitSharePercent,
                        active = cloud.active,
                        createdAtMillis = existing?.createdAtMillis ?: cloud.createdAtMillis,
                        updatedAtMillis = cloud.updatedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun deleteActor(actor: EconomicActor): Result<Unit> {
        return try {
            require(actor.id.isNotBlank()) { "ID de actor inválido." }
            dao.deleteActorById(actor.id)
            if (actor.localeId.isNotBlank()) {
                dao.updateLocaleStatus(
                    id = actor.localeId,
                    status = LocaleStatus.DISPONIBLE.name,
                    updatedAt = System.currentTimeMillis()
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveSettlement(
        existingId: String?,
        actorId: String,
        actorName: String,
        localeId: String,
        localeName: String,
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
        paymentReference: String = "",
        directorUsername: String = ""
    ): Result<SaveSettlementResult> {
        val breakdown = SettlementCalculator.calculateFromNetProfit(
            grossIncomeCents = grossIncomeCents,
            operatingExpensesCents = operatingExpensesCents,
            netProfitCents = netProfitCents,
            profitSharePercent = profitSharePercent,
            spaceRentCents = spaceRentCents,
            paidCents = paidCents
        )
        val cleanActorId = actorId.trim().take(128)
        val cleanActorName = actorName.trim().take(100)
        val cleanLocaleId = localeId.trim().take(128)
        val cleanLocaleName = localeName.trim().take(100)
        val cleanNotes = notes.trim().take(300)
        val cleanReference = paymentReference.trim().take(80)

        return try {
            require(cleanActorId.isNotEmpty()) { "Seleccione un actor económico." }
            require(cleanActorName.isNotEmpty()) { "Nombre de actor requerido." }
            val now = System.currentTimeMillis()
            val existing = existingId?.takeIf { it.isNotBlank() }?.let { dao.getSettlementById(it) }
            val id = existing?.id ?: UUID.randomUUID().toString()
            val previousPaid = existing?.paidCents ?: 0L
            val paidAt = if (breakdown.paidCents > 0L) existing?.paidAtMillis ?: now else null

            val resolvedChannel = if (breakdown.paidCents > 0L) {
                if (paymentChannel == PaymentChannel.NO_DEFINIDO) PaymentChannel.TRANSFERENCIA else paymentChannel
            } else {
                PaymentChannel.NO_DEFINIDO
            }

            val entity = SettlementEntity(
                id = id,
                actorId = cleanActorId,
                actorName = cleanActorName,
                localeId = cleanLocaleId,
                localeName = cleanLocaleName,
                periodYear = periodYear.toLong(),
                periodMonth = periodMonth.toLong(),
                grossIncomeCents = breakdown.grossIncomeCents,
                operatingExpensesCents = breakdown.operatingExpensesCents,
                netProfitCents = breakdown.netProfitCents,
                profitSharePercent = breakdown.profitSharePercent,
                profitShareCents = breakdown.profitShareCents,
                spaceRentCents = breakdown.spaceRentCents,
                totalDueCents = breakdown.totalDueCents,
                paidCents = breakdown.paidCents,
                paymentStatus = breakdown.paymentStatus.name,
                declaredBy = declaredBy.name,
                paymentChannel = resolvedChannel.name,
                paymentReference = if (breakdown.paidCents > 0L) cleanReference else "",
                notes = cleanNotes,
                paidAtMillis = paidAt,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            dao.upsertSettlement(entity)

            var createdEvent: PaymentEvent? = null
            val deltaPaid = breakdown.paidCents - previousPaid
            if (deltaPaid > 0L) {
                val eventEntity = PaymentEventEntity(
                    id = UUID.randomUUID().toString(),
                    settlementId = id,
                    actorId = cleanActorId,
                    actorName = cleanActorName,
                    localeName = cleanLocaleName,
                    periodYear = periodYear.toLong(),
                    periodMonth = periodMonth.toLong(),
                    amountCents = deltaPaid,
                    totalPaidAfterCents = breakdown.paidCents,
                    paymentChannel = resolvedChannel.name,
                    paymentReference = cleanReference,
                    reportedBy = declaredBy.name,
                    verificationStatus = if (declaredBy == DeclarationSource.ACTOR) {
                        PaymentVerificationStatus.POR_VERIFICAR.name
                    } else {
                        PaymentVerificationStatus.CONFIRMADO.name
                    },
                    verifiedByUsername = if (declaredBy == DeclarationSource.DIRECTOR) directorUsername else "",
                    note = cleanNotes,
                    createdAtMillis = now,
                    verifiedAtMillis = if (declaredBy == DeclarationSource.DIRECTOR) now else null
                )
                dao.upsertPaymentEvent(eventEntity)
                createdEvent = eventEntity.toDomain()
            }
            Result.success(SaveSettlementResult(settlement = entity.toDomain(), createdEvent = createdEvent))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerPayment(
        settlement: MonthlySettlement,
        newPaidCents: Long,
        paymentChannel: PaymentChannel,
        paymentReference: String,
        reportedBy: DeclarationSource,
        directorUsername: String = "",
        note: String = ""
    ): Result<PaymentEvent?> {
        return try {
            require(settlement.id.isNotBlank()) { "ID de liquidación inválido." }
            val safePaid = newPaidCents.coerceIn(0L, 200_000_000_000L)
            val status: PaymentStatus = SettlementCalculator.determinePaymentStatus(
                totalDueCents = settlement.totalDueCents,
                paidCents = safePaid
            )
            val channel = if (safePaid > 0L) paymentChannel else PaymentChannel.NO_DEFINIDO
            val ref = if (safePaid > 0L) paymentReference.trim().take(80) else ""
            val now = System.currentTimeMillis()
            dao.updateSettlementPayment(
                id = settlement.id,
                paidCents = safePaid,
                status = status.name,
                channel = channel.name,
                reference = ref,
                paidAt = if (safePaid > 0L) now else null,
                updatedAt = now
            )

            var createdEvent: PaymentEvent? = null
            val paymentDelta = safePaid - settlement.paidCents
            if (paymentDelta > 0L) {
                val isActorReport = reportedBy == DeclarationSource.ACTOR
                val eventEntity = PaymentEventEntity(
                    id = UUID.randomUUID().toString(),
                    settlementId = settlement.id,
                    actorId = settlement.actorId,
                    actorName = settlement.actorName,
                    localeName = settlement.localeName,
                    periodYear = settlement.periodYear,
                    periodMonth = settlement.periodMonth,
                    amountCents = paymentDelta,
                    totalPaidAfterCents = safePaid,
                    paymentChannel = channel.name,
                    paymentReference = ref,
                    reportedBy = reportedBy.name,
                    verificationStatus = if (isActorReport) {
                        PaymentVerificationStatus.POR_VERIFICAR.name
                    } else {
                        PaymentVerificationStatus.CONFIRMADO.name
                    },
                    verifiedByUsername = if (!isActorReport) directorUsername else "",
                    note = note.trim().take(200),
                    createdAtMillis = now,
                    verifiedAtMillis = if (!isActorReport) now else null
                )
                dao.upsertPaymentEvent(eventEntity)
                createdEvent = eventEntity.toDomain()
            }
            Result.success(createdEvent)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reportQuickActorPayment(
        actorName: String,
        periodYear: Int,
        periodMonth: Int,
        amountCents: Long,
        paymentChannel: PaymentChannel,
        paymentReference: String,
        note: String = "",
        actorId: String = ""
    ): Result<PaymentEvent> {
        val cleanName = actorName.trim().take(100)
        val cleanRef = paymentReference.trim().take(80)
        val cleanNote = note.trim().take(200)
        val cleanActorId = actorId.trim().take(128)
        return try {
            require(cleanName.isNotEmpty()) { "Ingresa el nombre de tu negocio o actor económico." }
            require(amountCents > 0L) { "El monto pagado debe ser mayor a 0.00 CUP." }
            require(cleanRef.isNotEmpty()) { "Ingresa el código de operación de Transfermóvil." }
            val resolvedChannel = if (paymentChannel == PaymentChannel.NO_DEFINIDO) {
                PaymentChannel.TRANSFERENCIA
            } else {
                paymentChannel
            }
            val now = System.currentTimeMillis()
            val eventEntity = PaymentEventEntity(
                id = UUID.randomUUID().toString(),
                settlementId = "",
                actorId = cleanActorId,
                actorName = cleanName,
                localeName = "Portal Actor",
                periodYear = periodYear.toLong(),
                periodMonth = periodMonth.toLong(),
                amountCents = amountCents.coerceIn(1L, 200_000_000_000L),
                totalPaidAfterCents = amountCents.coerceIn(1L, 200_000_000_000L),
                paymentChannel = resolvedChannel.name,
                paymentReference = cleanRef,
                reportedBy = DeclarationSource.ACTOR.name,
                verificationStatus = PaymentVerificationStatus.POR_VERIFICAR.name,
                verifiedByUsername = "",
                note = cleanNote,
                createdAtMillis = now,
                verifiedAtMillis = null
            )
            dao.upsertPaymentEvent(eventEntity)
            Result.success(eventEntity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun mergeCloudPaymentEvents(cloudEvents: List<CloudPaymentEvent>) {
        for (cloud in cloudEvents) {
            if (cloud.id.isBlank()) continue
            val existing = dao.getPaymentEventById(cloud.id)
            val merged = cloud.toDomain(
                existingLocaleName = existing?.localeName ?: "",
                existingTotalPaidAfterCents = existing?.totalPaidAfterCents
            )
            dao.upsertPaymentEvent(
                PaymentEventEntity(
                    id = merged.id,
                    settlementId = merged.settlementId,
                    actorId = merged.actorId,
                    actorName = merged.actorName,
                    localeName = merged.localeName,
                    periodYear = merged.periodYear,
                    periodMonth = merged.periodMonth,
                    amountCents = merged.amountCents,
                    totalPaidAfterCents = merged.totalPaidAfterCents,
                    paymentChannel = merged.paymentChannel,
                    paymentReference = merged.paymentReference,
                    reportedBy = merged.reportedBy,
                    verificationStatus = merged.verificationStatus,
                    verifiedByUsername = merged.verifiedByUsername,
                    note = merged.note,
                    createdAtMillis = existing?.createdAtMillis ?: merged.createdAtMillis,
                    verifiedAtMillis = merged.verifiedAtMillis
                )
            )
        }
    }

    suspend fun getPaymentEventById(eventId: String): PaymentEvent? =
        dao.getPaymentEventById(eventId)?.toDomain()

    suspend fun verifyPaymentEvent(
        eventId: String,
        status: PaymentVerificationStatus,
        verifiedByUsername: String,
        note: String = ""
    ): Result<Unit> {
        return try {
            require(eventId.isNotBlank()) { "Evento inválido." }
            val now = System.currentTimeMillis()
            dao.updatePaymentEventVerification(
                id = eventId,
                status = status.name,
                verifiedBy = verifiedByUsername.trim().take(40),
                verifiedAt = if (status == PaymentVerificationStatus.POR_VERIFICAR) null else now,
                note = note.trim().take(200)
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePaymentConfig(
        bankName: String,
        bankAccount: String,
        accountHolder: String,
        qrPaymentUrl: String
    ): Result<Unit> {
        return try {
            prefs.savePaymentConfig(
                bankName = bankName,
                bankAccount = bankAccount,
                accountHolder = accountHolder,
                qrPaymentUrl = qrPaymentUrl
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSettlementById(settlementId: String): MonthlySettlement? =
        dao.getSettlementById(settlementId)?.toDomain()

    suspend fun mergeCloudSettlements(cloudSettlements: List<MonthlySettlement>) {
        for (cloud in cloudSettlements) {
            if (cloud.id.isBlank()) continue
            val existing = dao.getSettlementById(cloud.id)
            if (existing == null || cloud.updatedAtMillis > existing.updatedAtMillis) {
                dao.upsertSettlement(
                    SettlementEntity(
                        id = cloud.id,
                        actorId = cloud.actorId,
                        actorName = cloud.actorName,
                        localeId = cloud.localeId,
                        localeName = cloud.localeName,
                        periodYear = cloud.periodYear,
                        periodMonth = cloud.periodMonth,
                        grossIncomeCents = cloud.grossIncomeCents,
                        operatingExpensesCents = cloud.operatingExpensesCents,
                        netProfitCents = cloud.netProfitCents,
                        profitSharePercent = cloud.profitSharePercent,
                        profitShareCents = cloud.profitShareCents,
                        spaceRentCents = cloud.spaceRentCents,
                        totalDueCents = cloud.totalDueCents,
                        paidCents = cloud.paidCents,
                        paymentStatus = cloud.paymentStatus,
                        declaredBy = cloud.declaredBy,
                        paymentChannel = cloud.paymentChannel,
                        paymentReference = cloud.paymentReference,
                        notes = cloud.notes,
                        paidAtMillis = cloud.paidAtMillis ?: existing?.paidAtMillis,
                        createdAtMillis = existing?.createdAtMillis ?: cloud.createdAtMillis,
                        updatedAtMillis = cloud.updatedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun deleteSettlement(settlementId: String): Result<Unit> {
        return try {
            require(settlementId.isNotBlank()) { "ID de liquidación inválido." }
            dao.deleteSettlementById(settlementId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Local Manager Users (PBKDF2-HMAC-SHA256)
    suspend fun registerManagerUser(
        username: String,
        fullName: String,
        role: ManagerRole,
        password: String
    ): Result<ManagerUser> {
        val cleanUsername = username.trim().lowercase().take(32)
        val cleanName = fullName.trim().take(80)
        val cleanPassword = password.trim()
        return try {
            require(cleanUsername.length >= 3 && cleanUsername.all { it.isLetterOrDigit() || it == '_' || it == '.' }) {
                "El usuario debe tener al menos 3 caracteres (letras, números, . o _)."
            }
            require(cleanName.isNotEmpty()) { "El nombre completo es obligatorio." }
            require(cleanPassword.length >= 4) { "La contraseña debe tener al menos 4 caracteres." }

            val existing = dao.getManagerUserByUsername(cleanUsername)
            require(existing == null) { "Ya existe un usuario Manager llamado '$cleanUsername'." }

            val hashed = withContext(Dispatchers.Default) {
                PasswordHasher.hashPassword(cleanPassword)
            }
            val now = System.currentTimeMillis()
            val entity = ManagerUserEntity(
                id = UUID.randomUUID().toString(),
                username = cleanUsername,
                fullName = cleanName,
                role = role.name,
                passwordHash = hashed.hashHex,
                passwordSalt = hashed.saltHex,
                active = true,
                createdAtMillis = now,
                lastLoginAtMillis = now
            )
            dao.upsertManagerUser(entity)
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun authenticateManagerUser(username: String, password: String): Result<ManagerUser> {
        val cleanUsername = username.trim().lowercase()
        val cleanPassword = password.trim()
        return try {
            require(cleanUsername.isNotEmpty()) { "Ingresa tu usuario." }
            require(cleanPassword.isNotEmpty()) { "Ingresa tu contraseña." }

            val entity = dao.getManagerUserByUsername(cleanUsername)
                ?: return Result.failure(IllegalArgumentException("Usuario o contraseña incorrectos."))

            if (!entity.active) {
                return Result.failure(IllegalStateException("Este usuario está desactivado."))
            }

            val valid = withContext(Dispatchers.Default) {
                PasswordHasher.verifyPassword(cleanPassword, entity.passwordHash, entity.passwordSalt)
            }
            if (!valid) {
                return Result.failure(IllegalArgumentException("Usuario o contraseña incorrectos."))
            }

            val now = System.currentTimeMillis()
            dao.updateManagerLastLogin(entity.id, now)
            Result.success(entity.copy(lastLoginAtMillis = now).toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setManagerUserActive(userId: String, active: Boolean, currentUserId: String?): Result<Unit> {
        return try {
            require(userId.isNotBlank()) { "Usuario inválido." }
            if (!active) {
                require(userId != currentUserId) { "No puedes desactivar tu propio usuario conectado." }
                val activeCount = dao.countActiveManagers()
                require(activeCount > 1) { "Debe quedar al menos un Manager activo." }
            }
            dao.updateManagerUserActive(userId, active)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetManagerPassword(userId: String, newPassword: String): Result<Unit> {
        val cleanPassword = newPassword.trim()
        return try {
            require(cleanPassword.length >= 4) { "La nueva contraseña debe tener al menos 4 caracteres." }
            val hashed = withContext(Dispatchers.Default) {
                PasswordHasher.hashPassword(cleanPassword)
            }
            dao.updateManagerPassword(userId, hashed.hashHex, hashed.saltHex)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Invoices for Manager Registration & Versat Sarasola Export
    suspend fun saveInvoice(
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
        costCenter: String,
        issuedByUsername: String
    ): Result<InvoiceRecord> {
        val cleanNo = invoiceNumber.trim().take(40)
        val cleanActor = actorName.trim().take(100)
        val cleanTaxId = actorTaxId.trim().take(40)
        val cleanConcept = concept.trim().take(200)
        val cleanRef = paymentReference.trim().take(80)
        val cleanDebit = debitAccountCode.trim().ifBlank { InvoiceRecord.DEFAULT_VERSAT_DEBIT_ACCOUNT }.take(30)
        val cleanCredit = creditAccountCode.trim().ifBlank { InvoiceRecord.DEFAULT_VERSAT_CREDIT_ACCOUNT }.take(30)
        val cleanCenter = costCenter.trim().ifBlank { InvoiceRecord.DEFAULT_VERSAT_COST_CENTER }.take(40)

        return try {
            require(cleanNo.isNotEmpty()) { "El número de factura es obligatorio." }
            require(cleanActor.isNotEmpty()) { "El nombre del actor / cliente es obligatorio." }
            require(totalAmountCents > 0L) { "El importe de la factura debe ser mayor a 0.00 CUP." }

            val now = System.currentTimeMillis()
            val existing = existingId?.takeIf { it.isNotBlank() }?.let { dao.getInvoiceById(it) }
            val id = existing?.id ?: UUID.randomUUID().toString()
            val entity = InvoiceEntity(
                id = id,
                invoiceNumber = cleanNo,
                paymentEventId = paymentEventId.trim().take(128),
                settlementId = settlementId.trim().take(128),
                actorId = actorId.trim().take(128),
                actorName = cleanActor,
                actorTaxId = cleanTaxId,
                concept = cleanConcept.ifBlank { "Aporte 30% utilidad neta y alquiler municipal" },
                periodYear = periodYear.toLong(),
                periodMonth = periodMonth.toLong(),
                profitShareCents = profitShareCents.coerceIn(0L, totalAmountCents),
                spaceRentCents = spaceRentCents.coerceIn(0L, totalAmountCents),
                totalAmountCents = totalAmountCents.coerceIn(1L, 200_000_000_000L),
                paymentChannel = if (paymentChannel == PaymentChannel.NO_DEFINIDO) {
                    PaymentChannel.TRANSFERENCIA.name
                } else {
                    paymentChannel.name
                },
                paymentReference = cleanRef,
                debitAccountCode = cleanDebit,
                creditAccountCode = cleanCredit,
                costCenter = cleanCenter,
                issuedByUsername = issuedByUsername.trim().take(40),
                exportedToVersat = existing?.exportedToVersat ?: false,
                issueDateMillis = existing?.issueDateMillis ?: now,
                exportedAtMillis = existing?.exportedAtMillis
            )
            dao.upsertInvoice(entity)
            Result.success(entity.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markInvoicesExported(invoiceIds: List<String>): Result<Unit> {
        return try {
            if (invoiceIds.isNotEmpty()) {
                dao.markInvoicesExported(invoiceIds, System.currentTimeMillis())
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteInvoice(invoiceId: String): Result<Unit> {
        return try {
            require(invoiceId.isNotBlank()) { "Factura inválida." }
            dao.deleteInvoiceById(invoiceId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
