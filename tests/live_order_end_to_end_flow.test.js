const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");
const vm = require("node:vm");

function loadLiveOrderInternals() {
  const source = fs.readFileSync("functions/index.js", "utf8");
  const wrapped = `${source}
module.exports = {
  LIVE_ACTIONS,
  LOCAL_SOURCE,
  PUBLIC_STATUS,
  liveBirthContract,
  liveOrderState,
  allowedLiveActions,
  liveActionEffect,
  publicTrackingResponse,
  publicStatusCode,
  validateLiveActor,
  validateExpectedVersion,
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

function applyTransition(api, current, action, actor, reason = "") {
  api.validateLiveActor(actor, current, action);
  api.validateExpectedVersion({expectedVersion: current.version}, current);
  const allowed = api.allowedLiveActions(current);
  assert.ok(allowed.includes(action), `Expected ${action} in ${allowed.join(", ")}`);
  const effect = api.liveActionEffect({action, reason}, current, actor);
  const next = api.liveOrderState({
    ...current,
    ...effect.patch,
    version: current.version + 1,
    nextAllowedActions: api.allowedLiveActions({
      ...current,
      ...effect.patch,
      version: current.version + 1,
    }),
  });
  return {effect, next};
}

test("complete live order flow closes from public creation to delivery with coherent contract", () => {
  const api = loadLiveOrderInternals();
  const liveBirth = api.liveBirthContract({
    orderType: "local_order",
    source: api.LOCAL_SOURCE,
    idempotencyKey: "ord_e2e_flow_000001",
    trackingNumber: "PDL-E2E001",
    snapshot: {kind: "test"},
  });
  const created = api.liveOrderState({
    ...liveBirth,
    storeId: "store-1",
    storeName: "Local Centro",
    customer: {name: "Ana"},
    items: [{name: "Pizza", quantity: 1}],
  });

  assert.equal(created.status, "created");
  assert.equal(created.operationalStatus, "waiting_admin_review");
  assert.equal(created.responsibleRole, "admin");
  assert.equal(created.currentResponsibleRole, "admin");
  assert.equal(created.assignedActorId, "");
  assert.deepEqual(Array.from(api.allowedLiveActions(created)), [
    api.LIVE_ACTIONS.LOCAL_ACCEPT,
    api.LIVE_ACTIONS.LOCAL_REJECT,
    api.LIVE_ACTIONS.OPEN_INCIDENT,
    api.LIVE_ACTIONS.CANCEL_ORDER,
    api.LIVE_ACTIONS.ADMIN_INTERVENE,
  ]);

  const storeActor = {uid: "store-1", role: "store"};
  const driverActor = {uid: "driver-9", role: "driver"};
  const foreignDriver = {uid: "driver-x", role: "driver"};

  const accepted = applyTransition(api, created, api.LIVE_ACTIONS.LOCAL_ACCEPT, storeActor).next;
  assert.equal(accepted.status, "accepted");
  assert.equal(accepted.operationalStatus, "local_accepted");
  assert.equal(accepted.responsibleRole, "store");
  assert.equal(accepted.assignedActorId, "store-1");
  assert.equal(accepted.version, 2);

  const preparing = applyTransition(api, accepted, api.LIVE_ACTIONS.LOCAL_MARK_PREPARING, storeActor).next;
  assert.equal(preparing.status, "preparing");
  assert.equal(preparing.operationalStatus, "preparing");
  assert.equal(preparing.version, 3);

  const ready = applyTransition(api, preparing, api.LIVE_ACTIONS.LOCAL_MARK_READY, storeActor).next;
  assert.equal(ready.status, "ready_for_pickup");
  assert.equal(ready.operationalStatus, "ready_for_pickup");
  assert.equal(ready.responsibleRole, "driver");
  assert.equal(ready.currentResponsibleRole, "driver");
  assert.equal(ready.assignedActorId, "");
  assert.deepEqual(Array.from(api.allowedLiveActions(ready)), [
    api.LIVE_ACTIONS.DRIVER_TAKE,
    api.LIVE_ACTIONS.OPEN_INCIDENT,
    api.LIVE_ACTIONS.CANCEL_ORDER,
    api.LIVE_ACTIONS.ADMIN_INTERVENE,
  ]);
  assert.equal(api.publicTrackingResponse(ready, "PDL-E2E001").status, "RECEIVED");

  assert.throws(
    () => api.validateLiveActor(foreignDriver, accepted, api.LIVE_ACTIONS.DRIVER_TAKE),
    /ya fue tomado por otro repartidor|acción/i,
  );

  const assigned = applyTransition(api, ready, api.LIVE_ACTIONS.DRIVER_TAKE, driverActor).next;
  assert.equal(assigned.status, "assigned_to_driver");
  assert.equal(assigned.operationalStatus, "driver_assigned");
  assert.equal(assigned.responsibleRole, "driver");
  assert.equal(assigned.assignedActorId, "driver-9");
  assert.equal(assigned.assignedActorRole, "driver");
  assert.equal(assigned.driverId, "driver-9");
  assert.equal(assigned.version, 5);

  assert.throws(
    () => api.validateLiveActor(foreignDriver, assigned, api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP),
    /no está asignado/i,
  );

  const pickedUp = applyTransition(api, assigned, api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP, driverActor).next;
  assert.equal(pickedUp.status, "picked_up");
  assert.equal(pickedUp.publicStatus, "Pedido retirado");
  assert.equal(api.publicTrackingResponse(pickedUp, "PDL-E2E001").status, "ON_THE_WAY");
  assert.equal(pickedUp.version, 6);

  const delivered = applyTransition(api, pickedUp, api.LIVE_ACTIONS.DRIVER_MARK_DELIVERED, driverActor).next;
  assert.equal(delivered.status, "delivered");
  assert.equal(delivered.operationalStatus, "delivered");
  assert.equal(delivered.archiveStatus, "archived");
  assert.equal(delivered.responsibleRole, "");
  assert.equal(delivered.currentResponsibleRole, "");
  assert.equal(delivered.assignedActorId, "");
  assert.equal(delivered.assignedActorRole, "");
  assert.equal(delivered.driverId, "driver-9");
  assert.equal(delivered.version, 7);
  assert.deepEqual(Array.from(api.allowedLiveActions(delivered)), []);

  const publicTracking = api.publicTrackingResponse(delivered, "PDL-E2E001");
  assert.equal(publicTracking.found, true);
  assert.equal(publicTracking.status, "DELIVERED");
  assert.equal(publicTracking.publicStatus, "Pedido cerrado");
  assert.equal(publicTracking.isClosed, true);
});

test("invalid live action path rejects stale version and cross-role operation", () => {
  const api = loadLiveOrderInternals();
  const created = api.liveOrderState({
    ...api.liveBirthContract({
      orderType: "local_order",
      source: api.LOCAL_SOURCE,
      idempotencyKey: "ord_e2e_flow_000002",
      trackingNumber: "PDL-E2E002",
      snapshot: {kind: "test"},
    }),
    storeId: "store-2",
  });

  assert.throws(
    () => api.validateExpectedVersion({expectedVersion: 99}, created),
    /Actualizá la vista/i,
  );
  assert.ok(!Array.from(api.allowedLiveActions(created)).includes(api.LIVE_ACTIONS.DRIVER_TAKE));
});
