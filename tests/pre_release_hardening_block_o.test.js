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
  liveBirthContract,
  liveOrderState,
  liveActionEffect,
  communicationRecordsForOrder,
  assistedDecisionForOrder,
  assistedDecisionOrderPatch,
  buildOperationalHealthReport,
  publicTrackingResponse,
  cleanLiveActionPayload,
  cleanPublicClaimPayload,
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

function baseOrder(id = "ord_o_live") {
  const api = loadInternals();
  return {
    id,
    ...api.liveBirthContract({
      orderType: "local_order",
      source: "public_local",
      idempotencyKey: id,
      trackingNumber: "PDL-O10001",
      snapshot: {
        orderType: "local_order",
        source: "public_local",
        store: {id: "store-1", name: "Local Norte"},
        items: [{productId: "prod-1", name: "Empanadas", quantity: 2, unitPrice: 1000, total: 2000}],
        pricing: {total: 2000},
      },
    }),
    storeId: "store-1",
    storeName: "Local Norte",
    customer: {name: "Ana", phone: "+5491122334455", address: "Calle Real 123"},
    financialStatus: "pending_review",
    paymentMethod: "cash",
    collectionRequired: true,
    amountToCollect: 2000,
    total: 2000,
  };
}

test("cross regression order to communication to assisted decision to health stays coherent", () => {
  const api = loadInternals();
  const order = baseOrder();
  const communications = api.communicationRecordsForOrder({
    orderId: order.id,
    order,
    eventType: "order_created",
    triggeredByRole: "public_user",
    triggeredByActorId: "",
    sourceEventId: "initial",
    now: {serverTimestamp: true},
  });
  const decision = api.assistedDecisionForOrder({
    orderId: order.id,
    order,
    sourceEventId: "initial",
    scope: "order_created",
    now: {serverTimestamp: true},
  });
  const orderWithAi = {...order, ...api.assistedDecisionOrderPatch(decision)};
  const report = api.buildOperationalHealthReport({
    generatedAt: "2026-06-10T12:00:00.000Z",
    orders: [orderWithAi],
    publicClaims: [],
    orderRelated: {
      [order.id]: {
        events: [{id: "initial", type: "order_created", summary: "Pedido creado", actorRole: "public_user"}],
        incidents: [],
        claims: [],
        communications,
        aiDecisions: [decision],
      },
    },
  });

  assert.equal(report.metrics.liveOrders, 1);
  assert.equal(report.metrics.preparedCommunicationOrders, 1);
  assert.ok(communications.some((record) => record.channel === api.COMMUNICATION_CHANNELS.INTERNAL && record.status === api.COMMUNICATION_STATUSES.PREPARED));
  assert.ok(communications.some((record) => record.channel === api.COMMUNICATION_CHANNELS.WHATSAPP && record.status === api.COMMUNICATION_STATUSES.DISABLED));
  assert.equal(decision.providerStatus, api.AI_PROVIDER_STATUS_DISABLED);
  assert.equal(report.metrics.pendingAiSuggestionOrders, 1);
  assert.equal(report.auditSummary.correctiveActionsExecuted, false);
});

test("cross regression public claim to safe communication to assisted decision to health without order mutation", () => {
  const api = loadInternals();
  const order = baseOrder("ord_o_claim");
  const cleanClaim = api.cleanPublicClaimPayload({
    trackingNumber: "PDL-O10001",
    type: "missing_item",
    reason: "Faltó un producto",
    description: "No llegó una parte del pedido",
    customerName: "Ana",
    contact: "+5491122334455",
  });
  const communications = api.communicationRecordsForOrder({
    orderId: order.id,
    order,
    eventType: "public_claim_received",
    triggeredByRole: "public_user",
    triggeredByActorId: "",
    sourceEventId: "claim_evt",
    claimId: "claim-1",
    now: {serverTimestamp: true},
  });
  const decision = api.assistedDecisionForOrder({
    orderId: order.id,
    order: {...order, publicStatus: "Reclamo recibido", needsAttention: true},
    sourceEventId: "claim_evt",
    scope: "public_claim",
    claimId: "claim-1",
    now: {serverTimestamp: true},
  });
  const report = api.buildOperationalHealthReport({
    generatedAt: "2026-06-10T12:00:00.000Z",
    orders: [{...order, needsAttention: true, ...api.assistedDecisionOrderPatch(decision)}],
    publicClaims: [{id: "claim-1", orderId: order.id, status: cleanClaim.status || "received"}],
    orderRelated: {
      [order.id]: {
        events: [{id: "claim_evt", type: "public_claim_received", summary: "Reclamo recibido", actorRole: "public_user"}],
        incidents: [],
        claims: [{id: "claim-1", status: "received"}],
        communications,
        aiDecisions: [decision],
      },
    },
  });

  assert.equal(cleanClaim.trackingNumber, "PDL-O10001");
  assert.equal(order.status, "created");
  assert.equal(report.metrics.publicClaimsReceived, 1);
  assert.equal(report.metrics.linkedPublicClaims, 1);
  assert.equal(report.metrics.requiresAttention, 1);
  assert.equal(decision.classification, "claim_risk");
  assert.ok(communications.every((record) => record.status !== "sent"));
});

