const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");
const {spawnSync} = require("node:child_process");

const publicApp = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const publicPlus = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicPlus.kt";
const adapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebasePublicPlusOrderAdapter.kt";
const useCase = "app/src/main/java/com/pedilo/app/core/usecase/CreatePublicPlusOrderUseCase.kt";
const validator = "app/src/main/java/com/pedilo/app/core/usecase/ValidatePublicPlusOrderDraftUseCase.kt";
const factory = "app/src/main/java/com/pedilo/app/core/model/PublicPlusOrderDraftFactory.kt";
const rules = "firestore.rules";
const fn = "functions/index.js";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("plus flow submits through the use case only on Confirmar pedido", () => {
  const app = read(publicApp);
  const plus = read(publicPlus);

  assert.match(app, /publicPlusOrderUseCase\(\)/);
  assert.match(app, /isSubmittingPlusOrder/);
  assert.match(app, /buildPlusOrderDraft\(currentRoute\.request\)/);
  assert.match(app, /createPlusOrder\(draft\)/);
  assert.match(app, /PublicRoute\.PlusTicket\(currentRoute\.request, result\.value\)/);
  assert.match(plus, /Confirmar pedido/);
  assert.match(plus, /Confirmando\.\.\./);
  assert.doesNotMatch(plus, /FirebaseFirestore|collection\("orders"\)|getHttpsCallable/);
});

test("buy and pickup screens only build local request data before confirmation", () => {
  const plus = read(publicPlus);

  assert.match(plus, /PublicPlusBuyScreen/);
  assert.match(plus, /PublicPlusPickupShippingScreen/);
  assert.match(plus, /onContinue\(.*PublicPlusRequest/s);
  assert.doesNotMatch(plus, /createPlusOrder|FirebasePublicPlusOrderAdapter/);
});

test("plus draft carries separated buy and pickup shipping contracts", () => {
  const plus = read(publicPlus);
  const draftFactory = read(factory);

  assert.match(plus, /PublicPlusRequestType\.Buy -> publicPlusBuyOrderDraft/);
  assert.match(plus, /PublicPlusRequestType\.PickupShipping -> publicPlusPickupShippingOrderDraft/);
  assert.match(draftFactory, /source = "public_plus_buy"/);
  assert.match(draftFactory, /source = "public_plus_pickup_shipping"/);
  assert.match(draftFactory, /requestType = PublicPlusOrderType\.BUY/);
  assert.match(draftFactory, /requestType = PublicPlusOrderType\.PICKUP_SHIPPING/);
});

test("plus use case validates before calling the adapter", () => {
  const source = read(useCase);
  const validation = read(validator);

  assert.match(source, /validateDraft\(draft\)/);
  assert.match(source, /orderPort\.createPlusOrder\(draft\)/);
  assert.match(validation, /public_plus_buy/);
  assert.match(validation, /public_plus_pickup_shipping/);
  assert.match(validation, /draft\.contact\.phone\.count\(Char::isDigit\) !in 8\.\.15/);
  assert.match(validation, /draft\.items\.isEmpty\(\)/);
  assert.match(validation, /draft\.sourceReference\.isBlankOrPlaceholder\(\)/);
  assert.match(validation, /placeholderValues/);
});

test("callable adapter calls createPlusOrder and never writes Firestore directly", () => {
  const source = read(adapter);

  assert.match(source, /getHttpsCallable\(CREATE_PLUS_ORDER\)/);
  assert.match(source, /CREATE_PLUS_ORDER = "createPlusOrder"/);
  assert.match(source, /toCallablePayload/);
  assert.doesNotMatch(source, /FirebaseFirestore|collection\("orders"\)|\.set\(|\.add\(|\.update\(|\.delete\(/);
});

test("plus UI does not write orders directly", () => {
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

test("orders remain closed to direct client writes", () => {
  const source = read(rules);
  const ordersBlock = source.match(/match \/orders\/\{orderId\} \{[\s\S]*?match \/events/)[0];

  assert.match(ordersBlock, /allow create, update, delete: if false/);
  assert.doesNotMatch(ordersBlock, /allow create: if true|allow write: if true/);
});

test("createPlusOrder validates and writes only orders", () => {
  const source = read(fn);
  const createPlusOrder = source.slice(
    source.indexOf("exports.createPlusOrder"),
    source.indexOf("exports.getPublicOrderTracking"),
  );

  assert.match(createPlusOrder, /exports\.createPlusOrder/);
  assert.match(source, /cleanPlusOrderPayload/);
  assert.match(source, /public_plus_buy/);
  assert.match(source, /public_plus_pickup_shipping/);
  assert.match(createPlusOrder, /collection\(ORDERS\)\.doc\(idempotencyKey\)/);
  assert.match(createPlusOrder, /plusOrderData/);
  assert.match(createPlusOrder, /trackingNumber/);
  assert.doesNotMatch(createPlusOrder, /collection\("(users|roles|payments|order_tracking)"\)|driverId|WhatsApp|whatsapp/);
});
