"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");
const vm = require("node:vm");

function read(path) {
  return fs.readFileSync(path, "utf8");
}

function loadInternals() {
  const source = read("functions/index.js");
  const wrapped = `${source}
module.exports = {
  LIVE_ACTIONS,
  COMMUNICATION_CHANNELS,
  COMMUNICATION_STATUSES,
  AI_PROVIDER_STATUS_DISABLED,
  cleanOrderPayload,
  cleanPlusOrderPayload,
  cleanPublicClaimPayload,
  cleanPublicCancellationPayload,
  cleanLiveActionPayload,
  liveBirthContract,
  liveOrderState,
  liveActionEffect,
  communicationRecordsForOrder,
  assistedDecisionForOrder,
  assistedDecisionOrderPatch,
  buildOperationalHealthReport,
  publicTrackingResponse,
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
        return {initializeApp() {}, firestore};
      }
      throw new Error(`Unexpected require: ${id}`);
    },
  };

  vm.runInNewContext(wrapped, sandbox, {filename: "functions/index.js"});
  return sandbox.module.exports;
}

function liveOrderFixture(id = "ord_final_audit") {
  const api = loadInternals();
  return {
    id,
    ...api.liveBirthContract({
      orderType: "local_order",
      source: "public_local",
      idempotencyKey: id,
      trackingNumber: "PDL-FINAL1",
      snapshot: {
        orderType: "local_order",
        source: "public_local",
        store: {id: "store-1", name: "Local Norte"},
        items: [{productId: "prod-1", name: "Producto real", quantity: 1, unitPrice: 1500, total: 1500}],
        pricing: {total: 1500},
      },
    }),
    storeId: "store-1",
    storeName: "Local Norte",
    customer: {name: "Ana", phone: "+5491122334455", address: "Calle Real 123"},
    paymentMethod: "cash",
    financialStatus: "collect_on_delivery",
    collectionRequired: true,
    amountToCollect: 1500,
    total: 1500,
  };
}

test("final pre-P critical callables remain present and payload validators reject weak inputs", () => {
  const source = read("functions/index.js");
  const api = loadInternals();

  for (const exported of [
    "createLocalOrder",
    "createPlusOrder",
    "getPublicOrderTracking",
    "requestPublicCancellation",
    "submitPublicClaim",
    "operateLiveOrder",
    "resolveAssistedDecision",
    "getOperationalHealth",
  ]) {
    assert.match(source, new RegExp(`exports\\.${exported}`), exported);
  }

  assert.throws(() => api.cleanOrderPayload({
    storeId: "store-1",
    storeName: "Local Norte",
    customer: {name: "Tu nombre", phone: "+5491122334455", address: "Calle Real 123"},
    paymentMethod: "cash",
    items: [{productId: "prod-1", name: "Producto real", quantity: 1}],
  }), /Revisá los datos/);
  assert.throws(() => api.cleanPlusOrderPayload({
    requestType: "buy",
    source: "public_plus_buy",
    contact: {name: "Ana", phone: "+5491122334455"},
    sourceReference: "Farmacia",
    destination: "Dirección",
    paymentMethod: "cash",
    items: [{name: "Producto real", detail: "1 unidad"}],
  }), /Revisá los datos/);
  assert.throws(() => api.cleanPublicClaimPayload({
    trackingNumber: "pedido",
    customerName: "Ana",
    contact: "+5491122334455",
    reason: "demora",
    description: "Pedido demorado",
  }), /número de pedido/);
  assert.throws(() => api.cleanPublicCancellationPayload({
    trackingNumber: "pedido",
    contact: "+5491122334455",
    reason: "cancelar",
  }), /número de pedido/);
  assert.throws(() => api.cleanLiveActionPayload({
    orderId: "ord_final_audit",
    action: api.LIVE_ACTIONS.OPEN_INCIDENT,
    expectedVersion: 1,
    reason: "x",
  }, "store-1"), /motivo operativo claro/);
  assert.doesNotMatch(source, /exports\.adminOrderAction|cleanAdminActionPayload|adminActionEffect|ADMIN_ACTIONS/);
});

test("final pre-P complete local and role flow preserves versions permissions audit and safe tracking", () => {
  const api = loadInternals();
  const born = liveOrderFixture();
  const accepted = {...born, ...api.liveActionEffect(
    {action: api.LIVE_ACTIONS.LOCAL_ACCEPT, reason: ""},
    api.liveOrderState(born),
    {uid: "store-1", role: "store"},
  ).patch, version: 2};
  const preparing = {...accepted, ...api.liveActionEffect(
    {action: api.LIVE_ACTIONS.LOCAL_MARK_PREPARING, reason: ""},
    api.liveOrderState(accepted),
    {uid: "store-1", role: "store"},
  ).patch, version: 3};
  const ready = {...preparing, ...api.liveActionEffect(
    {action: api.LIVE_ACTIONS.LOCAL_MARK_READY, reason: ""},
    api.liveOrderState(preparing),
    {uid: "store-1", role: "store"},
  ).patch, version: 4};
  const taken = {...ready, ...api.liveActionEffect(
    {action: api.LIVE_ACTIONS.DRIVER_TAKE, reason: ""},
    api.liveOrderState(ready),
    {uid: "driver-1", role: "driver"},
  ).patch, version: 5};
  const pickedUp = {...taken, ...api.liveActionEffect(
    {action: api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP, reason: ""},
    api.liveOrderState(taken),
    {uid: "driver-1", role: "driver"},
  ).patch, version: 6};
  const delivered = {...pickedUp, ...api.liveActionEffect(
    {action: api.LIVE_ACTIONS.DRIVER_MARK_DELIVERED, reason: ""},
    api.liveOrderState(pickedUp),
    {uid: "driver-1", role: "driver"},
  ).patch, version: 7};
  const tracking = api.publicTrackingResponse(delivered, "PDL-FINAL1");

  assert.equal(ready.currentResponsibleRole, "driver");
  assert.equal(taken.driverId, "driver-1");
  assert.equal(delivered.status, "delivered");
  assert.equal(delivered.archiveStatus, "archived");
  assert.equal(tracking.isClosed, true);
  assert.equal(tracking.storeName, "");
  assert.equal(tracking.summary, "");
  assert.equal(JSON.stringify(tracking).includes("driver-1"), false);
});

test("final pre-P incident claim communication AI health chain is observable but non-mutating", () => {
  const api = loadInternals();
  const born = liveOrderFixture("ord_final_health");
  const incidentEffect = api.liveActionEffect(
    {action: api.LIVE_ACTIONS.OPEN_INCIDENT, actionId: "act_final_incident", reason: "demora operativa"},
    api.liveOrderState(born),
    {uid: "store-1", role: "store"},
  );
  const incidentOrder = {...born, ...incidentEffect.patch};
  const communications = api.communicationRecordsForOrder({
    orderId: born.id,
    order: incidentOrder,
    eventType: api.LIVE_ACTIONS.OPEN_INCIDENT,
    triggeredByRole: "store",
    triggeredByActorId: "store-1",
    sourceEventId: "act_final_incident",
    incidentId: "act_final_incident",
    now: {serverTimestamp: true},
  });
  const decision = api.assistedDecisionForOrder({
    orderId: born.id,
    order: incidentOrder,
    sourceEventId: "act_final_incident",
    scope: "open_incident",
    incidentId: "act_final_incident",
    now: {serverTimestamp: true},
  });
  const report = api.buildOperationalHealthReport({
    generatedAt: "2026-06-10T12:00:00.000Z",
    orders: [{...incidentOrder, ...api.assistedDecisionOrderPatch(decision)}],
    publicClaims: [{id: "claim-final", orderId: born.id, status: "received"}],
    orderRelated: {
      [born.id]: {
        events: [{id: "act_final_incident", type: "open_incident", summary: incidentEffect.eventSummary, actorRole: "store"}],
        incidents: [{id: "act_final_incident", status: "open"}],
        claims: [{id: "claim-final", status: "received"}],
        communications,
        aiDecisions: [decision],
      },
    },
  });

  assert.ok(communications.some((record) => record.channel === api.COMMUNICATION_CHANNELS.WHATSAPP && record.status === api.COMMUNICATION_STATUSES.DISABLED));
  assert.ok(communications.every((record) => record.sentAt === null));
  assert.equal(decision.providerStatus, api.AI_PROVIDER_STATUS_DISABLED);
  assert.equal(decision.audit.noCriticalActionExecuted, true);
  assert.equal(report.metrics.openIncidentOrders, 1);
  assert.equal(report.metrics.pendingAiSuggestionOrders, 1);
  assert.equal(report.auditSummary.correctiveActionsExecuted, false);
});

test("final pre-P UI copy does not claim real external sending for sent communication status", () => {
  const joinedUi = [
    "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt",
    "app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt",
    "app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt",
  ].map(read).join("\n");

  assert.doesNotMatch(joinedUi, /Enviada por canal real/);
  assert.match(joinedUi, /Registrada como enviada; verificar canal/);
  assert.doesNotMatch(joinedUi, /WhatsApp enviado|push enviado|notificación enviada|pago confirmado|IA externa activa/i);
});

test("final pre-P source keeps production release deploy and direct order writes out of runtime", () => {
  const runtime = [
    "README.md",
    "firebase.json",
    "firestore.rules",
    "functions/index.js",
    "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt",
    "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt",
    "app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt",
    "app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt",
  ].map(read).join("\n");
  const android = fs.readdirSync("app/src/main/java/com/pedilo/app/core/firebase")
    .map((file) => read(`app/src/main/java/com/pedilo/app/core/firebase/${file}`))
    .join("\n");

  assert.doesNotMatch(runtime, /firebase deploy|bundleRelease|assembleRelease|Google Play listo|producción lista/i);
  assert.doesNotMatch(runtime, /twilio|meta graph|openai|anthropic|gemini|firebaseMessaging\.send/i);
  assert.doesNotMatch(android, /collection\(ORDERS\)[\s\S]{0,120}\.(set|update|delete|add)\(/);
});
