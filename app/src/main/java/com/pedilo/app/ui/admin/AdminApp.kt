package com.pedilo.app.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.core.model.AdminActiveOrdersBucket
import com.pedilo.app.core.model.AdminConfigState
import com.pedilo.app.core.model.AdminConfigUpdateRequest
import com.pedilo.app.core.model.AdminLiveOrderActionRequest
import com.pedilo.app.core.model.AdminOrderPrimaryPlacement
import com.pedilo.app.core.model.AdminOperationOrderClassification
import com.pedilo.app.core.model.AdminOperationOrderSignals
import com.pedilo.app.core.model.AdminOrderDetail
import com.pedilo.app.core.model.AdminOrderEvent
import com.pedilo.app.core.model.AdminOrderSummary
import com.pedilo.app.core.model.AdminOperationalHealthReport
import com.pedilo.app.core.model.AdminProblemOrdersBucket
import com.pedilo.app.core.model.AdminRoleUpdateRequest
import com.pedilo.app.core.model.AdminTeamUser
import com.pedilo.app.core.model.LiveOrderAction
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.runtime.adminOrdersUseCase
import com.pedilo.app.ui.admin.components.AdminBottomBar
import com.pedilo.app.ui.admin.components.AdminEntryCard
import com.pedilo.app.ui.admin.components.AdminHeader
import com.pedilo.app.ui.admin.components.AdminInfoPanel
import com.pedilo.app.ui.components.PediloTextField
import com.pedilo.app.ui.publicuser.PediloBg
import com.pedilo.app.ui.publicuser.PediloCardBrush
import com.pedilo.app.ui.publicuser.PediloCyan
import com.pedilo.app.ui.publicuser.PediloGreen
import com.pedilo.app.ui.publicuser.PediloLine
import com.pedilo.app.ui.publicuser.PediloMuted
import com.pedilo.app.ui.publicuser.PediloOrange
import com.pedilo.app.ui.publicuser.PediloPanel
import com.pedilo.app.ui.publicuser.PediloPanelSoft
import com.pedilo.app.ui.publicuser.PediloPink
import com.pedilo.app.ui.publicuser.PediloText
import com.pedilo.app.ui.publicuser.PediloWarning
import com.pedilo.app.ui.publicuser.pediloCardDepth
import java.util.Calendar
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.launch

enum class AdminRoot(val label: String) {
    Operation("Operación"),
    Configuration("Configuración"),
    RoleAccess("Equipo"),
}

internal enum class OperationOrderVariant {
    Normal,
    NeedsAttention,
    WithProblem,
    ActionUnavailable,
}

private enum class AdminOrderSection {
    Summary,
    Operation,
    Delivery,
    Payment,
    Problems,
    History,
    Options,
}

private sealed interface AdminRoute {
    data object Operation : AdminRoute
    data object Configuration : AdminRoute
    data object ConfigurationShippingFee : AdminRoute
    data object ConfigurationRainMode : AdminRoute
    data object ConfigurationBaseDeliveryFee : AdminRoute
    data object ConfigurationDistanceSurcharge : AdminRoute
    data object RoleAccess : AdminRoute
    data class OperationBranch(val list: AdminOperationList) : AdminRoute
    data class OperationQueue(val list: AdminOperationList) : AdminRoute
    data class OperationOrderDetail(
        val returnRoute: AdminRoute,
        val variant: OperationOrderVariant,
        val realOrderId: String? = null,
    ) : AdminRoute
    data class OperationGuidedAction(
        val detailRoute: OperationOrderDetail,
        val action: LiveOrderAction,
        val expectedVersion: Int,
    ) : AdminRoute
    data class OperationOrderSection(
        val detailRoute: OperationOrderDetail,
        val section: AdminOrderSection,
    ) : AdminRoute
    data class Section(val root: AdminRoot, val title: String) : AdminRoute
    data class RoleAccessSection(val section: AdminRoleAccessSection) : AdminRoute
    data class RoleAccessSubsection(val section: AdminRoleAccessSection, val title: String) : AdminRoute
    data class RoleAccessConvergence(
        val section: String,
        val subsection: String,
        val step: AdminRoleAccessConvergenceStep,
    ) : AdminRoute
}

private enum class AdminRoleAccessConvergenceStep {
    Account,
    CreateAccount,
    AccessEditor,
    ChangeRole,
    ToggleAccess,
    LinkEntity,
    Impact,
    SensitiveConfirmation,
    Result,
    Audit,
}

data class AdminEntry(
    val title: String,
    val note: String,
)

private enum class AdminOperationMetricTone {
    Neutral,
    Healthy,
    Warning,
    Danger,
}

private enum class AdminHumanIntent {
    Info,
    Success,
    Warning,
    Problem,
    Emergency,
    Audit,
    Edit,
    Preview,
    Impact,
    Confirm,
    Access,
}

private data class AdminOrderNavigationEntry(
    val section: AdminOrderSection,
    val icon: ImageVector,
    val title: String,
    val note: String,
)

private data class AdminDeskOrderRow(
    val order: AdminOrderSummary,
    val label: String,
    val status: String,
    val reason: String,
    val nextStep: String,
    val tone: AdminOperationMetricTone,
    val variant: OperationOrderVariant,
)

private data class AdminLiveBranch(
    val title: String,
    val state: String,
    val kind: AdminOperationListKind,
    val icon: ImageVector,
    val tone: AdminOperationMetricTone,
    val rows: List<AdminDeskOrderRow>,
)

private data class AdminBranchGroup(
    val title: String,
    val waitingFor: String,
    val actionBy: String,
    val resolution: String,
    val kind: AdminOperationListKind,
    val rows: List<AdminDeskOrderRow>,
)

private data class AdminGuidedActionChoice(
    val label: String,
    val result: String,
    val reason: String,
)

private data class AdminPendingLiveAction(
    val orderId: String,
    val action: LiveOrderAction,
    val expectedVersion: Int,
)

private data class AdminRoleAccessSection(
    val title: String,
    val summary: String,
    val contextTitle: String,
    val contextText: String,
    val entries: List<AdminEntry>,
)

private val adminBottomBarReservedPadding = 128.dp
private val adminContentBottomPadding = 24.dp
private val adminOperationHomeExpectedLabels = listOf(
    "Pedidos con problemas",
    "En espera de aceptación",
    "Aceptados",
    "En preparación",
    "En camino",
    "Entregados / cerrados con problemas",
)

private val roleAccessSections = listOf(
    AdminRoleAccessSection(
        title = "Usuarios del equipo",
        summary = "Vista general de cuentas vinculadas al sistema.",
        contextTitle = "Cuentas del equipo",
        contextText = "Estado de acceso y roles.",
        entries = listOf(
            AdminEntry("Cuentas activas", "Acceso habilitado en revisión"),
            AdminEntry("Cuentas en revisión", "Pendientes de validación"),
            AdminEntry("Roles asignados", "Distribución de Admin, Local y Repartidor"),
            AdminEntry("Estado de acceso", "Lectura de habilitación"),
            AdminEntry("Vínculos operativos", "Relación con entidad operativa"),
            AdminEntry("Actividad", "Historial de cambios"),
        ),
    ),
    AdminRoleAccessSection(
        title = "Administradores",
        summary = "Cuentas con alcance administrativo.",
        contextTitle = "Acceso administrativo",
        contextText = "Revisión de cuentas Admin.",
        entries = listOf(
            AdminEntry("Cuentas Admin", "Listado de acceso"),
            AdminEntry("Crear Admin", "No hay acción disponible"),
            AdminEntry("Estado de acceso", "Control de vigencia administrativa"),
            AdminEntry("Nivel de sensibilidad", "Impacto del acceso"),
            AdminEntry("Permisos visibles", "Alcance de acciones permitido"),
            AdminEntry("Actividad", "Historial de cambios"),
        ),
    ),
    AdminRoleAccessSection(
        title = "Locales store",
        summary = "Cuentas con rol Local.",
        contextTitle = "Cuentas store",
        contextText = "Organiza relación de cuenta y local sin editar la entidad comercial.",
        entries = listOf(
            AdminEntry("Cuentas Local", "Estado de cuentas store"),
            AdminEntry("Crear Local", "No hay acción disponible"),
            AdminEntry("Local vinculado", "Relación con local asignado"),
            AdminEntry("Vinculación pendiente", "Cuenta sin relación completa"),
            AdminEntry("Estado de acceso", "Lectura de habilitación de ingreso"),
            AdminEntry("Actividad", "Historial de cambios"),
        ),
    ),
    AdminRoleAccessSection(
        title = "Repartidores driver",
        summary = "Cuentas con rol Repartidor.",
        contextTitle = "Cuentas driver",
        contextText = "Organiza relación de cuenta y repartidor sin operar entregas.",
        entries = listOf(
            AdminEntry("Cuentas Repartidor", "Estado de cuentas driver"),
            AdminEntry("Crear Repartidor", "No hay acción disponible"),
            AdminEntry("Repartidor vinculado", "Relación con entidad de reparto"),
            AdminEntry("Vinculación pendiente", "Cuenta con vínculo incompleto"),
            AdminEntry("Estado de acceso", "Lectura de habilitación de ingreso"),
            AdminEntry("Actividad", "Historial de cambios"),
        ),
    ),
    AdminRoleAccessSection(
        title = "Altas pendientes",
        summary = "Cuentas en proceso de alta.",
        contextTitle = "Pendientes de alta",
        contextText = "Ordena estados previos a habilitación sin crear cuentas reales.",
        entries = listOf(
            AdminEntry("Cuentas por revisar", "Pendientes de validación administrativa"),
            AdminEntry("Rol previsto", "Perfil objetivo de la cuenta"),
            AdminEntry("Datos faltantes", "Información pendiente para completar"),
            AdminEntry("Estado pendiente", "Situación actual de la alta"),
            AdminEntry("Revisión antes de activar", "Chequeo previo a habilitación"),
            AdminEntry("Resultado", "Cierre"),
        ),
    ),
    AdminRoleAccessSection(
        title = "Usuarios inactivos",
        summary = "Cuentas con acceso detenido.",
        contextTitle = "Acceso inactivo",
        contextText = "Representa inactividad sin borrar historial ni reactivar cuentas.",
        entries = listOf(
            AdminEntry("Cuentas inactivas", "Acceso actualmente detenido"),
            AdminEntry("Acceso pausado", "Estado de ingreso"),
            AdminEntry("Motivo visible", "Causa administrativa declarada"),
            AdminEntry("Revisión pendiente", "Control previo a cambio de estado"),
            AdminEntry("Posible reactivación", "Ruta de revisión posterior"),
            AdminEntry("Actividad", "Historial de cambios"),
        ),
    ),
    AdminRoleAccessSection(
        title = "Vinculaciones pendientes",
        summary = "Cuentas con rol asignado y vínculo incompleto.",
        contextTitle = "Relaciones pendientes",
        contextText = "Ordena relaciones faltantes sin crear entidades ni aplicar vínculos reales.",
        entries = listOf(
            AdminEntry("Store sin local", "Relación comercial incompleta"),
            AdminEntry("Driver sin repartidor", "Relación operativa incompleta"),
            AdminEntry("Relación incompleta", "Pendiente de asociación final"),
            AdminEntry("Entidad pendiente", "Entidad destino por definir"),
            AdminEntry("Revisión de vínculo", "Control de consistencia de asociación"),
            AdminEntry("Resultado", "Cierre"),
        ),
    ),
    AdminRoleAccessSection(
        title = "Actividad de acceso",
        summary = "Cambios de roles, accesos y vínculos.",
        contextTitle = "Historial de acceso",
        contextText = "Cambios de cuenta y rol.",
        entries = listOf(
            AdminEntry("Cambios de rol", "Antes, después y motivo"),
            AdminEntry("Activaciones", "Accesos habilitados"),
            AdminEntry("Pausas de acceso", "Accesos detenidos"),
            AdminEntry("Reactivaciones", "Accesos restaurados"),
            AdminEntry("Vinculaciones", "Relaciones cuenta y entidad"),
            AdminEntry("Detalle de registro", "Quién, cuándo y resultado"),
        ),
    ),
)

private val roleAccessRootEntries = roleAccessSections.map {
    AdminEntry(it.title, "Abrir mundo")
}

@Composable
fun AdminApp(onSignOutConfirmed: () -> Unit) {
    var route by remember { mutableStateOf<AdminRoute>(AdminRoute.Operation) }
    var showSignOut by remember { mutableStateOf(false) }
    var readOnlyOrders by remember { mutableStateOf<List<AdminOrderSummary>>(emptyList()) }
    var readOnlyOrderDetails by remember { mutableStateOf<Map<String, AdminOrderDetail>>(emptyMap()) }
    var operationalHealth by remember { mutableStateOf<AdminOperationalHealthReport?>(null) }
    var teamUsers by remember { mutableStateOf<List<AdminTeamUser>>(emptyList()) }
    var adminConfig by remember { mutableStateOf(AdminConfigState()) }
    var accessMessage by remember { mutableStateOf("") }
    var accessError by remember { mutableStateOf("") }
    var configMessage by remember { mutableStateOf("") }
    var configError by remember { mutableStateOf("") }
    var operationMessage by remember { mutableStateOf("") }
    var operationError by remember { mutableStateOf("") }
    val adminOrders = remember { adminOrdersUseCase() }
    val scope = rememberCoroutineScope()

    fun loadOrderDetail(orderId: String, force: Boolean = false) {
        if (!force && readOnlyOrderDetails.containsKey(orderId)) return
        scope.launch {
            when (val result = adminOrders.getDetail(orderId)) {
                is CoreResult.Success -> readOnlyOrderDetails = readOnlyOrderDetails + (orderId to result.value)
                is CoreResult.Failure -> operationError = "No pudimos actualizar el pedido."
            }
        }
    }

    fun executePendingLiveAction(pending: AdminPendingLiveAction, reason: String) {
        scope.launch {
            operationMessage = ""
            operationError = ""
            when (val result = adminOrders.executeLive(
                AdminLiveOrderActionRequest(
                    orderId = pending.orderId,
                    action = pending.action,
                    expectedVersion = pending.expectedVersion,
                    reason = reason,
                ),
            )) {
                is CoreResult.Success -> {
                    operationMessage = result.value.humanMessage.ifBlank { result.value.eventSummary }
                    loadOrderDetail(pending.orderId, force = true)
                }
                is CoreResult.Failure -> {
                    operationError = result.error.adminHumanError()
                    loadOrderDetail(pending.orderId, force = true)
                }
            }
        }
    }

    fun recalculateOrderActions(orderId: String) {
        scope.launch {
            operationMessage = ""
            operationError = ""
            when (val result = adminOrders.recalculateActions(orderId)) {
                is CoreResult.Success -> {
                    val actions = result.value.nextAllowedActions.joinToString { it.adminActionLabel() }
                    operationMessage = result.value.humanMessage.ifBlank { result.value.eventSummary }
                        .let { message -> if (actions.isBlank()) message else "$message Podés: $actions." }
                    loadOrderDetail(orderId, force = true)
                }
                is CoreResult.Failure -> {
                    operationError = result.error.adminHumanError()
                    loadOrderDetail(orderId, force = true)
                }
            }
        }
    }


    fun updateTeamUser(request: AdminRoleUpdateRequest) {
        scope.launch {
            accessMessage = ""
            accessError = ""
            when (val result = adminOrders.updateTeamUser(request)) {
                is CoreResult.Success -> accessMessage = result.value.message
                is CoreResult.Failure -> accessError = result.error.adminHumanError()
            }
        }
    }

    fun updateAdminConfig(request: AdminConfigUpdateRequest) {
        scope.launch {
            configMessage = ""
            configError = ""
            when (val result = adminOrders.updateAdminConfig(request)) {
                is CoreResult.Success -> {
                    adminConfig = result.value.config ?: adminConfig.withAppliedConfigUpdate(request)
                    configMessage = result.value.message.ifBlank { "Guardado." }
                }
                is CoreResult.Failure -> configError = result.error.adminHumanError()
            }
        }
    }

    LaunchedEffect(Unit) {
        when (val result = adminOrders.getHealth()) {
            is CoreResult.Success -> operationalHealth = result.value
            is CoreResult.Failure -> operationalHealth = null
        }
        launch {
            adminOrders.observeTeamUsers().collect { result ->
                when (result) {
                    is CoreResult.Success -> teamUsers = result.value
                    is CoreResult.Failure -> accessError = "No pudimos leer usuarios y roles."
                }
            }
        }
        launch {
            adminOrders.observeAdminConfig().collect { result ->
                when (result) {
                    is CoreResult.Success -> {
                        adminConfig = result.value
                        configError = ""
                    }
                    is CoreResult.Failure -> configError = "No pudimos cargar configuración. Revisá conexión o sesión."
                }
            }
        }
        adminOrders.observe().collect { result ->
            when (result) {
                is CoreResult.Success -> readOnlyOrders = result.value
                is CoreResult.Failure -> readOnlyOrders = emptyList()
            }
        }
    }

    BackHandler(enabled = route !is AdminRoute.Operation && route !is AdminRoute.Configuration && route !is AdminRoute.RoleAccess) {
        route = when (val current = route) {
            is AdminRoute.RoleAccessSubsection -> AdminRoute.RoleAccessSection(current.section)
            is AdminRoute.RoleAccessConvergence -> when (current.step) {
                AdminRoleAccessConvergenceStep.Account -> AdminRoute.RoleAccessSubsection(
                    section = roleAccessSections.first { it.title == current.section },
                    title = current.subsection,
                )
                AdminRoleAccessConvergenceStep.CreateAccount -> current.copy(step = AdminRoleAccessConvergenceStep.Account)
                AdminRoleAccessConvergenceStep.AccessEditor -> current.copy(step = AdminRoleAccessConvergenceStep.Account)
                AdminRoleAccessConvergenceStep.ChangeRole -> current.copy(step = AdminRoleAccessConvergenceStep.Account)
                AdminRoleAccessConvergenceStep.ToggleAccess -> current.copy(step = AdminRoleAccessConvergenceStep.Account)
                AdminRoleAccessConvergenceStep.LinkEntity -> current.copy(step = AdminRoleAccessConvergenceStep.Account)
                AdminRoleAccessConvergenceStep.Impact -> current.copy(step = AdminRoleAccessConvergenceStep.Account)
                AdminRoleAccessConvergenceStep.SensitiveConfirmation -> current.copy(step = AdminRoleAccessConvergenceStep.Impact)
                AdminRoleAccessConvergenceStep.Result -> current.copy(step = AdminRoleAccessConvergenceStep.SensitiveConfirmation)
                AdminRoleAccessConvergenceStep.Audit -> current.copy(step = AdminRoleAccessConvergenceStep.Result)
            }
            is AdminRoute.RoleAccessSection -> AdminRoute.RoleAccess
            AdminRoute.ConfigurationRainMode,
            AdminRoute.ConfigurationBaseDeliveryFee,
            AdminRoute.ConfigurationDistanceSurcharge -> AdminRoute.ConfigurationShippingFee
            AdminRoute.ConfigurationShippingFee -> AdminRoute.Configuration
            is AdminRoute.OperationGuidedAction -> current.detailRoute
            is AdminRoute.OperationOrderSection -> current.detailRoute
            is AdminRoute.OperationOrderDetail -> current.returnRoute
            is AdminRoute.OperationQueue -> AdminRoute.OperationBranch(
                adminMainListForQueue(current.list),
            )
            is AdminRoute.OperationBranch -> AdminRoute.Operation
            is AdminRoute.Section -> when (current.root) {
                AdminRoot.Operation -> AdminRoute.Operation
                AdminRoot.Configuration -> AdminRoute.Configuration
                AdminRoot.RoleAccess -> AdminRoute.RoleAccess
            }
            AdminRoute.Operation -> AdminRoute.Operation
            AdminRoute.Configuration -> AdminRoute.Configuration
            AdminRoute.RoleAccess -> AdminRoute.RoleAccess
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PediloBg),
    ) {
        when (val current = route) {
            AdminRoute.Operation -> AdminOperationDeskScreen(
                orders = readOnlyOrders,
                onOpenBranch = { list ->
                    route = AdminRoute.OperationBranch(list)
                },
                onSignOut = { showSignOut = true },
            )
            AdminRoute.Configuration -> AdminRealConfigurationScreen(
                message = configMessage,
                error = configError,
                onOpenShippingFee = { route = AdminRoute.ConfigurationShippingFee },
            )
            AdminRoute.ConfigurationShippingFee -> AdminShippingFeeScreen(
                onRainMode = { route = AdminRoute.ConfigurationRainMode },
                onBaseDeliveryFee = { route = AdminRoute.ConfigurationBaseDeliveryFee },
                onDistanceSurcharge = { route = AdminRoute.ConfigurationDistanceSurcharge },
            )
            AdminRoute.ConfigurationRainMode -> AdminRainModeScreen(
                config = adminConfig,
                message = configMessage,
                error = configError,
                onToggle = { updateAdminConfig(AdminConfigUpdateRequest(field = "rainMode", enabled = !adminConfig.rainMode)) },
                onAmount = { value -> updateAdminConfig(AdminConfigUpdateRequest(field = "rainDeliveryFee", amount = value)) },
            )
            AdminRoute.ConfigurationBaseDeliveryFee -> AdminMoneyConfigScreen(
                title = "Costo de envío",
                currentValue = adminConfig.baseDeliveryFee,
                inputLabel = "Nuevo valor",
                onAmount = { value -> updateAdminConfig(AdminConfigUpdateRequest(field = "baseDeliveryFee", amount = value)) },
                message = configMessage,
                error = configError,
            )
            AdminRoute.ConfigurationDistanceSurcharge -> AdminMoneyConfigScreen(
                title = "Adicional por distancia",
                currentValue = adminConfig.distanceSurcharge,
                inputLabel = "Nuevo valor",
                onAmount = { value -> updateAdminConfig(AdminConfigUpdateRequest(field = "distanceSurcharge", amount = value)) },
                message = configMessage,
                error = configError,
            )
            AdminRoute.RoleAccess -> AdminRealRoleAccessScreen(
                users = teamUsers,
                message = accessMessage,
                error = accessError,
                onToggleActive = { user -> updateTeamUser(AdminRoleUpdateRequest(uid = user.uid, active = !user.active)) },
                onRole = { user, role -> updateTeamUser(AdminRoleUpdateRequest(uid = user.uid, role = role)) },
                onSignOut = { showSignOut = true },
            )
            is AdminRoute.OperationBranch -> AdminOperationBranchScreen(
                list = current.list,
                orders = readOnlyOrders,
                onOpenQueue = { queue ->
                    route = AdminRoute.OperationQueue(queue)
                },
            )
            is AdminRoute.OperationQueue -> AdminOperationQueueScreen(
                list = current.list,
                orders = readOnlyOrders,
                onOrderDetail = { row ->
                    route = AdminRoute.OperationOrderDetail(
                        returnRoute = AdminRoute.OperationQueue(current.list),
                        variant = row.variant,
                        realOrderId = row.order.id,
                    )
                },
            )
            is AdminRoute.OperationOrderDetail -> AdminOrderDetailScreen(
                variant = current.variant,
                orderId = current.realOrderId,
                summary = current.realOrderId?.let { id -> readOnlyOrders.firstOrNull { it.id == id } },
                detail = current.realOrderId?.let { readOnlyOrderDetails[it] },
                operationMessage = operationMessage,
                operationError = operationError,
                navigationOrigin = current.returnRoute.adminOrderOriginLabel(),
                onLoadDetail = { orderId -> loadOrderDetail(orderId) },
                onLiveAction = { action, version ->
                    if (current.realOrderId != null) {
                        route = AdminRoute.OperationGuidedAction(current, action, version)
                    }
                },
                onRepairActions = {
                    current.realOrderId?.let { orderId -> recalculateOrderActions(orderId) }
                },
                onSection = { section ->
                    route = AdminRoute.OperationOrderSection(current, section)
                },
                onBackToDesk = {
                    route = current.returnRoute
                    operationMessage = ""
                    operationError = ""
                },
                onBackToHome = {
                    route = AdminRoute.Operation
                    operationMessage = ""
                    operationError = ""
                },
            )
            is AdminRoute.OperationGuidedAction -> AdminGuidedActionScreen(
                detailRoute = current.detailRoute,
                action = current.action,
                expectedVersion = current.expectedVersion,
                summary = current.detailRoute.realOrderId?.let { id -> readOnlyOrders.firstOrNull { it.id == id } },
                detail = current.detailRoute.realOrderId?.let { readOnlyOrderDetails[it] },
                operationMessage = operationMessage,
                operationError = operationError,
                onConfirm = { reason ->
                    current.detailRoute.realOrderId?.let { orderId ->
                        executePendingLiveAction(
                            AdminPendingLiveAction(orderId, current.action, current.expectedVersion),
                            reason,
                        )
                    }
                },
                onBackToDetail = { route = current.detailRoute },
                onBackToQueue = { route = current.detailRoute.returnRoute },
                onBackToHome = { route = AdminRoute.Operation },
            )
            is AdminRoute.OperationOrderSection -> AdminOrderSectionScreen(
                section = current.section,
                variant = current.detailRoute.variant,
                orderId = current.detailRoute.realOrderId,
                summary = current.detailRoute.realOrderId?.let { id -> readOnlyOrders.firstOrNull { it.id == id } },
                detail = current.detailRoute.realOrderId?.let { readOnlyOrderDetails[it] },
            )
            is AdminRoute.RoleAccessSection,
            is AdminRoute.RoleAccessSubsection,
            is AdminRoute.RoleAccessConvergence -> AdminRealRoleAccessScreen(
                users = teamUsers,
                message = accessMessage,
                error = accessError,
                onToggleActive = { user -> updateTeamUser(AdminRoleUpdateRequest(uid = user.uid, active = !user.active)) },
                onRole = { user, role -> updateTeamUser(AdminRoleUpdateRequest(uid = user.uid, role = role)) },
                onSignOut = { showSignOut = true },
            )
            is AdminRoute.Section -> AdminSectionScreen(
                root = current.root,
                title = current.title,
                summary = "Revisá el contenido visible antes de avanzar.",
                panelTitle = "Consulta",
                panelText = "Este espacio muestra información de apoyo y deriva la operación a pedidos, configuración o equipo.",
            )
        }

        AdminBottomBar(
            current = route.root(),
            onOperation = { route = AdminRoute.Operation },
            onConfiguration = { route = AdminRoute.Configuration },
            onRoleAccess = { route = AdminRoute.RoleAccess },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }

    if (showSignOut) {
        AlertDialog(
            onDismissRequest = { showSignOut = false },
            title = { Text("¿Querés cerrar sesión?") },
            confirmButton = {
                TextButton(onClick = onSignOutConfirmed) {
                    Text("Sí")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOut = false }) {
                    Text("No")
                }
            },
        )
    }

}

