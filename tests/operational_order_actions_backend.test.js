const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");

const functionsPath = "functions/index.js";
const liveModelPath = "app/src/main/java/com/pedilo/app/core/model/LiveOrderContract.kt";
const adminUiPath = "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt";
const publicUiRoot = "app/src/main/java/com/pedilo/app/ui/publicuser";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("operateLiveOrder is the backend authority for operational actions", () => {
  const source = read(functionsPath);
  const callable = source.slice(
    source.indexOf("exports.operateLiveOrder"),
    source.indexOf("async function requireOperationalActor"),
  );

  assert.match(callable, /exports\.operateLiveOrder/);
  assert.match(callable, /requireOperationalActor\(request\)/);
  assert.match(callable, /cleanLiveActionPayload\(request\.data \|\| \{\}, actor\.uid\)/);
  assert.match(callable, /db\.runTransaction/);
  assert.match(callable, /eventRef = orderRef\.collection\("events"\)\.doc\(clean\.actionId\)/);
  assert.match(callable, /if \(eventSnap\.exists\)/);
  assert.match(callable, /validateExistingLiveEventReplay\(eventSnap, actor, clean\)/);
  assert.match(callable, /\{\.\.\.savedResult, idempotent: true\}/);
  assert.match(callable, /validateLiveActor\(actor, current, clean\.action\)/);
  assert.match(callable, /validateExpectedVersion\(clean, current\)/);
  assert.match(callable, /allowedLiveActions\(current\)/);
  assert.match(callable, /liveActionEffect\(clean, current, actor\)/);
  assert.match(callable, /tx\.update\(orderRef, \{/);
  assert.match(callable, /assistedDecisionOrderPatch\(assistedDecision\)/);
  assert.match(callable, /tx\.create\(eventRef/);
  assert.match(callable, /nextAllowedActions/);
  assert.match(callable, /writeAssistedDecision\(tx, orderRef, assistedDecision\)/);
});

test("operational actions include local driver admin incident and cancel minimums", () => {
  const source = read(functionsPath);
  const actions = source.slice(
    source.indexOf("const LIVE_ACTIONS"),
    source.indexOf("const TERMINAL_STATUSES"),
  );

  for (const action of [
    "local_accept",
    "local_reject",
    "local_mark_preparing",
    "local_mark_ready",
    "store_driver_request",
    "driver_take",
    "driver_mark_picked_up",
    "driver_mark_delivered",
    "cancel_order",
    "open_incident",
    "resolve_incident",
    "admin_intervene",
  ]) {
    assert.match(actions, new RegExp(action));
  }
});

test("operational callable validates auth role active profile and action payload", () => {
  const source = read(functionsPath);
  const actor = source.slice(
    source.indexOf("async function requireOperationalActor"),
    source.indexOf("function cleanLiveActionPayload"),
  );
  const payload = source.slice(
    source.indexOf("function cleanLiveActionPayload"),
    source.indexOf("function liveActionId"),
  );

  assert.match(actor, /request\.auth && request\.auth\.uid/);
  assert.match(actor, /collection\("users"\)\.doc\(uid\)\.get\(\)/);
  assert.match(actor, /OPERATIONAL_ROLES\.includes\(role\)/);
  assert.match(actor, /userSnap\.get\("active"\) !== true/);
  assert.match(actor, /driverCapacity: role === "driver" \? cleanDriverCapacity\(userSnap\.get\("driverCapacity"\) \|\| userSnap\.get\("maxActiveOrders"\)\) : 0/);

  assert.match(payload, /Object\.values\(LIVE_ACTIONS\)\.includes\(action\)/);
  assert.match(payload, /expectedVersion === null/);
  assert.match(payload, /reason\.length < 4/);
  assert.match(payload, /isSafeDocumentId\(actionId\)/);
  assert.match(payload, /liveActionId/);
});

test("actor validation keeps roles inside their own operational boundary", () => {
  const source = read(functionsPath);
  const validate = source.slice(
    source.indexOf("function validateLiveActor"),
    source.indexOf("function liveOrderState"),
  );

  assert.match(validate, /actor\.role === "admin"/);
  assert.match(validate, /actor\.role === "store"/);
  assert.match(validate, /current\.storeId !== actor\.uid/);
  assert.match(validate, /LIVE_ACTIONS\.LOCAL_ACCEPT/);
  assert.match(validate, /LIVE_ACTIONS\.LOCAL_MARK_READY/);
  assert.match(validate, /LIVE_ACTIONS\.STORE_DRIVER_REQUEST/);
  assert.match(validate, /actor\.role === "driver"/);
  assert.match(validate, /LIVE_ACTIONS\.DRIVER_TAKE/);
  assert.match(validate, /LIVE_ACTIONS\.DRIVER_MARK_DELIVERED/);
  assert.match(validate, /current\.assignedActorId !== actor\.uid && current\.driverId !== actor\.uid/);
});

test("live action effects update separated states responsibility and terminal closure", () => {
  const source = read(functionsPath);
  const effects = source.slice(
    source.indexOf("function liveActionEffect"),
    source.indexOf("function livePatch"),
  );

  for (const status of [
    "local_accepted",
    "preparing",
    "ready_for_pickup",
    "waiting_driver",
    "driver_assigned",
    "picked_up",
    "delivered",
    "cancelled_by_",
    "incident_open",
    "incident_resolved",
    "admin_intervention",
  ]) {
    assert.match(effects, new RegExp(status));
  }

  assert.match(effects, /financialStatus|communicationStatus|incidentStatus|archiveStatus/s);
  assert.match(effects, /responsibleRole: "driver"/);
  assert.match(effects, /currentResponsibleRole: "admin"/);
  assert.match(effects, /assignedActorId: actor\.uid/);
  assert.match(effects, /driverId: actor\.uid/);
  assert.match(effects, /storeDriverRequestStatus: "open"/);
  assert.match(effects, /archiveStatus: "archived"/);
});

test("allowed live actions move through local and driver lifecycle", () => {
  const source = read(functionsPath);
  const allowed = source.slice(
    source.indexOf("function allowedLiveActions"),
    source.indexOf("function liveActionEffect"),
  );

  assert.match(allowed, /TERMINAL_STATUSES\.includes\(status\)/);
  assert.match(allowed, /state\.activeIncident/);
  assert.match(allowed, /status === STATUS && state\.source === LOCAL_SOURCE/);
  assert.match(allowed, /LIVE_ACTIONS\.LOCAL_ACCEPT/);
  assert.match(allowed, /status === "accepted"/);
  assert.match(allowed, /LIVE_ACTIONS\.LOCAL_MARK_PREPARING/);
  assert.match(allowed, /if \(status === "accepted"\) return \[LIVE_ACTIONS\.LOCAL_MARK_PREPARING, \.\.\.actions\];/);
  assert.match(allowed, /status === "preparing"/);
  assert.match(allowed, /LIVE_ACTIONS\.LOCAL_MARK_READY/);
  assert.match(allowed, /if \(status === "preparing"\) return \[LIVE_ACTIONS\.LOCAL_MARK_READY, LIVE_ACTIONS\.STORE_DRIVER_REQUEST, \.\.\.actions\];/);
  assert.match(allowed, /status === "ready_for_pickup"/);
  assert.match(allowed, /LIVE_ACTIONS\.DRIVER_TAKE/);
  assert.match(allowed, /status === "assigned_to_driver"/);
  assert.match(allowed, /LIVE_ACTIONS\.DRIVER_MARK_PICKED_UP/);
  assert.match(allowed, /status === "picked_up"/);
  assert.match(allowed, /LIVE_ACTIONS\.DRIVER_MARK_DELIVERED/);
});

test("core vocabulary exposes operational action names without wiring UI", () => {
  const liveModel = read(liveModelPath);
  const adminUi = read(adminUiPath);
  const publicFiles = fs.readdirSync(publicUiRoot)
    .filter((file) => file.endsWith(".kt"))
    .map((file) => read(`${publicUiRoot}/${file}`))
    .join("\n");

  for (const token of [
    "enum class LiveOrderAction",
    "LocalAccept(\"local_accept\")",
    "StoreDriverRequest(\"store_driver_request\")",
    "DriverTake(\"driver_take\")",
    "DriverMarkDelivered(\"driver_mark_delivered\")",
    "AdminIntervene(\"admin_intervene\")",
  ]) {
    assert.match(liveModel, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  }

  assert.doesNotMatch(adminUi, /operateLiveOrder|local_accept|driver_take|driver_mark_delivered/);
  assert.doesNotMatch(publicFiles, /operateLiveOrder|local_accept|driver_take|driver_mark_delivered/);
});

test("advanced operation modules have backend authority and audit records", () => {
  const source = read(functionsPath);

  assert.match(source, /exports\.processOperationalTimeouts/);
  assert.match(source, /shouldApplyTimeoutFallback/);
  assert.match(source, /timeout_admin_review/);
  assert.match(source, /exports\.closeDriverCashbox/);
  assert.match(source, /CASHBOX_CLOSURES/);
  assert.match(source, /cashboxStatus: "closed"/);
  assert.match(source, /assertDriverCapacity\(tx, actor, clean\.orderId\)/);
  assert.match(source, /Capacidad del repartidor completa/);
  assert.match(source, /driver_requests/);
  assert.match(source, /store_driver_request/);
});
