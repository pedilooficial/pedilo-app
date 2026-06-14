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
  AI_PROVIDER_STATUS_DISABLED,
  ASSISTED_ENGINE_VERSION,
  AI_DECISION_STATUSES,
  deterministicAssistedAnalysis,
  assistedDecisionForOrder,
  assistedDecisionOrderPatch,
  cleanAssistedDecisionResolutionPayload,
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

test("assisted decision contract creates persistent suggestion data with audit", () => {
  const api = loadInternals();
  const decision = api.assistedDecisionForOrder({
    orderId: "ord_12345678",
    order: {
      status: "preparing",
      operationalStatus: "incident_open",
      financialStatus: "pending_review",
      communicationStatus: "prepared",
      activeIncident: true,
      incidentStatus: "open",
      needsAttention: true,
    },
    sourceEventId: "act_12345678",
    scope: "open_incident",
    incidentId: "act_12345678",
    now: {serverTimestamp: true},
  });

  for (const field of [
    "aiDecisionId",
    "orderId",
    "sourceEventId",
    "scope",
    "inputSummary",
    "classification",
    "riskLevel",
    "suggestedAction",
    "suggestedActionType",
    "confidence",
    "requiresHumanReview",
    "status",
    "createdAt",
    "ruleVersion",
    "engineVersion",
    "providerStatus",
    "audit",
  ]) {
    assert.ok(Object.hasOwn(decision, field), field);
  }
  assert.equal(decision.classification, "incident_risk");
  assert.equal(decision.riskLevel, "high");
  assert.equal(decision.requiresHumanReview, true);
  assert.equal(decision.status, "suggested");
  assert.equal(decision.providerStatus, "disabled");
  assert.equal(decision.audit.noCriticalActionExecuted, true);
});

test("deterministic engine covers incident claim communication finance incomplete and incoherent risks", () => {
  const api = loadInternals();

  assert.equal(api.deterministicAssistedAnalysis({activeIncident: true, incidentStatus: "open"}, {}).classification, "incident_risk");
  assert.equal(api.deterministicAssistedAnalysis({}, {claimId: "claim-1"}).classification, "claim_risk");
  assert.equal(api.deterministicAssistedAnalysis({communicationStatus: "failed"}, {}).classification, "communication_risk");
  assert.equal(api.deterministicAssistedAnalysis({financialStatus: "transfer_declared_pending"}, {}).classification, "financial_review");
  assert.equal(api.deterministicAssistedAnalysis({source: "public_local", storeId: "", trackingNumber: ""}, {}).classification, "incomplete_data");
  assert.equal(api.deterministicAssistedAnalysis({status: "delivered", archiveStatus: "live"}, {}).classification, "incoherent_state");
  assert.equal(api.deterministicAssistedAnalysis({status: "cancelled", financialReviewRequired: true}, {}).classification, "cancellation_financial_review");
});

test("external AI remains disabled and no critical order action is suggested as execution", () => {
  const api = loadInternals();
  const decision = api.assistedDecisionForOrder({
    orderId: "ord_12345678",
    order: {status: "cancelled", financialReviewRequired: true},
    sourceEventId: "cancel_evt",
    scope: "cancel_order",
    now: {serverTimestamp: true},
  });
  const patch = api.assistedDecisionOrderPatch(decision);

  assert.equal(api.AI_PROVIDER_STATUS_DISABLED, "disabled");
  assert.equal(decision.audit.noExternalProviderUsed, true);
  assert.equal(decision.audit.noCriticalActionExecuted, true);
  assert.equal(Object.hasOwn(patch, "status"), false);
  assert.equal(Object.hasOwn(patch, "financialStatus"), false);
  assert.equal(Object.hasOwn(patch, "activeIncident"), false);
  assert.doesNotMatch(decision.suggestedActionType, /cancel|confirm_payment|resolve_claim|block/i);
});

