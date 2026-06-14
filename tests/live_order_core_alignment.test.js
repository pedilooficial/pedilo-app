const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");
const vm = require("node:vm");

function loadLiveOrderInternals() {
  const source = fs.readFileSync("functions/index.js", "utf8");
  const wrapped = `${source}
module.exports = {
  LIVE_ACTIONS,
  LIVE_ORDER_STATES,
  LOCAL_SOURCE,
  PLUS_BUY_SOURCE,
  PLUS_PICKUP_SHIPPING_SOURCE,
  INITIAL_TIMEOUT_POLICY,
  INITIAL_FALLBACK_POLICY,
  liveBirthContract,
  liveOrderState,
  allowedLiveActions,
  liveActionEffect,
  liveOrderEvent,
  liveOrderSnapshot,
  publicTrackingResponse,
  validateLiveActor,
  validateExpectedVersion,
  validateLiveTransition,
};`;

  const sandbox = {
    module: {exports: {}},
    exports: {},
    require(id) {
      if (id === "node:crypto") return require("node:crypto");
      if (id === "firebase-functions/v2/https") {
        return {
          onCall: (_config, handler) => handler,
          HttpsError: class HttpsError extends Error {
            constructor(code, message) {
              super(message);
              this.code = code;
            }
          },
        };
      }
      if (id === "firebase-admin") {
        const firestore = function firestore() {
          return {};
        };
        firestore.FieldValue = {
          serverTimestamp() {
            return {serverTimestamp: true};
          },
        };
        return {
          initializeApp() {},
          firestore,
        };
      }
      throw new Error(`Unexpected require: ${id}`);
    },
  };

  vm.runInNewContext(wrapped, sandbox, {filename: "functions/index.js"});
  return sandbox.module.exports;
}

function bornLocal(api, extra = {}) {
  return api.liveOrderState({
    ...api.liveBirthContract({
      orderType: "local_order",
      source: api.LOCAL_SOURCE,
      idempotencyKey: "ord_core_alignment",
      trackingNumber: "PDL-B00001",
      snapshot: {
        orderType: "local_order",
        source: api.LOCAL_SOURCE,
        items: [{name: "Empanadas", quantity: 2}],
      },
    }),
    storeId: "store-1",
    ...extra,
  });
}

function transition(api, current, action, actor, reason = "motivo operativo") {
  api.validateLiveActor(actor, current, action);
  api.validateExpectedVersion({expectedVersion: current.version}, current);
  api.validateLiveTransition(current, action);
  assert.ok(api.allowedLiveActions(current).includes(action));
  const effect = api.liveActionEffect({action, reason}, current, actor);
  return api.liveOrderState({
    ...current,
    ...effect.patch,
    version: current.version + 1,
  });
}

test("live order state contract lists current states and executable timeout fallback policies", () => {
  const api = loadLiveOrderInternals();
  assert.deepEqual(Array.from(api.LIVE_ORDER_STATES.initial), ["created"]);
  assert.ok(api.LIVE_ORDER_STATES.operational.includes("ready_for_pickup"));
  assert.ok(api.LIVE_ORDER_STATES.terminal.includes("delivered"));
  assert.ok(api.LIVE_ORDER_STATES.financial.includes("pending_review"));
  assert.ok(api.LIVE_ORDER_STATES.financial.includes("collect_on_delivery"));
  assert.ok(api.LIVE_ORDER_STATES.financial.includes("transfer_declared_pending"));
  assert.ok(api.LIVE_ORDER_STATES.financial.includes("paid_declared"));
  assert.ok(api.LIVE_ORDER_STATES.communication.includes("closed"));
  assert.ok(api.LIVE_ORDER_STATES.incident.includes("open"));
  assert.ok(api.LIVE_ORDER_STATES.archive.includes("archived"));

  assert.equal(api.INITIAL_TIMEOUT_POLICY.mode, "worker");
  assert.equal(api.INITIAL_TIMEOUT_POLICY.executable, true);
  assert.equal(api.INITIAL_FALLBACK_POLICY.mode, "worker");
  assert.equal(api.INITIAL_FALLBACK_POLICY.executable, true);
});

test("live birth carries required fields and normalized snapshots without losing original payload", () => {
  const api = loadLiveOrderInternals();
  const order = bornLocal(api);

  for (const field of [
    "orderType",
    "source",
    "status",
    "publicStatus",
    "operationalStatus",
    "financialStatus",
    "communicationStatus",
    "incidentStatus",
    "archiveStatus",
    "responsibleRole",
    "currentResponsibleRole",
    "assignedActorId",
    "assignedActorRole",
    "storeId",
    "trackingNumber",
    "publicOrderNumber",
    "version",
    "priority",
    "needsAttention",
    "nextAllowedActions",
    "initialSnapshot",
    "liveSnapshot",
    "timeoutPolicy",
    "fallbackPolicy",
  ]) {
    assert.ok(Object.hasOwn(order, field), field);
  }

  assert.equal(order.initialSnapshot.schemaVersion, 1);
  assert.equal(order.initialSnapshot.payload.items[0].name, "Empanadas");
  assert.equal(order.initialSnapshot.publicSummary, "Empanadas");
  assert.deepEqual(order.initialSnapshot, order.liveSnapshot);
});

