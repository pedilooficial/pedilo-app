package com.pedilo.app.ui.publicuser

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedilo.app.R
import com.pedilo.app.ui.admin.AdminApp
import com.pedilo.app.ui.driver.DriverApp
import com.pedilo.app.ui.store.StoreApp
import com.pedilo.app.core.model.TeamLoginRequest
import com.pedilo.app.core.model.TeamLoginResult
import com.pedilo.app.core.model.TeamRole
import com.pedilo.app.core.model.PublicOrderTicket
import com.pedilo.app.core.result.CoreError
import com.pedilo.app.core.result.CoreResult
import com.pedilo.app.core.runtime.publicLocalOrderUseCase
import com.pedilo.app.core.runtime.publicPlusOrderUseCase
import com.pedilo.app.core.runtime.publicTrackingUseCase
import com.pedilo.app.core.runtime.teamAccessPort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface PublicRoute {
    data object Home : PublicRoute
    data object TeamLogin : PublicRoute
    data class TeamRolePlaceholder(val role: TeamRole) : PublicRoute
    data object Plus : PublicRoute
    data object PlusBuy : PublicRoute
    data object PlusPickupShipping : PublicRoute
    data class PlusConfirmation(val request: PublicPlusRequest) : PublicRoute
    data class PlusTicket(val request: PublicPlusRequest, val ticket: PublicOrderTicket) : PublicRoute
    data object Shop : PublicRoute
    data object Conventions : PublicRoute
    data object ConventionsInfo : PublicRoute
    data object ConventionsClaim : PublicRoute
    data object ConventionsTrackingEntry : PublicRoute
    data class PublicTracking(val orderNumber: String, val current: PublicBottomDestination) : PublicRoute
    data class ShopSubcategory(val name: String) : PublicRoute
    data class ShopSearch(val query: String, val origin: PublicBottomDestination) : PublicRoute
    data class HomeListing(val title: String, val query: String) : PublicRoute
    data object Local : PublicRoute
    data class LocalProductDetail(val product: LocalProduct) : PublicRoute
    data object LocalCart : PublicRoute
    data object LocalData : PublicRoute
    data class LocalConfirmation(val orderData: LocalOrderData) : PublicRoute
    data class LocalTicket(val orderData: LocalOrderData, val ticket: PublicOrderTicket) : PublicRoute
}

