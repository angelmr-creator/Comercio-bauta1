package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.data.model.DeclarationSource
import com.example.data.model.EconomicActor
import com.example.data.model.InvoiceRecord
import com.example.data.model.LocaleSpace
import com.example.data.model.MonthlySettlement
import com.example.data.model.PaymentEvent
import com.example.data.model.PaymentVerificationStatus
import com.example.data.remote.BautaCloudSyncRepository
import com.example.data.repository.BautaCommerceRepository
import com.example.ui.components.ActorEditorSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InvoiceEditorDialog
import com.example.ui.components.LocaleEditorSheet
import com.example.ui.components.ManagerLoginDialog
import com.example.ui.components.ManagerUsersSheet
import com.example.ui.components.PaymentConfigDialog
import com.example.ui.components.PaymentDialog
import com.example.ui.components.SettlementEditorSheet
import com.example.ui.components.attemptAutoSignIn
import com.example.ui.screens.ActorPortalScreen
import com.example.ui.screens.ActorsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LivePaymentsScreen
import com.example.ui.screens.LocalAccessScreen
import com.example.ui.screens.LocalesScreen
import com.example.ui.screens.MonthlySummaryScreen
import com.example.ui.screens.SettlementsScreen
import com.example.ui.state.CommerceUiState
import com.example.ui.state.CommerceViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.ComercioBautaTheme
import com.example.util.NetworkMonitor
import com.example.util.PaymentReceiptData
import com.example.util.PdfReceiptGenerator
import com.example.util.VersatSarasolaExporter

private const val ROUTE_MONTHLY_SUMMARY = "resumen_mensual"

enum class AppWorkspaceMode {
    DIRECTOR,
    ACTOR
}

