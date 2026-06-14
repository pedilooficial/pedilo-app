const fs = require("node:fs");
const test = require("node:test");
const assert = require("node:assert/strict");

const app = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const driverUi = "app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt";
const adapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt";
const port = "app/src/main/java/com/pedilo/app/core/port/DriverOrdersPort.kt";
const runtime = "app/src/main/java/com/pedilo/app/core/runtime/DriverRuntime.kt";
const useCase = "app/src/main/java/com/pedilo/app/core/usecase/GetDriverOrdersUseCase.kt";
const rules = "firestore.rules";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("driver role opens operational DriverApp from team access", () => {
  const source = read(app);

  assert.match(source, /import com\.pedilo\.app\.ui\.driver\.DriverApp/);
  assert.match(source, /TeamRole\.Driver -> DriverApp\(onSignOutConfirmed = onSignOutConfirmed\)/);
  assert.doesNotMatch(source, /TeamRole\.Driver -> TeamRolePlaceholderScreen/);
});

test("driver adapter reads available and assigned orders without direct writes", () => {
  const source = read(adapter);

  assert.match(source, /whereEqualTo\(RESPONSIBLE_ROLE, DRIVER_ROLE\)/);
  assert.match(source, /whereEqualTo\(ASSIGNED_ACTOR_ID, ""\)/);
  assert.match(source, /whereEqualTo\(DRIVER_ID, uid\)/);
  assert.match(source, /isVisibleToDriver\(uid\)/);
  assert.match(source, /val fallbackAddress = customer\?\.get\(ADDRESS\)\?\.toString\(\)\.orEmpty\(\)/);
  assert.match(source, /else if \(fallbackAddress\.isNotBlank\(\)\)/);
  assert.doesNotMatch(source, /collection\("orders"\)\.add|\.set\(|\.update\(|\.delete\(|writeBatch|runTransaction/);
});

test("driver port and runtime expose observe detail and operateLiveOrder only", () => {
  const joined = [port, runtime, useCase, adapter].map(read).join("\n");

  assert.match(joined, /interface DriverOrdersPort/);
  assert.match(joined, /observeAvailableAndAssignedOrders/);
  assert.match(joined, /getVisibleOrderDetail/);
  assert.match(joined, /executeDriverOrderAction/);
  assert.match(joined, /FirebaseDriverOrdersAdapter/);
  assert.match(joined, /getHttpsCallable\(OPERATE_LIVE_ORDER\)/);
  assert.match(joined, /OPERATE_LIVE_ORDER = "operateLiveOrder"/);
  assert.doesNotMatch(joined, /createLocalOrder|createPlusOrder|getPublicOrderTracking|adminOrderAction/);
});

test("driver UI shows real state version and nextAllowedActions without inventing wire names", () => {
  const source = read(driverUi);

  assert.match(source, /DriverMetricsRow/);
  assert.match(source, /DriverWorkPlanCard/);
  assert.match(source, /DriverEmptyState/);
  assert.match(source, /current\.version/);
  assert.match(source, /current\.operationalStatus/);
  assert.match(source, /current\.nextAllowedActions/);
  assert.match(source, /PendingDriverAction\(current\.id, action, current\.version\)/);
  assert.match(source, /availableOrders = orders\.filter/);
  assert.match(source, /assignedOrders = orders\.filter/);
  assert.match(source, /Siguiente: \$\{order\.driverNextStepLabel\(\)\}/);
  assert.match(source, /Cuando un local solicite repartidor/);
  assert.doesNotMatch(source, /"driver_take"|"driver_mark_picked_up"|"driver_mark_delivered"/);
});

test("driver UI connects take pickup delivered incident and cancel through operateLiveOrder request", () => {
  const source = read(driverUi);

  assert.match(source, /AdminLiveOrderActionRequest/);
  assert.match(source, /driverOrders\.execute/);
  assert.match(source, /expectedVersion = pending\.expectedVersion/);
  assert.match(source, /reason = reason/);
  assert.match(source, /Confirmar acción/);
  assert.match(source, /Motivo operativo/);
  for (const action of [
    "LiveOrderAction.DriverTake",
    "LiveOrderAction.DriverMarkPickedUp",
    "LiveOrderAction.DriverMarkDelivered",
    "LiveOrderAction.OpenIncident",
    "LiveOrderAction.CancelOrder",
  ]) {
    assert.match(source, new RegExp(action.replace(".", "\\.")));
  }
});

test("driver rules allow read for assigned orders and unassigned driver-ready orders only", () => {
  const source = read(rules);

  assert.match(source, /order\.driverId == request\.auth\.uid/);
  assert.match(source, /operatorRole\(\) == "driver"/);
  assert.match(source, /order\.responsibleRole == "driver"/);
  assert.match(source, /order\.assignedActorId == ""/);
});