test("valid live transitions enforce state order responsibility and terminal closure", () => {
  const api = loadLiveOrderInternals();
  const store = {uid: "store-1", role: "store"};
  const driver = {uid: "driver-1", role: "driver"};
  const otherDriver = {uid: "driver-2", role: "driver"};

  const created = bornLocal(api);
  assert.throws(
    () => api.validateLiveTransition(created, api.LIVE_ACTIONS.LOCAL_MARK_PREPARING),
    /aceptado/i,
  );
  assert.throws(
    () => api.validateLiveTransition(created, api.LIVE_ACTIONS.DRIVER_TAKE),
    /listo para retiro/i,
  );

  const accepted = transition(api, created, api.LIVE_ACTIONS.LOCAL_ACCEPT, store);
  const preparing = transition(api, accepted, api.LIVE_ACTIONS.LOCAL_MARK_PREPARING, store);
  const ready = transition(api, preparing, api.LIVE_ACTIONS.LOCAL_MARK_READY, store);

  assert.equal(ready.responsibleRole, "driver");
  assert.equal(ready.currentResponsibleRole, "driver");
  assert.deepEqual(Array.from(api.allowedLiveActions(ready)), [
    api.LIVE_ACTIONS.DRIVER_TAKE,
    api.LIVE_ACTIONS.OPEN_INCIDENT,
    api.LIVE_ACTIONS.CANCEL_ORDER,
    api.LIVE_ACTIONS.ADMIN_INTERVENE,
  ]);

  const assigned = transition(api, ready, api.LIVE_ACTIONS.DRIVER_TAKE, driver);
  assert.equal(assigned.assignedActorId, "driver-1");
  assert.equal(assigned.driverId, "driver-1");
  assert.throws(
    () => api.validateLiveActor(otherDriver, assigned, api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP),
    /no está asignado/i,
  );

  const pickedUp = transition(api, assigned, api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP, driver);
  assert.throws(
    () => api.validateLiveTransition(assigned, api.LIVE_ACTIONS.DRIVER_MARK_DELIVERED),
    /retirado/i,
  );
  const delivered = transition(api, pickedUp, api.LIVE_ACTIONS.DRIVER_MARK_DELIVERED, driver);

  assert.equal(delivered.status, "delivered");
  assert.equal(delivered.archiveStatus, "archived");
  assert.equal(delivered.communicationStatus, "closed");
  assert.deepEqual(Array.from(api.allowedLiveActions(delivered)), []);
  assert.throws(
    () => api.validateLiveTransition(delivered, api.LIVE_ACTIONS.CANCEL_ORDER),
    /cerrado/i,
  );
});

test("incident and admin intervention move responsibility to admin without reopening terminal orders", () => {
  const api = loadLiveOrderInternals();
  const store = {uid: "store-1", role: "store"};
  const admin = {uid: "admin-1", role: "admin"};
  const created = bornLocal(api);

  const incident = transition(api, created, api.LIVE_ACTIONS.OPEN_INCIDENT, store, "local sin stock");
  assert.equal(incident.activeIncident, true);
  assert.equal(incident.incidentStatus, "open");
  assert.equal(incident.currentResponsibleRole, "admin");
  assert.deepEqual(Array.from(api.allowedLiveActions(incident)), [
    api.LIVE_ACTIONS.RESOLVE_INCIDENT,
    api.LIVE_ACTIONS.CANCEL_ORDER,
    api.LIVE_ACTIONS.ADMIN_INTERVENE,
  ]);

  const resolved = transition(api, incident, api.LIVE_ACTIONS.RESOLVE_INCIDENT, admin, "resuelto por admin");
  assert.equal(resolved.activeIncident, false);
  assert.equal(resolved.incidentStatus, "resolved");
});

test("live order events carry minimum audit data and safe public tracking hides closed details", () => {
  const api = loadLiveOrderInternals();
  const current = bornLocal(api);
  const actor = {uid: "store-1", role: "store"};
  const effect = api.liveActionEffect({action: api.LIVE_ACTIONS.LOCAL_ACCEPT, reason: ""}, current, actor);
  const result = {
    orderId: "order-1",
    action: api.LIVE_ACTIONS.LOCAL_ACCEPT,
    status: "accepted",
    operationalStatus: "local_accepted",
    currentResponsibleRole: "store",
    archiveStatus: "live",
    version: current.version + 1,
  };
  const event = api.liveOrderEvent({
    clean: {orderId: "order-1", action: api.LIVE_ACTIONS.LOCAL_ACCEPT, actionId: "act_12345678", reason: ""},
    actor,
    current,
    result,
    summary: effect.eventSummary,
    now: {serverTimestamp: true},
  });

  assert.equal(event.previousStatus, "created");
  assert.equal(event.nextStatus, "accepted");
  assert.equal(event.previousVersion, 1);
  assert.equal(event.nextVersion, 2);
  assert.equal(event.audit.previousResponsibleRole, "admin");
  assert.equal(event.audit.nextResponsibleRole, "store");
  assert.equal(event.result.version, 2);

  const closedTracking = api.publicTrackingResponse({
    ...current,
    status: "cancelled",
    publicStatus: "Pedido cerrado",
    storeName: "Local privado",
    items: [{name: "Dato privado"}],
  }, "PDL-B00001");
  assert.equal(closedTracking.isClosed, true);
  assert.equal(closedTracking.storeName, "");
  assert.equal(closedTracking.summary, "");
});