@Composable
private fun AdminRootScreen(
    title: String,
    eyebrow: String,
    summary: String,
    entries: List<AdminEntry>,
    onEntry: (AdminEntry) -> Unit,
    onSignOut: () -> Unit,
    showSignOut: Boolean,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = title,
                eyebrow = eyebrow,
                summary = summary,
                onSignOut = onSignOut,
                showSignOut = showSignOut,
            )
        }
        items(entries) {
            AdminEntryCard(entry = it, onClick = { onEntry(it) })
        }
    }
}

@Composable
private fun AdminOperationBranchScreen(
    list: AdminOperationList,
    orders: List<AdminOrderSummary>,
    onOpenQueue: (AdminOperationList) -> Unit,
) {
    val groups = adminBranchGroups(list, orders).filter { it.rows.isNotEmpty() }
    val orderCount = orders.forOperationList(list.kind).size
    val toneColor = adminListToneColor(list.kind, orderCount)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = operationCompactTitle(list.title),
                eyebrow = "Operación",
                summary = if (orderCount == 0) list.emptyText else "$orderCount pedidos para abrir en cola",
                onSignOut = {},
                showSignOut = false,
            )
        }
        item {
            AdminBranchIntentPanel(
                title = list.title,
                detail = list.summary,
                count = orderCount,
                toneColor = toneColor,
            )
        }
        if (orderCount == 0) {
            item {
                AdminOrderMomentPanel(
                    title = list.emptyText,
                    detail = adminOperationEmptyText(list.kind),
                    highlighted = false,
                )
            }
        } else {
            groups.forEach { group ->
                item {
                    AdminSubBranchCard(group = group, onMore = {
                        onOpenQueue(AdminOperationList(
                            title = group.title,
                            summary = group.waitingFor,
                            emptyText = "No hay pedidos en ${group.title.lowercase()}",
                            kind = group.kind,
                        ))
                    })
                }
            }
        }
    }
}

@Composable
private fun AdminOperationDeskScreen(
    orders: List<AdminOrderSummary>,
    onOpenBranch: (AdminOperationList) -> Unit,
    onSignOut: () -> Unit,
) {
    val branches = adminLiveBranches(orders)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = "Admin Operación",
                eyebrow = "Pedidos vivos",
                summary = adminDeskSummary(orders),
                onSignOut = onSignOut,
                showSignOut = true,
            )
        }
        item {
            AdminOperationPulseBand(branches = branches)
        }
        items(branches, key = { it.title }) { branch ->
            AdminLiveBranchCard(
                branch = branch,
                orders = orders,
                onMore = {
                    onOpenBranch(AdminOperationList(
                        title = branch.title,
                        summary = branch.state,
                        emptyText = "Sin pedidos",
                        kind = branch.kind,
                    ))
                },
            )
        }
    }
}

