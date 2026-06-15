"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");

function read(path) {
  return fs.readFileSync(path, "utf8");
}

const app = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const teamAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseTeamAccessAdapter.kt";
const adminUi = "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt";
const adminAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt";
const publicTrackingModel = "app/src/main/java/com/pedilo/app/core/model/PublicTrackingState.kt";
const storeAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseStoreOrdersAdapter.kt";
const driverAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt";

test("admin access depends on active admin role and does not route other roles into admin", () => {
  const appSource = read(app);
  const adapterSource = read(teamAdapter);

  assert.match(adapterSource, /profile\.getBoolean\(ACTIVE\) != true/);
  assert.match(adapterSource, /TeamRole\.fromWire\(profile\.getString\(ROLE\)\.orEmpty\(\)\)/);
  assert.match(appSource, /role == TeamRole\.Admin/);
  assert.match(appSource, /TeamRole\.Admin -> AdminApp\(onSignOutConfirmed = onSignOutConfirmed\)/);
  assert.match(appSource, /TeamRole\.Local -> StoreApp\(onSignOutConfirmed = onSignOutConfirmed\)/);
  assert.match(appSource, /TeamRole\.Driver -> DriverApp\(onSignOutConfirmed = onSignOutConfirmed\)/);
});

test("admin operation reads real order models and mutates only through callable backend", () => {
  const ui = read(adminUi);
  const adapter = read(adminAdapter);

  assert.match(adapter, /db\.collection\(ORDERS\)\.addSnapshotListener/);
  assert.match(adapter, /getOrderDetailReadOnly/);
  assert.match(adapter, /getOrderEventsReadOnly/);
  assert.match(adapter, /getHttpsCallable\(OPERATE_LIVE_ORDER\)/);
  assert.match(adapter, /"expectedVersion" to expectedVersion/);
  assert.match(ui, /adminOrders\.executeLive/);
  assert.match(ui, /AdminLiveOrderActionRequest/);
  assert.match(ui, /expectedVersion = pending\.expectedVersion/);
  assert.match(ui, /loadOrderDetail\(pending\.orderId, force = true\)/);
  assert.doesNotMatch(ui, /collection\("orders"\).*\.set|collection\("orders"\).*\.add|collection\("orders"\).*\.update|collection\("orders"\).*\.delete|writeBatch|runTransaction/s);
});

test("admin order detail shows backend allowed actions and safe empty action state", () => {
  const ui = read(adminUi);
  const detail = ui.slice(
    ui.indexOf("private fun AdminOrderDetailScreen"),
    ui.indexOf("@Composable\nprivate fun AdminOrderSectionScreen"),
  );

  assert.match(detail, /allowedActions = detail\?\.nextAllowedActions \?: summary\?\.nextAllowedActions\.orEmpty\(\)/);
  assert.match(detail, /adminVisibleGuidedActions/);
  assert.match(detail, /visibleActions/);
  assert.match(detail, /AdminHumanSituationCard/);
  assert.match(detail, /AdminGuidedActionsPanel/);
  assert.match(detail, /AdminOrderDataSheet/);
  assert.match(detail, /AdminOrderCompactTimeline/);
  assert.match(detail, /verticalScroll\(rememberScrollState\(\)\)/);
  assert.match(ui, /AdminGuidedActionScreen[\s\S]*LazyColumn\(/);
  assert.match(ui, /Problema actual/);
  assert.match(ui, /Resolver ahora/);
  assert.match(ui, /Ticket del pedido/);
  assert.match(ui, /Canales/);
  assert.match(ui, /AdminWhatsAppActionButton/);
  assert.match(ui, /Confirmar resultado/);
  assert.doesNotMatch(detail, /Sin acción necesaria ahora|Revisá historial|Datos para decidir|No requerido ahora|Debe revisar|order_created|consultas|local_accept|driver_take|driver_mark_delivered|force_status|Datos secundarios|Volver a mesa|Actualizar acciones del pedido|Rama operativa|No aplica/);
});

test("admin configuration and role access are real persisted admin surfaces", () => {
  const ui = read(adminUi);
  const adapter = read(adminAdapter);

  assert.match(ui, /AdminRealConfigurationScreen/);
  assert.match(ui, /AdminRealRoleAccessScreen/);
  assert.match(ui, /AdminConfigurationPresentationCard/);
  assert.match(ui, /AdminShippingFeeScreen/);
  assert.match(ui, /AdminRainModeScreen/);
  assert.match(ui, /"Tarifa\\nenvío"/);
  assert.match(ui, /"Costo de envío"/);
  assert.match(ui, /"Adicional por distancia"/);
  assert.match(ui, /rainDeliveryFee/);
  assert.match(ui, /baseDeliveryFee/);
  assert.match(ui, /distanceSurcharge/);
  assert.match(ui, /Usuarios, roles y accesos/);
  assert.match(ui, /Activar acceso/);
  assert.match(adapter, /observeAdminConfig/);
  assert.match(adapter, /observeTeamUsers/);
  assert.match(adapter, /updateAdminConfig/);
  assert.match(adapter, /updateTeamUser/);
  assert.match(adapter, /db\.collection\(USERS\)/);
  assert.match(adapter, /db\.collection\(ADMIN_CONFIG\)/);
  assert.doesNotMatch(ui, /sin guardar datos reales|No se aplicaron cambios reales|Confirmar de forma visual|guardar borrador visual|confirmar visualmente|maqueta|prototipo|Alta de roles|ruta heredada/);
  assert.doesNotMatch(adapter, /collection\(ORDERS\)\.document\(.+\.update|collection\(ORDERS\)\.document\(.+\.set/);
});

test("admin errors are human controlled and public store driver compatibility remains protected", () => {
  const ui = read(adminUi);
  const publicModel = read(publicTrackingModel);
  const store = read(storeAdapter);
  const driver = read(driverAdapter);

  assert.match(ui, /Error operativo/);
  assert.match(ui, /adminHumanError\(\)/);
  assert.match(ui, /No pudimos actualizar el pedido/);
  assert.doesNotMatch(publicModel, /responsibleRole|currentResponsibleRole|assignedActorId|driverId|events|incidents|audit|payload/i);
  assert.match(store, /whereEqualTo\(STORE_ID, uid\)/);
  assert.match(driver, /whereEqualTo\(RESPONSIBLE_ROLE, DRIVER_ROLE\)/);
  assert.match(driver, /whereEqualTo\(ASSIGNED_ACTOR_ID, ""\)/);
});
