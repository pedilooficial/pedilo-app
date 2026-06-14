const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");
const vm = require("node:vm");

const app = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const teamAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseTeamAccessAdapter.kt";
const driverUi = "app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt";
const adapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt";
const rules = "firestore.rules";
const storeUi = "app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt";
const adminUi = "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

function loadLiveOrderInternals() {
  const source = read("functions/index.js");
  const wrapped = `${source}
module.exports = {
  LIVE_ACTIONS,
  LOCAL_SOURCE,
  liveBirthContract,
  liveOrderState,
  allowedLiveActions,
  liveActionEffect,
  validateLiveActor,
  validateLiveTransition,
  validateExpectedVersion,
  validateExistingLiveEventReplay,
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

function applyTransition(api, current, action, actor, reason = "motivo operativo") {
  api.validateLiveActor(actor, current, action);
  api.validateExpectedVersion({expectedVersion: current.version}, current);
  api.validateLiveTransition(current, action);
  assert.ok(api.allowedLiveActions(current).includes(action), `Expected ${action} to be enabled`);
  const effect = api.liveActionEffect({action, reason}, current, actor);
  const next = api.liveOrderState({
    ...current,
    ...effect.patch,
    version: current.version + 1,
  });
  return {
    ...next,
    nextAllowedActions: api.allowedLiveActions(next),
  };
}

test("driver access remains role based active and does not mix admin store or public domains", () => {
  const appSource = read(app);
  const accessSource = read(teamAdapter);

  assert.match(appSource, /TeamRole\.Driver -> DriverApp\(onSignOutConfirmed = onSignOutConfirmed\)/);
  assert.match(accessSource, /profile\.getBoolean\(ACTIVE\) != true/);
  assert.match(accessSource, /TeamRole\.fromWire\(profile\.getString\(ROLE\)\.orEmpty\(\)\)/);
  assert.match(accessSource, /return@runCatching TeamLoginResult\.NoAccess/);
  assert.doesNotMatch(driverUi + adapter, /AdminApp|StoreApp|createLocalOrder|createPlusOrder|getPublicOrderTracking/);
});

test("driver reads only backend-declared available orders or its own assigned orders", () => {
  const source = read(adapter);
  const ruleSource = read(rules);

  assert.match(source, /whereEqualTo\(RESPONSIBLE_ROLE, DRIVER_ROLE\)/);
  assert.match(source, /whereEqualTo\(ASSIGNED_ACTOR_ID, ""\)/);
  assert.match(source, /whereEqualTo\(DRIVER_ID, uid\)/);
  assert.match(source, /isAvailableToCurrentDriver/);
  assert.match(source, /driverAllowedActions\(uid\)/);
  assert.match(source, /actions\.filter \{ it == LiveOrderAction\.DriverTake \}/);
  assert.match(source, /nextAllowedActions\(\)\.contains\(LiveOrderAction\.DriverTake\)/);
  assert.match(source, /getString\(DRIVER_ID\)\.orEmpty\(\)\.isBlank\(\)/);
  assert.match(source, /getString\(CURRENT_RESPONSIBLE_ROLE\)/);
  assert.match(ruleSource, /order\.currentResponsibleRole == "driver"/);
  assert.match(ruleSource, /order\.driverId == ""/);
  assert.match(ruleSource, /order\.nextAllowedActions\.hasAny\(\["driver_take"\]\)/);
});

test("driver detail exposes operational essentials and no raw payload or real finance claims", () => {
  const source = read(driverUi);

  for (const text of [
    "Tipo:",
    "Estado operativo",
    "Acción necesaria",
    "Acciones permitidas por backend",
    "Incidencia",
    "Capacidad",
    "La toma de pedidos respeta la capacidad activa",
    "Cobro y caja",
    "Cobro operativo requerido",
    "Cerrar caja operativa",
  ]) {
    assert.match(source, new RegExp(text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  }
  assert.doesNotMatch(source, /payload crudo|pasarela real|banco real|liquidación real/i);
});

test("driver actions use operateLiveOrder with version and only backend nextAllowedActions", () => {
  const source = read(driverUi);
  const firebase = read(adapter);

  assert.match(source, /items\(current\.nextAllowedActions\)/);
  assert.match(source, /PendingDriverAction\(current\.id, action, current\.version\)/);
  assert.match(source, /expectedVersion = pending\.expectedVersion/);
  assert.match(firebase, /getHttpsCallable\(OPERATE_LIVE_ORDER\)/);
  assert.doesNotMatch(firebase, /\.set\(|\.update\(|\.delete\(|writeBatch|runTransaction/);
  assert.doesNotMatch(source, /"driver_take"|"driver_mark_picked_up"|"driver_mark_delivered"/);
});

test("backend driver lifecycle rejects double take foreign operation stale version and invalid ordering", () => {
  const api = loadLiveOrderInternals();
  const store = {uid: "store-1", role: "store"};
  const driver = {uid: "driver-1", role: "driver"};
  const otherDriver = {uid: "driver-2", role: "driver"};
  const created = api.liveOrderState({
    ...api.liveBirthContract({
      orderType: "local_order",
      source: api.LOCAL_SOURCE,
      idempotencyKey: "ord_driver_align_001",
      trackingNumber: "PDL-DRIVER001",
      snapshot: {kind: "test"},
    }),
    storeId: "store-1",
  });

  const accepted = applyTransition(api, created, api.LIVE_ACTIONS.LOCAL_ACCEPT, store);
  const preparing = applyTransition(api, accepted, api.LIVE_ACTIONS.LOCAL_MARK_PREPARING, store);
  const ready = applyTransition(api, preparing, api.LIVE_ACTIONS.LOCAL_MARK_READY, store);

  assert.deepEqual(Array.from(api.allowedLiveActions(ready)), [
    api.LIVE_ACTIONS.DRIVER_TAKE,
    api.LIVE_ACTIONS.OPEN_INCIDENT,
    api.LIVE_ACTIONS.CANCEL_ORDER,
    api.LIVE_ACTIONS.ADMIN_INTERVENE,
  ]);

  const assigned = applyTransition(api, ready, api.LIVE_ACTIONS.DRIVER_TAKE, driver);
  assert.equal(assigned.assignedActorId, "driver-1");
  assert.equal(assigned.driverId, "driver-1");

  assert.throws(
    () => api.validateLiveActor(otherDriver, assigned, api.LIVE_ACTIONS.DRIVER_TAKE),
    /ya fue tomado|asignado/i,
  );
  assert.throws(
    () => api.validateLiveActor(otherDriver, assigned, api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP),
    /no está asignado/i,
  );
  assert.throws(
    () => api.validateExpectedVersion({expectedVersion: assigned.version - 1}, assigned),
    /Actualizá la vista/i,
  );
  assert.throws(
    () => api.validateLiveTransition(ready, api.LIVE_ACTIONS.DRIVER_MARK_DELIVERED),
    /retirado antes de entregarse/i,
  );

  const pickedUp = applyTransition(api, assigned, api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP, driver);
  assert.throws(
    () => api.validateLiveTransition(pickedUp, api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP),
    /asignado antes de retirarse/i,
  );

  const delivered = applyTransition(api, pickedUp, api.LIVE_ACTIONS.DRIVER_MARK_DELIVERED, driver);
  assert.deepEqual(Array.from(api.allowedLiveActions(delivered)), []);
});

test("store cannot request a driver before the order entered preparation", () => {
  const api = loadLiveOrderInternals();
  const store = {uid: "store-1", role: "store"};
  const created = api.liveOrderState({
    ...api.liveBirthContract({
      orderType: "local_order",
      source: api.LOCAL_SOURCE,
      idempotencyKey: "ord_driver_request_gate",
      trackingNumber: "PDL-REQ001",
      snapshot: {kind: "test"},
    }),
    storeId: "store-1",
  });

  const accepted = applyTransition(api, created, api.LIVE_ACTIONS.LOCAL_ACCEPT, store);
  assert.ok(!api.allowedLiveActions(accepted).includes(api.LIVE_ACTIONS.STORE_DRIVER_REQUEST));
  assert.throws(
    () => api.validateLiveTransition(accepted, api.LIVE_ACTIONS.STORE_DRIVER_REQUEST),
    /preparación antes de pedir repartidor/i,
  );

  const preparing = applyTransition(api, accepted, api.LIVE_ACTIONS.LOCAL_MARK_PREPARING, store);
  assert.ok(api.allowedLiveActions(preparing).includes(api.LIVE_ACTIONS.STORE_DRIVER_REQUEST));
});

test("existing live action replay is only idempotent for the same actor and action", () => {
  const api = loadLiveOrderInternals();
  const eventSnap = {
    get(field) {
      return {
        actorUid: "driver-1",
        actorRole: "driver",
        type: api.LIVE_ACTIONS.DRIVER_TAKE,
      }[field];
    },
  };

  assert.doesNotThrow(() =>
    api.validateExistingLiveEventReplay(
      eventSnap,
      {uid: "driver-1", role: "driver"},
      {action: api.LIVE_ACTIONS.DRIVER_TAKE},
    ),
  );
  assert.throws(
    () => api.validateExistingLiveEventReplay(
      eventSnap,
      {uid: "driver-2", role: "driver"},
      {action: api.LIVE_ACTIONS.DRIVER_TAKE},
    ),
    /no corresponde a este operador/i,
  );
  assert.throws(
    () => api.validateExistingLiveEventReplay(
      eventSnap,
      {uid: "driver-1", role: "driver"},
      {action: api.LIVE_ACTIONS.DRIVER_MARK_PICKED_UP},
    ),
    /no corresponde a este operador/i,
  );
});

test("basic incident and cancel are shown only when backend allows them with reason", () => {
  const api = loadLiveOrderInternals();
  const ui = read(driverUi);
  const source = read(adapter);

  assert.match(source, /LiveOrderAction\.OpenIncident/);
  assert.match(source, /LiveOrderAction\.CancelOrder/);
  assert.match(ui, /requiresDriverReason/);
  assert.match(ui, /Motivo operativo/);

  const assigned = api.liveOrderState({
    status: "assigned_to_driver",
    source: api.LOCAL_SOURCE,
    responsibleRole: "driver",
    currentResponsibleRole: "driver",
    assignedActorId: "driver-1",
    driverId: "driver-1",
    version: 4,
  });
  assert.ok(api.allowedLiveActions(assigned).includes(api.LIVE_ACTIONS.OPEN_INCIDENT));
  const incident = applyTransition(api, assigned, api.LIVE_ACTIONS.OPEN_INCIDENT, {uid: "driver-1", role: "driver"});
  assert.equal(incident.activeIncident, true);
  assert.equal(incident.currentResponsibleRole, "admin");
});

test("driver alignment keeps public admin and store boundaries intact", () => {
  const publicSource = read(app);
  const adminSource = read(adminUi);
  const storeSource = read(storeUi);
  const driverSource = read(driverUi);

  assert.match(publicSource, /TeamRole\.Local -> StoreApp/);
  assert.match(publicSource, /TeamRole\.Driver -> DriverApp/);
  assert.match(adminSource, /AdminLiveOrderActionRequest/);
  assert.match(storeSource, /StoreApp/);
  assert.doesNotMatch(driverSource, /collection\("orders"\)|FirebaseFirestore|AdminOperation|StoreActionCard/);
});
