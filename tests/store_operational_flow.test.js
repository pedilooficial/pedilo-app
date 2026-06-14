const fs = require("node:fs");
const test = require("node:test");
const assert = require("node:assert/strict");

const app = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const storeUi = "app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt";
const adapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseStoreOrdersAdapter.kt";
const port = "app/src/main/java/com/pedilo/app/core/port/StoreOrdersPort.kt";
const runtime = "app/src/main/java/com/pedilo/app/core/runtime/StoreRuntime.kt";
const useCase = "app/src/main/java/com/pedilo/app/core/usecase/GetStoreOrdersUseCase.kt";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("store role opens operational StoreApp without touching driver UI", () => {
  const source = read(app);

  assert.match(source, /import com\.pedilo\.app\.ui\.store\.StoreApp/);
  assert.match(source, /TeamRole\.Local -> StoreApp\(onSignOutConfirmed = onSignOutConfirmed\)/);
  assert.match(source, /TeamRole\.Driver -> DriverApp\(onSignOutConfirmed = onSignOutConfirmed\)/);
});

test("store adapter reads only own orders by auth uid", () => {
  const source = read(adapter);

  assert.match(source, /auth\.currentUser\?\.uid/);
  assert.match(source, /whereEqualTo\(STORE_ID, uid\)/);
  assert.match(source, /doc\.getString\(STORE_ID\)\.orEmpty\(\) != uid/);
  assert.doesNotMatch(source, /FirebaseFirestore\.getInstance\(\)\.collection\("orders"\)\.add|\.set\(|\.update\(|\.delete\(|writeBatch|runTransaction/);
});

test("store port and runtime expose own orders and operateLiveOrder only", () => {
  const joined = [port, runtime, useCase, adapter].map(read).join("\n");

  assert.match(joined, /interface StoreOrdersPort/);
  assert.match(joined, /observeOwnOrders/);
  assert.match(joined, /getOwnOrderDetail/);
  assert.match(joined, /getOwnProducts/);
  assert.match(joined, /executeStoreOrderAction/);
  assert.match(joined, /FirebaseStoreOrdersAdapter/);
  assert.match(joined, /getHttpsCallable\(OPERATE_LIVE_ORDER\)/);
  assert.match(joined, /OPERATE_LIVE_ORDER = "operateLiveOrder"/);
  assert.doesNotMatch(joined, /createLocalOrder|createPlusOrder|getPublicOrderTracking|adminOrderAction/);
});

test("store UI shows only nextAllowedActions and never invents action wire names", () => {
  const source = read(storeUi);

  assert.match(source, /StoreCatalogPanel/);
  assert.match(source, /StoreMetricsRow/);
  assert.match(source, /StoreWorkPlanCard/);
  assert.match(source, /StoreEmptyState/);
  assert.match(source, /Catálogo propio leído desde Firestore/);
  assert.match(source, /current\.nextAllowedActions\.isEmpty\(\)/);
  assert.match(source, /items\(current\.nextAllowedActions\)/);
  assert.match(source, /StoreActionCard\(action = action/);
  assert.match(source, /PendingStoreAction\(current\.id, action, current\.version\)/);
  assert.match(source, /Siguiente: \$\{order\.storeNextStepLabel\(\)\}/);
  assert.match(source, /Cuando entre un pedido de este local/);
  assert.doesNotMatch(source, /"local_accept"|"local_reject"|"local_mark_preparing"|"local_mark_ready"|"driver_take"/);
});

test("store UI connects accept reject preparation ready and incident through operateLiveOrder request", () => {
  const source = read(storeUi);

  assert.match(source, /AdminLiveOrderActionRequest/);
  assert.match(source, /storeOrders\.execute/);
  assert.match(source, /expectedVersion = pending\.expectedVersion/);
  assert.match(source, /reason = reason/);
  assert.match(source, /Confirmar acción/);
  assert.match(source, /Motivo operativo/);
  for (const action of [
    "LiveOrderAction.LocalAccept",
    "LiveOrderAction.LocalReject",
    "LiveOrderAction.LocalMarkPreparing",
    "LiveOrderAction.LocalMarkReady",
    "LiveOrderAction.StoreDriverRequest",
    "LiveOrderAction.CancelOrder",
    "LiveOrderAction.OpenIncident",
  ]) {
    assert.match(source, new RegExp(action.replace(".", "\\.")));
  }
});

test("store flow keeps direct order writes out of UI and adapter", () => {
  const joined = [storeUi, adapter].map(read).join("\n");

  assert.doesNotMatch(joined, /collection\("orders"\)\.add|collection\("orders"\).*\.set|\.update\(|\.delete\(|writeBatch|runTransaction/);
  assert.doesNotMatch(joined, /datos demo|PDL-123456|pedido demo/i);
});
