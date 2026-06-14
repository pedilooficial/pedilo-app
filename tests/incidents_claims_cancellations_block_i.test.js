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
  cleanLiveActionPayload,
  cleanPublicClaimPayload,
  liveOrderEvent,
  publicTrackingResponse,
  incidentDocument,
  incidentResolutionPatch,
  cancellationAuditPatch,
  cancellationNeedsFinancialReview,
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

test("incident document contract persists source impact priority and linked action", () => {
  const api = loadInternals();
  const doc = api.incidentDocument({
    incidentId: "act_incident123",
    orderId: "ord_12345678",
    clean: {action: api.LIVE_ACTIONS.OPEN_INCIDENT, reason: "demora operativa"},
    current: {status: "preparing", operationalStatus: "preparing"},
    actor: {uid: "store-1", role: "store"},
    now: {serverTimestamp: true},
    priority: "high",
    type: "delay",
  });

  for (const field of [
    "incidentId",
    "orderId",
    "status",
    "type",
    "reason",
    "description",
    "sourceRole",
    "sourceActorId",
    "createdAt",
    "updatedAt",
    "publicImpact",
    "operationalImpact",
    "priority",
    "linkedAction",
  ]) {
    assert.ok(Object.hasOwn(doc, field), field);
  }
  assert.equal(doc.status, "open");
  assert.equal(doc.sourceRole, "store");
  assert.equal(doc.linkedAction, api.LIVE_ACTIONS.OPEN_INCIDENT);
});

test("resolution and cancellation audit preserve actor previous state and financial review", () => {
  const api = loadInternals();
  const resolution = api.incidentResolutionPatch({
    status: "resolved",
    clean: {reason: "stock repuesto"},
    actor: {uid: "admin-1", role: "admin"},
    now: {serverTimestamp: true},
  });
  assert.equal(resolution.resolvedByRole, "admin");
  assert.equal(resolution.resolutionNote, "stock repuesto");

  const cancellation = api.cancellationAuditPatch({
    clean: {reason: "cliente pidió cancelar"},
    current: {
      status: "picked_up",
      operationalStatus: "picked_up",
      financialStatus: "collect_on_delivery",
      publicStatus: "Pedido retirado",
      archiveStatus: "live",
      collectionRequired: true,
      amountToCollect: 120000,
    },
    actor: {uid: "driver-1", role: "driver"},
  });
  assert.equal(cancellation.cancelledByRole, "driver");
  assert.equal(cancellation.cancelledByActorId, "driver-1");
  assert.equal(cancellation.previousStatus, "picked_up");
  assert.equal(cancellation.financialStatusAtCancellation, "collect_on_delivery");
  assert.equal(cancellation.financialReviewRequired, true);
  assert.match(cancellation.financialReviewNote, /revisión financiera mínima/);
});

test("public claims validate persist separately and tracking remains safe", () => {
  const api = loadInternals();

  assert.throws(
    () => api.cleanPublicClaimPayload({trackingNumber: "pedido", customerName: "Ana", contact: "+5491122334455", reason: "demora", description: "Pedido demorado"}),
    /número de pedido válido/,
  );
  const clean = api.cleanPublicClaimPayload({
    trackingNumber: "pdl-abc123",
    customerName: "Ana",
    contact: "+5491122334455",
    reason: "demora",
    description: "El pedido está demorado hace rato",
    type: "delay",
  });
  assert.equal(clean.trackingNumber, "PDL-ABC123");
  assert.equal(clean.type, "delay");

  const tracking = api.publicTrackingResponse({
    status: "preparing",
    activeIncident: true,
    incidentStatus: "open",
    publicStatus: "Pedido en revisión operativa",
    storeName: "Local visible",
    customer: {phone: "interno"},
  }, "PDL-ABC123");
  assert.equal(tracking.status, "UNDER_REVIEW");
  assert.equal(tracking.publicStatus, "Pedido en revisión operativa");
  assert.equal(Object.hasOwn(tracking, "customer"), false);
});

test("backend callables and rules separate public claims from live order mutations", () => {
  const functions = read("functions/index.js");
  const rules = read("firestore.rules");
  const callable = functions.slice(
    functions.indexOf("exports.submitPublicClaim"),
    functions.indexOf("exports.resolveAssistedDecision"),
  );

  assert.match(callable, /db\.collection\(PUBLIC_CLAIMS\)\.doc\(\)/);
  assert.match(callable, /collection\("claims"\)\.doc\(claimRef\.id\)/);
  assert.match(callable, /no_live_order_mutation/);
  assert.doesNotMatch(callable, /tx\.update\(db\.collection\(ORDERS\)|activeIncident|incidentStatus|nextAllowedActions/);
  assert.match(rules, /match \/claims\/\{claimId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/public_claims\/\{claimId\}[\s\S]*?allow read: if isAdmin\(\)/);
  assert.match(rules, /match \/public_claims\/\{claimId\}[\s\S]*?allow create, update, delete: if false/);
});

test("store driver admin and public UI use backend paths without fake problem actions", () => {
  const store = read("app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt");
  const driver = read("app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt");
  const admin = read("app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt");
  const claimAdapter = read("app/src/main/java/com/pedilo/app/core/firebase/FirebasePublicClaimAdapter.kt");
  const claimUi = read("app/src/main/java/com/pedilo/app/ui/publicuser/PublicConventions.kt");

  assert.match(store, /LiveOrderAction\.OpenIncident/);
  assert.match(driver, /LiveOrderAction\.OpenIncident/);
  assert.match(admin, /ResolveIncident|resolve_incident|pendingLiveAction/);
  assert.match(claimAdapter, /getHttpsCallable\(SUBMIT_PUBLIC_CLAIM\)/);
  assert.match(claimUi, /Enviar reclamo/);
  assert.doesNotMatch(`${store}\n${driver}\n${admin}\n${claimUi}`, /collection\("orders"\)|collection\("incidents"\)|\.set\(|\.update\(|runTransaction/);
});
