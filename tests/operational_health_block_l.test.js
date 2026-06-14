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
  HEALTH_STATUSES,
  MODULE_HEALTH,
  buildOperationalHealthReport,
  operationalHealthMetrics,
  orderConsistencyWarnings,
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

function sampleHealthReport() {
  const api = loadInternals();
  return api.buildOperationalHealthReport({
    generatedAt: "2026-06-10T12:00:00.000Z",
    orders: [
      {
        id: "ord_live",
        status: "preparing",
        archiveStatus: "live",
        nextAllowedActions: ["local_mark_ready"],
        needsAttention: true,
        activeIncident: true,
        incidentStatus: "open",
        communicationStatus: "prepared",
        financialStatus: "pending_review",
        aiRequiresHumanReview: true,
        aiRiskLevel: "high",
        currentResponsibleRole: "store",
        source: "public_local",
        storeId: "store-1",
        collectionRequired: true,
        amountToCollect: 2500,
        paymentMethod: "cash",
        total: 2500,
      },
      {
        id: "ord_terminal_live",
        status: "delivered",
        archiveStatus: "live",
        nextAllowedActions: [],
        activeIncident: true,
        incidentStatus: "none",
        communicationStatus: "failed",
        aiRequiresHumanReview: true,
        financialReviewRequired: true,
        financialNotes: "",
        source: "public_local",
        storeId: "",
        collectionRequired: true,
        amountToCollect: 0,
        total: -1,
      },
      {
        id: "ord_cancelled",
        status: "cancelled",
        archiveStatus: "archived",
        nextAllowedActions: [],
        communicationStatus: "disabled",
        financialStatus: "paid_declared",
      },
    ],
    publicClaims: [
      {id: "claim_linked", orderId: "ord_live", status: "received"},
      {id: "claim_unlinked", orderId: "", status: "received"},
    ],
    orderRelated: {
      ord_live: {
        events: [{id: "evt_open", type: "open_incident", summary: "Incidencia abierta", actorRole: "store"}],
        incidents: [{id: "inc_open", status: "open"}],
        claims: [{id: "claim_linked", status: "received"}],
        communications: [{id: "comm_prepared", status: "prepared", channel: "internal"}],
        aiDecisions: [{id: "aid_1", status: "suggested"}],
      },
      ord_terminal_live: {
        events: [{id: "evt_cancel", type: "cancel_by_admin", summary: "Cierre operativo", actorRole: "admin"}],
        incidents: [{id: "inc_resolved", status: "resolved"}],
        communications: [{id: "comm_prepared", status: "prepared"}],
        aiDecisions: [],
      },
      ord_cancelled: {
        events: [],
        incidents: [],
        communications: [],
        aiDecisions: [{id: "aid_done", status: "rejected"}],
      },
    },
  });
}

test("health contract and minimum operational metrics exist", () => {
  const report = sampleHealthReport();

  for (const field of ["healthStatus", "severity", "scope", "source", "generatedAt", "metrics", "modules", "alerts", "criticalEvents"]) {
    assert.ok(Object.hasOwn(report, field), field);
  }
  assert.equal(report.healthStatus, "critical");
  assert.equal(report.metrics.liveOrders, 1);
  assert.equal(report.metrics.requiresAttention, 1);
  assert.equal(report.metrics.openIncidentOrders, 2);
  assert.equal(report.metrics.cancelledOrders, 1);
  assert.equal(report.metrics.closedOrders, 2);
  assert.equal(report.metrics.failedCommunicationOrders, 1);
  assert.equal(report.metrics.preparedCommunicationOrders, 1);
  assert.equal(report.metrics.disabledCommunicationOrders, 1);
  assert.equal(report.metrics.financialReviewOrders, 2);
  assert.equal(report.metrics.pendingAiSuggestionOrders, 2);
  assert.equal(report.metrics.publicClaimsReceived, 2);
  assert.equal(report.metrics.linkedPublicClaims, 1);
  assert.equal(report.metrics.unlinkedPublicClaims, 1);
  assert.equal(report.metrics.actionsPendingByRole.store, 1);
});