test("cross regression incident and failed communication stay safe for public tracking and visible to health", () => {
  const api = loadInternals();
  const order = baseOrder("ord_o_incident");
  const effect = api.liveActionEffect(
    {action: api.LIVE_ACTIONS.OPEN_INCIDENT, reason: "demora operativa"},
    api.liveOrderState(order),
    {uid: "store-1", role: "store"},
  );
  const incidentOrder = {...order, ...effect.patch};
  const communications = api.communicationRecordsForOrder({
    orderId: order.id,
    order: incidentOrder,
    eventType: api.LIVE_ACTIONS.OPEN_INCIDENT,
    triggeredByRole: "store",
    triggeredByActorId: "store-1",
    sourceEventId: "act_incident",
    incidentId: "act_incident",
    now: {serverTimestamp: true},
  });
  const failedOrder = {...incidentOrder, communicationStatus: "failed"};
  const tracking = api.publicTrackingResponse({
    ...failedOrder,
    failureReason: "internal provider stack",
    actorUid: "store-1",
  }, "PDL-O10001");
  const report = api.buildOperationalHealthReport({
    generatedAt: "2026-06-10T12:00:00.000Z",
    orders: [failedOrder],
    publicClaims: [],
    orderRelated: {
      [order.id]: {
        events: [{id: "act_incident", type: "open_incident", summary: effect.eventSummary, actorRole: "store"}],
        incidents: [{id: "act_incident", status: "open"}],
        claims: [],
        communications,
        aiDecisions: [],
      },
    },
  });

  assert.equal(incidentOrder.activeIncident, true);
  assert.equal(incidentOrder.incidentStatus, "open");
  assert.equal(tracking.status, "UNDER_REVIEW");
  assert.equal(JSON.stringify(tracking).includes("provider"), false);
  assert.equal(JSON.stringify(tracking).includes("store-1"), false);
  assert.equal(report.metrics.openIncidentOrders, 1);
  assert.equal(report.metrics.failedCommunicationOrders, 1);
  assert.ok(report.alerts.some((alert) => alert.warningCode === "FAILED_COMMUNICATION_WITHOUT_RECORD"));
});

test("cross regression cancellation keeps audit financial review and closed public tracking", () => {
  const api = loadInternals();
  const current = {
    ...api.liveOrderState(baseOrder("ord_o_cancel")),
    financialStatus: "collect_on_delivery",
    collectionRequired: true,
    amountToCollect: 2000,
    publicStatus: "Pedido recibido",
  };
  const effect = api.liveActionEffect(
    {action: api.LIVE_ACTIONS.CANCEL_ORDER, reason: "usuario solicitó cancelar"},
    current,
    {uid: "admin-1", role: "admin"},
  );
  const cancelled = {...baseOrder("ord_o_cancel"), ...effect.patch};
  const tracking = api.publicTrackingResponse(cancelled, "PDL-O10001");
  const report = api.buildOperationalHealthReport({
    generatedAt: "2026-06-10T12:00:00.000Z",
    orders: [cancelled],
    publicClaims: [],
    orderRelated: {
      [cancelled.id]: {
        events: [{id: "evt_cancel", type: "cancel_order", summary: effect.eventSummary, actorRole: "admin", previousStatus: "created", nextStatus: "cancelled"}],
        incidents: [],
        claims: [],
        communications: [],
        aiDecisions: [],
      },
    },
  });

  assert.equal(cancelled.status, "cancelled");
  assert.equal(cancelled.archiveStatus, "archived");
  assert.equal(cancelled.financialReviewRequired, true);
  assert.match(cancelled.financialReviewNote, /revisión financiera mínima/);
  assert.equal(tracking.isClosed, true);
  assert.equal(tracking.publicStatus, "Pedido cerrado");
  assert.equal(report.metrics.cancelledOrders, 1);
  assert.equal(report.metrics.financialReviewOrders, 1);
  assert.equal(report.criticalEvents[0].type, "cancel_order");
});

