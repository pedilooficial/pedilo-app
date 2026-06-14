"use strict";

const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");

const functionsPath = "functions/index.js";
const modelPath = "app/src/main/java/com/pedilo/app/core/model/AdminOrderReadModels.kt";
const liveModelPath = "app/src/main/java/com/pedilo/app/core/model/LiveOrderContract.kt";
const adapterPath = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt";
const adminUiPath = "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt";
const rulesPath = "firestore.rules";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("admin operational core exposes responsible role and live actions only", () => {
  const model = read(modelPath);
  const liveModel = read(liveModelPath);

  for (const field of ["responsibleRole", "nextAllowedActions", "needsAttention", "activeIncident", "priority"]) {
    assert.match(model, new RegExp(`val ${field}`));
  }
  for (const action of [
    "cancel_order",
    "open_incident",
    "resolve_incident",
    "admin_intervene",
  ]) {
    assert.match(liveModel, new RegExp(action));
  }
  assert.doesNotMatch(model, /AdminOrderAction|mark_admin_reviewed|force_status|assign_responsible|clear_responsible/);
  assert.doesNotMatch(model, /Firebase|Firestore|androidx\.compose/);
});

test("admin live callable validates auth role state version and writes audit events", () => {
  const source = read(functionsPath);
  const callable = source.slice(
    source.indexOf("exports.operateLiveOrder"),
    source.indexOf("exports.adminUpdateTeamUser"),
  );

  assert.match(callable, /exports\.operateLiveOrder/);
  assert.match(source, /request\.auth/);
  assert.match(source, /collection\("users"\)\.doc\(uid\)/);
  assert.match(callable, /requireOperationalActor\(request\)/);
  assert.match(source, /userSnap\.get\("active"\) !== true/);
  assert.match(callable, /db\.runTransaction/);
  assert.match(callable, /validateExpectedVersion\(clean, current\)/);
  assert.match(callable, /allowedLiveActions\(current\)/);
  assert.match(callable, /orderRef\.collection\("events"\)/);
  assert.match(callable, /orderRef\.collection\("incidents"\)/);
  assert.match(callable, /nextAllowedActions/);
  assert.doesNotMatch(source, /exports\.adminOrderAction|cleanAdminActionPayload|adminActionEffect|allowedAdminActions|ADMIN_ACTIONS/);
});

test("admin can repair published nextAllowedActions through backend only", () => {
  const source = read(functionsPath);
  const adapter = read(adapterPath);
  const port = read("app/src/main/java/com/pedilo/app/core/port/AdminOrdersPort.kt");
  const usecase = read("app/src/main/java/com/pedilo/app/core/usecase/GetAdminOperationOrdersUseCase.kt");
  const ui = read(adminUiPath);
  const callable = source.slice(
    source.indexOf("exports.adminRecalculateOrderActions"),
    source.indexOf("exports.adminUpdateTeamUser"),
  );

  assert.match(callable, /exports\.adminRecalculateOrderActions/);
  assert.match(callable, /requireAdminActor\(request\)/);
  assert.match(callable, /allowedLiveActions\(current\)/);
  assert.match(callable, /nextAllowedActions: recalculatedActions/);
  assert.match(callable, /version: nextVersion/);
  assert.match(callable, /tx\.update\(orderRef/);
  assert.match(callable, /tx\.create\(eventRef/);
  assert.match(callable, /admin_recalculate_order_actions/);
  assert.match(adapter, /getHttpsCallable\(ADMIN_RECALCULATE_ORDER_ACTIONS\)/);
  assert.match(adapter, /ADMIN_RECALCULATE_ORDER_ACTIONS = "adminRecalculateOrderActions"/);
  assert.match(port, /recalculateOrderActions\(orderId: String\)/);
  assert.match(usecase, /recalculateActions\(orderId: String\)/);
  assert.match(ui, /adminOrders\.recalculateActions\(orderId\)/);
  assert.doesNotMatch(ui.slice(ui.indexOf("private fun AdminOrderDetailScreen"), ui.indexOf("private fun AdminOrderSectionScreen")), /Actualizar acciones del pedido/);
});

test("admin adapter observes orders and calls backend for mutations only", () => {
  const source = read(adapterPath);

  assert.match(source, /addSnapshotListener/);
  assert.match(source, /getHttpsCallable\(OPERATE_LIVE_ORDER\)/);
  assert.match(source, /getHttpsCallable\(ADMIN_RECALCULATE_ORDER_ACTIONS\)/);
  assert.match(source, /OPERATE_LIVE_ORDER = "operateLiveOrder"/);
  assert.match(source, /getOrderEventsReadOnly/);
  assert.match(source, /collection\(EVENTS\)/);
  assert.match(source, /LiveOrderAction\.fromWire/);
  assert.doesNotMatch(source, /fallbackAdminLiveActions|ifEmpty\s*\{\s*fallback|activeIncident.*ResolveIncident/s);
  assert.doesNotMatch(source, /AdminOrderOperations\.allowedActions/);
  assert.doesNotMatch(source, /ADMIN_ORDER_ACTION|adminOrderAction|executeAdminOrderAction|AdminOrderActionRequest/);
  assert.doesNotMatch(source, /\.set\(|\.update\(|\.delete\(|writeBatch|runTransaction/);
});

test("admin UI executes only backend-provided operational actions with confirmation", () => {
  const source = read(adminUiPath);
  const detailScreen = source.slice(
    source.indexOf("private fun AdminOrderDetailScreen"),
    source.indexOf("private fun AdminOrderSectionScreen"),
  );

  assert.match(source, /AdminLiveOrderActionRequest/);
  assert.match(source, /adminOrders\.executeLive/);
  assert.match(source, /OperationGuidedAction/);
  assert.match(source, /AdminGuidedActionScreen/);
  assert.match(source, /AdminGuidedResultPanel/);
  assert.match(source, /Confirmar resultado/);
  assert.match(source, /loadOrderDetail\(pending\.orderId, force = true\)/);
  assert.match(detailScreen, /allowedActions/);
  assert.match(detailScreen, /nextAllowedActions/);
  assert.match(detailScreen, /adminVisibleGuidedActions/);
  assert.match(detailScreen, /AdminGuidedActionsPanel/);
  assert.match(detailScreen, /AdminHumanSituationCard/);
  assert.match(source, /AdminOrderSectionScreen/);
  assert.match(source, /AdminOrderSection\.History -> listOf/);
  assert.match(source, /detail\?\.events\.orEmpty\(\)/);
  assert.doesNotMatch(detailScreen, /Más acciones|Motivo operativo|Acción o lectura|Datos secundarios|Rama operativa|No aplica/);
  assert.doesNotMatch(detailScreen, /local_accept|driver_take|driver_mark_delivered/);
  assert.doesNotMatch(source, /collection\("orders"\).*\.set|collection\("orders"\).*\.add|collection\("orders"\).*\.update|collection\("orders"\).*\.delete|runTransaction|writeBatch/s);
});

test("rules keep orders and audit subcollections protected from client writes", () => {
  const source = read(rulesPath);
  const ordersBlock = source.match(/match \/orders\/\{orderId\} \{[\s\S]*?\n    \}/)[0];

  assert.match(ordersBlock, /allow create, update, delete: if false/);
  assert.match(ordersBlock, /match \/events\/\{eventId\}/);
  assert.match(ordersBlock, /match \/incidents\/\{incidentId\}/);
  assert.match(ordersBlock, /allow write: if false/);
});
