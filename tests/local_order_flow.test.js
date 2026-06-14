const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");
const {spawnSync} = require("node:child_process");

const publicApp = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const publicLocal = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicLocal.kt";
const adapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebasePublicOrderAdapter.kt";
const draftFactory = "app/src/main/java/com/pedilo/app/core/model/PublicLocalOrderDraftFactory.kt";
const rules = "firestore.rules";
const fn = "functions/index.js";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("local flow only submits through the use case on Confirmar pedido", () => {
  const app = read(publicApp);
  const local = read(publicLocal);

  assert.match(app, /publicLocalOrderUseCase\(\)/);
  assert.match(app, /onConfirm = \{/);
  assert.match(app, /buildLocalOrderDraft/);
  assert.match(app, /createLocalOrder\(draft\)/);
  assert.match(app, /isSubmittingLocalOrder/);
  assert.match(app, /PublicRoute\.LocalTicket\(currentRoute\.orderData, result\.value\)/);

  assert.match(local, /Confirmar pedido/);
  assert.match(local, /isSubmitting/);
  assert.match(local, /Confirmando\.\.\./);
  assert.doesNotMatch(local, /FirebaseFirestore|collection\("orders"\)|getHttpsCallable/);
});

test("cart, data, and confirmation screens do not write Firebase directly", () => {
  const result = spawnSync(
    "grep",
    [
      "-R",
      "collection(\"orders\"\\|collection('orders'",
      "-n",
      "app/src/main/java/com/pedilo/app/ui/publicuser",
    ],
    {encoding: "utf8"},
  );
  assert.notEqual(result.status, 0, result.stdout);
});

test("order draft carries real local catalog fields", () => {
  const local = read(publicLocal);
  const factory = read(draftFactory);

  assert.match(local, /val id: String/);
  assert.match(local, /val storeId: String/);
  assert.match(local, /productId = item\.product\.id/);
  assert.match(local, /storeId = item\.product\.storeId/);
  assert.match(local, /unitPriceCents = item\.product\.price\.toLong\(\) \* 100L/);
  assert.match(local, /publicLocalOrderDraft/);
  assert.match(factory, /source = "public_local"/);
});

test("local cart total matches backend item price contract without unsupported extras", () => {
  const local = read(publicLocal);

  assert.match(local, /fun localCartTotal\(items: List<LocalCartItem>\): Int = items\.sumOf \{ item ->\s*item\.product\.price \* item\.quantity\s*\}/);
  assert.match(local, /fun localGrandTotal\(items: List<LocalCartItem>\): Int = localCartTotal\(items\)/);
  assert.match(local, /LocalCartItem\(product, quantity, notes\.trim\(\)\)/);
  assert.doesNotMatch(local, /Extra queso|Salsa aparte|localDeliveryCost|Envío", formatLocalMoney|if \(item\.size == "Grande"\)|item\.extras/);
});

test("callable adapter calls createLocalOrder and never writes Firestore directly", () => {
  const source = read(adapter);

  assert.match(source, /getHttpsCallable\(CREATE_LOCAL_ORDER\)/);
  assert.match(source, /CREATE_LOCAL_ORDER = "createLocalOrder"/);
  assert.match(source, /toCallablePayload/);
  assert.doesNotMatch(source, /FirebaseFirestore|collection\("orders"\)|\.set\(|\.add\(|\.update\(|\.delete\(/);
});

test("orders remain closed to direct client writes", () => {
  const source = read(rules);
  const ordersBlock = source.match(/match \/orders\/\{orderId\} \{[\s\S]*?match \/events/)[0];

  assert.match(ordersBlock, /allow create, update, delete: if false/);
  assert.doesNotMatch(ordersBlock, /allow create: if true|allow write: if true/);
});

test("createLocalOrder validates and writes only orders", () => {
  const source = read(fn);
  const createLocalOrder = source.slice(
    source.indexOf("exports.createLocalOrder"),
    source.indexOf("exports.createPlusOrder"),
  );

  assert.match(createLocalOrder, /exports\.createLocalOrder/);
  assert.match(createLocalOrder, /cleanOrderPayload/);
  assert.match(source, /rawItems\.length === 0/);
  assert.match(source, /isValidPhone/);
  assert.match(createLocalOrder, /collection\(ORDERS\)\.doc\(idempotencyKey\)/);
  assert.match(createLocalOrder, /source: LOCAL_SOURCE/);
  assert.match(createLocalOrder, /liveBirthContract/);
  assert.match(createLocalOrder, /const delivery = \{\s*addressLine: clean\.customer\.address,\s*locality: "",\s*\}/);
  assert.match(createLocalOrder, /snapshot: \{[\s\S]*delivery,/);
  assert.match(createLocalOrder, /customer: clean\.customer,[\s\S]*delivery,/);
  assert.match(createLocalOrder, /trackingNumber/);
  assert.doesNotMatch(createLocalOrder, /collection\("(users|roles|payments|order_tracking)"\)|driverId|WhatsApp|whatsapp/);
});