test("rules and source hardening keep roles payload versions and direct writes protected", () => {
  const rules = read("firestore.rules");
  const functions = read("functions/index.js");

  assert.match(rules, /function operatorActive\(\)[\s\S]*?data\.active == true/);
  assert.match(rules, /isAdmin\(\)/);
  assert.match(rules, /order\.storeId == request\.auth\.uid/);
  assert.match(rules, /order\.driverId == request\.auth\.uid/);
  assert.match(rules, /order\.nextAllowedActions\.hasAny\(\["driver_take"\]\)/);
  assert.match(rules, /match \/orders\/\{orderId\}[\s\S]*?allow create, update, delete: if false/);
  assert.match(rules, /match \/events\/\{eventId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/public_claims\/\{claimId\}[\s\S]*?allow read: if isAdmin\(\)/);
  assert.match(rules, /match \/users\/\{userId\}[\s\S]*?allow create, update, delete: if isAdmin\(\)/);

  assert.match(functions, /requireOperationalActor\(request\)/);
  assert.match(functions, /userSnap\.get\("active"\) !== true/);
  assert.match(functions, /validateExpectedVersion\(clean, current\)/);
  assert.match(functions, /allowedLiveActions\(current\)/);
  assert.match(functions, /if \(eventSnap\.exists\)/);

  const api = loadInternals();
  assert.throws(
    () => api.cleanLiveActionPayload({orderId: "ord_o_cancel", action: api.LIVE_ACTIONS.CANCEL_ORDER, expectedVersion: 1, reason: "x"}, "store-1"),
    /motivo operativo claro/,
  );
  assert.throws(
    () => api.cleanLiveActionPayload({orderId: "ord_o_cancel", action: api.LIVE_ACTIONS.DRIVER_TAKE, reason: ""}, "driver-1"),
    /versión esperada/,
  );
});

test("anti demo production provider and fake sent claims remain absent from runtime", () => {
  const runtime = [
    "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt",
    "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt",
    "app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt",
    "app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt",
    "functions/index.js",
    "README.md",
    "firebase.json",
  ].map(read).join("\n");

  assert.doesNotMatch(runtime, /WhatsApp enviado|push enviado|notificación enviada|IA externa activa|pago confirmado|producción lista|Google Play listo/i);
  assert.doesNotMatch(runtime, /twilio|meta graph|openai|anthropic|gemini|firebaseMessaging\.send/i);
  assert.doesNotMatch(runtime, /firebase deploy|bundleRelease|assembleRelease|play console/i);
  assert.match(runtime, /Tarjeta no está disponible: no hay pasarela de pago activa/);
  assert.match(runtime, /No secure WhatsApp provider is configured/);
});

test("synthetic local load computes health for 1000 orders without firebase writes", () => {
  const api = loadInternals();
  const orders = Array.from({length: 1000}, (_, index) => {
    const status = index % 10 === 0 ? "cancelled" : index % 5 === 0 ? "delivered" : "preparing";
    return {
      id: `ord_load_${index}`,
      status,
      archiveStatus: status === "preparing" ? "live" : "archived",
      nextAllowedActions: status === "preparing" ? ["local_mark_ready"] : [],
      activeIncident: index % 20 === 0,
      incidentStatus: index % 20 === 0 ? "open" : "none",
      communicationStatus: index % 25 === 0 ? "failed" : "prepared",
      financialStatus: index % 7 === 0 ? "transfer_declared_pending" : "collect_on_delivery",
      needsAttention: index % 20 === 0,
      aiRequiresHumanReview: index % 13 === 0,
      aiRiskLevel: index % 13 === 0 ? "high" : "low",
      currentResponsibleRole: "store",
      source: "public_local",
      storeId: "store-1",
      collectionRequired: true,
      amountToCollect: 1500,
      total: 1500,
    };
  });
  const orderRelated = Object.fromEntries(orders.map((order, index) => [order.id, {
    events: index % 20 === 0 ? [{id: `evt_${index}`, type: "open_incident", summary: "Carga local", actorRole: "store"}] : [],
    incidents: index % 20 === 0 ? [{id: `inc_${index}`, status: "open"}] : [],
    claims: [],
    communications: [{id: `comm_${index}`, status: order.communicationStatus}],
    aiDecisions: order.aiRequiresHumanReview ? [{id: `aid_${index}`, status: "suggested"}] : [],
  }]));
  const report = api.buildOperationalHealthReport({
    generatedAt: "2026-06-10T12:00:00.000Z",
    orders,
    publicClaims: [],
    orderRelated,
  });

  assert.equal(report.metrics.liveOrders, 800);
  assert.equal(report.metrics.cancelledOrders, 100);
  assert.equal(report.metrics.closedOrders, 200);
  assert.equal(report.metrics.failedCommunicationOrders, 40);
  assert.equal(report.metrics.openIncidentOrders, 50);
  assert.equal(report.metrics.pendingAiSuggestionOrders, 77);
  assert.equal(report.auditSummary.correctiveActionsExecuted, false);
  assert.equal(orders[0].status, "cancelled");
});