enum class DirectorDestination(
    val route: String,
    val labelRes: Int,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Home(
        route = "inicio",
        labelRes = R.string.nav_home,
        title = "Manager Bauta",
        selectedIcon = Icons.Rounded.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    Actors(
        route = "actores",
        labelRes = R.string.nav_actors,
        title = "Actores Económicos",
        selectedIcon = Icons.Rounded.Groups,
        unselectedIcon = Icons.Outlined.Groups
    ),
    Locales(
        route = "locales",
        labelRes = R.string.nav_locales,
        title = "Locales Municipales",
        selectedIcon = Icons.Rounded.Storefront,
        unselectedIcon = Icons.Outlined.Storefront
    ),
    Settlements(
        route = "cobros",
        labelRes = R.string.nav_settlements,
        title = "Cobros y Liquidaciones",
        selectedIcon = Icons.Rounded.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    ),
    LiveMonitor(
        route = "en_vivo",
        labelRes = R.string.nav_live,
        title = "Pagos en Tiempo Real",
        selectedIcon = Icons.Rounded.NotificationsActive,
        unselectedIcon = Icons.Outlined.NotificationsActive
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComercioBautaTheme {
                ComercioBautaApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ComercioBautaApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel: CommerceViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val repository = BautaCommerceRepository(app.applicationContext)
                val cloudSync = BautaCloudSyncRepository(app.applicationContext)
                val networkFlow = NetworkMonitor.observeIsOnline(app.applicationContext)
                CommerceViewModel(
                    repository = repository,
                    cloudSync = cloudSync,
                    isOnlineFlow = networkFlow
                )
            }
        }
    )

    val uiStateWrapper by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        val credentialManager = CredentialManager.create(context)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { viewModel.onCloudAuthSuccess() },
            onUnauthenticated = {},
            scope = this
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var workspaceMode by rememberSaveable { mutableStateOf(AppWorkspaceMode.ACTOR) }
    var actorLoginCompleted by rememberSaveable { mutableStateOf(false) }
    var actorLoginNameInput by rememberSaveable { mutableStateOf("") }
    var portalSelectedActorId by rememberSaveable { mutableStateOf<String?>(null) }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: DirectorDestination.Home.route
    val currentDestination = DirectorDestination.entries.firstOrNull { it.route == currentRoute }
        ?: DirectorDestination.Home

    // Modal state
    var showLocaleSheet by remember { mutableStateOf(false) }
    var editingLocale by remember { mutableStateOf<LocaleSpace?>(null) }

    var showActorSheet by remember { mutableStateOf(false) }
    var editingActor by remember { mutableStateOf<EconomicActor?>(null) }

    var showSettlementSheet by remember { mutableStateOf(false) }
    var editingSettlement by remember { mutableStateOf<MonthlySettlement?>(null) }
    var preselectedActor by remember { mutableStateOf<EconomicActor?>(null) }
    var settlementFromActorPortal by remember { mutableStateOf(false) }

    var paymentTargetSettlement by remember { mutableStateOf<MonthlySettlement?>(null) }
    var paymentIsActorPortalMode by remember { mutableStateOf(false) }
    var showPaymentConfigDialog by remember { mutableStateOf(false) }
    var showManagerUsersSheet by remember { mutableStateOf(false) }
    var showManagerLoginDialog by remember { mutableStateOf(false) }

    var showInvoiceDialog by remember { mutableStateOf(false) }
    var editingInvoice by remember { mutableStateOf<InvoiceRecord?>(null) }
    var prefillInvoiceEvent by remember { mutableStateOf<PaymentEvent?>(null) }

    var pendingReceiptData by remember { mutableStateOf<PaymentReceiptData?>(null) }
    var pendingVersatInvoices by remember { mutableStateOf<List<InvoiceRecord>>(emptyList()) }

    val pdfReceiptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val receipt = pendingReceiptData
        pendingReceiptData = null
        if (uri != null && receipt != null) {
            PdfReceiptGenerator.writeReceiptToUri(context, uri, receipt)
                .onSuccess {
                    viewModel.emitMessage("Recibo PDF guardado (${receipt.receiptCode})")
                }
                .onFailure { err ->
                    viewModel.emitMessage(err.localizedMessage ?: "No se pudo guardar el recibo PDF")
                }
        }
    }

    val versatCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val toExport = pendingVersatInvoices
        pendingVersatInvoices = emptyList()
        if (uri != null && toExport.isNotEmpty()) {
            VersatSarasolaExporter.writeCsvToUri(context, uri, toExport)
                .onSuccess {
                    viewModel.markInvoicesExported(toExport.map { it.id })
                    viewModel.emitMessage("Exportado a Versat Sarasola (CSV · ${toExport.size} factura(s))")
                }
                .onFailure { err ->
                    viewModel.emitMessage(err.localizedMessage ?: "Error al exportar CSV Versat")
                }
        }
    }

    val versatXmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/xml")
    ) { uri ->
        val toExport = pendingVersatInvoices
        pendingVersatInvoices = emptyList()
        if (uri != null && toExport.isNotEmpty()) {
            VersatSarasolaExporter.writeXmlToUri(context, uri, toExport)
                .onSuccess {
                    viewModel.markInvoicesExported(toExport.map { it.id })
                    viewModel.emitMessage("Exportado a Versat Sarasola (XML · ${toExport.size} factura(s))")
                }
                .onFailure { err ->
                    viewModel.emitMessage(err.localizedMessage ?: "Error al exportar XML Versat")
                }
        }
    }

    fun navigateDirector(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openNewLocale() {
        editingLocale = null
        showLocaleSheet = true
    }

    fun openNewActor() {
        editingActor = null
        showActorSheet = true
    }

    fun openNewSettlement(actor: EconomicActor? = null, fromActorPortal: Boolean = false) {
        editingSettlement = null
        preselectedActor = actor
        settlementFromActorPortal = fromActorPortal
        showSettlementSheet = true
    }

    fun requestReturnToDirector(uiState: CommerceUiState) {
        if (uiState.activeManagerUser != null) {
            workspaceMode = AppWorkspaceMode.DIRECTOR
        } else {
            showManagerLoginDialog = true
        }
    }

    fun switchToActorPortal() {
        viewModel.signOutManager()
        workspaceMode = AppWorkspaceMode.ACTOR
        actorLoginCompleted = true
    }

    when (val state = uiStateWrapper) {
        UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "Cargando datos locales de Comercio Bauta...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is UiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Rounded.Storefront,
                    title = "Error al cargar base de datos local",
                    subtitle = state.message
                )
            }
        }

        is UiState.Success -> {
            val uiState: CommerceUiState = state.data
            val isDirectorAuthenticated = workspaceMode == AppWorkspaceMode.DIRECTOR && uiState.activeManagerUser != null

            LaunchedEffect(isDirectorAuthenticated) {
                viewModel.setDirectorMode(isDirectorAuthenticated)
            }

            if (!isDirectorAuthenticated && !actorLoginCompleted) {
                LocalAccessScreen(
                    actors = uiState.actors,
                    cloudSync = uiState.cloudSync,
                    onCloudAuthSuccess = viewModel::onCloudAuthSuccess,
                    onCloudAuthError = viewModel::onCloudAuthError,
                    onEnterActorPortal = { selectedId, typedName, legalType, repName, phone, taxId ->
                        actorLoginNameInput = typedName
                        workspaceMode = AppWorkspaceMode.ACTOR
                        viewModel.registerOrSelectActorForPortal(
                            selectedActorId = selectedId,
                            businessNameInput = typedName,
                            legalType = legalType,
                            representativeName = repName,
                            phone = phone,
                            taxId = taxId
                        ) { resolvedId ->
                            if (!resolvedId.isNullOrBlank()) {
                                portalSelectedActorId = resolvedId
                                actorLoginCompleted = true
                            }
                        }
                    },
                    onRequestPrivateAccess = {
                        requestReturnToDirector(uiState)
                    }
                )
            } else {
                if (!isDirectorAuthenticated) {
                    BackHandler {
                        actorLoginCompleted = false
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useNavRail = maxWidth >= 840.dp

                    Row(modifier = Modifier.fillMaxSize()) {
                        if (useNavRail && isDirectorAuthenticated) {
                            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                                DirectorDestination.entries.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateDirector(dest.route) },
                                        icon = {
                                            DestinationNavIcon(
                                                destination = dest,
                                                selected = selected,
                                                unverifiedCount = uiState.unverifiedPaymentsCount
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(dest.labelRes),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        modifier = Modifier.testTag("nav_${dest.route}")
                                    )
                                }
                            }
                        }

                        Scaffold(
                            modifier = Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.background,
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                            topBar = {
                                TopAppBar(
                                    navigationIcon = {
                                        if (isDirectorAuthenticated && currentRoute == ROUTE_MONTHLY_SUMMARY) {
                                            IconButton(
                                                onClick = { navController.popBackStack() },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                    contentDescription = "Volver"
                                                )
                                            }
                                        }
                                    },
                                    title = {
                                        Text(
                                            text = when {
                                                !isDirectorAuthenticated -> "Comercio Bauta"
                                                currentRoute == ROUTE_MONTHLY_SUMMARY -> "Resumen Mensual (Firestore)"
                                                else -> currentDestination.title
                                            },
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = if (!isDirectorAuthenticated) {
                                                Modifier.combinedClickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null,
                                                    onClick = {},
                                                    onLongClick = { requestReturnToDirector(uiState) }
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                    },
                                    actions = {
                                        if (isDirectorAuthenticated) {
                                            IconButton(
                                                onClick = { navigateDirector(ROUTE_MONTHLY_SUMMARY) },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("top_bar_monthly_summary_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Assessment,
                                                    contentDescription = "Resumen Mensual Firestore"
                                                )
                                            }
                                            IconButton(
                                                onClick = { showPaymentConfigDialog = true },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("configure_payment_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.QrCode2,
                                                    contentDescription = stringResource(R.string.cd_configure_payment)
                                                )
                                            }
                                            IconButton(
                                                onClick = { showManagerUsersSheet = true },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("manager_users_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.ManageAccounts,
                                                    contentDescription = stringResource(R.string.cd_manager_users)
                                                )
                                            }
                                            FilledTonalButton(
                                                onClick = { switchToActorPortal() },
                                                modifier = Modifier
                                                    .padding(end = 6.dp)
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("switch_workspace_mode_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Person,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "Portal Actor", maxLines = 1)
                                            }
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            },
                        bottomBar = {
                            if (!useNavRail && isDirectorAuthenticated) {
                                NavigationBar(
                                    windowInsets = WindowInsets.navigationBars,
                                    containerColor = MaterialTheme.colorScheme.surface
                                ) {
                                    DirectorDestination.entries.forEach { dest ->
                                        val selected = currentRoute == dest.route
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navigateDirector(dest.route) },
                                            icon = {
                                                DestinationNavIcon(
                                                    destination = dest,
                                                    selected = selected,
                                                    unverifiedCount = uiState.unverifiedPaymentsCount
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = stringResource(dest.labelRes),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            },
                                            modifier = Modifier.testTag("nav_${dest.route}")
                                        )
                                    }
                                }
                            }
                        },
                        floatingActionButton = {
                            if (isDirectorAuthenticated && currentRoute != ROUTE_MONTHLY_SUMMARY) {
                                when (currentDestination) {
                                    DirectorDestination.Home -> {
                                        FloatingActionButton(
                                            onClick = { openNewSettlement(null, fromActorPortal = false) },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.testTag("fab_add_settlement_home")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = stringResource(R.string.cd_add_settlement)
                                            )
                                        }
                                    }
                                    DirectorDestination.Actors -> {
                                        FloatingActionButton(
                                            onClick = { openNewActor() },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.testTag("fab_add_actor")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = stringResource(R.string.cd_add_actor)
                                            )
                                        }
                                    }
                                    DirectorDestination.Locales -> {
                                        FloatingActionButton(
                                            onClick = { openNewLocale() },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.testTag("fab_add_locale")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = stringResource(R.string.cd_add_locale)
                                            )
                                        }
                                    }
                                    DirectorDestination.Settlements -> {
                                        FloatingActionButton(
                                            onClick = { openNewSettlement(null, fromActorPortal = false) },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.testTag("fab_add_settlement")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = stringResource(R.string.cd_add_settlement)
                                            )
                                        }
                                    }
                                    DirectorDestination.LiveMonitor -> Unit
                                }
                            }
                        }
                    ) { innerPadding ->
                        if (!isDirectorAuthenticated) {
                            Box(modifier = Modifier.padding(innerPadding)) {
                                ActorPortalScreen(
                                    uiState = uiState,
                                    selectedActorId = portalSelectedActorId,
                                    defaultActorName = actorLoginNameInput,
                                    onSelectActor = { portalSelectedActorId = it },
                                    onUpdateActorProfile = viewModel::updateActorProfileFromPortal,
                                    onPreviousMonth = viewModel::previousMonth,
                                    onNextMonth = viewModel::nextMonth,
                                    onDeclareMonth = { actor ->
                                        openNewSettlement(actor, fromActorPortal = true)
                                    },
                                    onReportDigitalPayment = { settlement ->
                                        paymentIsActorPortalMode = true
                                        paymentTargetSettlement = settlement
                                    },
                                    onReportQuickPayment = { name, year, month, amount, channel, ref, note, actorId ->
                                        viewModel.reportQuickActorPayment(
                                            actorName = name,
                                            periodYear = year,
                                            periodMonth = month,
                                            amountCents = amount,
                                            paymentChannel = channel,
                                            paymentReference = ref,
                                            note = note,
                                            actorId = actorId
                                        )
                                    },
                                    onDownloadEventReceiptPdf = { event ->
                                        val matchedActor = uiState.actors.firstOrNull {
                                            it.id == event.actorId || it.name.equals(event.actorName, ignoreCase = true)
                                        }
                                        val receipt = PdfReceiptGenerator.fromPaymentEvent(
                                            event = event,
                                            paymentConfig = uiState.paymentConfig,
                                            actorTaxId = matchedActor?.taxId.orEmpty(),
                                            fallbackLocaleName = matchedActor?.localeName.orEmpty()
                                        )
                                        pendingReceiptData = receipt
                                        pdfReceiptLauncher.launch(PdfReceiptGenerator.suggestedFileName(receipt))
                                    },
                                    onDownloadSettlementReceiptPdf = { settlement ->
                                        val matchedEvent = uiState.paymentEvents.firstOrNull { it.settlementId == settlement.id }
                                        val syntheticEvent = matchedEvent ?: PaymentEvent(
                                            id = settlement.id,
                                            settlementId = settlement.id,
                                            actorId = settlement.actorId,
                                            actorName = settlement.actorName,
                                            localeName = settlement.localeName,
                                            periodYear = settlement.periodYear,
                                            periodMonth = settlement.periodMonth,
                                            amountCents = settlement.paidCents,
                                            totalPaidAfterCents = settlement.paidCents,
                                            paymentChannel = settlement.paymentChannel,
                                            paymentReference = settlement.paymentReference,
                                            reportedBy = settlement.declaredBy,
                                            verificationStatus = PaymentVerificationStatus.CONFIRMADO.name,
                                            verifiedByUsername = "",
                                            note = settlement.notes,
                                            createdAtMillis = settlement.paidAtMillis ?: settlement.updatedAtMillis
                                        )
                                        val matchedActor = uiState.actors.firstOrNull { it.id == settlement.actorId }
                                        val receipt = PdfReceiptGenerator.fromPaymentEvent(
                                            event = syntheticEvent,
                                            paymentConfig = uiState.paymentConfig,
                                            actorTaxId = matchedActor?.taxId.orEmpty(),
                                            fallbackLocaleName = settlement.localeName
                                        )
                                        pendingReceiptData = receipt
                                        pdfReceiptLauncher.launch(PdfReceiptGenerator.suggestedFileName(receipt))
                                    },
                                    onCloudAuthSuccess = viewModel::onCloudAuthSuccess,
                                    onCloudAuthError = viewModel::onCloudAuthError,
                                    onJoinWorkspace = viewModel::joinCloudWorkspace,
                                    onForceSync = viewModel::forceSyncNow,
                                    onCloudSignOutComplete = viewModel::onCloudSignOutComplete,
                                    onChangeBusinessSession = { actorLoginCompleted = false },
                                    onRequestPrivateAccess = { requestReturnToDirector(uiState) }
                                )
                            }
                        } else {
                            NavHost(
                                navController = navController,
                                startDestination = DirectorDestination.Home.route,
                                modifier = Modifier.padding(innerPadding)
                            ) {
                                composable(DirectorDestination.Home.route) {
                                    DashboardScreen(
                                        uiState = uiState,
                                        onPreviousMonth = viewModel::previousMonth,
                                        onNextMonth = viewModel::nextMonth,
                                        onAddLocale = { openNewLocale() },
                                        onAddActor = { openNewActor() },
                                        onAddSettlement = { actor -> openNewSettlement(actor, false) },
                                        onOpenPaymentDialog = { settlement ->
                                            paymentIsActorPortalMode = false
                                            paymentTargetSettlement = settlement
                                        },
                                        onEditSettlement = { settlement ->
                                            editingSettlement = settlement
                                            preselectedActor = null
                                            settlementFromActorPortal = false
                                            showSettlementSheet = true
                                        },
                                        onSavePaymentConfig = viewModel::savePaymentConfig,
                                        onNavigateToActors = { navigateDirector(DirectorDestination.Actors.route) },
                                        onNavigateToLocales = { navigateDirector(DirectorDestination.Locales.route) },
                                        onNavigateToSettlements = { navigateDirector(DirectorDestination.Settlements.route) },
                                        onNavigateToLiveMonitor = { navigateDirector(DirectorDestination.LiveMonitor.route) },
                                        onNavigateToMonthlySummary = { navigateDirector(ROUTE_MONTHLY_SUMMARY) },
                                        onVerifyPaymentEvent = viewModel::verifyPaymentEvent,
                                        onDownloadReceiptPdf = { event ->
                                            val matchedActor = uiState.actors.firstOrNull {
                                                it.id == event.actorId || it.name.equals(event.actorName, ignoreCase = true)
                                            }
                                            val receipt = PdfReceiptGenerator.fromPaymentEvent(
                                                event = event,
                                                paymentConfig = uiState.paymentConfig,
                                                actorTaxId = matchedActor?.taxId.orEmpty(),
                                                fallbackLocaleName = matchedActor?.localeName.orEmpty()
                                            )
                                            pendingReceiptData = receipt
                                            pdfReceiptLauncher.launch(PdfReceiptGenerator.suggestedFileName(receipt))
                                        },
                                        onAddInvoice = {
                                            editingInvoice = null
                                            prefillInvoiceEvent = null
                                            showInvoiceDialog = true
                                        },
                                        onCreateInvoiceFromEvent = { event ->
                                            editingInvoice = uiState.invoices.firstOrNull { it.paymentEventId == event.id }
                                            prefillInvoiceEvent = event
                                            showInvoiceDialog = true
                                        },
                                        onEditInvoice = { inv ->
                                            editingInvoice = inv
                                            prefillInvoiceEvent = null
                                            showInvoiceDialog = true
                                        },
                                        onDeleteInvoice = viewModel::deleteInvoice,
                                        onExportVersatCsv = {
                                            if (uiState.invoices.isNotEmpty()) {
                                                pendingVersatInvoices = uiState.invoices
                                                versatCsvLauncher.launch(
                                                    VersatSarasolaExporter.suggestedCsvFileName(
                                                        uiState.selectedMonth.year,
                                                        uiState.selectedMonth.month
                                                    )
                                                )
                                            }
                                        },
                                        onExportVersatXml = {
                                            if (uiState.invoices.isNotEmpty()) {
                                                pendingVersatInvoices = uiState.invoices
                                                versatXmlLauncher.launch(
                                                    VersatSarasolaExporter.suggestedXmlFileName(
                                                        uiState.selectedMonth.year,
                                                        uiState.selectedMonth.month
                                                    )
                                                )
                                            }
                                        },
                                        onCloudAuthSuccess = viewModel::onCloudAuthSuccess,
                                        onCloudAuthError = viewModel::onCloudAuthError,
                                        onJoinWorkspace = viewModel::joinCloudWorkspace,
                                        onCreateNewWorkspace = viewModel::createNewCloudWorkspace,
                                        onForceSync = viewModel::forceSyncNow,
                                        onCloudSignOutComplete = viewModel::onCloudSignOutComplete
                                    )
                                }
                                composable(DirectorDestination.Actors.route) {
                                    ActorsScreen(
                                        uiState = uiState,
                                        onSearchQueryChange = viewModel::setActorSearchQuery,
                                        onSelectLegalTypeFilter = viewModel::setActorLegalTypeFilter,
                                        onAddActor = { openNewActor() },
                                        onEditActor = { actor ->
                                            editingActor = actor
                                            showActorSheet = true
                                        },
                                        onCreateSettlementForActor = { actor ->
                                            openNewSettlement(actor, fromActorPortal = false)
                                        }
                                    )
                                }
                                composable(DirectorDestination.Locales.route) {
                                    LocalesScreen(
                                        uiState = uiState,
                                        onSelectStatusFilter = viewModel::setLocaleStatusFilter,
                                        onAddLocale = { openNewLocale() },
                                        onEditLocale = { locale ->
                                            editingLocale = locale
                                            showLocaleSheet = true
                                        }
                                    )
                                }
                                composable(DirectorDestination.Settlements.route) {
                                    SettlementsScreen(
                                        uiState = uiState,
                                        onPreviousMonth = viewModel::previousMonth,
                                        onNextMonth = viewModel::nextMonth,
                                        onSelectStatusFilter = viewModel::setSettlementStatusFilter,
                                        onAddSettlement = { openNewSettlement(null, false) },
                                        onEditSettlement = { settlement ->
                                            editingSettlement = settlement
                                            preselectedActor = null
                                            settlementFromActorPortal = false
                                            showSettlementSheet = true
                                        },
                                        onOpenPaymentDialog = { settlement ->
                                            paymentIsActorPortalMode = false
                                            paymentTargetSettlement = settlement
                                        }
                                    )
                                }
                                composable(DirectorDestination.LiveMonitor.route) {
                                    LivePaymentsScreen(
                                        uiState = uiState,
                                        onVerifyPaymentEvent = viewModel::verifyPaymentEvent,
                                        onDownloadReceiptPdf = { event ->
                                            val matchedActor = uiState.actors.firstOrNull {
                                                it.id == event.actorId || it.name.equals(event.actorName, ignoreCase = true)
                                            }
                                            val receipt = PdfReceiptGenerator.fromPaymentEvent(
                                                event = event,
                                                paymentConfig = uiState.paymentConfig,
                                                actorTaxId = matchedActor?.taxId.orEmpty(),
                                                fallbackLocaleName = matchedActor?.localeName.orEmpty()
                                            )
                                            pendingReceiptData = receipt
                                            pdfReceiptLauncher.launch(PdfReceiptGenerator.suggestedFileName(receipt))
                                        },
                                        onCreateInvoiceFromEvent = { event ->
                                            editingInvoice = uiState.invoices.firstOrNull { it.paymentEventId == event.id }
                                            prefillInvoiceEvent = event
                                            showInvoiceDialog = true
                                        },
                                        onCloudAuthSuccess = viewModel::onCloudAuthSuccess,
                                        onCloudAuthError = viewModel::onCloudAuthError,
                                        onJoinWorkspace = viewModel::joinCloudWorkspace,
                                        onCreateNewWorkspace = viewModel::createNewCloudWorkspace,
                                        onForceSync = viewModel::forceSyncNow,
                                        onCloudSignOutComplete = viewModel::onCloudSignOutComplete
                                    )
                                }
                                composable(ROUTE_MONTHLY_SUMMARY) {
                                    MonthlySummaryScreen(
                                        uiState = uiState,
                                        onPreviousMonth = viewModel::previousMonth,
                                        onNextMonth = viewModel::nextMonth,
                                        onCloudAuthSuccess = viewModel::onCloudAuthSuccess,
                                        onCloudAuthError = viewModel::onCloudAuthError,
                                        onSyncSettlementsToFirestore = viewModel::syncSettlementsToFirestore,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }

                        if (showLocaleSheet) {
                            LocaleEditorSheet(
                                initialLocale = editingLocale,
                                onDismiss = { showLocaleSheet = false },
                                onSave = viewModel::saveLocale,
                                onDelete = viewModel::deleteLocale
                            )
                        }

                        if (showActorSheet) {
                            ActorEditorSheet(
                                initialActor = editingActor,
                                locales = uiState.locales,
                                onDismiss = { showActorSheet = false },
                                onSave = viewModel::saveActor,
                                onDelete = viewModel::deleteActor
                            )
                        }

                        if (showSettlementSheet) {
                            SettlementEditorSheet(
                                initialSettlement = editingSettlement,
                                preselectedActor = preselectedActor,
                                actors = uiState.actors,
                                selectedMonth = uiState.selectedMonth,
                                isActorPortalMode = settlementFromActorPortal,
                                onDismiss = { showSettlementSheet = false },
                                onSave = { id, actor, year, month, gross, expenses, netProfit, percent, rent, paid, notes, channel, reference ->
                                    viewModel.saveSettlement(
                                        existingId = id,
                                        actor = actor,
                                        periodYear = year,
                                        periodMonth = month,
                                        grossIncomeCents = gross,
                                        operatingExpensesCents = expenses,
                                        netProfitCents = netProfit,
                                        profitSharePercent = percent,
                                        spaceRentCents = rent,
                                        paidCents = if (settlementFromActorPortal) 0L else paid,
                                        notes = notes,
                                        declaredBy = if (settlementFromActorPortal) {
                                            DeclarationSource.ACTOR
                                        } else {
                                            DeclarationSource.DIRECTOR
                                        },
                                        paymentChannel = channel,
                                        paymentReference = reference
                                    )
                                },
                                onDelete = if (settlementFromActorPortal) null else viewModel::deleteSettlement
                            )
                        }

                        paymentTargetSettlement?.let { target ->
                            PaymentDialog(
                                settlement = target,
                                paymentConfig = uiState.paymentConfig,
                                isActorPortalMode = paymentIsActorPortalMode,
                                onDismiss = { paymentTargetSettlement = null },
                                onConfirmPayment = { newPaidCents, channel, reference ->
                                    viewModel.registerPayment(
                                        settlement = target,
                                        newPaidCents = newPaidCents,
                                        paymentChannel = channel,
                                        paymentReference = reference,
                                        reportedBy = if (paymentIsActorPortalMode) {
                                            DeclarationSource.ACTOR
                                        } else {
                                            DeclarationSource.DIRECTOR
                                        }
                                    )
                                }
                            )
                        }

                        if (showPaymentConfigDialog) {
                            PaymentConfigDialog(
                                initialConfig = uiState.paymentConfig,
                                onDismiss = { showPaymentConfigDialog = false },
                                onSave = viewModel::savePaymentConfig
                            )
                        }

                        if (showManagerUsersSheet) {
                            ManagerUsersSheet(
                                activeUser = uiState.activeManagerUser,
                                users = uiState.managerUsers,
                                onDismiss = { showManagerUsersSheet = false },
                                onCreateUser = viewModel::createManagerUser,
                                onToggleActive = viewModel::toggleManagerUserActive,
                                onResetPassword = viewModel::resetManagerPassword,
                                onSignOut = {
                                    viewModel.signOutManager()
                                    workspaceMode = AppWorkspaceMode.ACTOR
                                    actorLoginCompleted = false
                                }
                            )
                        }

                        if (showInvoiceDialog) {
                            InvoiceEditorDialog(
                                initialInvoice = editingInvoice,
                                prefillEvent = prefillInvoiceEvent,
                                actors = uiState.actors,
                                selectedMonth = uiState.selectedMonth,
                                nextInvoiceSequence = uiState.invoices.size + 1,
                                onDismiss = {
                                    showInvoiceDialog = false
                                    editingInvoice = null
                                    prefillInvoiceEvent = null
                                },
                                onSave = { existingId, no, eventId, settlementId, actorId, actorName, taxId, concept, year, month, profitShare, spaceRent, total, channel, ref, debit, credit, costCenter ->
                                    viewModel.saveInvoice(
                                        existingId = existingId,
                                        invoiceNumber = no,
                                        paymentEventId = eventId,
                                        settlementId = settlementId,
                                        actorId = actorId,
                                        actorName = actorName,
                                        actorTaxId = taxId,
                                        concept = concept,
                                        periodYear = year,
                                        periodMonth = month,
                                        profitShareCents = profitShare,
                                        spaceRentCents = spaceRent,
                                        totalAmountCents = total,
                                        paymentChannel = channel,
                                        paymentReference = ref,
                                        debitAccountCode = debit,
                                        creditAccountCode = credit,
                                        costCenter = costCenter
                                    )
                                    showInvoiceDialog = false
                                    editingInvoice = null
                                    prefillInvoiceEvent = null
                                }
                            )
                        }
                    }
                }
            }
            }

            if (showManagerLoginDialog) {
                ManagerLoginDialog(
                    managerUsers = uiState.managerUsers,
                    onDismiss = { showManagerLoginDialog = false },
                    onSignIn = { username, password ->
                        viewModel.signInManager(username, password).also { res ->
                            if (res.isSuccess) {
                                workspaceMode = AppWorkspaceMode.DIRECTOR
                            }
                        }
                    },
                    onRegisterFirstManager = { username, fullName, role, password ->
                        viewModel.registerFirstManagerAndSignIn(username, fullName, role, password).also { res ->
                            if (res.isSuccess) {
                                workspaceMode = AppWorkspaceMode.DIRECTOR
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DestinationNavIcon(
    destination: DirectorDestination,
    selected: Boolean,
    unverifiedCount: Int
) {
    val iconVector = if (selected) destination.selectedIcon else destination.unselectedIcon
    val label = stringResource(destination.labelRes)
    if (destination == DirectorDestination.LiveMonitor && unverifiedCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (unverifiedCount > 99) "99+" else "$unverifiedCount")
                }
            }
        ) {
            Icon(imageVector = iconVector, contentDescription = label)
        }
    } else {
        Icon(imageVector = iconVector, contentDescription = label)
    }
}
