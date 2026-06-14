const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");

const functionsPath = "functions/index.js";
const liveModelPath = "app/src/main/java/com/pedilo/app/core/model/LiveOrderContract.kt";
const adminModelPath = "app/src/main/java/com/pedilo/app/core/model/AdminOrderReadModels.kt";
const adminAdapterPath = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt";
const rulesPath = "firestore.rules";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("new public orders are born as non-floating Pedido Vivo Universal records", () => {
  const source = read(functionsPath);
  const contract = source.slice(
    source.indexOf("function liveBirthContract"),
    source.indexOf("async function createOrderWithInitialEvent"),
  );

  for (const field of [
    "orderType",
    "status: STATUS",
    "publicStatus: PUBLIC_STATUS",
    "operationalStatus: \"waiting_admin_review\"",
    "financialStatus: FINANCIAL_STATUS_PENDING",
    "communicationStatus: COMMUNICATION_STATUS_PREPARED",
    "incidentStatus: INCIDENT_STATUS_NONE",
    "archiveStatus: ARCHIVE_STATUS_LIVE",
    "currentResponsibleRole: responsibleRole",
    "assignedActorId: ASSIGNED_ACTOR_UNASSIGNED",
    "nextAllowedActions",
    "const normalizedSnapshot = liveOrderSnapshot",
    "liveSnapshot: normalizedSnapshot",
    "initialSnapshot: normalizedSnapshot",
    "timeoutPolicy: INITIAL_TIMEOUT_POLICY",
    "fallbackPolicy: INITIAL_FALLBACK_POLICY",
    "version: LIVE_ORDER_VERSION",
    "idempotencyKey",
  ]) {
    assert.match(contract, new RegExp(field.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  }
});

test("local and plus creation use conservative public idempotency keys", () => {
  const source = read(functionsPath);
  const local = source.slice(source.indexOf("exports.createLocalOrder"), source.indexOf("exports.createPlusOrder"));
  const plus = source.slice(source.indexOf("exports.createPlusOrder"), source.indexOf("exports.getPublicOrderTracking"));

  assert.match(local, /publicIdempotencyKey\(LOCAL_SOURCE, clean\)/);
  assert.match(local, /collection\(ORDERS\)\.doc\(idempotencyKey\)/);
  assert.match(plus, /publicIdempotencyKey\(clean\.source, clean\)/);
  assert.match(plus, /collection\(ORDERS\)\.doc\(idempotencyKey\)/);
  assert.match(source, /createHash\("sha256"\)/);
  assert.match(source, /stableStringify/);
  assert.match(source, /replace\(\/\^ord_/);
  assert.match(source, /clean\.slice\(-6\)\.toUpperCase\(\)/);
});

test("order birth is transactional and writes the initial audit event once", () => {
  const source = read(functionsPath);
  const birth = source.slice(
    source.indexOf("async function createOrderWithInitialEvent"),
    source.indexOf("function publicIdempotencyKey"),
  );

  assert.match(birth, /db\.runTransaction/);
  assert.match(birth, /const existing = await tx\.get\(orderRef\)/);
  assert.match(birth, /if \(existing\.exists\) return/);
  assert.match(birth, /tx\.create\(orderRef, \{/);
  assert.match(birth, /assistedDecisionOrderPatch\(assistedDecision\)/);
  assert.match(birth, /orderRef\.collection\("events"\)\.doc\("initial"\)/);
  assert.match(birth, /type: "order_created"/);
  assert.match(birth, /actorRole: "public_user"/);
  assert.match(birth, /version: orderData\.version/);
  assert.match(birth, /previousVersion: 0/);
  assert.match(birth, /nextVersion: orderData\.version/);
  assert.match(birth, /result: \{/);
  assert.match(birth, /audit: \{/);
  assert.match(birth, /idempotencyKey: orderData\.idempotencyKey/);
  assert.match(birth, /communicationRecordsForOrder/);
  assert.match(birth, /writeOrderCommunications\(tx, orderRef, communicationRecords\)/);
  assert.match(birth, /writeAssistedDecision\(tx, orderRef, assistedDecision\)/);
});

test("admin live action path carries version concurrency without legacy operation", () => {
  const source = read(functionsPath);
  const liveCallable = source.slice(
    source.indexOf("exports.operateLiveOrder"),
    source.indexOf("exports.adminUpdateTeamUser"),
  );
  const clean = source.slice(
    source.indexOf("function cleanLiveActionPayload"),
    source.indexOf("function liveActionId"),
  );

  assert.match(clean, /expectedVersion/);
  assert.match(liveCallable, /validateExpectedVersion\(clean, current\)/);
  assert.match(liveCallable, /version: current\.version \+ 1/);
  assert.doesNotMatch(source, /exports\.adminOrderAction|cleanAdminActionPayload|adminActionEffect|ADMIN_ACTIONS/);

  const adminUi = read("app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt");
  assert.match(adminUi, /adminOrders\.executeLive/);
  assert.doesNotMatch(adminUi, /executeAdminOrderAction|AdminOrderActionRequest|adminOrders\.execute\(/);
});

test("core and admin adapter expose live order birth vocabulary read-only", () => {
  const liveModel = read(liveModelPath);
  const adminModel = read(adminModelPath);
  const adapter = read(adminAdapterPath);

  for (const token of [
    "LiveOrderType",
    "LocalOrder(\"local_order\")",
    "DirectPurchase(\"direct_purchase\")",
    "PickupShipping(\"pickup_shipping\")",
    "WaitingAdminReview(\"waiting_admin_review\")",
    "LiveOrderBirthState",
  ]) {
    assert.match(liveModel, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  }

  for (const field of [
    "orderType",
    "financialStatus",
    "communicationStatus",
    "incidentStatus",
    "archiveStatus",
    "currentResponsibleRole",
    "assignedActorId",
    "assignedActorRole",
    "version",
    "idempotencyKey",
  ]) {
    assert.match(adminModel, new RegExp(`val ${field}`));
    assert.match(adapter, new RegExp(field === "version" ? "version = .*version\\(\\)" : `${field} = .*getString`));
  }

  assert.doesNotMatch(adapter, /\.set\(|\.update\(|\.delete\(|writeBatch|runTransaction/);
});

test("rules keep live order and audit subcollections protected from client writes", () => {
  const rules = read(rulesPath);
  const ordersBlock = rules.match(/match \/orders\/\{orderId\} \{[\s\S]*?\n    \}/)[0];

  assert.match(ordersBlock, /allow create, update, delete: if false/);
  assert.match(ordersBlock, /match \/events\/\{eventId\}/);
  assert.match(ordersBlock, /match \/incidents\/\{incidentId\}/);
  assert.match(ordersBlock, /match \/communications\/\{communicationId\}/);
  assert.match(ordersBlock, /allow write: if false/);
});