@Composable
private fun AdminOperationPulseBand(branches: List<AdminLiveBranch>) {
    val problems = branches.firstOrNull { it.kind == AdminOperationListKind.AllProblems }?.rows?.size ?: 0
    val waiting = branches.firstOrNull { it.kind == AdminOperationListKind.ActiveWaitingStore }?.rows?.size ?: 0
    val moving = branches.firstOrNull { it.kind == AdminOperationListKind.AllInDelivery }?.rows?.size ?: 0
    val toneColor = when {
        problems > 0 -> PediloPink
        waiting > 0 -> PediloWarning
        moving > 0 -> PediloCyan
        else -> PediloGreen
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(toneColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.46f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                Text("Pulso operativo", color = toneColor, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(adminOperationPulseText(problems, waiting, moving), color = PediloText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
            }
            AdminStatusChip(adminOperationPriorityLabel(AdminOperationListKind.AllProblems, problems), toneColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AdminPulseMetric("Problemas", problems, PediloPink, Modifier.weight(1f))
            AdminPulseMetric("Esperan", waiting, PediloWarning, Modifier.weight(1f))
            AdminPulseMetric("En camino", moving, PediloCyan, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdminPulseMetric(
    label: String,
    value: Int,
    toneColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value.toString(), color = toneColor, fontSize = 21.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = PediloMuted, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdminQueueFocusPanel(
    list: AdminOperationList,
    count: Int,
    toneColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(toneColor.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(PediloPanel.copy(alpha = 0.74f), RoundedCornerShape(8.dp))
                .border(1.dp, toneColor.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                .size(46.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(count.toString(), color = toneColor, fontSize = 22.sp, lineHeight = 25.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(adminOperationPriorityLabel(list.kind, count), color = toneColor, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text(list.summary, color = PediloText, fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
            Text(adminOperationOutcomeText(list.kind), color = PediloMuted, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun AdminBranchIntentPanel(
    title: String,
    detail: String,
    count: Int,
    toneColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(toneColor.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
            .border(1.dp, toneColor.copy(alpha = 0.34f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(PediloPanel.copy(alpha = 0.72f), RoundedCornerShape(10.dp))
                .size(42.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(count.toString(), color = toneColor, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = PediloText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(detail, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun AdminLiveBranchCard(
    branch: AdminLiveBranch,
    orders: List<AdminOrderSummary>,
    onMore: () -> Unit,
) {
    val toneColor = adminListToneColor(branch.kind, branch.rows.size)
    val groups = adminBranchGroups(
        AdminOperationList(branch.title, branch.state, "Sin pedidos", branch.kind),
        orders,
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val isOpenable = branch.rows.isNotEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed && isOpenable) 0.992f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(toneColor.copy(alpha = if (pressed && isOpenable) 0.18f else 0.12f), PediloPanelSoft, PediloPanel),
                ),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, toneColor.copy(alpha = if (pressed && isOpenable) 0.74f else 0.48f), RoundedCornerShape(8.dp))
            .then(
                if (isOpenable) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onMore)
                } else {
                    Modifier
                },
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(toneColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .border(1.dp, toneColor.copy(alpha = 0.36f), RoundedCornerShape(6.dp))
                        .size(38.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(branch.icon, contentDescription = branch.title, tint = toneColor, modifier = Modifier.size(22.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
                    Text(branch.title, color = PediloText, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
                    if (branch.rows.isNotEmpty()) {
                        Text(branch.state, color = PediloMuted, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(branch.rows.size.toString(), color = toneColor, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
                AdminStatusChip(adminOperationPriorityLabel(branch.kind, branch.rows.size), toneColor)
            }
        }
        if (branch.rows.isEmpty()) {
                AdminOrderMomentPanel(
                    title = "Sin pedidos",
                    detail = adminOperationEmptyText(branch.kind),
                    highlighted = false,
                )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 76.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                groups.take(4).forEach { group ->
                    AdminMainBranchPreview(group = group)
                }
            }
        }
        if (branch.rows.isNotEmpty()) {
            AdminBranchOutcomeRow(
                kind = branch.kind,
                toneColor = toneColor,
            )
            AdminInlineActionButton(
                title = "Ver grupos",
                subtitle = "${groups.count { it.rows.isNotEmpty() }} grupos",
                toneColor = toneColor,
                onClick = onMore,
            )
        }
    }
}

@Composable
private fun AdminBranchOutcomeRow(
    kind: AdminOperationListKind,
    toneColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloBg.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Al confirmar", color = PediloMuted, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
        Text(adminOperationOutcomeText(kind), color = toneColor, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdminMainBranchPreview(group: AdminBranchGroup) {
    val toneColor = adminListToneColor(group.kind, group.rows.size)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(toneColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.24f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(group.title, color = PediloText, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("${group.rows.size}", color = toneColor, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold)
        }
        val sample = group.rows.take(2).joinToString(" · ") { it.label }
        Text(sample.ifBlank { group.waitingFor }, color = PediloMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(group.resolution, color = toneColor, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdminSubBranchCard(
    group: AdminBranchGroup,
    onMore: () -> Unit,
) {
    val toneColor = adminListToneColor(group.kind, group.rows.size)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.992f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) toneColor.copy(alpha = 0.14f) else PediloPanel, RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = if (pressed) 0.74f else 0.48f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onMore)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(group.title, color = PediloText, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
                Text(group.waitingFor, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${group.rows.size}", color = toneColor, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
                AdminStatusChip(adminOperationPriorityLabel(group.kind, group.rows.size), toneColor)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AdminMiniOperationFact("Actúa", group.actionBy, toneColor, Modifier.weight(1f))
            AdminMiniOperationFact("Se resuelve con", group.resolution, toneColor, Modifier.weight(1f))
        }
        AdminInlineActionButton(
            title = "Ver pedidos",
            subtitle = "${group.rows.size} casos",
            toneColor = toneColor,
            onClick = onMore,
        )
    }
}

@Composable
private fun AdminMiniOperationFact(
    label: String,
    value: String,
    toneColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(toneColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(label, color = toneColor, fontSize = 10.sp, lineHeight = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = PediloText, fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdminOperationQueueScreen(
    list: AdminOperationList,
    orders: List<AdminOrderSummary>,
    onOrderDetail: (AdminDeskOrderRow) -> Unit,
) {
    val rows = orders.forOperationList(list.kind).map { it.adminOperationsRow() }
    val toneColor = adminListToneColor(list.kind, rows.size)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = list.title,
                eyebrow = "Pedidos",
                summary = if (rows.isEmpty()) list.emptyText else "${rows.size} pedidos",
                onSignOut = {},
                showSignOut = false,
            )
        }
        item {
            AdminQueueFocusPanel(list = list, count = rows.size, toneColor = toneColor)
        }
        if (rows.isEmpty()) {
            item {
                AdminOrderMomentPanel(
                    title = list.emptyText,
                    detail = "Cuando un pedido entre en este estado, aparece acá con su próxima acción.",
                    highlighted = false,
                )
            }
        } else {
            items(rows, key = { it.order.id }) { row ->
                AdminQueueOrderCard(row = row, toneColor = toneColor, onClick = { onOrderDetail(row) })
            }
        }
    }
}

@Composable
private fun AdminQueueOrderCard(
    row: AdminDeskOrderRow,
    toneColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.992f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) toneColor.copy(alpha = 0.12f) else PediloPanel, RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = if (pressed) 0.74f else 0.42f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(5.dp)
                .height(112.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(toneColor, RoundedCornerShape(8.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(row.label, color = PediloText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(row.order.adminActorLabel(), color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                AdminStatusChip(row.order.adminElapsedLabel(), toneColor)
            }
            Text(row.reason, color = toneColor, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(row.nextStep, color = PediloText, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AdminStatusChip(row.status, toneColor)
                Text("Ver pedido · Abrir ficha", color = PediloText, fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun AdminOperationalHealthPanel(
    health: AdminOperationalHealthReport?,
    orders: List<AdminOrderSummary>,
) {
    val metrics = health?.metrics
    val fallbackLive = orders.count { it.archiveStatus == "live" && it.status !in listOf("delivered", "closed", "archived", "cancelled", "canceled") }
    val healthLabel = health?.healthStatus?.adminHealthLabel() ?: "Sin alertas recibidas"
    val alertText = health?.alerts?.firstOrNull()?.warningMessage ?: "No hay alertas críticas informadas"
    val audit = health?.auditSummary

    AdminInfoPanel(
        title = "Salud operativa",
        text = "Estado $healthLabel · ${health?.alerts?.size ?: 0} alertas · $alertText",
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AdminHealthMetric("Vivos", (metrics?.liveOrders ?: fallbackLive).toString(), Modifier.weight(1f))
            AdminHealthMetric("Atención", (metrics?.requiresAttention ?: orders.count { it.needsAttention }).toString(), Modifier.weight(1f))
            AdminHealthMetric("Comunicación", (metrics?.failedCommunicationOrders ?: orders.count { it.communicationStatus == "failed" }).toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AdminHealthMetric("Finanzas", (metrics?.financialReviewOrders ?: 0).toString(), Modifier.weight(1f))
            AdminHealthMetric("Incidencias", (metrics?.openIncidentOrders ?: orders.count { it.activeIncident }).toString(), Modifier.weight(1f))
            AdminHealthMetric("IA pendiente", (metrics?.pendingAiSuggestionOrders ?: orders.count { it.aiRequiresHumanReview }).toString(), Modifier.weight(1f))
        }
        AdminInfoPanel(
            title = "Registro transversal",
            text = "Eventos ${audit?.orderEventRecords ?: 0} · Incidencias ${audit?.incidentRecords ?: 0} · Comunicaciones ${audit?.communicationRecords ?: 0} · Reclamos públicos ${metrics?.publicClaimsReceived ?: 0}",
        )
    }
}

@Composable
private fun AdminHealthMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel)
            .border(1.dp, PediloLine, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = PediloMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = PediloText, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun AdminOperationMetricTone.operationToneColor(): Color =
    when (this) {
        AdminOperationMetricTone.Neutral -> PediloMuted
        AdminOperationMetricTone.Healthy -> PediloGreen
        AdminOperationMetricTone.Warning -> PediloWarning
        AdminOperationMetricTone.Danger -> PediloWarning
    }

private fun adminBranchTone(kind: AdminOperationListKind, count: Int): AdminOperationMetricTone =
    when {
        count == 0 -> AdminOperationMetricTone.Neutral
        kind in setOf(
            AdminOperationListKind.AllAttention,
            AdminOperationListKind.AllProblems,
            AdminOperationListKind.AllBlocked,
            AdminOperationListKind.ProblemStoreNotResponding,
            AdminOperationListKind.ProblemUserClaim,
            AdminOperationListKind.ProblemDelayed,
            AdminOperationListKind.ProblemWithoutResponsible,
            AdminOperationListKind.ProblemOperationalReview,
        ) -> AdminOperationMetricTone.Danger
        kind in setOf(
            AdminOperationListKind.ActiveWaitingStore,
            AdminOperationListKind.ActiveWaitingDriver,
            AdminOperationListKind.ActiveReviewState,
            AdminOperationListKind.TodayReview,
            AdminOperationListKind.Unclassified,
        ) -> AdminOperationMetricTone.Warning
        kind in setOf(
            AdminOperationListKind.AllPreparing,
            AdminOperationListKind.AllInDelivery,
            AdminOperationListKind.ActivePreparing,
            AdminOperationListKind.ActiveInDelivery,
        ) -> AdminOperationMetricTone.Healthy
        else -> AdminOperationMetricTone.Neutral
    }

@Composable
private fun AdminQueueHeader(
    title: String,
    count: Int,
    state: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PediloPanel.copy(alpha = 0.88f), RoundedCornerShape(12.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.62f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            Text(title, color = PediloText, fontSize = 16.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text(state, color = PediloMuted, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
        }
        Text(count.toString(), color = PediloOrange, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun adminOrderVisibleNumber(
    summary: AdminOrderSummary?,
    detail: AdminOrderDetail?,
    orderId: String?,
): String =
    listOf(
        detail?.trackingNumber,
        summary?.trackingNumber,
        detail?.publicOrderNumber,
        summary?.publicOrderNumber,
        orderId?.take(8),
    ).firstOrNull { !it.isNullOrBlank() }?.let { "#$it" } ?: "#____"

private fun String?.adminDisplayValue(fallback: String = "—"): String =
    this?.trim()?.takeIf { it.isNotBlank() } ?: fallback

private fun String?.adminHumanText(): String =
    this?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { text ->
            val decoded = if (!text.contains("%")) {
                text
            } else {
                runCatching {
                    URLDecoder.decode(text, StandardCharsets.UTF_8.name())
                }.getOrDefault(text)
            }
            decoded.adminHumanInternalText()
        }
        .orEmpty()

private fun String.adminHumanInternalText(): String {
    val clean = trim()
    val lower = clean.lowercase()
    return when {
        "nextallowedactions" in lower || "acciones permitidas" in lower -> "Acciones del pedido actualizadas"
        "recalculateactions" in lower -> "Acciones del pedido actualizadas"
        "executelive" in lower -> "Pedido actualizado"
        "operationstatus" in lower -> "Estado del pedido actualizado"
        "admin_action_execute" in lower -> "Admin registró una acción"
        "admin action" in lower -> "Acción de Admin registrada"
        "cierre_admin_real" in lower -> "Admin cerró el caso"
        "revision operativa real" in lower || "revisión operativa real" in lower -> "Admin registró seguimiento operativo"
        "status_updated" in lower -> "El pedido cambió de estado"
        "back" + "end" in lower -> "Sistema actualizado"
        "public_app" in lower -> "Pedido recibido desde la app"
        "raw " + "status" in lower -> "Estado del pedido actualizado"
        lower == "order" + "_created" -> "Pedido creado"
        lower == "order_updated" -> "Pedido actualizado"
        lower == "incident_opened" -> "Incidencia abierta"
        lower == "incident_closed" -> "Incidencia resuelta"
        lower == "local" + "_accept" -> "Pedido aceptado por local"
        lower == "local_reject" -> "Pedido rechazado por local"
        lower == "local_mark_preparing" -> "Local inició preparación"
        lower == "local_mark_ready" -> "Pedido listo para retirar"
        lower == "store_driver_request" -> "Se pidió repartidor"
        lower == "driver" + "_take" -> "Repartidor tomó el pedido"
        lower == "driver_mark_picked_up" -> "Pedido retirado"
        lower == "driver" + "_mark_delivered" -> "Pedido entregado"
        lower == "cancel_order" -> "Pedido cancelado"
        lower == "open_incident" -> "Incidencia abierta"
        lower == "resolve_incident" -> "Incidencia resuelta"
        lower == "admin_intervene" -> "Admin tomó intervención"
        else -> clean
    }
}

private fun String.adminEventTypeLabel(): String =
    adminHumanInternalText().adminDisplayValue("Movimiento")

private fun String.adminHumanStatusValue(fallback: String = "—"): String =
    when (trim().lowercase()) {
        "" -> fallback
        "created" -> "Pedido recibido"
        "waiting_admin_review", "timeout_admin_review", "admin_intervention" -> "Revisar pedido"
        "incident_open" -> "Incidencia abierta"
        "incident_resolved" -> "Incidencia resuelta"
        "local" + "_accepted" -> "Aceptado por local"
        "rejected_by_store" -> "Rechazado por local"
        "preparing" -> "Preparando"
        "ready_for_pickup" -> "Listo para retirar"
        "waiting_driver" -> "Buscando repartidor"
        "driver_assigned" -> "Repartidor asignado"
        "picked_up" -> "Retirado"
        "cancelled", "canceled" -> "Cancelado"
        "cancelled_by_admin", "canceled_by_admin" -> "Cancelado por Admin"
        "cancelled_by_public", "canceled_by_public" -> "Cancelado por persona usuaria"
        "cancelled_by_store", "canceled_by_store" -> "Cancelado por local"
        "cancelled_by_driver", "canceled_by_driver" -> "Cancelado por repartidor"
        "delivered" -> "Entregado"
        "closed" -> "Cerrado"
        "archived" -> "Archivado"
        else -> trim().replace("_", " ").replaceFirstChar { it.uppercase() }
    }

private fun List<String>?.adminItemsSummary(): String =
    this?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n") ?: "Detalle no informado"

private fun AdminOrderDetail?.adminPersonName(fallback: String = "Sin responsable visible"): String =
    this?.component15().adminDisplayValue(fallback)

private fun adminHumanOperationStatus(
    publicStatus: String,
    operationalStatus: String,
    rawStatus: String,
    hasProblem: Boolean,
): String {
    val visible = "$publicStatus $operationalStatus"
    return when {
        visible.contains("local no responde", ignoreCase = true) ||
            visible.contains("sin respuesta", ignoreCase = true) -> "Local sin respuesta"
        visible.contains("demora", ignoreCase = true) ||
            visible.contains("retras", ignoreCase = true) -> "Demorado"
        visible.contains("preparando", ignoreCase = true) -> "Preparando"
        visible.contains("esperando repartidor", ignoreCase = true) -> "Buscando repartidor"
        visible.contains("en entrega", ignoreCase = true) -> "En camino"
        hasProblem -> "Con problema"
        publicStatus.isNotBlank() -> publicStatus.adminHumanStatusValue()
        else -> rawStatus.adminHumanStatusValue("Revisar datos")
    }
}

private fun adminOrderProblemFocus(
    variant: OperationOrderVariant,
    publicStatus: String,
    operationalStatus: String,
    needsAttention: Boolean,
    activeIncident: Boolean,
): Pair<String, String>? {
    val statusText = "$publicStatus $operationalStatus"
    return when {
        statusText.contains("local no responde", ignoreCase = true) ||
            statusText.contains("sin respuesta", ignoreCase = true) -> "Local sin respuesta" to "Requiere revisión"
        statusText.contains("demora", ignoreCase = true) ||
            statusText.contains("retras", ignoreCase = true) -> "Demorado" to "Requiere revisión"
        activeIncident -> "Incidencia registrada" to "Requiere revisión"
        needsAttention || variant == OperationOrderVariant.WithProblem -> "Requiere revisión" to "Con problema"
        variant == OperationOrderVariant.NeedsAttention -> "Esperando respuesta" to "Requiere seguimiento"
        else -> null
    }
}

private fun operationIconFor(title: String): ImageVector =
    when (title) {
        "Hoy", "Ingresaron hoy" -> Icons.Outlined.CalendarToday
        "Pedidos", "Pedidos pendientes / atención" -> Icons.AutoMirrored.Outlined.ReceiptLong
        "Activos", "Activos de hoy" -> Icons.Outlined.Bolt
        "Problemas", "Problemas de hoy", "Pedidos con problemas", "Con problemas", "Con incidencias" -> Icons.Outlined.ReportProblem
        "Cerrados", "Cerrados de hoy", "Pedidos cerrados", "Finalizados" -> Icons.Outlined.TaskAlt
        "Cancelados", "Pausados" -> Icons.Outlined.Cancel
        "Demorados", "Pedidos detenidos o bloqueados", "Con demoras" -> Icons.Outlined.Schedule
        "Esperando local", "Local no responde", "Locales", "Local / Retiro", "Retiro" -> Icons.Outlined.Storefront
        "Preparando", "Pedidos en preparación", "Compra" -> Icons.Outlined.Restaurant
        "Esperando repartidor", "Buscando repartidor", "En servicio", "Disponibles", "Repartidores" -> Icons.Outlined.TwoWheeler
        "En entrega", "En camino", "Pedidos en camino", "Entrega" -> Icons.Outlined.LocalShipping
        "Reclamo de persona usuaria" -> Icons.Outlined.Feedback
        "Sin responsable" -> Icons.Outlined.PersonOff
        "Revisar pedido", "Revisar estado", "Revisar hoy" -> Icons.Outlined.Search
        "Operaciones" -> Icons.Outlined.Search
        "Operando" -> Icons.Outlined.CheckCircle
        "Pago" -> Icons.Outlined.CreditCard
        "Historial" -> Icons.Outlined.History
        "Opciones" -> Icons.Outlined.MoreHoriz
        "Resumen" -> Icons.Outlined.Dashboard
        else -> Icons.Outlined.ChevronRight
    }

private fun operationCompactTitle(title: String): String =
    when (title) {
        "Con incidencias" -> "Incidencias"
        "Con demoras" -> "Demoras"
        "Reclamo de persona usuaria" -> "Reclamos"
        "Local no responde" -> "Local sin respuesta"
        "Esperando repartidor", "Buscando repartidor" -> "Buscando repartidor"
        "En entrega" -> "En camino"
        else -> title
    }

private fun operationHomeViewTitle(title: String): String =
    title

private fun adminDeskSummary(orders: List<AdminOrderSummary>): String {
    val active = orders.forPrimaryPlacement(AdminOrderPrimaryPlacement.ACTIVE).size
    val problems = orders.forPrimaryPlacement(AdminOrderPrimaryPlacement.PROBLEM).size
    return when {
        problems > 0 -> "$problems problemas · $active activos"
        active > 0 -> "$active activos"
        orders.isEmpty() -> "Sin pedidos para operar"
        else -> "${orders.size} pedidos para revisar"
    }
}

private fun adminLiveBranches(orders: List<AdminOrderSummary>): List<AdminLiveBranch> {
    fun branch(title: String, state: String, kind: AdminOperationListKind, tone: AdminOperationMetricTone): AdminLiveBranch {
        val rows = orders.forOperationList(kind).map { it.adminOperationsRow() }
        return AdminLiveBranch(
            title = title,
            state = state,
            kind = kind,
            icon = operationIconFor(title),
            tone = if (rows.isEmpty()) AdminOperationMetricTone.Neutral else tone,
            rows = rows,
        )
    }
    return listOf(
        branch("Pedidos con problemas", "Necesitan intervención", AdminOperationListKind.AllProblems, AdminOperationMetricTone.Danger),
        branch("En espera de aceptación", "Esperan respuesta inicial", AdminOperationListKind.ActiveWaitingStore, AdminOperationMetricTone.Warning),
        branch("Aceptados", "Aceptados y esperando próximo paso", AdminOperationListKind.ActiveReviewState, AdminOperationMetricTone.Healthy),
        branch("En preparación", "El pedido se está preparando", AdminOperationListKind.AllPreparing, AdminOperationMetricTone.Healthy),
        branch("En camino", "Reparto asignado o entrega en curso", AdminOperationListKind.AllInDelivery, AdminOperationMetricTone.Healthy),
        branch("Entregados / cerrados con problemas", "Cierres y reclamos posteriores", AdminOperationListKind.AllClosed, AdminOperationMetricTone.Neutral),
    )
}

private fun adminBranchGroups(
    list: AdminOperationList,
    orders: List<AdminOrderSummary>,
): List<AdminBranchGroup> {
    fun group(
        title: String,
        waitingFor: String,
        actionBy: String,
        resolution: String,
        kind: AdminOperationListKind,
    ): AdminBranchGroup =
        AdminBranchGroup(
            title = title,
            waitingFor = waitingFor,
            actionBy = actionBy,
            resolution = resolution,
            kind = kind,
            rows = orders.forOperationList(kind).map { it.adminOperationsRow() },
        )

    val groups = when (list.kind) {
        AdminOperationListKind.AllProblems -> listOf(
            group("Local no responde", "El local no confirmó dentro del tiempo esperado.", "Admin / local", "Contactar local o cancelar con aviso.", AdminOperationListKind.ProblemStoreNotResponding),
            group("Sin repartidor", "El pedido necesita responsable de entrega.", "Admin / reparto", "Buscar repartidor, asignar manualmente o subir prioridad.", AdminOperationListKind.ProblemWithoutResponsible),
            group("Pago con conflicto", "El pago necesita revisión antes de seguir.", "Admin / persona usuaria", "Revisar pago o contactar persona usuaria.", AdminOperationListKind.ProblemPaymentConflict),
            group("Repartidor con inconveniente", "El reparto informó un problema.", "Admin / repartidor", "Contactar repartidor, reasignar o avisar a la persona usuaria.", AdminOperationListKind.ProblemDriverIssue),
            group("Persona usuaria no responde", "Hace falta respuesta de la persona que pidió.", "Admin / persona usuaria", "Contactar persona usuaria y registrar resultado.", AdminOperationListKind.ProblemUserNotResponding),
            group("Demora crítica", "El pedido superó el tiempo normal.", "Admin / responsable actual", "Resolver demora o abrir incidencia.", AdminOperationListKind.ProblemDelayed),
            group("Incidencia abierta", "Hay un caso operativo pendiente.", "Admin", "Resolver incidencia o dejar seguimiento.", AdminOperationListKind.ProblemOperationalReview),
        )
        AdminOperationListKind.AllClosed -> listOf(
            group("Entregados correctamente", "Pedidos terminados sin señal de problema.", "Consulta", "Abrir para revisar ticket e historial.", AdminOperationListKind.ClosedDelivered),
            group("Cancelados", "Pedidos cerrados por cancelación.", "Consulta", "Abrir para revisar motivo e historial.", AdminOperationListKind.ClosedCancelledProblem),
            group("Cerrados con incidencia", "Pedidos cerrados con problema operativo.", "Admin", "Revisar lectura posterior.", AdminOperationListKind.ClosedWithIncident),
            group("Reclamos posteriores", "La persona usuaria avisó algo después del cierre.", "Admin", "Revisar reclamo y seguimiento.", AdminOperationListKind.ClosedPostClaim),
        )
        AdminOperationListKind.AllPreparing,
        AdminOperationListKind.ActivePreparing -> listOf(
            group("Preparando normal", "El local está preparando.", "Local", "Seguir avance normal.", AdminOperationListKind.PreparingNormal),
            group("Listos para retirar", "El pedido está listo para salir.", "Local / reparto", "Confirmar retiro o pedir repartidor.", AdminOperationListKind.PreparingReadyForPickup),
            group("Preparación demorada", "La preparación se extendió.", "Admin / local", "Revisar demora antes de que escale.", AdminOperationListKind.PreparingDelayed),
        )
        AdminOperationListKind.AllInDelivery,
        AdminOperationListKind.ActiveInDelivery -> listOf(
            group("Repartidor asignado", "Ya hay responsable de reparto.", "Repartidor", "Esperar retiro o confirmar avance.", AdminOperationListKind.DeliveryDriverAssigned),
            group("Retirado", "El pedido salió del local.", "Repartidor", "Seguir entrega.", AdminOperationListKind.DeliveryPickedUp),
            group("En entrega", "El pedido está camino al destino.", "Repartidor", "Seguir entrega.", AdminOperationListKind.ActiveInDelivery),
            group("Entrega demorada", "La entrega superó el tiempo esperado.", "Admin / repartidor", "Revisar demora o contactar repartidor.", AdminOperationListKind.DeliveryDelayed),
        )
        AdminOperationListKind.ActiveWaitingStore -> listOf(
            group("Esperando local", "El local todavía debe responder.", "Local", "Aceptar, rechazar o pedir intervención si no responde.", AdminOperationListKind.ActiveWaitingStore),
            group("Esperando confirmación operativa", "Admin debe mirar antes de avanzar.", "Admin", "Revisar el pedido y dejar próximo paso.", AdminOperationListKind.ActiveWaitingOperationalConfirmation),
        )
        AdminOperationListKind.ActiveWaitingDriver -> listOf(
            group("Falta repartidor", "El pedido necesita responsable de entrega.", "Admin / repartidor", "Asignar, esperar toma o revisar disponibilidad.", AdminOperationListKind.ActiveWaitingDriver),
        )
        AdminOperationListKind.ActiveReviewState -> listOf(
            group("Aceptados por local", "El local aceptó el pedido.", "Local", "Esperar inicio de preparación.", AdminOperationListKind.AcceptedByStore),
            group("Esperando inicio de preparación", "Aceptado y sin preparación activa.", "Local", "Confirmar que empezó a preparar.", AdminOperationListKind.AcceptedWaitingPreparation),
            group("Listos para pasar a preparación", "El siguiente paso es preparación.", "Local", "Marcar preparación cuando corresponda.", AdminOperationListKind.AcceptedReadyToPrepare),
        )
        AdminOperationListKind.Unclassified,
        AdminOperationListKind.TodayReview -> listOf(
            group("Revisión operativa", "El estado necesita lectura humana.", "Admin", "Revisar el pedido y decidir el próximo paso.", list.kind),
        )
        else -> listOf(
            group(list.title, list.summary, "Según pedido", "Abrir cada pedido para decidir el próximo paso.", list.kind),
        )
    }
    return groups
}

private fun AdminOrderSummary.adminOperationsRow(): AdminDeskOrderRow {
    val signals = AdminOperationOrderSignals.from(this)
    val placement = AdminOperationOrderClassification.primaryPlacement(signals)
    val hasAction = nextAllowedActions.isNotEmpty()
    val problemBucket = AdminOperationOrderClassification.problemBucket(signals)
    val activeBucket = AdminOperationOrderClassification.activeBucket(signals)
    val problemKind = adminProblemListKindFor(this, null, signals)
    val status = when {
        placement == AdminOrderPrimaryPlacement.FINISHED -> "Finalizado"
        placement == AdminOrderPrimaryPlacement.CANCELLED -> "Cancelado"
        placement == AdminOrderPrimaryPlacement.UNCLASSIFIED -> "Revisar pedido"
        problemKind == AdminOperationListKind.ProblemPaymentConflict -> "Pago con conflicto"
        problemKind == AdminOperationListKind.ProblemDriverIssue -> "Repartidor con inconveniente"
        problemKind == AdminOperationListKind.ProblemUserNotResponding -> "Persona usuaria no responde"
        problemBucket == AdminProblemOrdersBucket.STORE_NOT_RESPONDING -> "Local no responde"
        problemBucket == AdminProblemOrdersBucket.CUSTOMER_CLAIM -> "Reclamo de persona usuaria"
        problemBucket == AdminProblemOrdersBucket.DELAYED -> "Demora crítica"
        problemBucket == AdminProblemOrdersBucket.WITHOUT_RESPONSIBLE -> "Sin repartidor"
        activeIncident -> "Incidencia abierta"
        activeBucket == AdminActiveOrdersBucket.WAITING_STORE -> "Esperando local"
        activeBucket == AdminActiveOrdersBucket.PREPARING -> "Preparando"
        activeBucket == AdminActiveOrdersBucket.WAITING_DRIVER -> "Sin repartidor"
        activeBucket == AdminActiveOrdersBucket.IN_DELIVERY -> "En camino"
        activeBucket == AdminActiveOrdersBucket.REVIEW_STATE -> "Revisar pedido"
        else -> adminHumanOperationStatus(
            publicStatus = publicStatus,
            operationalStatus = operationalStatus,
            rawStatus = this.status,
            hasProblem = needsAttention || activeIncident,
        )
    }
    val reason = when {
        placement == AdminOrderPrimaryPlacement.FINISHED -> "Pedido terminado"
        placement == AdminOrderPrimaryPlacement.CANCELLED -> "Pedido cancelado"
        placement == AdminOrderPrimaryPlacement.UNCLASSIFIED -> "Necesita lectura operativa"
        problemKind == AdminOperationListKind.ProblemPaymentConflict -> "Hay que revisar el pago"
        problemKind == AdminOperationListKind.ProblemDriverIssue -> "El reparto informó un inconveniente"
        problemKind == AdminOperationListKind.ProblemUserNotResponding -> "Hace falta respuesta de la persona usuaria"
        problemBucket == AdminProblemOrdersBucket.STORE_NOT_RESPONDING -> "El local no confirmó"
        problemBucket == AdminProblemOrdersBucket.CUSTOMER_CLAIM -> "Persona usuaria avisó un problema"
        problemBucket == AdminProblemOrdersBucket.DELAYED -> "Superó el tiempo esperado"
        problemBucket == AdminProblemOrdersBucket.WITHOUT_RESPONSIBLE -> "No hay repartidor asignado"
        activeIncident -> "Tiene incidencia activa"
        activeBucket == AdminActiveOrdersBucket.WAITING_STORE -> "Esperando respuesta del local"
        activeBucket == AdminActiveOrdersBucket.PREPARING -> "El local está preparando"
        activeBucket == AdminActiveOrdersBucket.WAITING_DRIVER -> "Listo para asignar o retirar"
        activeBucket == AdminActiveOrdersBucket.IN_DELIVERY -> "Va hacia destino"
        hasAction -> "Listo para avanzar"
        else -> "En seguimiento"
    }
    val nextStep = when {
        hasAction -> nextAllowedActions.first().adminActionLabel()
        placement in setOf(AdminOrderPrimaryPlacement.FINISHED, AdminOrderPrimaryPlacement.CANCELLED) -> "Solo lectura"
        placement == AdminOrderPrimaryPlacement.PROBLEM -> "Abrir y resolver bloqueo"
        placement == AdminOrderPrimaryPlacement.UNCLASSIFIED -> "Revisar pedido"
        else -> "Abrir seguimiento"
    }
    return AdminDeskOrderRow(
        order = this,
        label = if (trackingNumber.isNotBlank()) "Pedido #$trackingNumber" else "Pedido ${id.take(8)}",
        status = status,
        reason = reason,
        nextStep = nextStep,
        tone = when {
            problemKind != null -> AdminOperationMetricTone.Danger
            placement == AdminOrderPrimaryPlacement.PROBLEM -> AdminOperationMetricTone.Danger
            placement == AdminOrderPrimaryPlacement.UNCLASSIFIED -> AdminOperationMetricTone.Warning
            hasAction -> AdminOperationMetricTone.Healthy
            else -> AdminOperationMetricTone.Neutral
        },
        variant = when {
            placement == AdminOrderPrimaryPlacement.PROBLEM -> OperationOrderVariant.WithProblem
            placement == AdminOrderPrimaryPlacement.UNCLASSIFIED -> OperationOrderVariant.ActionUnavailable
            activeBucket == AdminActiveOrdersBucket.WAITING_STORE -> OperationOrderVariant.NeedsAttention
            placement in setOf(AdminOrderPrimaryPlacement.FINISHED, AdminOrderPrimaryPlacement.CANCELLED) -> OperationOrderVariant.ActionUnavailable
            else -> OperationOrderVariant.Normal
        },
    )
}

private fun String.adminHealthLabel(): String = when (this) {
    "ok" -> "OK"
    "warning" -> "Advertencia"
    "critical" -> "Crítico"
    "disabled" -> "Deshabilitado"
    "prepared" -> "Preparado"
    "not_implemented" -> "No disponible"
    "not_ready" -> "No listo"
    "pending_o" -> "Pendiente O"
    else -> ifBlank { "Desconocido" }
}

@Composable
private fun AdminRealConfigurationScreen(
    message: String,
    error: String,
    onOpenShippingFee: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            AdminConfigurationPresentationCard()
        }
        if (message.isNotBlank()) item(span = { GridItemSpan(maxLineSpan) }) { AdminInfoPanel(title = "Guardado", text = message) }
        if (error.isNotBlank()) item(span = { GridItemSpan(maxLineSpan) }) { AdminInfoPanel(title = "Revisar configuración", text = error) }
        item {
            AdminConfigurationSquareCard(
                title = "Tarifa\nenvío",
                toneColor = PediloOrange,
                onClick = onOpenShippingFee,
            )
        }
    }
}

@Composable
private fun AdminRealRoleAccessScreen(
    users: List<AdminTeamUser>,
    message: String,
    error: String,
    onToggleActive: (AdminTeamUser) -> Unit,
    onRole: (AdminTeamUser, String) -> Unit,
    onSignOut: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = "Equipo",
                eyebrow = "Admin",
                summary = "Usuarios, roles y accesos",
                onSignOut = onSignOut,
                showSignOut = true,
            )
        }
        if (message.isNotBlank()) item { AdminInfoPanel(title = "Guardado", text = message) }
        if (error.isNotBlank()) item { AdminInfoPanel(title = "Error", text = error) }
        if (users.isEmpty()) {
            item { AdminInfoPanel(title = "Sin cuentas visibles", text = "Sin usuarios para revisar") }
        }
        items(users, key = { it.uid }) { user ->
            AdminTeamUserCard(user = user, onToggleActive = { onToggleActive(user) }, onRole = { role -> onRole(user, role) })
        }
    }
}

@Composable
private fun AdminConfigurationPresentationCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pediloCardDepth(RoundedCornerShape(8.dp))
            .background(PediloPanel, RoundedCornerShape(8.dp))
            .border(1.dp, PediloOrange.copy(alpha = 0.34f), RoundedCornerShape(8.dp))
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Outlined.Settings, contentDescription = null, tint = PediloOrange, modifier = Modifier.size(54.dp))
        Text("Configuración", color = PediloText, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text("Admin", color = PediloMuted, fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AdminConfigurationSquareCard(
    title: String,
    toneColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(if (pressed) 0.986f else 1f)
            .pediloCardDepth(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(
                    listOf(toneColor.copy(alpha = if (pressed) 0.24f else 0.16f), PediloPanelSoft, PediloPanel),
                ),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, toneColor.copy(alpha = if (pressed) 0.72f else 0.44f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = toneColor, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = PediloText, fontSize = 21.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun AdminShippingFeeScreen(
    onRainMode: () -> Unit,
    onBaseDeliveryFee: () -> Unit,
    onDistanceSurcharge: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Tarifa envío", color = PediloText, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
        }
        item { AdminShippingFeeButton("Modo lluvia", onRainMode) }
        item { AdminShippingFeeButton("Costo de envío", onBaseDeliveryFee) }
        item { AdminShippingFeeButton("Adicional por distancia", onDistanceSurcharge) }
    }
}

@Composable
private fun AdminShippingFeeButton(title: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.99f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(PediloOrange.copy(alpha = if (pressed) 0.20f else 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, PediloOrange.copy(alpha = if (pressed) 0.72f else 0.42f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = PediloText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = PediloOrange, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun AdminRainModeScreen(
    config: AdminConfigState,
    message: String,
    error: String,
    onToggle: () -> Unit,
    onAmount: (Long) -> Unit,
) {
    var amountText by remember(config.rainDeliveryFee) { mutableStateOf(config.rainDeliveryFee.toString()) }
    var inputError by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Modo lluvia", color = PediloText, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold) }
        item {
            AdminValuePanel(
                title = if (config.rainMode) "Modo lluvia activo" else "Modo lluvia desactivado",
                value = adminMoney(config.rainDeliveryFee),
                toneColor = if (config.rainMode) PediloGreen else PediloMuted,
            )
        }
        item {
            AdminInlineActionButton(
                title = if (config.rainMode) "Desactivar Modo lluvia" else "Activar Modo lluvia",
                subtitle = "Guardar",
                toneColor = if (config.rainMode) PediloWarning else PediloGreen,
                onClick = onToggle,
            )
        }
        item {
            AdminMoneyEditor(
                title = "Asignar tarifa de lluvia",
                currentValue = config.rainDeliveryFee,
                inputLabel = "Nueva tarifa",
                amountText = amountText,
                onAmountText = { amountText = it.filter(Char::isDigit) },
                onSubmit = {
                    val amount = amountText.toLongOrNull()
                    if (amount == null) {
                        inputError = "Cargá un valor numérico."
                    } else {
                        inputError = ""
                        onAmount(amount)
                    }
                },
            )
        }
        if (message.isNotBlank()) item { AdminInfoPanel(title = "Guardado", text = message) }
        if (error.isNotBlank()) item { AdminInfoPanel(title = "Revisar configuración", text = error) }
        if (inputError.isNotBlank()) item { AdminInfoPanel(title = "Revisar valor", text = inputError) }
    }
}

@Composable
private fun AdminMoneyConfigScreen(
    title: String,
    currentValue: Long,
    inputLabel: String,
    onAmount: (Long) -> Unit,
    message: String,
    error: String,
) {
    var amountText by remember(currentValue) { mutableStateOf(currentValue.toString()) }
    var inputError by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(title, color = PediloText, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold) }
        item {
            AdminMoneyEditor(
                title = title,
                currentValue = currentValue,
                inputLabel = inputLabel,
                amountText = amountText,
                onAmountText = { amountText = it.filter(Char::isDigit) },
                onSubmit = {
                    val amount = amountText.toLongOrNull()
                    if (amount == null) {
                        inputError = "Cargá un valor numérico."
                    } else {
                        inputError = ""
                        onAmount(amount)
                    }
                },
            )
        }
        if (message.isNotBlank()) item { AdminInfoPanel(title = "Guardado", text = message) }
        if (error.isNotBlank()) item { AdminInfoPanel(title = "Revisar configuración", text = error) }
        if (inputError.isNotBlank()) item { AdminInfoPanel(title = "Revisar valor", text = inputError) }
    }
}

@Composable
private fun AdminMoneyEditor(
    title: String,
    currentValue: Long,
    inputLabel: String,
    amountText: String,
    onAmountText: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel, RoundedCornerShape(8.dp))
            .border(1.dp, PediloOrange.copy(alpha = 0.36f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = PediloText, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text("Valor actual", color = PediloMuted, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
        Text(adminMoney(currentValue), color = PediloOrange, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
        PediloTextField(value = amountText, onValueChange = onAmountText, label = inputLabel, singleLine = true)
        AdminInlineActionButton(
            title = "Cargar cambio ahora",
            subtitle = "Guardar",
            toneColor = PediloOrange,
            onClick = onSubmit,
        )
    }
}

@Composable
private fun AdminValuePanel(title: String, value: String, toneColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(toneColor.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.38f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = PediloText, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text(value, color = toneColor, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun adminMoney(value: Long): String = "\$$value"

private fun AdminConfigState.withAppliedConfigUpdate(request: AdminConfigUpdateRequest): AdminConfigState =
    when (request.field) {
        "rainMode" -> request.enabled?.let { copy(rainMode = it) } ?: this
        "rainDeliveryFee" -> request.amount?.let { copy(rainDeliveryFee = it) } ?: this
        "baseDeliveryFee" -> request.amount?.let { copy(baseDeliveryFee = it) } ?: this
        "distanceSurcharge" -> request.amount?.let { copy(distanceSurcharge = it) } ?: this
        else -> this
    }

@Composable
private fun AdminTeamUserCard(
    user: AdminTeamUser,
    onToggleActive: () -> Unit,
    onRole: (String) -> Unit,
) {
    val label = user.displayName.ifBlank { user.email.ifBlank { user.uid } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PediloPanel, RoundedCornerShape(18.dp))
            .border(1.dp, if (user.active) PediloGreen.copy(alpha = 0.45f) else PediloWarning.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, color = PediloText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AdminStatusChip(label = user.role.adminRoleLabel(), toneColor = PediloCyan)
            AdminStatusChip(label = if (user.active) "activo" else "inactivo", toneColor = if (user.active) PediloGreen else PediloWarning)
        }
        if (user.storeId.isNotBlank() || user.driverId.isNotBlank()) {
            Text("Vínculo: ${listOf(user.storeId, user.driverId).filter { it.isNotBlank() }.joinToString()}", color = PediloMuted, fontSize = 12.sp)
        }
        AdminInlineActionButton(
            title = if (user.active) "Desactivar acceso" else "Activar acceso",
            subtitle = "Guardar cambio",
            toneColor = if (user.active) PediloWarning else PediloGreen,
            onClick = onToggleActive,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("admin", "store", "driver").forEach { role ->
                AdminInlineActionButton(
                    title = "Cambiar a ${role.adminRoleLabel()}",
                    subtitle = user.role.adminRoleLabel(),
                    toneColor = PediloOrange,
                    onClick = { onRole(role) },
                )
            }
        }
    }
}

@Composable
private fun AdminInlineActionButton(
    title: String,
    subtitle: String,
    toneColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.99f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(toneColor.copy(alpha = if (pressed) 0.18f else 0.10f), RoundedCornerShape(12.dp))
            .border(1.dp, toneColor.copy(alpha = if (pressed) 0.70f else 0.36f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = PediloText, fontSize = 15.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = toneColor, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
    }
}

private fun String.adminRoleLabel(): String = when (trim().lowercase()) {
    "admin" -> "Admin"
    "store" -> "Local"
    "driver" -> "Repartidor"
    "system", "back" + "end" -> "Sistema"
    "public", "public_app", "cust" + "omer", "cli" + "ent" -> "Persona usuaria"
    else -> adminDisplayValue("Sin rol")
}

@Composable
private fun AdminRoleAccessSectionScreen(
    section: AdminRoleAccessSection,
    onEntry: (AdminEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = section.title,
                eyebrow = "Equipo",
                summary = section.summary,
                onSignOut = {},
                showSignOut = false,
            )
        }
        item {
            AdminInfoPanel(title = section.contextTitle, text = section.contextText)
        }
        items(section.entries) {
            AdminEntryCard(entry = it, onClick = { onEntry(it) })
        }
    }
}

@Composable
private fun AdminSectionScreen(
    root: AdminRoot,
    title: String,
    summary: String,
    panelTitle: String,
    panelText: String,
    onRoleAccessConvergence: (AdminRoleAccessConvergenceStep) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = title,
                eyebrow = root.label,
                summary = summary,
                onSignOut = {},
                showSignOut = false,
            )
        }
        item {
            AdminInfoPanel(
                title = panelTitle,
                text = panelText,
            )
        }
        if (root == AdminRoot.RoleAccess) {
            val isAccessAudit = panelTitle == "Actividad de acceso"
            val initialStep = when (title) {
                "Altas pendientes" -> AdminRoleAccessConvergenceStep.CreateAccount
                "Usuarios inactivos" -> AdminRoleAccessConvergenceStep.ToggleAccess
                "Vinculaciones pendientes" -> AdminRoleAccessConvergenceStep.LinkEntity
                else -> if (isAccessAudit) {
                    AdminRoleAccessConvergenceStep.Audit
                } else {
                    AdminRoleAccessConvergenceStep.Account
                }
            }
            item {
                AdminEntryCard(
                    entry = if (isAccessAudit) {
                        AdminEntry("Detalle de registro", "Consulta de accesos sin edición")
                    } else {
                        AdminEntry("Cuenta concreta", "Abrir detalle de cuenta y acciones disponibles")
                    },
                    onClick = { onRoleAccessConvergence(initialStep) },
                )
            }
        }
    }
}

@Composable
private fun AdminRoleAccessConvergenceScreen(
    section: String,
    subsection: String,
    step: AdminRoleAccessConvergenceStep,
    onNext: (AdminRoleAccessConvergenceStep) -> Unit,
) {
    val title = when (step) {
        AdminRoleAccessConvergenceStep.Account -> "Cuenta concreta"
        AdminRoleAccessConvergenceStep.CreateAccount -> "Alta de cuenta"
        AdminRoleAccessConvergenceStep.AccessEditor -> "Editor de acceso"
        AdminRoleAccessConvergenceStep.ChangeRole -> "Cambio de rol"
        AdminRoleAccessConvergenceStep.ToggleAccess -> "Activar o desactivar"
        AdminRoleAccessConvergenceStep.LinkEntity -> "Vincular entidad"
        AdminRoleAccessConvergenceStep.Impact -> "Impacto"
        AdminRoleAccessConvergenceStep.SensitiveConfirmation -> "Confirmación sensible"
        AdminRoleAccessConvergenceStep.Result -> "Resultado"
        AdminRoleAccessConvergenceStep.Audit -> "Actividad de acceso"
    }
    val summary = when (step) {
        AdminRoleAccessConvergenceStep.Account -> "Detalle de cuenta y acciones de acceso."
        AdminRoleAccessConvergenceStep.CreateAccount -> "Alta pendiente"
        AdminRoleAccessConvergenceStep.AccessEditor -> "Datos de acceso"
        AdminRoleAccessConvergenceStep.ChangeRole -> "Revisión de cambio entre Admin, Local y Repartidor."
        AdminRoleAccessConvergenceStep.ToggleAccess -> "Revisión de habilitación o detención de ingreso."
        AdminRoleAccessConvergenceStep.LinkEntity -> "Revisión de vínculo operativo de la cuenta."
        AdminRoleAccessConvergenceStep.Impact -> "Evaluación de alcance antes de confirmar."
        AdminRoleAccessConvergenceStep.SensitiveConfirmation -> "Validación final previa al resultado."
        AdminRoleAccessConvergenceStep.Result -> "Resultado"
        AdminRoleAccessConvergenceStep.Audit -> "Consulta de cuenta, rol, vínculo y resultado."
    }
    val context = when (step) {
        AdminRoleAccessConvergenceStep.Account -> "Cuenta: persona@equipo.com · Nombre visible: Referencia de cuenta · Rol: Local · Estado: En revisión · Puede ingresar: Pendiente."
        AdminRoleAccessConvergenceStep.CreateAccount -> "Email, nombre visible, rol previsto y vínculo requerido."
        AdminRoleAccessConvergenceStep.AccessEditor -> "Nombre visible, observación administrativa y estado de revisión."
        AdminRoleAccessConvergenceStep.ChangeRole -> "Rol actual y rol nuevo: Admin, Local o Repartidor."
        AdminRoleAccessConvergenceStep.ToggleAccess -> "Estado actual y nuevo estado de ingreso."
        AdminRoleAccessConvergenceStep.LinkEntity -> "Cuenta y vínculo operativo pendiente.\nLa entidad se gestiona en su área responsable."
        AdminRoleAccessConvergenceStep.Impact -> "Acceso, rol y vínculo."
        AdminRoleAccessConvergenceStep.SensitiveConfirmation -> "Acción sensible en revisión final para cuenta del equipo.\nConfirmá sólo cuando los datos sean correctos."
        AdminRoleAccessConvergenceStep.Result -> "Cambio registrado. Volver a Equipo."
        AdminRoleAccessConvergenceStep.Audit -> "Qué cambió, quién lo revisó, cuándo, valor anterior, valor nuevo, rol afectado, cuenta afectada, entidad vinculada, motivo y resultado."
    }
    val next = when (step) {
        AdminRoleAccessConvergenceStep.Account -> AdminRoleAccessConvergenceStep.AccessEditor
        AdminRoleAccessConvergenceStep.CreateAccount -> AdminRoleAccessConvergenceStep.Impact
        AdminRoleAccessConvergenceStep.AccessEditor -> AdminRoleAccessConvergenceStep.ChangeRole
        AdminRoleAccessConvergenceStep.ChangeRole -> AdminRoleAccessConvergenceStep.Impact
        AdminRoleAccessConvergenceStep.ToggleAccess -> AdminRoleAccessConvergenceStep.Impact
        AdminRoleAccessConvergenceStep.LinkEntity -> AdminRoleAccessConvergenceStep.Impact
        AdminRoleAccessConvergenceStep.Impact -> AdminRoleAccessConvergenceStep.SensitiveConfirmation
        AdminRoleAccessConvergenceStep.SensitiveConfirmation -> AdminRoleAccessConvergenceStep.Result
        AdminRoleAccessConvergenceStep.Result -> AdminRoleAccessConvergenceStep.Audit
        AdminRoleAccessConvergenceStep.Audit -> AdminRoleAccessConvergenceStep.Audit
    }
    val actionLabel = when (step) {
        AdminRoleAccessConvergenceStep.Account -> "Abrir editor de acceso"
        AdminRoleAccessConvergenceStep.CreateAccount -> "Ver condiciones"
        AdminRoleAccessConvergenceStep.AccessEditor -> "Revisar cambio de rol"
        AdminRoleAccessConvergenceStep.ChangeRole -> "Evaluar impacto"
        AdminRoleAccessConvergenceStep.ToggleAccess -> "Evaluar impacto"
        AdminRoleAccessConvergenceStep.LinkEntity -> "Evaluar impacto"
        AdminRoleAccessConvergenceStep.Impact -> "Confirmar revisión"
        AdminRoleAccessConvergenceStep.SensitiveConfirmation -> "Entendido"
        AdminRoleAccessConvergenceStep.Result -> "Consultar historial de accesos"
        AdminRoleAccessConvergenceStep.Audit -> "Revisar registro"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AdminHeader(title = title, eyebrow = "Equipo", summary = summary, onSignOut = {}, showSignOut = false) }
        item { AdminInfoPanel(title = "Contexto", text = "Sección: $section · Subsección: $subsection\n$context") }
        item {
            AdminInfoPanel(
                title = "Roles permitidos",
                text = "Admin · Local · Repartidor",
            )
        }
        item {
            AdminActionCard(
                title = actionLabel,
                note = "Continuar.",
                onClick = { onNext(next) },
            )
        }
    }
}

@Composable
private fun AdminOrderDetailScreen(
    variant: OperationOrderVariant,
    orderId: String?,
    summary: AdminOrderSummary?,
    detail: AdminOrderDetail?,
    operationMessage: String,
    operationError: String,
    navigationOrigin: String,
    onLoadDetail: (String) -> Unit,
    onLiveAction: (LiveOrderAction, Int) -> Unit,
    onRepairActions: () -> Unit,
    onSection: (AdminOrderSection) -> Unit,
    onBackToDesk: () -> Unit,
    onBackToHome: () -> Unit,
) {
    orderId?.let { LaunchedEffect(it) { onLoadDetail(it) } }
    val visibleNumber = adminOrderVisibleNumber(summary, detail, orderId)
    val publicStatus = detail?.publicStatus ?: summary?.publicStatus.orEmpty()
    val operationalStatus = detail?.operationalStatus ?: summary?.operationalStatus.orEmpty()
    val needsAttention = detail?.needsAttention ?: summary?.needsAttention ?: false
    val activeIncident = detail?.activeIncident ?: summary?.activeIncident ?: false
    val storeName = detail?.storeName ?: summary?.storeName.orEmpty()
    val signals = AdminOperationOrderSignals(
        status = detail?.status ?: summary?.status.orEmpty(),
        publicStatus = publicStatus,
        operationalStatus = operationalStatus,
        responsibleRole = detail?.responsibleRole ?: summary?.responsibleRole.orEmpty(),
        needsAttention = needsAttention,
        activeIncident = activeIncident,
        source = detail?.source ?: summary?.source.orEmpty(),
        requestType = detail?.requestType ?: summary?.requestType.orEmpty(),
    )
    val placement = AdminOperationOrderClassification.primaryPlacement(signals)
    val activeBucket = AdminOperationOrderClassification.activeBucket(signals)
    val problemFocus = adminOrderProblemFocus(
        variant = variant,
        publicStatus = publicStatus,
        operationalStatus = operationalStatus,
        needsAttention = needsAttention,
        activeIncident = activeIncident,
    )
    val statusText = when {
        placement == AdminOrderPrimaryPlacement.UNCLASSIFIED -> "Revisar pedido"
        placement == AdminOrderPrimaryPlacement.ACTIVE &&
            AdminOperationOrderClassification.activeBucket(signals) == AdminActiveOrdersBucket.REVIEW_STATE -> "Revisar estado"
        else -> adminHumanOperationStatus(
            publicStatus = publicStatus,
            operationalStatus = operationalStatus,
            rawStatus = detail?.status ?: summary?.status.orEmpty(),
            hasProblem = problemFocus != null,
        )
    }
    val allowedActions = detail?.nextAllowedActions ?: summary?.nextAllowedActions.orEmpty()
    val expectedVersion = detail?.version ?: summary?.version ?: 0
    val problemKind = adminProblemListKindFor(summary, detail, signals)
    val toneColor = adminListToneColor(problemKind ?: adminKindForPlacement(placement, activeBucket), 1)
    val visibleActions = adminVisibleGuidedActions(
        allowedActions = allowedActions,
        problemKind = problemKind,
        placement = placement,
        activeBucket = activeBucket,
    )
    val ticketFacts = adminHumanTicketFacts(visibleNumber, storeName, statusText, summary, detail)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        AdminHumanDetailHeader(
            number = visibleNumber,
            status = statusText,
            origin = navigationOrigin,
            problem = problemFocus?.first,
            toneColor = toneColor,
            onBack = onBackToDesk,
        )
        if (operationMessage.isNotBlank()) {
            AdminOrderResultStrip(
                message = operationMessage.adminHumanText().adminDisplayValue("Pedido actualizado"),
                onBackToBranch = onBackToDesk,
                onBackToHome = onBackToHome,
                onHistory = { onSection(AdminOrderSection.History) },
            )
            AdminOrderCompactTimeline(events = detail?.events.orEmpty())
            Spacer(Modifier.height(adminContentBottomPadding))
            return@Column
        }
        if (operationError.isNotBlank()) {
            AdminInfoPanel(title = "No se pudo avanzar", text = operationError)
        }
        AdminHumanSituationCard(
            status = statusText,
            problem = problemFocus,
            problemKind = problemKind,
            operator = adminProblemOperatorLabel(problemKind, activeBucket, placement),
            afterResolve = adminOperationOutcomeText(problemKind ?: adminKindForPlacement(placement, activeBucket)),
            toneColor = toneColor,
            detailLoaded = detail != null,
        )
        if (visibleActions.isNotEmpty()) {
            AdminGuidedActionsPanel(
                actions = visibleActions,
                problemKind = problemKind,
                status = statusText,
                expectedVersion = expectedVersion,
                toneColor = toneColor,
                onLiveAction = onLiveAction,
            )
        } else {
            AdminOrderMomentPanel(
                title = "Sin acción guiada ahora",
                detail = adminNoGuidedActionText(problemKind, statusText),
                highlighted = false,
                eyebrow = "Lectura operativa",
            )
        }
        AdminOrderDataSheet(title = "Ticket del pedido", facts = ticketFacts)
        AdminOrderCompactTimeline(events = detail?.events.orEmpty())
        AdminSecondaryActionRow(title = "Volver", note = navigationOrigin, onClick = onBackToDesk)
        Spacer(Modifier.height(adminContentBottomPadding))
    }
}

@Composable
private fun AdminHumanDetailHeader(
    number: String,
    status: String,
    origin: String,
    problem: String?,
    toneColor: Color,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel, RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.42f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Volver",
                tint = PediloText,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(role = Role.Button, onClick = onBack),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(number, color = PediloText, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(origin, color = PediloMuted, fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Bold)
            }
            AdminStatusChip(problem ?: status, toneColor)
        }
    }
}

@Composable
private fun AdminHumanSituationCard(
    status: String,
    problem: Pair<String, String>?,
    problemKind: AdminOperationListKind?,
    operator: String,
    afterResolve: String,
    toneColor: Color,
    detailLoaded: Boolean,
) {
    val title = problem?.first ?: status
    val detail = when {
        !detailLoaded -> "Cargando datos del pedido para operar con contexto."
        problem != null -> problem.second
        else -> "El pedido no muestra un problema activo en esta ficha."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(toneColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.52f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(if (problemKind != null) "Problema actual" else "Estado actual", color = toneColor, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text(title, color = PediloText, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
        Text(detail, color = PediloText, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AdminMiniOperationFact("Actúa", operator, toneColor, Modifier.weight(1f))
            AdminMiniOperationFact("Al resolver", afterResolve, toneColor, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdminGuidedActionsPanel(
    actions: List<LiveOrderAction>,
    problemKind: AdminOperationListKind?,
    status: String,
    expectedVersion: Int,
    toneColor: Color,
    onLiveAction: (LiveOrderAction, Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanelSoft, RoundedCornerShape(8.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Resolver ahora", color = PediloText, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold)
        if (actions.isEmpty()) {
            Text(
                adminNoGuidedActionText(problemKind, status),
                color = PediloMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            actions.forEach { action ->
                AdminInlineActionButton(
                    title = action.adminGuidedActionLabel(problemKind),
                    subtitle = action.adminGuidedActionSubtitle(problemKind),
                    toneColor = toneColor,
                    onClick = { onLiveAction(action, expectedVersion) },
                )
            }
        }
    }
}

@Composable
private fun AdminGuidedActionScreen(
    detailRoute: AdminRoute.OperationOrderDetail,
    action: LiveOrderAction,
    expectedVersion: Int,
    summary: AdminOrderSummary?,
    detail: AdminOrderDetail?,
    operationMessage: String,
    operationError: String,
    onConfirm: (String) -> Unit,
    onBackToDetail: () -> Unit,
    onBackToQueue: () -> Unit,
    onBackToHome: () -> Unit,
) {
    val visibleNumber = adminOrderVisibleNumber(summary, detail, detailRoute.realOrderId)
    val signals = AdminOperationOrderSignals(
        status = detail?.status ?: summary?.status.orEmpty(),
        publicStatus = detail?.publicStatus ?: summary?.publicStatus.orEmpty(),
        operationalStatus = detail?.operationalStatus ?: summary?.operationalStatus.orEmpty(),
        responsibleRole = detail?.responsibleRole ?: summary?.responsibleRole.orEmpty(),
        needsAttention = detail?.needsAttention ?: summary?.needsAttention ?: false,
        activeIncident = detail?.activeIncident ?: summary?.activeIncident ?: false,
        source = detail?.source ?: summary?.source.orEmpty(),
        requestType = detail?.requestType ?: summary?.requestType.orEmpty(),
    )
    val problemKind = adminProblemListKindFor(summary, detail, signals)
    val toneColor = adminListToneColor(problemKind ?: AdminOperationListKind.ActiveReviewState, 1)
    val choices = action.adminGuidedResultChoices(problemKind)
    var selectedReason by remember(action, visibleNumber) { mutableStateOf(choices.firstOrNull()?.reason.orEmpty()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = action.adminGuidedActionLabel(problemKind),
                eyebrow = "Resolver pedido",
                summary = visibleNumber,
                onSignOut = {},
                showSignOut = false,
            )
        }
        if (operationMessage.isNotBlank()) {
            item {
                AdminOrderResultStrip(
                    message = operationMessage.adminHumanText().adminDisplayValue("Pedido actualizado"),
                    onBackToBranch = onBackToQueue,
                    onBackToHome = onBackToHome,
                    onHistory = onBackToDetail,
                )
            }
        } else {
            if (operationError.isNotBlank()) {
                item { AdminInfoPanel(title = "No se pudo resolver", text = operationError) }
            }
            item {
                AdminGuidedActionPanel(
                    action = action,
                    problemKind = problemKind,
                    visibleNumber = visibleNumber,
                    summary = summary,
                    detail = detail,
                    toneColor = toneColor,
                )
            }
            item {
                AdminGuidedResultPanel(
                    choices = choices,
                    selectedReason = selectedReason,
                    toneColor = toneColor,
                    onSelected = { selectedReason = it },
                    onConfirm = { onConfirm(selectedReason) },
                )
            }
            item {
                AdminSecondaryActionRow("Volver al pedido", "Revisar la ficha antes de resolver.", onBackToDetail)
            }
        }
    }
}

@Composable
private fun AdminGuidedActionPanel(
    action: LiveOrderAction,
    problemKind: AdminOperationListKind?,
    visibleNumber: String,
    summary: AdminOrderSummary?,
    detail: AdminOrderDetail?,
    toneColor: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.linearGradient(listOf(toneColor.copy(alpha = 0.14f), PediloPanelSoft, PediloPanel)),
                RoundedCornerShape(8.dp),
            )
            .border(1.dp, toneColor.copy(alpha = 0.52f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(action.adminGuidedObjective(problemKind), color = PediloText, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.ExtraBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AdminStatusChip(visibleNumber, toneColor)
            Text(action.adminGuidedActionSubtitle(problemKind), color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        AdminOrderDataSheet(
            title = "Datos para resolver",
            facts = adminHumanTicketFacts(visibleNumber, detail?.storeName ?: summary?.storeName.orEmpty(), "", summary, detail).take(5),
        )
        AdminGuidedChannelsPanel(
            action = action,
            problemKind = problemKind,
            detail = detail,
            toneColor = toneColor,
        )
        AdminOrderDataSheet(
            title = "Sugerencias",
            facts = action.adminGuidedSuggestions(problemKind),
        )
    }
}

@Composable
private fun AdminGuidedChannelsPanel(
    action: LiveOrderAction,
    problemKind: AdminOperationListKind?,
    detail: AdminOrderDetail?,
    toneColor: Color,
) {
    val label = action.adminGuidedActionLabel(problemKind)
    val phone = detail?.component16().orEmpty()
    val canUsePersonPhone = phone.isNotBlank() && label in setOf(
        "Revisar pago",
        "Contactar persona usuaria",
        "Cancelar pedido",
    )
    val shouldExplainMissingStorePhone = label == "Contactar local"
    if (!canUsePersonPhone && !shouldExplainMissingStorePhone) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text("Canales", color = PediloText, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
        if (canUsePersonPhone) {
            AdminWhatsAppActionButton(phone = phone, toneColor = toneColor)
        } else {
            Text(
                "No hay teléfono directo del local en esta ficha. Registrá el resultado de la llamada o contacto externo antes de confirmar.",
                color = PediloMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AdminWhatsAppActionButton(
    phone: String,
    toneColor: Color,
) {
    val uriHandler = LocalUriHandler.current
    val digits = phone.filter { it.isDigit() }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.99f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(toneColor.copy(alpha = if (pressed) 0.18f else 0.10f), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = if (pressed) 0.70f else 0.34f), RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = {
                    if (digits.isNotBlank()) uriHandler.openUri("https://wa.me/$digits")
                },
            )
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Enviar WhatsApp", color = PediloText, fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold)
        Text(phone, color = toneColor, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AdminGuidedResultPanel(
    choices: List<AdminGuidedActionChoice>,
    selectedReason: String,
    toneColor: Color,
    onSelected: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel, RoundedCornerShape(8.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Resultado", color = PediloText, fontSize = 17.sp, lineHeight = 21.sp, fontWeight = FontWeight.ExtraBold)
        choices.forEach { choice ->
            val selected = selectedReason == choice.reason
            AdminInlineActionButton(
                title = choice.label,
                subtitle = choice.result,
                toneColor = if (selected) toneColor else PediloMuted,
                onClick = { onSelected(choice.reason) },
            )
        }
        AdminPrimaryActionButton(label = "Confirmar resultado", onClick = onConfirm)
    }
}

@Composable
private fun AdminOrderSectionScreen(
    section: AdminOrderSection,
    variant: OperationOrderVariant,
    orderId: String?,
    summary: AdminOrderSummary?,
    detail: AdminOrderDetail?,
) {
    val visibleNumber = adminOrderVisibleNumber(summary, detail, orderId)
    val source = detail?.source ?: summary?.source.orEmpty()
    val requestType = detail?.requestType ?: summary?.requestType.orEmpty()
    val identity = AdminOperationOrderClassification.operationalIdentity(source, requestType)
    val operationFunction = AdminOperationOrderClassification.operationalFunction(source, requestType)
    val publicStatus = detail?.publicStatus ?: summary?.publicStatus.orEmpty()
    val operationalStatus = detail?.operationalStatus ?: summary?.operationalStatus.orEmpty()
    val needsAttention = detail?.needsAttention ?: summary?.needsAttention ?: false
    val activeIncident = detail?.activeIncident ?: summary?.activeIncident ?: false
    val signals = AdminOperationOrderSignals(
        status = detail?.status ?: summary?.status.orEmpty(),
        publicStatus = publicStatus,
        operationalStatus = operationalStatus,
        responsibleRole = detail?.responsibleRole ?: summary?.responsibleRole.orEmpty(),
        needsAttention = needsAttention,
        activeIncident = activeIncident,
        source = source,
        requestType = requestType,
    )
    val placement = AdminOperationOrderClassification.primaryPlacement(signals)
    val storeName = detail?.storeName ?: summary?.storeName.orEmpty()
    val problem = adminOrderProblemFocus(
        variant = variant,
        publicStatus = publicStatus,
        operationalStatus = operationalStatus,
        needsAttention = needsAttention,
        activeIncident = activeIncident,
    )
    val status = when {
        placement == AdminOrderPrimaryPlacement.UNCLASSIFIED -> "Revisar datos"
        placement == AdminOrderPrimaryPlacement.ACTIVE &&
            AdminOperationOrderClassification.activeBucket(signals) == AdminActiveOrdersBucket.REVIEW_STATE -> "Revisar pedido"
        else -> adminHumanOperationStatus(
            publicStatus = publicStatus,
            operationalStatus = operationalStatus,
            rawStatus = detail?.status ?: summary?.status.orEmpty(),
            hasProblem = problem != null,
        )
    }
    val title = when (section) {
        AdminOrderSection.Summary -> "Resumen"
        AdminOrderSection.Operation -> adminOrderOperationSectionTitle(identity)
        AdminOrderSection.Delivery -> "Entrega"
        AdminOrderSection.Payment -> "Pago"
        AdminOrderSection.Problems -> "Problemas"
        AdminOrderSection.History -> "Historial"
        AdminOrderSection.Options -> "Opciones"
    }
    val facts = when (section) {
        AdminOrderSection.Summary -> listOf(
            "Tipo" to identity,
            "Estado" to status,
            "Comunicación" to adminCommunicationStatusLabel(detail?.communicationStatus ?: summary?.communicationStatus.orEmpty()),
            "Trabajo" to operationFunction,
            "Referencia" to (problem?.first ?: storeName.adminDisplayValue(detail?.itemsSummary.adminItemsSummary())),
        )
        AdminOrderSection.Operation -> adminOrderOperationFacts(identity, storeName, detail)
        AdminOrderSection.Delivery -> listOf("Persona" to detail.adminPersonName())
        AdminOrderSection.Payment -> adminFinancialFacts(summary, detail)
        AdminOrderSection.Problems -> listOf(
            "Estado" to (problem?.first ?: "Sin problemas"),
            "Seguimiento" to (problem?.second ?: "Sin problemas"),
            "Lectura" to adminAssistedClassificationLabel(detail?.aiClassification ?: summary?.aiClassification.orEmpty()),
            "Riesgo" to adminAssistedRiskLabel(detail?.aiRiskLevel ?: summary?.aiRiskLevel.orEmpty()),
            "Sugerencia" to (detail?.aiSuggestedAction ?: summary?.aiSuggestedAction.orEmpty()).adminDisplayValue("Sin sugerencia"),
            "Revisar" to if (detail?.aiRequiresHumanReview ?: summary?.aiRequiresHumanReview ?: false) "Sí" else "No",
        )
        AdminOrderSection.History -> listOf(
            "Último movimiento" to detail?.lastEventSummary.adminHumanText().adminDisplayValue("Sin movimientos cargados"),
        ) + detail?.events.orEmpty().mapIndexed { index, event ->
            val actor = event.actorRole.adminRoleLabel()
            "Evento ${index + 1}" to listOf(
                event.summary.adminHumanText().adminDisplayValue(event.type.adminEventTypeLabel()),
                actor,
                event.reason.adminHumanText().takeIf { it.isNotBlank() },
            ).filterNotNull().joinToString(" · ")
        }
        AdminOrderSection.Options -> (detail?.nextAllowedActions ?: summary?.nextAllowedActions.orEmpty())
            .map { it.adminActionLabel() to it.adminActionImpact() }
            .ifEmpty { listOf("Opciones" to "Sin acciones") }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = adminBottomBarReservedPadding),
        contentPadding = PaddingValues(top = 18.dp, bottom = adminContentBottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AdminHeader(
                title = title,
                eyebrow = "Pedido $visibleNumber",
                summary = identity,
                onSignOut = {},
                showSignOut = false,
            )
        }
        item { AdminOrderFactPanel(title = title, facts = facts) }
    }
}

private fun adminOrderOperationSectionTitle(identity: String): String =
    when (identity) {
        AdminOperationOrderClassification.IDENTITY_PLUS_BUY -> "Compra"
        AdminOperationOrderClassification.IDENTITY_LOCAL_PICKUP -> "Local / Retiro"
        else -> "Retiro"
    }

private fun adminOrderOperationFacts(
    identity: String,
    storeName: String,
    detail: AdminOrderDetail?,
): List<Pair<String, String>> =
    when (identity) {
        AdminOperationOrderClassification.IDENTITY_PLUS_BUY -> listOf(
            "Detalle de compra" to detail?.itemsSummary.adminItemsSummary(),
        )
        AdminOperationOrderClassification.IDENTITY_LOCAL_PICKUP -> listOf(
            "Local" to storeName.adminDisplayValue("Local no informado"),
            "Retiro" to "Dato de retiro no informado",
        )
        else -> listOf("Lugar de retiro" to storeName.adminDisplayValue("Lugar no informado"))
    }

private fun adminMoreDataNote(storeName: String, detail: AdminOrderDetail?): String =
    storeName.adminDisplayValue(detail?.itemsSummary.adminItemsSummary())

private fun adminDeliveryDetailNote(detail: AdminOrderDetail?): String =
    detail.adminPersonName("Todavía no hay datos de entrega cargados para consultar.")

private fun adminDeliverySummary(summary: AdminOrderSummary?, detail: AdminOrderDetail?): String {
    val actor = detail?.assignedActorRole ?: summary?.assignedActorRole.orEmpty()
    val orderType = detail?.orderType ?: summary?.orderType.orEmpty()
    return when {
        actor.isNotBlank() -> actor.adminRoleLabel()
        orderType.contains("pickup", ignoreCase = true) -> "Retiro"
        orderType.contains("shipping", ignoreCase = true) -> "Entrega"
        else -> "No hay entrega cargada. Si el pedido requiere envío, este dato falta para coordinarlo."
    }
}

private fun adminDriverSummary(summary: AdminOrderSummary?, detail: AdminOrderDetail?): String {
    val assigned = detail?.assignedActorId ?: summary?.assignedActorId.orEmpty()
    val role = detail?.assignedActorRole ?: summary?.assignedActorRole.orEmpty()
    return when {
        assigned.isNotBlank() -> role.adminRoleLabel().adminDisplayValue("Asignado")
        role.isNotBlank() -> role.adminRoleLabel()
        else -> ""
    }
}

private fun adminPaymentSummary(summary: AdminOrderSummary?, detail: AdminOrderDetail?): String {
    val status = adminFinancialStatusLabel(detail?.financialStatus ?: summary?.financialStatus.orEmpty())
    val total = (detail?.total ?: summary?.total).adminMoneyLabel()
    return listOf(status, total.takeIf { it != "Monto no informado" }).filterNotNull().joinToString(" · ")
        .ifBlank { "Pago en revisión. No hay importe cargado para consultar." }
}

private fun adminOrderTimeSummary(summary: AdminOrderSummary?, detail: AdminOrderDetail?): String =
    when {
        (detail?.createdAtMillis ?: summary?.createdAtMillis).isAdminToday() -> "Ingresó hoy"
        detail?.updatedAtMillis != null -> "Actualizado"
        else -> ""
    }

private fun AdminOrderEvent.adminTimelineText(): String {
    val summaryText = summary.adminHumanText()
    val typeText = type.adminEventTypeLabel()
    val actorText = actorRole.adminRoleLabel().takeUnless { it == "Sin rol" }
    val fact = summaryText.adminDisplayValue(typeText)
    val action = fact.adminTimelineActionText()
    val actorAndFact = actorText?.let { "$it $action" } ?: fact
    return listOf(
        createdAtMillis.adminClockLabel(),
        actorAndFact,
        reason.adminHumanText().takeIf { it.isNotBlank() },
    ).filterNotNull().filter { it.isNotBlank() }.joinToString(" — ")
}

private fun Long?.adminClockLabel(): String {
    val millis = this ?: return ""
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
    return "$hour:$minute"
}

private fun String.adminTimelineActionText(): String =
    when (this) {
        "Incidencia abierta" -> "abrió una incidencia"
        "Incidencia resuelta" -> "resolvió una incidencia"
        "Pedido actualizado" -> "actualizó el pedido"
        "Pedido creado" -> "creó el pedido"
        "Pedido aceptado por local" -> "aceptó el pedido"
        "Pedido rechazado por local" -> "rechazó el pedido"
        "Pedido cancelado" -> "canceló el pedido"
        "Admin registró una acción" -> "registró una acción"
        "Admin cerró el caso" -> "cerró el caso"
        "Admin registró seguimiento operativo" -> "registró seguimiento operativo"
        else -> replaceFirstChar { it.lowercase() }
    }

private fun MutableList<Pair<String, String>>.addVisible(label: String, value: String?) {
    value?.trim()
        ?.takeIf { it.isNotBlank() && it != "—" && it.lowercase() != listOf("no", "aplica").joinToString(" ") }
        ?.let { add(label to it) }
}

private fun adminCanRepairPublishedActions(status: String, archiveStatus: String): Boolean {
    val cleanStatus = status.trim().lowercase()
    val cleanArchive = archiveStatus.trim().lowercase()
    return cleanArchive != "archived" &&
        cleanStatus !in setOf("delivered", "closed", "archived", "cancelled", "canceled")
}

private fun adminFinancialFacts(summary: AdminOrderSummary?, detail: AdminOrderDetail?): List<Pair<String, String>> =
    listOf(
        "Estado financiero" to adminFinancialStatusLabel(detail?.financialStatus ?: summary?.financialStatus.orEmpty()),
        "Método" to adminPaymentMethodLabel(detail?.paymentMethod ?: summary?.paymentMethod.orEmpty()),
        "Total" to (detail?.total ?: summary?.total).adminMoneyLabel(),
        "Monto a cobrar" to (detail?.amountToCollect ?: summary?.amountToCollect).adminMoneyLabel(),
        "Cobro requerido" to if (detail?.collectionRequired ?: summary?.collectionRequired ?: false) "Sí" else "No",
        "Responsable de cobro" to (detail?.cashResponsibleRole ?: summary?.cashResponsibleRole.orEmpty()).adminDisplayValue("Sin cobro asignado"),
        "Nota financiera" to detail?.financialNotes.adminDisplayValue("Sin nota financiera"),
    )

private fun adminPaymentMethodLabel(value: String): String =
    when (value.trim()) {
        "cash" -> "Efectivo"
        "transfer" -> "Transferencia declarada"
        "already_paid" -> "Pago declarado"
        else -> "Pago en revisión"
    }

private fun adminFinancialStatusLabel(value: String): String =
    when (value.trim()) {
        "collect_on_delivery" -> "Cobro en entrega"
        "transfer_declared_pending" -> "Transferencia pendiente"
        "paid_declared" -> "Pago declarado"
        "pending_review" -> "Revisión financiera"
        "settlement_pending" -> "Rendición pendiente"
        "settled" -> "Cerrado"
        "disputed" -> "Disputa"
        "rejected" -> "Rechazado"
        else -> value.adminDisplayValue("Pago en revisión")
    }

private fun adminCommunicationStatusLabel(value: String): String =
    when (value.trim()) {
        "received" -> "Recibida"
        "pending" -> "Pendiente"
        "prepared" -> "Lista"
        "sent" -> "Enviada"
        "failed" -> "Fallida"
        "closed" -> "Cerrada"
        "disabled" -> "Canal apagado"
        else -> value.adminDisplayValue("Sin estado")
    }

private fun adminAssistedRiskLabel(value: String): String =
    when (value.trim()) {
        "low" -> "Bajo"
        "medium" -> "Medio"
        "high" -> "Alto"
        "critical" -> "Crítico"
        else -> value.adminDisplayValue("Sin riesgo")
    }

private fun adminAssistedClassificationLabel(value: String): String =
    when (value.trim()) {
        "normal_order" -> "Pedido normal"
        "requires_review" -> "Requiere revisión"
        "incident_risk" -> "Riesgo por incidencia"
        "claim_risk" -> "Riesgo por reclamo"
        "communication_risk" -> "Riesgo de comunicación"
        "financial_review" -> "Revisión financiera"
        "incomplete_data" -> "Datos incompletos"
        "incoherent_state" -> "Estado incoherente"
        "cancellation_financial_review" -> "Cancelación con revisión financiera"
        else -> value.adminDisplayValue("Sin clasificación")
    }

private fun String?.adminMoneyLabel(): String {
    val cents = this?.toLongOrNull() ?: return this.adminDisplayValue("Monto no informado")
    return "\$${cents / 100}"
}

private fun LiveOrderAction.adminActionLabel(): String =
    when (this) {
        LiveOrderAction.LocalAccept -> "Confirmar aceptación"
        LiveOrderAction.LocalReject -> "Confirmar rechazo"
        LiveOrderAction.LocalMarkPreparing -> "Confirmar preparación"
        LiveOrderAction.LocalMarkReady -> "Confirmar pedido listo"
        LiveOrderAction.StoreDriverRequest -> "Solicitar repartidor"
        LiveOrderAction.DriverTake -> "Asignar repartidor"
        LiveOrderAction.DriverMarkPickedUp -> "Confirmar retiro"
        LiveOrderAction.DriverMarkDelivered -> "Confirmar entrega"
        LiveOrderAction.CancelOrder -> "Cancelar pedido"
        LiveOrderAction.OpenIncident -> "Abrir incidencia"
        LiveOrderAction.ResolveIncident -> "Resolver incidencia"
        LiveOrderAction.AdminIntervene -> "Tomar intervención"
    }

private fun LiveOrderAction.adminActionImpact(): String =
    when (this) {
        LiveOrderAction.LocalAccept -> "El pedido queda aceptado y puede prepararse."
        LiveOrderAction.LocalReject -> "El pedido se cierra con un motivo claro."
        LiveOrderAction.LocalMarkPreparing -> "El pedido queda marcado como en preparación."
        LiveOrderAction.LocalMarkReady -> "El pedido queda listo para retiro o entrega."
        LiveOrderAction.StoreDriverRequest -> "Se pide un repartidor para este pedido."
        LiveOrderAction.DriverTake -> "El pedido queda con responsable de reparto."
        LiveOrderAction.DriverMarkPickedUp -> "El pedido queda retirado del local."
        LiveOrderAction.DriverMarkDelivered -> "El pedido queda entregado."
        LiveOrderAction.CancelOrder -> "El pedido se cancela con motivo."
        LiveOrderAction.OpenIncident -> "Se abre una incidencia para seguir el problema."
        LiveOrderAction.ResolveIncident -> "La incidencia activa queda cerrada."
        LiveOrderAction.AdminIntervene -> "Admin queda como responsable de la resolución."
    }

private fun LiveOrderAction.adminGuidedActionLabel(problemKind: AdminOperationListKind?): String =
    when (problemKind) {
        AdminOperationListKind.ProblemStoreNotResponding -> when (this) {
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident -> "Contactar local"
            LiveOrderAction.CancelOrder -> "Cancelar pedido"
            LiveOrderAction.LocalReject -> "Registrar rechazo del local"
            else -> adminActionLabel()
        }
        AdminOperationListKind.ProblemWithoutResponsible,
        AdminOperationListKind.ActiveWaitingDriver -> when (this) {
            LiveOrderAction.StoreDriverRequest -> "Buscar repartidor"
            LiveOrderAction.DriverTake -> "Asignar manualmente"
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident -> "Subir prioridad"
            else -> adminActionLabel()
        }
        AdminOperationListKind.ProblemPaymentConflict -> when (this) {
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident -> "Revisar pago"
            LiveOrderAction.CancelOrder -> "Cancelar pedido"
            else -> adminActionLabel()
        }
        AdminOperationListKind.ProblemDriverIssue -> when (this) {
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident -> "Contactar repartidor"
            LiveOrderAction.CancelOrder -> "Cancelar pedido"
            else -> adminActionLabel()
        }
        AdminOperationListKind.ProblemUserNotResponding,
        AdminOperationListKind.ProblemUserClaim -> when (this) {
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.ResolveIncident -> "Contactar persona usuaria"
            LiveOrderAction.CancelOrder -> "Cancelar pedido"
            else -> adminActionLabel()
        }
        AdminOperationListKind.ProblemDelayed -> when (this) {
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident -> "Resolver demora"
            LiveOrderAction.CancelOrder -> "Cancelar pedido"
            else -> adminActionLabel()
        }
        else -> adminActionLabel()
    }

private fun LiveOrderAction.adminGuidedActionSubtitle(problemKind: AdminOperationListKind?): String =
    when (adminGuidedActionLabel(problemKind)) {
        "Contactar local" -> "WhatsApp, chat interno y resultado"
        "Buscar repartidor" -> "Disponibilidad, prioridad y asignación"
        "Asignar manualmente" -> "Elegir responsable y confirmar"
        "Revisar pago" -> "Comprobante, persona usuaria y decisión"
        "Contactar repartidor" -> "Estado de entrega y alternativa"
        "Contactar persona usuaria" -> "Aviso, respuesta y cierre"
        "Cancelar pedido" -> "Motivo, aviso y confirmación"
        else -> adminActionImpact()
    }

private fun LiveOrderAction.adminGuidedObjective(problemKind: AdminOperationListKind?): String =
    when (adminGuidedActionLabel(problemKind)) {
        "Contactar local" -> "Confirmar si el local puede preparar el pedido."
        "Buscar repartidor" -> "Encontrar una persona disponible para llevar el pedido."
        "Asignar manualmente" -> "Dejar el pedido con responsable de reparto."
        "Revisar pago" -> "Resolver si el pago permite avanzar o si hay que contactar a la persona usuaria."
        "Contactar repartidor" -> "Entender el inconveniente y decidir si continúa o se reasigna."
        "Contactar persona usuaria" -> "Aclarar la situación y dejar una respuesta registrada."
        "Cancelar pedido" -> "Cerrar el pedido con motivo y aviso claro."
        else -> adminActionImpact()
    }

private fun LiveOrderAction.adminGuidedSuggestions(problemKind: AdminOperationListKind?): List<Pair<String, String>> =
    when (adminGuidedActionLabel(problemKind)) {
        "Contactar local" -> listOf(
            "Mensaje sugerido" to "Hola, tenés un pedido pendiente en Pédilo. ¿Podés confirmarlo para avanzar?",
            "Alternativas" to "Locales del mismo rubro o productos parecidos si rechaza.",
        )
        "Buscar repartidor",
        "Asignar manualmente" -> listOf(
            "Prioridad" to "Subir prioridad si el pedido ya está listo.",
            "Alternativa" to "Asignar manualmente si nadie lo toma.",
        )
        "Revisar pago" -> listOf(
            "Control" to "Comparar monto declarado con total del pedido.",
            "Alternativa" to "Contactar persona usuaria antes de cancelar.",
        )
        "Contactar repartidor" -> listOf(
            "Primero" to "Confirmar ubicación y posibilidad de continuar.",
            "Alternativa" to "Reasignar o avisar demora a la persona usuaria.",
        )
        "Contactar persona usuaria" -> listOf(
            "Mensaje sugerido" to "Hola, estamos revisando tu pedido y necesitamos confirmar un dato para seguir.",
            "Alternativa" to "Registrar que no respondió y mantener seguimiento.",
        )
        "Cancelar pedido" -> listOf(
            "Motivo" to "Elegí el motivo real antes de confirmar.",
            "Aviso" to "La persona usuaria debe recibir un mensaje claro.",
        )
        else -> listOf("Siguiente paso" to adminActionImpact())
    }

private fun LiveOrderAction.adminGuidedResultChoices(problemKind: AdminOperationListKind?): List<AdminGuidedActionChoice> =
    when (adminGuidedActionLabel(problemKind)) {
        "Contactar local" -> listOf(
            AdminGuidedActionChoice("El local confirmó", "El pedido debería pasar a preparación.", "Local confirmó el pedido"),
            AdminGuidedActionChoice("El local rechazó", "Queda registrado para buscar alternativa o cancelar.", "Local rechazó el pedido"),
            AdminGuidedActionChoice("No respondió", "El pedido queda con seguimiento crítico.", "Local no respondió"),
        )
        "Buscar repartidor",
        "Asignar manualmente" -> listOf(
            AdminGuidedActionChoice("Repartidor encontrado", "El pedido pasa a reparto asignado.", "Repartidor asignado"),
            AdminGuidedActionChoice("Sin disponibilidad", "El pedido mantiene prioridad de búsqueda.", "Sin repartidor disponible"),
        )
        "Revisar pago" -> listOf(
            AdminGuidedActionChoice("Pago validado", "El pedido puede seguir.", "Pago revisado y validado"),
            AdminGuidedActionChoice("Contactar persona usuaria", "Queda registrado el pedido de confirmación.", "Persona usuaria contactada por pago"),
            AdminGuidedActionChoice("Pago con problema", "El conflicto queda abierto.", "Pago con conflicto confirmado"),
        )
        "Cancelar pedido" -> listOf(
            AdminGuidedActionChoice("Persona usuaria avisada", "El pedido se cerrará con aviso registrado.", "Pedido cancelado con aviso a persona usuaria"),
            AdminGuidedActionChoice("Aviso manual pendiente", "El pedido se cerrará dejando pendiente el aviso.", "Pedido cancelado con aviso manual pendiente"),
        )
        else -> listOf(
            AdminGuidedActionChoice("Resuelto", "El pedido se actualizará al confirmar.", "${adminGuidedActionLabel(problemKind)} resuelto"),
            AdminGuidedActionChoice("Requiere seguimiento", "El pedido queda con intervención registrada.", "${adminGuidedActionLabel(problemKind)} requiere seguimiento"),
        )
    }

private fun LiveOrderAction.requiresAdminReason(): Boolean =
    this in setOf(
        LiveOrderAction.LocalReject,
        LiveOrderAction.CancelOrder,
        LiveOrderAction.OpenIncident,
        LiveOrderAction.ResolveIncident,
        LiveOrderAction.AdminIntervene,
    )

private fun CoreError.adminHumanError(): String =
    when (this) {
        is CoreError.Operational -> humanMessage.ifBlank { "Error operativo" }
        CoreError.NotAvailable -> "No pudimos conectar."
        CoreError.IncompleteData -> "No pudimos ejecutar la acción con la información disponible."
        is CoreError.Validation -> "Revisá los datos antes de confirmar."
        CoreError.Unknown -> "No pudimos ejecutar la acción."
    }

@Composable
private fun AdminOrderNavigationCard(entry: AdminOrderNavigationEntry, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.988f else 1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (pressed) PediloPanel else PediloPanelSoft, RoundedCornerShape(14.dp))
            .border(1.dp, if (pressed) PediloOrange.copy(alpha = 0.72f) else PediloLine, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 72.dp)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(entry.icon, contentDescription = entry.title, tint = PediloOrange, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(entry.title, color = PediloText, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(entry.note, color = PediloMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = PediloMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AdminPrimaryActionButton(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.988f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) PediloGreen.copy(alpha = 0.26f) else PediloGreen.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .border(1.dp, PediloGreen.copy(alpha = 0.62f), RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PediloText, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun AdminSecondaryActionRow(title: String, note: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.992f else 1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) PediloPanel else PediloPanelSoft, RoundedCornerShape(8.dp))
            .border(1.dp, if (pressed) PediloOrange.copy(alpha = 0.56f) else PediloLine, RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = PediloText, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.ExtraBold)
            Text(note, color = PediloMuted, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = PediloMuted, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun AdminOrderDataSheet(title: String, facts: List<Pair<String, String>>) {
    val visibleFacts = facts.filter { (_, value) -> value.trim().isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanelSoft.copy(alpha = 0.82f), RoundedCornerShape(8.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Text(title, color = PediloText, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
        visibleFacts.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(label, color = PediloMuted, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
                        Text(value, color = PediloText, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminOrderCompactTimeline(events: List<AdminOrderEvent>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .border(1.dp, PediloLine.copy(alpha = 0.48f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text("Historial reciente", color = PediloMuted, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold)
        events.take(3).ifEmpty { listOf(AdminOrderEvent("", "", "Sin movimientos cargados todavía", "", "", null)) }
            .forEach { event ->
                Text(
                    event.adminTimelineText(),
                    color = PediloText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

@Composable
private fun AdminMoreDataInline(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Más información del pedido", color = PediloMuted, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold)
        Text("Ver datos", color = PediloMuted, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminOrderResultActions(
    onBackToBranch: () -> Unit,
    onBackToHome: () -> Unit,
    onHistory: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
        AdminSecondaryActionRow("Volver a la rama", "Seguir con la lista desde donde abriste el pedido.", onBackToBranch)
        AdminSecondaryActionRow("Volver a Operación", "Ir a la mesa principal.", onBackToHome)
        AdminSecondaryActionRow("Ver historial reciente", "Consultar los últimos movimientos humanos.", onHistory)
    }
}

@Composable
private fun AdminOrderResultStrip(
    message: String,
    onBackToBranch: () -> Unit,
    onBackToHome: () -> Unit,
    onHistory: () -> Unit,
) {
    val toneColor = adminResultToneColor(message)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(listOf(toneColor.copy(alpha = 0.16f), PediloPanelSoft, PediloPanel)), RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AdminStatusChip(adminResultStateLabel(message), toneColor)
        Text(message, color = PediloText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(adminResultNextStep(message), color = PediloMuted, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AdminCompactTextAction("Ver pedidos", onBackToBranch, Modifier.weight(1f))
            AdminCompactTextAction("Operación", onBackToHome, Modifier.weight(1f))
            AdminCompactTextAction("Historial", onHistory, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AdminCompactTextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = label,
        color = PediloText,
        fontSize = 12.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanelSoft, RoundedCornerShape(8.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 9.dp),
    )
}

@Composable
private fun AdminDetailSituationPanel(
    number: String,
    status: String,
    placement: String,
    responsible: String,
    tone: AdminOperationMetricTone,
) {
    val toneColor = tone.operationToneColor()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PediloPanel, RoundedCornerShape(8.dp))
            .border(1.dp, toneColor.copy(alpha = 0.44f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(number, color = PediloText, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.ExtraBold)
            AdminStatusChip(label = placement, toneColor = toneColor)
        }
        Text(status, color = toneColor, fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.ExtraBold)
        Text(responsible, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AdminOrderMomentPanel(
    title: String,
    detail: String,
    highlighted: Boolean,
    eyebrow: String? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanelSoft, RoundedCornerShape(15.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(15.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        eyebrow?.let {
            Text(it, color = PediloMuted, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            title,
            color = if (highlighted) PediloWarning else PediloGreen,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(detail, color = PediloText, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun AdminStatusChip(label: String, toneColor: Color) {
    Text(
        text = label,
        color = toneColor,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(toneColor.copy(alpha = 0.12f), RoundedCornerShape(50))
            .border(1.dp, toneColor.copy(alpha = 0.34f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    )
}

@Composable
private fun AdminOrderFactPanel(title: String, facts: List<Pair<String, String>>) {
    val visibleFacts = facts.filter { (_, value) -> value.trim().isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PediloPanelSoft, RoundedCornerShape(8.dp))
            .border(1.dp, PediloLine, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, color = PediloText, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.ExtraBold)
        visibleFacts.ifEmpty { listOf("Estado" to "Sin datos necesarios ahora") }.forEach { (label, value) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, color = PediloMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(value, color = PediloText, fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AdminOrderTimelinePanel(title: String, events: List<AdminOrderEvent>) {
    AdminOrderFactPanel(
        title = title,
        facts = events.take(4).mapIndexed { index, event ->
            val actor = event.actorRole.adminRoleLabel()
            "Movimiento ${index + 1}" to listOf(
                event.summary.adminHumanText().adminDisplayValue(event.type.adminEventTypeLabel()),
                actor,
                event.reason.adminHumanText().takeIf { it.isNotBlank() },
            ).filterNotNull().joinToString(" · ")
        }.ifEmpty {
            listOf("Historial" to "Sin movimientos cargados todavía")
        },
    )
}

@Composable
private fun AdminActionCard(title: String, note: String, onClick: () -> Unit) {
    val intent = adminHumanIntentFor(title, note)
    val toneColor = intent.adminIntentColor()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.986f else 1f)
            .pediloCardDepth(RoundedCornerShape(15.dp))
            .background(
                Brush.linearGradient(listOf(toneColor.copy(alpha = if (pressed) 0.20f else 0.12f), PediloPanelSoft, PediloPanel)),
                RoundedCornerShape(15.dp),
            )
            .border(1.dp, if (pressed) toneColor.copy(alpha = 0.78f) else toneColor.copy(alpha = 0.34f), RoundedCornerShape(15.dp))
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(title, color = PediloText, fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.ExtraBold)
        Text(note, color = PediloMuted, fontSize = 13.sp, lineHeight = 17.sp)
    }
}

private fun adminHumanIntentFor(title: String, note: String): AdminHumanIntent {
    val text = "$title $note"
    return when {
        text.contains("Auditor", ignoreCase = true) || text.contains("registro", ignoreCase = true) -> AdminHumanIntent.Audit
        text.contains("Confirm", ignoreCase = true) || text.contains("sensible", ignoreCase = true) -> AdminHumanIntent.Confirm
        text.contains("Volver", ignoreCase = true) -> AdminHumanIntent.Success
        text.contains("Impacto", ignoreCase = true) || text.contains("Emergencia", ignoreCase = true) -> AdminHumanIntent.Impact
        text.contains("Revisar", ignoreCase = true) -> AdminHumanIntent.Preview
        text.contains("Editar", ignoreCase = true) || text.contains("Ajustar", ignoreCase = true) -> AdminHumanIntent.Edit
        text.contains("Problema", ignoreCase = true) || text.contains("paus", ignoreCase = true) || text.contains("detenido", ignoreCase = true) -> AdminHumanIntent.Problem
        text.contains("revisión", ignoreCase = true) || text.contains("pendiente", ignoreCase = true) || text.contains("incompleto", ignoreCase = true) -> AdminHumanIntent.Warning
        text.contains("cuenta", ignoreCase = true) || text.contains("rol", ignoreCase = true) || text.contains("víncul", ignoreCase = true) || text.contains("acceso", ignoreCase = true) -> AdminHumanIntent.Access
        text.contains("activo", ignoreCase = true) || text.contains("publicable", ignoreCase = true) || text.contains("listo", ignoreCase = true) -> AdminHumanIntent.Success
        else -> AdminHumanIntent.Info
    }
}

private fun AdminHumanIntent.adminIntentColor(): Color =
    when (this) {
        AdminHumanIntent.Info -> PediloMuted
        AdminHumanIntent.Success -> PediloGreen
        AdminHumanIntent.Warning -> PediloWarning
        AdminHumanIntent.Problem -> PediloPink
        AdminHumanIntent.Emergency -> PediloWarning
        AdminHumanIntent.Audit -> PediloPink
        AdminHumanIntent.Edit -> PediloOrange
        AdminHumanIntent.Preview -> PediloCyan
        AdminHumanIntent.Impact -> PediloWarning
        AdminHumanIntent.Confirm -> PediloOrange
        AdminHumanIntent.Access -> PediloPink
    }

private fun AdminHumanIntent.adminIntentLabel(): String =
    when (this) {
        AdminHumanIntent.Info -> "Lectura"
        AdminHumanIntent.Success -> "Listo"
        AdminHumanIntent.Warning -> "Revisar"
        AdminHumanIntent.Problem -> "Bloqueo"
        AdminHumanIntent.Emergency -> "Emergencia"
        AdminHumanIntent.Audit -> "Historial"
        AdminHumanIntent.Edit -> "Editable"
        AdminHumanIntent.Preview -> "Revisar"
        AdminHumanIntent.Impact -> "Impacto"
        AdminHumanIntent.Confirm -> "Confirmación"
        AdminHumanIntent.Access -> "Acceso"
    }

private fun AdminRoute.root(): AdminRoot = when (this) {
    AdminRoute.Operation -> AdminRoot.Operation
    AdminRoute.Configuration -> AdminRoot.Configuration
    AdminRoute.ConfigurationShippingFee -> AdminRoot.Configuration
    AdminRoute.ConfigurationRainMode -> AdminRoot.Configuration
    AdminRoute.ConfigurationBaseDeliveryFee -> AdminRoot.Configuration
    AdminRoute.ConfigurationDistanceSurcharge -> AdminRoot.Configuration
    AdminRoute.RoleAccess -> AdminRoot.RoleAccess
    is AdminRoute.OperationBranch -> AdminRoot.Operation
    is AdminRoute.OperationOrderDetail -> AdminRoot.Operation
    is AdminRoute.OperationOrderSection -> AdminRoot.Operation
    is AdminRoute.RoleAccessSection -> AdminRoot.RoleAccess
    is AdminRoute.RoleAccessSubsection -> AdminRoot.RoleAccess
    is AdminRoute.RoleAccessConvergence -> AdminRoot.RoleAccess
    is AdminRoute.OperationQueue -> AdminRoot.Operation
    is AdminRoute.OperationGuidedAction -> AdminRoot.Operation
    is AdminRoute.Section -> root
}

private fun AdminRoute.adminOrderOriginLabel(): String =
    when (this) {
        AdminRoute.Operation -> "Volver a Operación"
        is AdminRoute.OperationBranch -> "Volver a grupos"
        is AdminRoute.OperationQueue -> "Volver a pedidos"
        else -> "Volver a Operación"
    }

private fun adminMainListForQueue(list: AdminOperationList): AdminOperationList {
    val mainKind = when (list.kind) {
        AdminOperationListKind.ProblemStoreNotResponding,
        AdminOperationListKind.ProblemUserClaim,
        AdminOperationListKind.ProblemDelayed,
        AdminOperationListKind.ProblemWithoutResponsible,
        AdminOperationListKind.ProblemPaymentConflict,
        AdminOperationListKind.ProblemDriverIssue,
        AdminOperationListKind.ProblemUserNotResponding,
        AdminOperationListKind.ProblemOperationalReview -> AdminOperationListKind.AllProblems
        AdminOperationListKind.ActiveWaitingStore,
        AdminOperationListKind.ActiveWaitingOperationalConfirmation -> AdminOperationListKind.ActiveWaitingStore
        AdminOperationListKind.AcceptedByStore,
        AdminOperationListKind.AcceptedWaitingPreparation,
        AdminOperationListKind.AcceptedReadyToPrepare -> AdminOperationListKind.ActiveReviewState
        AdminOperationListKind.PreparingNormal,
        AdminOperationListKind.PreparingReadyForPickup,
        AdminOperationListKind.PreparingDelayed -> AdminOperationListKind.AllPreparing
        AdminOperationListKind.DeliveryDriverAssigned,
        AdminOperationListKind.DeliveryPickedUp,
        AdminOperationListKind.DeliveryDelayed -> AdminOperationListKind.AllInDelivery
        AdminOperationListKind.ClosedDelivered,
        AdminOperationListKind.ClosedCancelledProblem,
        AdminOperationListKind.ClosedWithIncident,
        AdminOperationListKind.ClosedPostClaim -> AdminOperationListKind.AllClosed
        else -> list.kind
    }
    return adminLiveBranches(emptyList()).firstOrNull { it.kind == mainKind }?.let {
        AdminOperationList(it.title, it.state, "Sin pedidos", it.kind)
    } ?: AdminOperationList("Operación", "Pedidos", "Sin pedidos", mainKind)
}

private fun adminKindForPlacement(
    placement: AdminOrderPrimaryPlacement,
    activeBucket: AdminActiveOrdersBucket?,
): AdminOperationListKind =
    when {
        placement == AdminOrderPrimaryPlacement.PROBLEM -> AdminOperationListKind.AllProblems
        placement == AdminOrderPrimaryPlacement.FINISHED -> AdminOperationListKind.ClosedDelivered
        placement == AdminOrderPrimaryPlacement.CANCELLED -> AdminOperationListKind.ClosedCancelledProblem
        activeBucket == AdminActiveOrdersBucket.WAITING_STORE -> AdminOperationListKind.ActiveWaitingStore
        activeBucket == AdminActiveOrdersBucket.PREPARING -> AdminOperationListKind.PreparingNormal
        activeBucket == AdminActiveOrdersBucket.WAITING_DRIVER -> AdminOperationListKind.ActiveWaitingDriver
        activeBucket == AdminActiveOrdersBucket.IN_DELIVERY -> AdminOperationListKind.ActiveInDelivery
        else -> AdminOperationListKind.ActiveReviewState
    }

private fun adminProblemListKindFor(
    summary: AdminOrderSummary?,
    detail: AdminOrderDetail?,
    signals: AdminOperationOrderSignals,
): AdminOperationListKind? {
    val financial = detail?.financialStatus ?: summary?.financialStatus.orEmpty()
    val operational = detail?.operationalStatus ?: summary?.operationalStatus.orEmpty()
    val public = detail?.publicStatus ?: summary?.publicStatus.orEmpty()
    return when {
        financial.contains("disputed", ignoreCase = true) ||
            financial.contains("rejected", ignoreCase = true) ||
            financial.contains("conflict", ignoreCase = true) ||
            financial.contains("conflicto", ignoreCase = true) -> AdminOperationListKind.ProblemPaymentConflict
        operational.contains("driver_issue", ignoreCase = true) ||
            operational.contains("repartidor report", ignoreCase = true) ||
            public.contains("repartidor", ignoreCase = true) && public.contains("problema", ignoreCase = true) -> AdminOperationListKind.ProblemDriverIssue
        public.contains("cliente no responde", ignoreCase = true) ||
            operational.contains("cliente no responde", ignoreCase = true) -> AdminOperationListKind.ProblemUserNotResponding
        else -> when (AdminOperationOrderClassification.problemBucket(signals)) {
            AdminProblemOrdersBucket.STORE_NOT_RESPONDING -> AdminOperationListKind.ProblemStoreNotResponding
            AdminProblemOrdersBucket.CUSTOMER_CLAIM -> AdminOperationListKind.ProblemUserClaim
            AdminProblemOrdersBucket.DELAYED -> AdminOperationListKind.ProblemDelayed
            AdminProblemOrdersBucket.WITHOUT_RESPONSIBLE -> AdminOperationListKind.ProblemWithoutResponsible
            AdminProblemOrdersBucket.OPERATIONAL_REVIEW -> AdminOperationListKind.ProblemOperationalReview
            null -> null
        }
    }
}

private fun adminVisibleGuidedActions(
    allowedActions: List<LiveOrderAction>,
    problemKind: AdminOperationListKind?,
    placement: AdminOrderPrimaryPlacement,
    activeBucket: AdminActiveOrdersBucket?,
): List<LiveOrderAction> {
    val preferred = when (problemKind) {
        AdminOperationListKind.ProblemStoreNotResponding -> listOf(
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.CancelOrder,
            LiveOrderAction.OpenIncident,
            LiveOrderAction.LocalReject,
        )
        AdminOperationListKind.ProblemWithoutResponsible,
        AdminOperationListKind.ActiveWaitingDriver -> listOf(
            LiveOrderAction.StoreDriverRequest,
            LiveOrderAction.DriverTake,
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident,
        )
        AdminOperationListKind.ProblemPaymentConflict -> listOf(
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident,
            LiveOrderAction.CancelOrder,
        )
        AdminOperationListKind.ProblemDriverIssue -> listOf(
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident,
            LiveOrderAction.CancelOrder,
        )
        AdminOperationListKind.ProblemUserClaim,
        AdminOperationListKind.ProblemUserNotResponding -> listOf(
            LiveOrderAction.ResolveIncident,
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.CancelOrder,
        )
        AdminOperationListKind.ProblemDelayed,
        AdminOperationListKind.ProblemOperationalReview -> listOf(
            LiveOrderAction.AdminIntervene,
            LiveOrderAction.OpenIncident,
            LiveOrderAction.CancelOrder,
            LiveOrderAction.ResolveIncident,
        )
        else -> when {
            activeBucket == AdminActiveOrdersBucket.WAITING_DRIVER -> listOf(LiveOrderAction.StoreDriverRequest, LiveOrderAction.DriverTake)
            placement == AdminOrderPrimaryPlacement.ACTIVE -> allowedActions.take(2)
            else -> allowedActions.take(1)
        }
    }
    return preferred.filter { it in allowedActions }.ifEmpty { allowedActions.take(if (problemKind == null) 1 else 2) }
}

private fun adminNoGuidedActionText(problemKind: AdminOperationListKind?, status: String): String =
    when (problemKind) {
        null -> "No hay una acción guiada disponible para $status."
        else -> "Este problema necesita lectura, pero todavía no hay una acción segura para resolverlo desde acá."
    }

private fun AdminOrderSummary.adminActorLabel(): String =
    storeName.ifBlank {
        when {
            assignedActorRole.contains("driver", ignoreCase = true) -> "Repartidor asignado"
            assignedActorRole.isNotBlank() -> assignedActorRole.adminRoleLabel()
            else -> "Pedido Pédilo"
        }
    }

private fun AdminOrderSummary.adminElapsedLabel(): String {
    val created = createdAtMillis ?: return "Sin hora"
    val minutes = ((System.currentTimeMillis() - created) / 60000L).coerceAtLeast(0)
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "Hace ${minutes} min"
        minutes < 1440 -> "Hace ${minutes / 60} h"
        else -> "Hace ${minutes / 1440} d"
    }
}

private fun adminHumanTicketFacts(
    visibleNumber: String,
    storeName: String,
    status: String,
    summary: AdminOrderSummary?,
    detail: AdminOrderDetail?,
): List<Pair<String, String>> = buildList {
    addVisible("Pedido", visibleNumber)
    addVisible("Estado", status)
    addVisible("Persona usuaria", detail?.component15())
    addVisible("Teléfono", detail?.component16())
    addVisible("Local / origen", storeName)
    addVisible("Pedido solicitado", detail?.itemsSummary.adminItemsSummary())
    addVisible("Destino", detail?.component17())
    addVisible("Pago", adminPaymentHumanLine(summary, detail))
    addVisible("Total", adminMoneyOrBlank(detail?.total ?: summary?.total))
    addVisible("Repartidor", adminDriverSummary(summary, detail))
    addVisible("Último movimiento", detail?.lastEventSummary.adminHumanText())
}

private fun adminPaymentHumanLine(summary: AdminOrderSummary?, detail: AdminOrderDetail?): String {
    val method = detail?.paymentMethod ?: summary?.paymentMethod.orEmpty()
    val amount = detail?.amountToCollect ?: summary?.amountToCollect.orEmpty()
    return listOf(adminPaymentMethodLabel(method), amount.takeIf { it.isNotBlank() }?.adminMoneyLabel()).filterNotNull()
        .joinToString(" · ")
}

private fun adminMoneyOrBlank(value: String?): String =
    value?.takeIf { it.isNotBlank() }?.adminMoneyLabel().orEmpty()

private fun adminResultToneColor(message: String): Color {
    val clean = message.lowercase()
    return when {
        "cancel" in clean || "rechaz" in clean -> PediloOrange
        "incidencia" in clean || "seguimiento" in clean || "no respondió" in clean || "problema" in clean -> PediloWarning
        "resuelt" in clean || "confirm" in clean || "acept" in clean || "entreg" in clean || "asign" in clean || "valid" in clean -> PediloGreen
        else -> PediloCyan
    }
}

private fun adminResultStateLabel(message: String): String {
    val clean = message.lowercase()
    return when {
        "cancel" in clean -> "Pedido cancelado"
        "incidencia" in clean || "seguimiento" in clean || "no respondió" in clean -> "Quedó en seguimiento"
        "problema" in clean -> "Requiere revisión"
        "resuelt" in clean || "confirm" in clean || "acept" in clean || "entreg" in clean || "asign" in clean || "valid" in clean -> "Pedido avanzado"
        else -> "Cambio registrado"
    }
}

private fun adminResultNextStep(message: String): String {
    val clean = message.lowercase()
    return when {
        "cancel" in clean -> "Volvé a pedidos para revisar el cierre y el aviso."
        "incidencia" in clean || "seguimiento" in clean || "no respondió" in clean || "problema" in clean ->
            "Volvé a pedidos para seguir el caso pendiente."
        "resuelt" in clean || "confirm" in clean || "acept" in clean || "entreg" in clean || "asign" in clean || "valid" in clean ->
            "Volvé a pedidos para ver la nueva etapa."
        else -> "Volvé a pedidos para ver dónde quedó."
    }
}

private fun adminOperationPulseText(problems: Int, waiting: Int, moving: Int): String =
    when {
        problems > 0 -> "$problems pedidos necesitan intervención ahora"
        waiting > 0 -> "$waiting pedidos esperan respuesta inicial"
        moving > 0 -> "$moving pedidos se están moviendo"
        else -> "Operación sin bloqueos visibles"
    }

private fun adminOperationPriorityLabel(kind: AdminOperationListKind, count: Int): String =
    when {
        count == 0 -> "sin casos"
        kind in setOf(
            AdminOperationListKind.AllProblems,
            AdminOperationListKind.ProblemStoreNotResponding,
            AdminOperationListKind.ProblemPaymentConflict,
            AdminOperationListKind.ProblemDriverIssue,
            AdminOperationListKind.ProblemDelayed,
            AdminOperationListKind.ProblemOperationalReview,
        ) -> "prioridad alta"
        kind in setOf(
            AdminOperationListKind.ActiveWaitingStore,
            AdminOperationListKind.ActiveWaitingOperationalConfirmation,
            AdminOperationListKind.ActiveWaitingDriver,
            AdminOperationListKind.ProblemWithoutResponsible,
            AdminOperationListKind.ProblemUserNotResponding,
            AdminOperationListKind.ProblemUserClaim,
            AdminOperationListKind.PreparingDelayed,
            AdminOperationListKind.DeliveryDelayed,
        ) -> "revisar pronto"
        else -> "seguimiento"
    }

private fun adminOperationOutcomeText(kind: AdminOperationListKind): String =
    when (kind) {
        AdminOperationListKind.AllProblems,
        AdminOperationListKind.ProblemStoreNotResponding -> "vuelve a preparación o cierre"
        AdminOperationListKind.ProblemWithoutResponsible,
        AdminOperationListKind.ActiveWaitingDriver -> "pasa a reparto asignado"
        AdminOperationListKind.ProblemPaymentConflict -> "pasa a seguir o queda bloqueado"
        AdminOperationListKind.ProblemDriverIssue -> "sigue entrega o se reasigna"
        AdminOperationListKind.ProblemUserNotResponding,
        AdminOperationListKind.ProblemUserClaim -> "queda respondido o en seguimiento"
        AdminOperationListKind.ProblemDelayed,
        AdminOperationListKind.PreparingDelayed,
        AdminOperationListKind.DeliveryDelayed -> "baja demora o escala incidencia"
        AdminOperationListKind.ActiveWaitingStore,
        AdminOperationListKind.ActiveWaitingOperationalConfirmation -> "pasa a aceptados"
        AdminOperationListKind.ActiveReviewState,
        AdminOperationListKind.AcceptedByStore,
        AdminOperationListKind.AcceptedWaitingPreparation,
        AdminOperationListKind.AcceptedReadyToPrepare -> "pasa a preparación"
        AdminOperationListKind.AllPreparing,
        AdminOperationListKind.ActivePreparing,
        AdminOperationListKind.PreparingNormal,
        AdminOperationListKind.PreparingReadyForPickup -> "pasa a retiro o entrega"
        AdminOperationListKind.AllInDelivery,
        AdminOperationListKind.ActiveInDelivery,
        AdminOperationListKind.DeliveryDriverAssigned,
        AdminOperationListKind.DeliveryPickedUp -> "pasa a entregado"
        AdminOperationListKind.AllClosed,
        AdminOperationListKind.ClosedDelivered,
        AdminOperationListKind.ClosedCancelledProblem,
        AdminOperationListKind.ClosedWithIncident,
        AdminOperationListKind.ClosedPostClaim -> "queda para lectura final"
        else -> "vuelve a la cola correcta"
    }

private fun adminOperationEmptyText(kind: AdminOperationListKind): String =
    when (kind) {
        AdminOperationListKind.AllProblems -> "No hay bloqueos operativos visibles."
        AdminOperationListKind.ActiveWaitingStore -> "No hay locales esperando aceptación."
        AdminOperationListKind.ActiveReviewState -> "No hay aceptados pendientes de avance."
        AdminOperationListKind.AllPreparing -> "No hay pedidos en preparación."
        AdminOperationListKind.AllInDelivery -> "No hay entregas en curso."
        AdminOperationListKind.AllClosed -> "No hay cierres para revisar."
        else -> "Nada para atender acá."
    }

private fun adminProblemOperatorLabel(
    problemKind: AdminOperationListKind?,
    activeBucket: AdminActiveOrdersBucket?,
    placement: AdminOrderPrimaryPlacement,
): String =
    when (problemKind) {
        AdminOperationListKind.ProblemStoreNotResponding -> "Admin y local"
        AdminOperationListKind.ProblemWithoutResponsible -> "Admin y reparto"
        AdminOperationListKind.ProblemPaymentConflict -> "Admin y persona usuaria"
        AdminOperationListKind.ProblemDriverIssue -> "Admin y repartidor"
        AdminOperationListKind.ProblemUserNotResponding,
        AdminOperationListKind.ProblemUserClaim -> "Admin y persona usuaria"
        AdminOperationListKind.ProblemDelayed,
        AdminOperationListKind.ProblemOperationalReview -> "Admin"
        else -> when {
            activeBucket == AdminActiveOrdersBucket.WAITING_STORE -> "Local"
            activeBucket == AdminActiveOrdersBucket.PREPARING -> "Local"
            activeBucket == AdminActiveOrdersBucket.WAITING_DRIVER -> "Admin y reparto"
            activeBucket == AdminActiveOrdersBucket.IN_DELIVERY -> "Repartidor"
            placement == AdminOrderPrimaryPlacement.FINISHED || placement == AdminOrderPrimaryPlacement.CANCELLED -> "Consulta"
            else -> "Admin"
        }
    }

private fun adminListToneColor(kind: AdminOperationListKind, count: Int = 1): Color {
    if (count == 0) return PediloMuted
    return when (kind) {
        AdminOperationListKind.AllProblems -> PediloPink
        AdminOperationListKind.ProblemStoreNotResponding -> PediloOrange
        AdminOperationListKind.ProblemWithoutResponsible -> PediloCyan
        AdminOperationListKind.ProblemPaymentConflict -> PediloPink
        AdminOperationListKind.ProblemDriverIssue -> PediloWarning
        AdminOperationListKind.ProblemUserNotResponding -> PediloOrange
        AdminOperationListKind.ProblemDelayed -> PediloWarning
        AdminOperationListKind.ProblemUserClaim -> PediloPink
        AdminOperationListKind.ProblemOperationalReview -> PediloOrange
        AdminOperationListKind.ActiveWaitingStore,
        AdminOperationListKind.ActiveWaitingOperationalConfirmation -> PediloWarning
        AdminOperationListKind.ActiveWaitingDriver,
        AdminOperationListKind.DeliveryDriverAssigned -> PediloCyan
        AdminOperationListKind.AllPreparing,
        AdminOperationListKind.ActivePreparing,
        AdminOperationListKind.PreparingNormal,
        AdminOperationListKind.PreparingReadyForPickup,
        AdminOperationListKind.AcceptedByStore,
        AdminOperationListKind.AcceptedWaitingPreparation,
        AdminOperationListKind.AcceptedReadyToPrepare -> PediloGreen
        AdminOperationListKind.AllInDelivery,
        AdminOperationListKind.ActiveInDelivery,
        AdminOperationListKind.DeliveryPickedUp -> PediloCyan
        AdminOperationListKind.PreparingDelayed,
        AdminOperationListKind.DeliveryDelayed -> PediloWarning
        AdminOperationListKind.ClosedWithIncident,
        AdminOperationListKind.ClosedPostClaim,
        AdminOperationListKind.ClosedCancelledProblem -> PediloOrange
        else -> PediloMuted
    }
}

private fun List<AdminOrderSummary>.forOperationList(kind: AdminOperationListKind): List<AdminOrderSummary> {
    val signals = this.map { it to AdminOperationOrderSignals.from(it) }
    val todaySignals = signals.filter { (order, _) -> order.createdAtMillis.isAdminToday() }
    return when (kind) {
        AdminOperationListKind.AllAttention -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) in setOf(
                AdminOrderPrimaryPlacement.PROBLEM,
                AdminOrderPrimaryPlacement.UNCLASSIFIED,
            )
        }.map { it.first }
        AdminOperationListKind.AllPreparing -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.PREPARING
        }.map { it.first }
        AdminOperationListKind.AllInDelivery -> signals.filter { (order, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                (
                    AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.IN_DELIVERY ||
                        order.operationalStatus.contains("driver_assigned", ignoreCase = true) ||
                        order.operationalStatus.contains("repartidor asignado", ignoreCase = true) ||
                        order.operationalStatus.contains("picked_up", ignoreCase = true) ||
                        order.operationalStatus.contains("retirado", ignoreCase = true)
                    )
        }.map { it.first }
        AdminOperationListKind.AllProblems -> signals.filter { (order, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM ||
                order.financialStatus.contains("disputed", ignoreCase = true) ||
                order.financialStatus.contains("rejected", ignoreCase = true) ||
                order.financialStatus.contains("conflict", ignoreCase = true) ||
                order.financialStatus.contains("conflicto", ignoreCase = true)
        }.map { it.first }
        AdminOperationListKind.AllBlocked -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM &&
                AdminOperationOrderClassification.problemBucket(s) in setOf(
                    AdminProblemOrdersBucket.STORE_NOT_RESPONDING,
                    AdminProblemOrdersBucket.DELAYED,
                    AdminProblemOrdersBucket.WITHOUT_RESPONSIBLE,
                    AdminProblemOrdersBucket.OPERATIONAL_REVIEW,
                )
        }.map { it.first }
        AdminOperationListKind.AllClosed -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) in setOf(
                AdminOrderPrimaryPlacement.FINISHED,
                AdminOrderPrimaryPlacement.CANCELLED,
            )
        }.map { it.first }
        AdminOperationListKind.TodayAll -> todaySignals.map { it.first }
        AdminOperationListKind.TodayActive -> todaySignals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE
        }.map { it.first }
        AdminOperationListKind.TodayProblems -> todaySignals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM
        }.map { it.first }
        AdminOperationListKind.TodayClosed -> todaySignals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) in setOf(
                AdminOrderPrimaryPlacement.FINISHED,
                AdminOrderPrimaryPlacement.CANCELLED,
            )
        }.map { it.first }
        AdminOperationListKind.TodayReview -> todaySignals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.UNCLASSIFIED
        }.map { it.first }
        AdminOperationListKind.Unclassified -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.UNCLASSIFIED
        }.map { it.first }
        AdminOperationListKind.ClosedFinished -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.FINISHED
        }.map { it.first }
        AdminOperationListKind.ClosedCancelled -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.CANCELLED
        }.map { it.first }
        AdminOperationListKind.ActiveWaitingStore -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.WAITING_STORE
        }.map { it.first }
        AdminOperationListKind.ActivePreparing -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.PREPARING
        }.map { it.first }
        AdminOperationListKind.ActiveWaitingDriver -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.WAITING_DRIVER
        }.map { it.first }
        AdminOperationListKind.ActiveInDelivery -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.IN_DELIVERY
        }.map { it.first }
        AdminOperationListKind.ActiveWaitingOperationalConfirmation -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.REVIEW_STATE
        }.map { it.first }
        AdminOperationListKind.ActiveReviewState -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                (
                    AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.REVIEW_STATE ||
                        s.operationalStatus.contains("local" + "_accepted", ignoreCase = true) ||
                        s.operationalStatus.contains("acept", ignoreCase = true)
                    )
        }.map { it.first }
        AdminOperationListKind.ProblemStoreNotResponding -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM &&
                AdminOperationOrderClassification.problemBucket(s) == AdminProblemOrdersBucket.STORE_NOT_RESPONDING
        }.map { it.first }
        AdminOperationListKind.ProblemUserClaim -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM &&
                AdminOperationOrderClassification.problemBucket(s) == AdminProblemOrdersBucket.CUSTOMER_CLAIM
        }.map { it.first }
        AdminOperationListKind.ProblemDelayed -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM &&
                AdminOperationOrderClassification.problemBucket(s) == AdminProblemOrdersBucket.DELAYED
        }.map { it.first }
        AdminOperationListKind.ProblemWithoutResponsible -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM &&
                AdminOperationOrderClassification.problemBucket(s) == AdminProblemOrdersBucket.WITHOUT_RESPONSIBLE
        }.map { it.first }
        AdminOperationListKind.ProblemPaymentConflict -> signals.filter { (order, _) ->
            order.financialStatus.contains("disputed", ignoreCase = true) ||
                order.financialStatus.contains("rejected", ignoreCase = true) ||
                order.financialStatus.contains("conflict", ignoreCase = true) ||
                order.financialStatus.contains("conflicto", ignoreCase = true)
        }.map { it.first }
        AdminOperationListKind.ProblemDriverIssue -> signals.filter { (order, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM &&
                (
                    order.publicStatus.contains("repartidor", ignoreCase = true) ||
                        order.operationalStatus.contains("driver_issue", ignoreCase = true) ||
                        order.operationalStatus.contains("repartidor report", ignoreCase = true)
                    )
        }.map { it.first }
        AdminOperationListKind.ProblemUserNotResponding -> signals.filter { (order, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM &&
                (
                    order.publicStatus.contains("cliente no responde", ignoreCase = true) ||
                        order.operationalStatus.contains("cliente no responde", ignoreCase = true)
                    )
        }.map { it.first }
        AdminOperationListKind.ProblemOperationalReview -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.PROBLEM &&
                AdminOperationOrderClassification.problemBucket(s) == AdminProblemOrdersBucket.OPERATIONAL_REVIEW
        }.map { it.first }
        AdminOperationListKind.AcceptedByStore,
        AdminOperationListKind.AcceptedWaitingPreparation,
        AdminOperationListKind.AcceptedReadyToPrepare -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                (
                    s.operationalStatus.contains("local" + "_accepted", ignoreCase = true) ||
                        s.operationalStatus.contains("acept", ignoreCase = true) ||
                        AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.REVIEW_STATE
                    )
        }.map { it.first }
        AdminOperationListKind.PreparingNormal -> signals.filter { (order, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                AdminOperationOrderClassification.activeBucket(s) == AdminActiveOrdersBucket.PREPARING &&
                !order.operationalStatus.contains("ready", ignoreCase = true) &&
                !AdminOperationOrderClassification.hasRealDelaySignal(s)
        }.map { it.first }
        AdminOperationListKind.PreparingReadyForPickup -> signals.filter { (order, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                (
                    order.operationalStatus.contains("ready_for_pickup", ignoreCase = true) ||
                        order.operationalStatus.contains("listo", ignoreCase = true)
                    )
        }.map { it.first }
        AdminOperationListKind.PreparingDelayed -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.hasRealDelaySignal(s) &&
                s.operationalStatus.contains("prepar", ignoreCase = true)
        }.map { it.first }
        AdminOperationListKind.DeliveryDriverAssigned -> signals.filter { (order, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                (
                    order.operationalStatus.contains("driver_assigned", ignoreCase = true) ||
                        order.operationalStatus.contains("repartidor asignado", ignoreCase = true)
                    )
        }.map { it.first }
        AdminOperationListKind.DeliveryPickedUp -> signals.filter { (order, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.ACTIVE &&
                (
                    order.operationalStatus.contains("picked_up", ignoreCase = true) ||
                        order.operationalStatus.contains("retirado", ignoreCase = true)
                    )
        }.map { it.first }
        AdminOperationListKind.DeliveryDelayed -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.hasRealDelaySignal(s) &&
                (
                    s.operationalStatus.contains("entrega", ignoreCase = true) ||
                        s.operationalStatus.contains("delivery", ignoreCase = true)
                    )
        }.map { it.first }
        AdminOperationListKind.ClosedDelivered -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.FINISHED &&
                !s.activeIncident &&
                !s.needsAttention
        }.map { it.first }
        AdminOperationListKind.ClosedCancelledProblem -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) == AdminOrderPrimaryPlacement.CANCELLED
        }.map { it.first }
        AdminOperationListKind.ClosedWithIncident -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) in setOf(
                AdminOrderPrimaryPlacement.FINISHED,
                AdminOrderPrimaryPlacement.CANCELLED,
            ) && (s.activeIncident || s.needsAttention)
        }.map { it.first }
        AdminOperationListKind.ClosedPostClaim -> signals.filter { (_, s) ->
            AdminOperationOrderClassification.primaryPlacement(s) in setOf(
                AdminOrderPrimaryPlacement.FINISHED,
                AdminOrderPrimaryPlacement.CANCELLED,
            ) && (
                s.publicStatus.contains("reclamo", ignoreCase = true) ||
                    s.publicStatus.contains("problema", ignoreCase = true)
                )
        }.map { it.first }
        else -> emptyList()
    }.distinctBy { it.id }
}

private fun List<AdminOrderSummary>.forPrimaryPlacement(
    placement: AdminOrderPrimaryPlacement,
): List<AdminOrderSummary> =
    filter {
        AdminOperationOrderClassification.primaryPlacement(AdminOperationOrderSignals.from(it)) == placement
    }.distinctBy { it.id }

private fun List<AdminOrderSummary>.forPrimaryPlacements(
    vararg placements: AdminOrderPrimaryPlacement,
): List<AdminOrderSummary> =
    filter {
        AdminOperationOrderClassification.primaryPlacement(AdminOperationOrderSignals.from(it)) in placements
    }.distinctBy { it.id }

private fun Long?.isAdminToday(): Boolean {
    if (this == null) return false
    val now = Calendar.getInstance()
    val date = Calendar.getInstance().apply { timeInMillis = this@isAdminToday }
    return now.get(Calendar.ERA) == date.get(Calendar.ERA) &&
        now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}

private fun AdminOrderPrimaryPlacement.adminPlacementLabel(): String =
    when (this) {
        AdminOrderPrimaryPlacement.PROBLEM -> "Con problemas"
        AdminOrderPrimaryPlacement.ACTIVE -> "Activo"
        AdminOrderPrimaryPlacement.FINISHED -> "Finalizado"
        AdminOrderPrimaryPlacement.CANCELLED -> "Cancelado"
        AdminOrderPrimaryPlacement.UNCLASSIFIED -> "Revisar pedido"
    }