@Composable
fun PublicApp() {
    PublicTheme {
        val context = LocalContext.current
        var showSplash by remember { mutableStateOf(true) }
        var catalogState by remember { mutableStateOf(PublicCatalogState()) }
        val scope = rememberCoroutineScope()
        val createLocalOrder = remember { publicLocalOrderUseCase() }
        val createPlusOrder = remember { publicPlusOrderUseCase() }
        val getPublicTracking = remember { publicTrackingUseCase() }
        val teamAccess = remember { teamAccessPort() }
        val teamSessionStore = remember { TeamSessionStore(context) }
        var activeTeamSession by remember { mutableStateOf(teamSessionStore.readPersistedSession()) }

        LaunchedEffect(Unit) {
            catalogState = withContext(Dispatchers.IO) { loadPublicCatalogState() }
        }

        if (showSplash) {
            PublicBrandSplash(onFinished = { showSplash = false })
            return@PublicTheme
        }

        var route by remember {
            mutableStateOf<PublicRoute>(
                activeTeamSession?.let { PublicRoute.TeamRolePlaceholder(it.role) } ?: PublicRoute.Home,
            )
        }
        val history = remember { mutableStateListOf<PublicRoute>() }
        val localCart = remember { mutableStateListOf<LocalCartItem>() }
        var selectedLocalStoreId by remember { mutableStateOf<String?>(null) }
        var localCategory by remember { mutableStateOf(LocalCategory.Featured) }
        var localOrderPlaced by remember { mutableStateOf(false) }
        var pendingLocalExit by remember { mutableStateOf<PublicRoute?>(null) }
        var isSubmittingLocalOrder by remember { mutableStateOf(false) }
        var localOrderError by remember { mutableStateOf<String?>(null) }
        var isSubmittingPlusOrder by remember { mutableStateOf(false) }
        var plusOrderError by remember { mutableStateOf<String?>(null) }
        var isTeamLoginLoading by remember { mutableStateOf(false) }
        var teamLoginError by remember { mutableStateOf<String?>(null) }

        fun isLocalRoute(target: PublicRoute): Boolean = when (target) {
            PublicRoute.Local,
            is PublicRoute.LocalProductDetail,
            PublicRoute.LocalCart,
            PublicRoute.LocalData,
            is PublicRoute.LocalConfirmation,
            is PublicRoute.LocalTicket -> true
            else -> false
        }

        fun hasActiveLocalCart(): Boolean = localCart.isNotEmpty() && !localOrderPlaced

        fun navigateTo(next: PublicRoute) {
            if (route != next) {
                history.add(route)
                route = next
            }
        }

        fun goHome() {
            if (isLocalRoute(route) && hasActiveLocalCart()) {
                pendingLocalExit = PublicRoute.Home
                return
            }
            history.clear()
            route = PublicRoute.Home
        }

        fun goShop() {
            if (isLocalRoute(route) && hasActiveLocalCart()) {
                pendingLocalExit = PublicRoute.Shop
                return
            }
            history.clear()
            route = PublicRoute.Shop
        }

        fun goPlus() {
            if (isLocalRoute(route) && hasActiveLocalCart()) {
                pendingLocalExit = PublicRoute.Plus
                return
            }
            navigateTo(PublicRoute.Plus)
        }

        fun confirmLocalExit() {
            val target = pendingLocalExit ?: PublicRoute.Home
            localCart.clear()
            localOrderPlaced = false
            pendingLocalExit = null
            history.clear()
            route = target
        }

        fun openLocalStore(storeId: String) {
            val cleanStoreId = storeId.trim()
            if (cleanStoreId.isBlank()) return
            if (selectedLocalStoreId != cleanStoreId) {
                localCart.clear()
                localOrderPlaced = false
                localCategory = LocalCategory.Featured
            }
            selectedLocalStoreId = cleanStoreId
            navigateTo(PublicRoute.Local)
        }

        fun logicalParent(current: PublicRoute): PublicRoute? = when (current) {
            PublicRoute.Home -> null
            PublicRoute.TeamLogin -> PublicRoute.Home
            is PublicRoute.TeamRolePlaceholder -> null
            PublicRoute.Shop -> PublicRoute.Home
            PublicRoute.Conventions -> PublicRoute.Home
            PublicRoute.ConventionsInfo,
            PublicRoute.ConventionsClaim,
            PublicRoute.ConventionsTrackingEntry -> PublicRoute.Conventions
            is PublicRoute.PublicTracking -> when (current.current) {
                PublicBottomDestination.Home -> PublicRoute.ConventionsTrackingEntry
                PublicBottomDestination.Shop -> PublicRoute.Shop
                PublicBottomDestination.Plus -> PublicRoute.Plus
            }
            PublicRoute.Plus -> history.lastOrNull()?.takeIf { it is PublicRoute.Shop || it is PublicRoute.Conventions || it is PublicRoute.Home } ?: PublicRoute.Home
            PublicRoute.PlusBuy,
            PublicRoute.PlusPickupShipping -> PublicRoute.Plus
            is PublicRoute.PlusConfirmation -> when (current.request.type) {
                PublicPlusRequestType.Buy -> PublicRoute.PlusBuy
                PublicPlusRequestType.PickupShipping -> PublicRoute.PlusPickupShipping
            }
            is PublicRoute.PlusTicket -> PublicRoute.Home
            is PublicRoute.ShopSubcategory -> PublicRoute.Shop
            is PublicRoute.ShopSearch -> when (current.origin) {
                PublicBottomDestination.Home -> PublicRoute.Home
                PublicBottomDestination.Shop -> PublicRoute.Shop
                PublicBottomDestination.Plus -> PublicRoute.Plus
            }
            is PublicRoute.HomeListing -> PublicRoute.Home
            PublicRoute.Local -> history.lastOrNull()?.takeIf { !isLocalRoute(it) } ?: PublicRoute.Shop
            is PublicRoute.LocalProductDetail -> PublicRoute.Local
            PublicRoute.LocalCart -> PublicRoute.Local
            PublicRoute.LocalData -> PublicRoute.LocalCart
            is PublicRoute.LocalConfirmation -> PublicRoute.LocalData
            is PublicRoute.LocalTicket -> PublicRoute.Home
        }

        fun handleNativeBack() {
            val parent = logicalParent(route)
            if (parent == null) {
                return
            }
            if (isLocalRoute(route) && hasActiveLocalCart() && !isLocalRoute(parent)) {
                pendingLocalExit = parent
            } else {
                route = parent
                history.clear()
            }
        }

        BackHandler(enabled = route != PublicRoute.Home) {
            handleNativeBack()
        }

        when (route) {
            PublicRoute.Home -> PublicHomeScreen(
                catalogState = catalogState,
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onTeam = {
                    teamLoginError = null
                    navigateTo(PublicRoute.TeamLogin)
                },
                onSearch = { navigateTo(PublicRoute.ShopSearch("", PublicBottomDestination.Home)) },
                onConventions = { navigateTo(PublicRoute.Conventions) },
                onCategory = { navigateTo(PublicRoute.HomeListing(it, it)) },
                onOffer = {
                    openLocalStore(it)
                },
                onAllOffers = { navigateTo(PublicRoute.HomeListing("Ofertas", "Ofertas")) },
                onLocal = {
                    openLocalStore(it)
                },
                onAllLocals = { navigateTo(PublicRoute.HomeListing("Nuevos locales", "Nuevos locales")) },
            )
            PublicRoute.TeamLogin -> TeamLoginScreen(
                isLoading = isTeamLoginLoading,
                errorMessage = teamLoginError,
                onLogin = { user, secret, keepSignedIn ->
                    if (!isTeamLoginLoading) {
                        isTeamLoginLoading = true
                        teamLoginError = null
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                teamAccess.login(
                                    TeamLoginRequest(
                                        user = user,
                                        secret = secret,
                                        keepSignedIn = keepSignedIn,
                                    ),
                                )
                            }
                            isTeamLoginLoading = false
                            when (result) {
                                is TeamLoginResult.Success -> {
                                    activeTeamSession = result.session
                                    teamSessionStore.save(result.session)
                                    history.clear()
                                    route = PublicRoute.TeamRolePlaceholder(result.session.role)
                                }
                                TeamLoginResult.NoAccess -> {
                                    teamLoginError = "No encontramos un acceso activo para este usuario."
                                }
                            }
                        }
                    }
                },
            )
            is PublicRoute.TeamRolePlaceholder -> {
                val role = (route as PublicRoute.TeamRolePlaceholder).role
                val onSignOutConfirmed = {
                    teamAccess.signOut()
                    activeTeamSession = null
                    teamSessionStore.clear()
                    history.clear()
                    route = PublicRoute.Home
                }
                if (role == TeamRole.Admin) {
                    AdminApp(onSignOutConfirmed = onSignOutConfirmed)
                } else {
                    when (role) {
                        TeamRole.Admin -> AdminApp(onSignOutConfirmed = onSignOutConfirmed)
                        TeamRole.Local -> StoreApp(onSignOutConfirmed = onSignOutConfirmed)
                        TeamRole.Driver -> DriverApp(onSignOutConfirmed = onSignOutConfirmed)
                    }
                }
            }
            PublicRoute.Plus -> PublicPlusChoiceScreen(
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onBuy = { navigateTo(PublicRoute.PlusBuy) },
                onPickupShipping = { navigateTo(PublicRoute.PlusPickupShipping) },
            )
            PublicRoute.PlusBuy -> PublicPlusBuyScreen(
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onContinue = {
                    plusOrderError = null
                    navigateTo(PublicRoute.PlusConfirmation(it))
                },
            )
            PublicRoute.PlusPickupShipping -> PublicPlusPickupShippingScreen(
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onContinue = {
                    plusOrderError = null
                    navigateTo(PublicRoute.PlusConfirmation(it))
                },
            )
            is PublicRoute.PlusConfirmation -> PublicPlusConfirmationScreen(
                request = (route as PublicRoute.PlusConfirmation).request,
                isSubmitting = isSubmittingPlusOrder,
                submitError = plusOrderError,
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onConfirm = {
                    if (!isSubmittingPlusOrder) {
                        isSubmittingPlusOrder = true
                        plusOrderError = null
                        val currentRoute = route as PublicRoute.PlusConfirmation
                        val draft = buildPlusOrderDraft(currentRoute.request)
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { createPlusOrder(draft) }
                            isSubmittingPlusOrder = false
                            when (result) {
                                is CoreResult.Success -> {
                                    navigateTo(PublicRoute.PlusTicket(currentRoute.request, result.value))
                                }
                                is CoreResult.Failure -> {
                                    plusOrderError = result.error.toLocalOrderMessage()
                                }
                            }
                        }
                    }
                },
            )
            is PublicRoute.PlusTicket -> PublicPlusTicketScreen(
                request = (route as PublicRoute.PlusTicket).request,
                ticket = (route as PublicRoute.PlusTicket).ticket,
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onTracking = { navigateTo(PublicRoute.PublicTracking(it, PublicBottomDestination.Plus)) },
            )
            PublicRoute.Shop -> PublicShopScreen(
                catalogState = catalogState,
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onSearch = { navigateTo(PublicRoute.ShopSearch(it, PublicBottomDestination.Shop)) },
                onTracking = { navigateTo(PublicRoute.PublicTracking(it, PublicBottomDestination.Shop)) },
                onSubcategory = { navigateTo(PublicRoute.ShopSubcategory(it)) },
                onViewLocal = {
                    catalogState.stores.firstOrNull()?.id?.let { storeId -> openLocalStore(storeId) }
                },
            )
            is PublicRoute.ShopSearch -> PublicShopSearchScreen(
                query = (route as PublicRoute.ShopSearch).query,
                current = (route as PublicRoute.ShopSearch).origin,
                catalogState = catalogState,
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onViewLocal = {
                    openLocalStore(it)
                },
            )
            is PublicRoute.ShopSubcategory -> PublicShopSubcategoryScreen(
                title = (route as PublicRoute.ShopSubcategory).name,
                catalogState = catalogState,
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onViewLocal = {
                    openLocalStore(it)
                },
            )
            is PublicRoute.HomeListing -> PublicShopSearchScreen(
                query = (route as PublicRoute.HomeListing).query,
                current = PublicBottomDestination.Home,
                titleOverride = (route as PublicRoute.HomeListing).title,
                catalogState = catalogState,
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onViewLocal = {
                    openLocalStore(it)
                },
            )
            PublicRoute.Conventions -> PublicConventionsScreen(
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onInfo = { navigateTo(PublicRoute.ConventionsInfo) },
                onClaim = { navigateTo(PublicRoute.ConventionsClaim) },
                onTracking = { navigateTo(PublicRoute.ConventionsTrackingEntry) },
            )
            PublicRoute.ConventionsInfo -> PublicConventionsInfoScreen(
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
            PublicRoute.ConventionsClaim -> PublicConventionsClaimScreen(
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
            PublicRoute.ConventionsTrackingEntry -> PublicConventionsTrackingEntryScreen(
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
                onSubmit = { navigateTo(PublicRoute.PublicTracking(it, PublicBottomDestination.Home)) },
            )
            is PublicRoute.PublicTracking -> PublicShopTrackingScreen(
                orderNumber = (route as PublicRoute.PublicTracking).orderNumber,
                current = (route as PublicRoute.PublicTracking).current,
                getTracking = getPublicTracking,
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
            PublicRoute.Local -> PublicLocalScreen(
                selectedCategory = localCategory,
                cartItems = localCart,
                catalogState = catalogState,
                selectedStoreId = selectedLocalStoreId,
                onCategory = { localCategory = it },
                onProduct = { navigateTo(PublicRoute.LocalProductDetail(it)) },
                onCart = { navigateTo(PublicRoute.LocalCart) },
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
            is PublicRoute.LocalProductDetail -> PublicLocalProductScreen(
                product = (route as PublicRoute.LocalProductDetail).product,
                onAddToCart = {
                    localOrderPlaced = false
                    localCart.add(it)
                    navigateTo(PublicRoute.LocalCart)
                },
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
            PublicRoute.LocalCart -> PublicLocalCartScreen(
                cartItems = localCart,
                onMoreProducts = { navigateTo(PublicRoute.Local) },
                onContinue = { navigateTo(PublicRoute.LocalData) },
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
            PublicRoute.LocalData -> PublicLocalDataScreen(
                cartItems = localCart,
                onContinue = { navigateTo(PublicRoute.LocalConfirmation(it)) },
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
            is PublicRoute.LocalConfirmation -> PublicLocalConfirmationScreen(
                cartItems = localCart,
                orderData = (route as PublicRoute.LocalConfirmation).orderData,
                isSubmitting = isSubmittingLocalOrder,
                submitError = localOrderError,
                onEditData = { navigateTo(PublicRoute.LocalData) },
                onConfirm = {
                    if (!isSubmittingLocalOrder) {
                        isSubmittingLocalOrder = true
                        localOrderError = null
                        val currentRoute = route as PublicRoute.LocalConfirmation
                        val draft = buildLocalOrderDraft(
                            store = catalogState.selectedStoreForOrder(selectedLocalStoreId, localCart),
                            cartItems = localCart,
                            orderData = currentRoute.orderData,
                        )
                        scope.launch {
                            val result = withContext(Dispatchers.IO) { createLocalOrder(draft) }
                            isSubmittingLocalOrder = false
                            when (result) {
                                is CoreResult.Success -> {
                                    localOrderPlaced = true
                                    navigateTo(PublicRoute.LocalTicket(currentRoute.orderData, result.value))
                                }
                                is CoreResult.Failure -> {
                                    localOrderError = result.error.toLocalOrderMessage()
                                }
                            }
                        }
                    }
                },
                onHome = { goHome() },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
            is PublicRoute.LocalTicket -> PublicLocalTicketScreen(
                cartItems = localCart,
                orderData = (route as PublicRoute.LocalTicket).orderData,
                ticket = (route as PublicRoute.LocalTicket).ticket,
                onTracking = { navigateTo(PublicRoute.PublicTracking(it, PublicBottomDestination.Shop)) },
                onHome = {
                    localCart.clear()
                    localOrderPlaced = false
                    goHome()
                },
                onPlus = { goPlus() },
                onShop = { goShop() },
            )
        }

        pendingLocalExit?.let {
            AlertDialog(
                onDismissRequest = { pendingLocalExit = null },
                title = {
                    Text("Salir del local")
                },
                text = {
                    Text("Tenés productos del local en el carrito. Si salís, se vacía este pedido.")
                },
                confirmButton = {
                    TextButton(onClick = { confirmLocalExit() }) {
                        Text("Salir y vaciar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingLocalExit = null }) {
                        Text("Seguir en el local")
                    }
                },
            )
        }
    }
}

private fun PublicCatalogState.selectedStoreForOrder(selectedStoreId: String?, cartItems: List<LocalCartItem>) =
    cartItems.firstOrNull()?.product?.storeId
        ?.let { storeId -> stores.firstOrNull { it.id == storeId } }
        ?: selectedStoreId?.let { storeId -> stores.firstOrNull { it.id == storeId } }
        ?: stores.firstOrNull()

private fun CoreError.toLocalOrderMessage(): String = when (this) {
    is CoreError.Validation -> "Revisá los datos del pedido antes de confirmar."
    is CoreError.Operational -> "No pudimos confirmar el pedido. Probá de nuevo."
    CoreError.IncompleteData -> "Faltan datos para confirmar el pedido."
    CoreError.NotAvailable -> "No pudimos confirmar el pedido. Probá de nuevo."
    CoreError.Unknown -> "Ocurrió un error al confirmar el pedido."
}

@Composable
private fun PublicBrandSplash(onFinished: () -> Unit) {
    val nativeSplashShowsLogo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var showLogo by remember { mutableStateOf(false) }
    var showWordmark by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (showLogo) 1f else 0.94f,
        animationSpec = tween(durationMillis = 760, easing = EaseInOutCubic),
        label = "pediloLogoScale",
    )

    LaunchedEffect(Unit) {
        if (!nativeSplashShowsLogo) {
            delay(120)
            showLogo = true
            delay(1130)
            showWordmark = true
            delay(400)
            showLogo = false
        } else {
            showWordmark = true
        }
        delay(1250)
        showWordmark = false
        delay(60)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PediloBg),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = showLogo,
            enter = fadeIn(animationSpec = tween(durationMillis = 620, easing = EaseInOutCubic)),
            exit = fadeOut(animationSpec = tween(durationMillis = 420, easing = EaseInOutCubic)),
        ) {
            Image(
                painter = painterResource(R.drawable.pedilo_logo_mark),
                contentDescription = "Pédilo!",
                modifier = Modifier
                    .size(154.dp)
                    .scale(logoScale),
            )
        }

        AnimatedVisibility(
            visible = showWordmark,
            enter = fadeIn(animationSpec = tween(durationMillis = 420, easing = EaseInOutCubic)),
            exit = fadeOut(animationSpec = tween(durationMillis = 320, easing = EaseInOutCubic)),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Pédilo!",
                    fontSize = 46.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        brush = Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrange, PediloOrangeDark)),
                        shadow = Shadow(PediloOrangeDark.copy(alpha = 0.55f), Offset(0f, 4f), 13f),
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "todos tus pedidos en un solo lugar",
                    color = PediloText,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PublicPhasePlaceholder(
    title: String,
    body: String,
    onHome: () -> Unit,
    onPlus: () -> Unit,
    onShop: () -> Unit,
) {
    PublicShell(
        current = if (title == "Tienda") PublicBottomDestination.Shop else PublicBottomDestination.Plus,
        onHome = onHome,
        onPlus = onPlus,
        onShop = onShop,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PediloBg)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Pédilo!",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(brush = Brush.verticalGradient(listOf(PediloWarning, PediloOrangeSoft, PediloOrangeDark))),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = title,
                color = PediloText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                color = PediloMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
