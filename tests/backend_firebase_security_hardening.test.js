"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");
const vm = require("node:vm");

function read(path) {
  return fs.readFileSync(path, "utf8");
}

function loadSecurityInternals() {
  const source = read("functions/index.js");
  const wrapped = `${source}
module.exports = {
  LIVE_ACTIONS,
  cleanLiveActionPayload,
  cleanOrderPayload,
  cleanPlusOrderPayload,
  cleanPublicCancellationPayload,
  isValidPhone,
  isValidTrackingNumber,
  publicTrackingResponse,
  liveOrderEvent,
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

test("rules require active valid operator profiles and keep public order writes closed", () => {
  const rules = read("firestore.rules");

  assert.match(rules, /function operatorActive\(\)/);
  assert.match(rules, /data\.active == true/);
  assert.match(rules, /operatorRole\(\) == "admin" && operatorActive\(\)/);
  assert.match(rules, /operatorRole\(\) in \["store", "driver", "admin"\] && operatorActive\(\)/);
  assert.match(rules, /match \/orders\/\{orderId\}/);
  assert.match(rules, /allow create, update, delete: if false/);
  assert.match(rules, /match \/events\/\{eventId\}[\s\S]*?allow write: if false/);
  assert.match(rules, /match \/incidents\/\{incidentId\}[\s\S]*?allow write: if false/);
  assert.doesNotMatch(rules, /allow write: if true|allow read: if true/);
});

test("public payload validation rejects placeholders invalid phones and unavailable tracking shape", () => {
  const api = loadSecurityInternals();

  assert.equal(api.isValidPhone("+5491122334455"), true);
  assert.equal(api.isValidPhone("1234567"), false);
  assert.equal(api.isValidPhone("+1+2223334444"), false);
  assert.equal(api.isValidTrackingNumber("PDL-ABC123"), true);
  assert.equal(api.isValidTrackingNumber("pedido"), false);
  assert.equal(api.isValidTrackingNumber("ABC123"), false);

  assert.throws(
    () => api.cleanOrderPayload({
      storeId: "store-1",
      storeName: "Local Centro",
      customer: {name: "Nombre", phone: "+5491122334455", address: "Calle 123"},
      paymentMethod: "cash",
      items: [{productId: "prod-1", name: "Pizza", quantity: 1}],
    }),
    /Revisá los datos/,
  );
  assert.throws(
    () => api.cleanOrderPayload({
      storeId: "store-1",
      storeName: "Local Centro",
      customer: {name: "Ana", phone: "1234567", address: "Calle 123"},
      paymentMethod: "cash",
      items: [{productId: "prod-1", name: "Pizza", quantity: 1}],
    }),
    /Faltan datos/,
  );
  assert.throws(
    () => api.cleanOrderPayload({
      storeId: "store-1",
      storeName: "Local Centro",
      customer: {name: "Ana", phone: "+5491122334455", address: "Calle 123"},
      paymentMethod: "cash",
      items: [{productId: "producto", name: "Pizza", quantity: 1}],
    }),
    /carrito/,
  );
  assert.throws(
    () => api.cleanPlusOrderPayload({
      requestType: "buy",
      source: "public_plus_buy",
      contact: {name: "Ana", phone: "+5491122334455"},
      sourceReference: "Tienda",
      destination: "Calle real 123",
      paymentMethod: "cash",
      items: [{name: "Medicamento", detail: "producto"}],
    }),
    /productos/,
  );
  assert.throws(
    () => api.cleanPublicCancellationPayload({
      trackingNumber: "pedido",
      contact: "+5491122334455",
      reason: "cancelar",
    }),
    /número de pedido/,
  );
});

test("operational payloads require safe ids expected versions and controlled terminal tracking", () => {
  const api = loadSecurityInternals();

  assert.throws(
    () => api.cleanLiveActionPayload({orderId: "ord_12345678", action: api.LIVE_ACTIONS.LOCAL_ACCEPT}, "store-1"),
    /versión esperada/,
  );
  assert.throws(
    () => api.cleanLiveActionPayload({orderId: "../orders/x", action: api.LIVE_ACTIONS.LOCAL_ACCEPT, expectedVersion: 1}, "store-1"),
    /acción operativa válida/,
  );
  assert.equal(Object.hasOwn(api, "cleanAdminActionPayload"), false);

  const event = api.liveOrderEvent({
    clean: {orderId: "ord_12345678", action: api.LIVE_ACTIONS.OPEN_INCIDENT, actionId: "act_12345678", reason: "sin stock"},
    actor: {uid: "store-1", role: "store"},
    current: {status: "created", operationalStatus: "waiting_admin_review", version: 1, currentResponsibleRole: "admin", archiveStatus: "live"},
    result: {status: "created", operationalStatus: "incident_open", version: 2, currentResponsibleRole: "admin", archiveStatus: "live"},
    summary: "store abrió incidencia: sin stock",
    now: {serverTimestamp: true},
  });
  assert.equal(event.audit.action, api.LIVE_ACTIONS.OPEN_INCIDENT);
  assert.equal(event.audit.actorRole, "store");
  assert.equal(event.previousVersion, 1);
  assert.equal(event.nextVersion, 2);

  const closedTracking = api.publicTrackingResponse({
    status: "delivered",
    publicStatus: "Pedido cerrado",
    storeName: "Local interno",
    items: [{name: "Dato interno"}],
    customer: {phone: "1122334455", address: "Privada 123"},
  }, "PDL-ABC123");
  assert.equal(closedTracking.isClosed, true);
  assert.equal(closedTracking.storeName, "");
  assert.equal(closedTracking.summary, "");
  assert.equal(Object.hasOwn(closedTracking, "customer"), false);
});

test("functions source rejects inactive invalid and nonexistent operational users", () => {
  const source = read("functions/index.js");
  const adminActor = source.slice(
    source.indexOf("async function requireAdminActor"),
    source.indexOf("async function requireOperationalActor"),
  );
  const operationalActor = source.slice(
    source.indexOf("async function requireOperationalActor"),
    source.indexOf("function cleanLiveActionPayload"),
  );

  assert.match(source, /const OPERATIONAL_ROLES = \["admin", "store", "driver"\]/);
  assert.match(adminActor, /!userSnap\.exists \|\| role !== "admin" \|\| userSnap\.get\("active"\) !== true/);
  assert.match(operationalActor, /!userSnap\.exists \|\| !OPERATIONAL_ROLES\.includes\(role\) \|\| userSnap\.get\("active"\) !== true/);
  assert.doesNotMatch(source, /stack|console\.error\(.*request\.data|throw new Error/);
});