test("admin can resolve suggestion state only as audit without operational mutation", () => {
  const api = loadInternals();

  const clean = api.cleanAssistedDecisionResolutionPayload({
    orderId: "ord_12345678",
    aiDecisionId: "aid_12345678",
    status: "rejected",
    resolutionNote: "no aplica",
  });
  assert.equal(clean.orderId, "ord_12345678");
  assert.equal(clean.aiDecisionId, "aid_12345678");
  assert.equal(clean.status, "rejected");
  assert.equal(clean.resolutionNote, "no aplica");
  assert.throws(
    () => api.cleanAssistedDecisionResolutionPayload({orderId: "ord_12345678", aiDecisionId: "aid_12345678", status: "cancel_order", resolutionNote: "x"}),
    /sugerencia/,
  );

  const source = read("functions/index.js");
  const callable = source.slice(
    source.indexOf("exports.resolveAssistedDecision"),
    source.indexOf("exports.getOperationalHealth"),
  );
  assert.match(callable, /requireAdminActor\(request\)/);
  assert.match(callable, /noCriticalActionExecuted: true/);
  assert.doesNotMatch(callable, /tx\.update\(orderRef|liveActionEffect|adminActionEffect|financialStatus:|status: "cancelled"/);
});

test("suggestions are written from order events claims and communications without direct client writes", () => {
  const functions = read("functions/index.js");
  const rules = read("firestore.rules");

  assert.match(functions, /writeAssistedDecision\(tx, orderRef, assistedDecision\)/);
  assert.match(functions, /writeAssistedDecision\(tx, db\.collection\(ORDERS\)\.doc\(linkedOrderId\), claimAssistedDecision\)/);
  assert.match(functions, /assistedDecisionForOrder/);
  assert.match(rules, /match \/ai_decisions\/\{aiDecisionId\}[\s\S]*?canReadOrder/);
  assert.match(rules, /match \/ai_decisions\/\{aiDecisionId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/orders\/\{orderId\}[\s\S]*?allow create, update, delete: if false/);
});

test("admin store and driver expose only appropriate assisted decision summaries", () => {
  const adminModel = read("app/src/main/java/com/pedilo/app/core/model/AdminOrderReadModels.kt");
  const adminUi = read("app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt");
  const storeAdapter = read("app/src/main/java/com/pedilo/app/core/firebase/FirebaseStoreOrdersAdapter.kt");
  const storeUi = read("app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt");
  const driverAdapter = read("app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt");
  const driverUi = read("app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt");

  assert.match(adminModel, /aiRiskLevel/);
  assert.match(adminModel, /aiClassification/);
  assert.match(adminUi, /adminAssistedRiskLabel/);
  assert.match(adminUi, /adminAssistedClassificationLabel/);
  assert.match(storeAdapter, /safeAssistanceSummary/);
  assert.match(driverAdapter, /safeDriverAssistanceSummary/);
  assert.match(storeUi, /Ayuda operativa/);
  assert.match(driverUi, /Ayuda operativa/);
  assert.doesNotMatch(`${storeUi}\n${driverUi}`, /aiRiskLevel|aiClassification|score|prompt/i);
});

test("public tracking never exposes internal assisted decision data", () => {
  const api = loadInternals();
  const tracking = api.publicTrackingResponse({
    status: "preparing",
    aiRequiresHumanReview: true,
    aiRiskLevel: "critical",
    aiClassification: "incoherent_state",
    aiSuggestedAction: "intervención Admin sugerida",
    publicStatus: "Pedido en preparación",
    customer: {phone: "+5491122334455"},
  }, "PDL-ABC123");

  assert.equal(tracking.status, "UNDER_REVIEW");
  assert.equal(tracking.publicStatus, "Pedido en revisión operativa");
  assert.doesNotMatch(JSON.stringify(tracking), /ai|risk|critical|classification|sugerida|prompt/i);
});

test("no AI provider secrets credentials tokens or prompt payloads were added", () => {
  const joined = [
    "functions/index.js",
    "firestore.rules",
    "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt",
    "app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt",
    "app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt",
  ].map(read).join("\n");

  assert.doesNotMatch(joined, /api[_-]?key|secret|token|bearer|authorization|openai|anthropic|gemini|prompt|model:/i);
});
