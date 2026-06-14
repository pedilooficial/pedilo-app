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
  COMMUNICATION_CHANNELS,
  COMMUNICATION_STATUSES,
  COMMUNICATION_TEMPLATES,
  LIVE_ACTIONS,
  LIVE_ORDER_STATES,
  communicationTemplate,
  communicationStatusForChannel,
  communicationRecordsForOrder,
  publicTrackingResponse,
  liveActionEffect,
  liveOrderState,
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

test("communication contract creates persistent records with channel state template target and actor", () => {
  const api = loadInternals();
  const records = api.communicationRecordsForOrder({
    orderId: "ord_12345678",
    order: {
      storeId: "store-1",
      driverId: "driver-1",
      currentResponsibleRole: "driver",
      customer: {phone: "+5491122334455"},
    },
    eventType: "open_incident",
    triggeredByRole: "store",
    triggeredByActorId: "store-1",
    sourceEventId: "act_incident",
    incidentId: "act_incident",
    now: {serverTimestamp: true},
  });

  assert.ok(records.length >= 5);
  for (const record of records) {
    for (const field of [
      "communicationId",
      "orderId",
      "eventType",
      "channel",
      "targetRole",
      "targetUserId",
      "targetPhone",
      "status",
      "messageType",
      "templateKey",
      "messageBody",
      "createdAt",
      "triggeredByRole",
      "triggeredByActorId",
      "sourceEventId",
      "publicSafe",
    ]) {
      assert.ok(Object.hasOwn(record, field), field);
    }
    assert.notEqual(record.status, "sent");
  }
  assert.ok(records.some((record) => record.channel === "internal" && record.targetRole === "admin"));
  assert.ok(records.some((record) => record.channel === "internal" && record.targetRole === "store" && record.targetUserId === "store-1"));
  assert.ok(records.some((record) => record.channel === "internal" && record.targetRole === "driver" && record.targetUserId === "driver-1"));
  assert.ok(records.some((record) => record.channel === "public_tracking" && record.publicSafe === true));
});

test("external channels are disabled without secure providers and never marked sent", () => {
  const api = loadInternals();

  assert.equal(api.communicationStatusForChannel(api.COMMUNICATION_CHANNELS.WHATSAPP), "disabled");
  assert.equal(api.communicationStatusForChannel(api.COMMUNICATION_CHANNELS.PUSH), "disabled");
  assert.equal(api.communicationStatusForChannel(api.COMMUNICATION_CHANNELS.INTERNAL), "prepared");

  const records = api.communicationRecordsForOrder({
    orderId: "ord_12345678",
    order: {customer: {phone: "+5491122334455"}},
    eventType: "order_created",
    triggeredByRole: "public_user",
    triggeredByActorId: "",
    sourceEventId: "initial",
    now: {serverTimestamp: true},
  });
  const external = records.filter((record) => ["whatsapp", "push"].includes(record.channel));
  assert.equal(external.length, 2);
  assert.ok(external.every((record) => record.status === "disabled"));
  assert.ok(external.every((record) => record.sentAt === null));
  assert.ok(external.every((record) => record.failureReason.includes("No secure")));
});

test("communicationStatus is a real live order state and transitions coherently", () => {
  const api = loadInternals();
  for (const status of ["received", "pending", "prepared", "sent", "failed", "closed", "disabled"]) {
    assert.ok(api.LIVE_ORDER_STATES.communication.includes(status), status);
  }

  const order = api.liveOrderState({status: "preparing", communicationStatus: "received"});
  const incident = api.liveActionEffect(
    {action: api.LIVE_ACTIONS.OPEN_INCIDENT, reason: "demora"},
    order,
    {uid: "store-1", role: "store"},
  );
  assert.equal(incident.patch.communicationStatus, "prepared");

  const delivered = api.liveActionEffect(
    {action: api.LIVE_ACTIONS.DRIVER_MARK_DELIVERED, reason: ""},
    api.liveOrderState({status: "picked_up", communicationStatus: "prepared"}),
    {uid: "driver-1", role: "driver"},
  );
  assert.equal(delivered.patch.communicationStatus, "closed");
});

test("templates centralize safe messages for order incidents claims cancellations and failures", () => {
  const api = loadInternals();
  for (const key of [
    "order_created",
    "local_accept",
    "local_mark_preparing",
    "local_mark_ready",
    "driver_take",
    "driver_mark_picked_up",
    "driver_mark_delivered",
    "cancel_order",
    "open_incident",
    "resolve_incident",
    "public_claim_received",
    "communication_failed",
    "phone_validation_prepared",
  ]) {
    assert.equal(api.communicationTemplate(key).templateKey, key);
    assert.ok(api.COMMUNICATION_TEMPLATES[key].length > 10);
  }
});

test("public tracking stays safe when communication has failed", () => {
  const api = loadInternals();
  const tracking = api.publicTrackingResponse({
    status: "preparing",
    communicationStatus: "failed",
    publicStatus: "Pedido en preparación",
    customer: {phone: "+5491122334455"},
    actorUid: "admin-1",
    failureReason: "provider stack trace",
  }, "PDL-ABC123");

  assert.equal(tracking.status, "UNDER_REVIEW");
  assert.equal(tracking.publicStatus, "Pedido en revisión operativa");
  assert.equal(Object.hasOwn(tracking, "customer"), false);
  assert.equal(JSON.stringify(tracking).includes("provider"), false);
  assert.equal(JSON.stringify(tracking).includes("admin-1"), false);
});

test("communication writes are backend-only and role scoped in rules", () => {
  const rules = read("firestore.rules");
  const functions = read("functions/index.js");

  assert.match(rules, /match \/communications\/\{communicationId\}[\s\S]*?canReadOrder/);
  assert.match(rules, /match \/communications\/\{communicationId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/public_claims\/\{claimId\}[\s\S]*?match \/communications\/\{communicationId\}/);
  assert.match(functions, /writeOrderCommunications\(tx, orderRef, communicationRecords\)/);
  assert.match(functions, /writeClaimCommunications\(tx, claimRef, claimCommunicationRecords\)/);
});

test("admin store driver expose communication state without fake push or external sends", () => {
  const adminUi = read("app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt");
  const storeUi = read("app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt");
  const driverUi = read("app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt");
  const storeAdapter = read("app/src/main/java/com/pedilo/app/core/firebase/FirebaseStoreOrdersAdapter.kt");
  const driverAdapter = read("app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt");

  assert.match(adminUi, /adminCommunicationStatusLabel/);
  assert.match(storeUi, /storeCommunicationLabel/);
  assert.match(driverUi, /driverCommunicationLabel/);
  assert.match(storeAdapter, /COMMUNICATION_STATUS = "communicationStatus"/);
  assert.match(driverAdapter, /COMMUNICATION_STATUS = "communicationStatus"/);
  assert.doesNotMatch(`${adminUi}\n${storeUi}\n${driverUi}`, /push enviado|notificación enviada|enviado por WhatsApp/i);
});

test("no secrets credentials or provider tokens were added for communication", () => {
  const joined = [
    "functions/index.js",
    "firestore.rules",
    "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt",
    "app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt",
    "app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt",
  ].map(read).join("\n");

  assert.doesNotMatch(joined, /api[_-]?key|secret|token|bearer|authorization|twilio|meta graph|firebaseMessaging\.send/i);
});