test("health detects live order consistency warnings without automatic correction", () => {
  const report = sampleHealthReport();
  const codes = report.alerts.map((alert) => alert.warningCode);

  for (const code of [
    "TERMINAL_ORDER_STILL_LIVE",
    "ACTIVE_INCIDENT_WITH_NONE_STATUS",
    "FAILED_COMMUNICATION_WITHOUT_RECORD",
    "AI_REVIEW_WITHOUT_DECISION",
    "FINANCIAL_REVIEW_WITHOUT_REASON",
    "COLLECTION_REQUIRED_INVALID_AMOUNT",
    "LOCAL_ORDER_WITHOUT_STORE_ID",
    "INVALID_TOTAL",
  ]) {
    assert.ok(codes.includes(code), code);
  }
  assert.equal(report.auditSummary.correctiveActionsExecuted, false);
  assert.equal(report.alerts.every((alert) => alert.requiresAdminReview), true);

  const functions = read("functions/index.js");
  const callable = functions.slice(functions.indexOf("exports.getOperationalHealth"), functions.indexOf("exports.processOperationalTimeouts"));
  assert.match(callable, /requireAdminActor\(request\)/);
  assert.doesNotMatch(callable, /tx\.update|tx\.set|tx\.create|tx\.delete|runTransaction/);
});

test("communication finance incidents claims and assisted decision health are summarized", () => {
  const report = sampleHealthReport();

  assert.equal(report.metrics.collectOnDeliveryOrders, 1);
  assert.equal(report.metrics.paidDeclaredUnconfirmed, 1);
  assert.equal(report.metrics.collectionPendingOrders, 1);
  assert.equal(report.metrics.openIncidents, 1);
  assert.equal(report.metrics.resolvedIncidents, 1);
  assert.equal(report.metrics.unresolvedIncidents, 1);
  assert.equal(report.metrics.aiSuggested, 1);
  assert.equal(report.metrics.aiRejected, 1);
  assert.equal(report.metrics.highRiskAi, 1);
  assert.equal(report.metrics.engineVersion, "deterministic_rules_v1");
  assert.equal(report.metrics.providerStatus, "disabled");
  assert.equal(report.criticalEvents.length, 2);
});

test("module status is honest and disabled modules are not false production errors", () => {
  const report = sampleHealthReport();
  const modules = Object.fromEntries(report.modules.map((module) => [module.key, module]));

  assert.equal(modules.whatsapp.moduleStatus, "disabled");
  assert.equal(modules.push_fcm.moduleStatus, "disabled");
  assert.equal(modules.external_ai.moduleStatus, "disabled");
  assert.equal(modules.driver_capacity.moduleStatus, "ok");
  assert.equal(modules.timeouts_fallbacks.moduleStatus, "prepared");
  assert.equal(modules.store_driver_request.moduleStatus, "ok");
  assert.equal(modules.cashbox_operational.moduleStatus, "prepared");
  assert.equal(modules.bank_gateway.moduleStatus, "not_implemented");
  assert.equal(modules.google_play.moduleStatus, "not_ready");
  assert.equal(modules.production.moduleStatus, "not_ready");
  assert.equal(modules.load_hardening.moduleStatus, "pending_o");
  assert.equal(report.metrics.whatsappDisabled, true);
  assert.equal(report.metrics.externalAiDisabled, true);
});

test("admin dashboard exposes health while public store and driver do not see global health", () => {
  const adminPort = read("app/src/main/java/com/pedilo/app/core/port/AdminOrdersPort.kt");
  const adminAdapter = read("app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt");
  const adminUi = read("app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt");
  const publicUi = read("app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt");
  const storeUi = read("app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt");
  const driverUi = read("app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt");

  assert.match(adminPort, /getOperationalHealth/);
  assert.match(adminAdapter, /GET_OPERATIONAL_HEALTH = "getOperationalHealth"/);
  assert.match(adminUi, /AdminOperationalHealthPanel/);
  assert.match(adminUi, /Salud operativa/);
  assert.doesNotMatch(`${publicUi}\n${storeUi}\n${driverUi}`, /AdminOperationalHealthPanel|getOperationalHealth|Salud operativa/);
});

test("rules remain strict and internal audit is not public", () => {
  const rules = read("firestore.rules");
  const functions = read("functions/index.js");

  assert.match(rules, /match \/orders\/\{orderId\}[\s\S]*?allow create, update, delete: if false/);
  assert.match(rules, /match \/events\/\{eventId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/incidents\/\{incidentId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/communications\/\{communicationId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/ai_decisions\/\{aiDecisionId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/public_claims\/\{claimId\}[\s\S]*?allow read: if isAdmin\(\)/);
  assert.doesNotMatch(rules, /match \/health|match \/metrics|allow read: if true/);
  assert.match(functions, /exports.getOperationalHealth/);
  assert.match(functions, /exposesPublicAudit: false/);
});
