const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");
const vm = require("node:vm");

const functionsPath = "functions/index.js";
const publicTrackingModel = "app/src/main/java/com/pedilo/app/core/model/PublicTrackingState.kt";
const publicTrackingAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebasePublicTrackingAdapter.kt";
const publicTrackingUi = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicShopTracking.kt";
const adminModel = "app/src/main/java/com/pedilo/app/core/model/AdminOrderReadModels.kt";
const adminAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt";
const adminUi = "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt";
const storeModel = "app/src/main/java/com/pedilo/app/core/model/StoreOrderModels.kt";
const storeAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseStoreOrdersAdapter.kt";
const storeUi = "app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt";
const driverModel = "app/src/main/java/com/pedilo/app/core/model/DriverOrderModels.kt";
const driverAdapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseDriverOrdersAdapter.kt";
const driverUi = "app/src/main/java/com/pedilo/app/ui/driver/DriverApp.kt";
const rules = "firestore.rules";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

function loadFinancialInternals() {
  const source = read(functionsPath);
  const wrapped = `${source}
module.exports = {
  LIVE_ORDER_STATES,
  PAYMENT_METHOD_CASH,
  PAYMENT_METHOD_TRANSFER,
  PAYMENT_METHOD_CARD,
  PAYMENT_METHOD_ALREADY_PAID,
  buildFinancialContract,
  normalizePaymentMethod,
  parsePublicAmountToCents,
  publicTrackingResponse,
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

  vm.runInNewContext(wrapped, sandbox, {filename: functionsPath});
  return sandbox.module.exports;
}

test("financial contract normalizes cash transfer and declared paid without external gateway", () => {
  const api = loadFinancialInternals();

  const cash = api.buildFinancialContract({
    paymentMethod: "cash",
    subtotal: 125000,
    source: "public_local",
    orderType: "local_order",
  });
  assert.equal(cash.financialStatus, "collect_on_delivery");
  assert.equal(cash.amountToCollect, 125000);
  assert.equal(cash.collectionRequired, true);
  assert.equal(cash.cashResponsibleRole, "driver");
  assert.equal(cash.financialSnapshot.total, 125000);

  const transfer = api.buildFinancialContract({
    paymentMethod: "transferencia",
    subtotal: 90000,
    source: "public_plus_buy",
    orderType: "direct_purchase",
  });
  assert.equal(transfer.financialStatus, "transfer_declared_pending");
  assert.equal(transfer.amountToCollect, 0);
  assert.match(transfer.financialNotes, /no validada bancariamente/i);

  const declared = api.buildFinancialContract({
    paymentMethod: "Ya está pago",
    subtotal: 0,
    source: "public_plus_pickup_shipping",
    orderType: "pickup_shipping",
  });
  assert.equal(declared.financialStatus, "paid_declared");
  assert.equal(declared.collectionRequired, false);
});

test("financial contract rejects invalid payment methods negative amounts and card gateway claims", () => {
  const api = loadFinancialInternals();

  assert.throws(
    () => api.buildFinancialContract({paymentMethod: "card", subtotal: 1000, source: "x", orderType: "x"}),
    /pasarela de pago/i,
  );
  assert.throws(
    () => api.buildFinancialContract({paymentMethod: "cripto", subtotal: 1000, source: "x", orderType: "x"}),
    /forma de pago válida/i,
  );
  assert.throws(
    () => api.buildFinancialContract({paymentMethod: "cash", subtotal: -1, source: "x", orderType: "x"}),
    /monto subtotal/i,
  );
  assert.throws(
    () => api.parsePublicAmountToCents("-100"),
    /monto informado/i,
  );
  assert.equal(api.parsePublicAmountToCents("$18.500"), 1850000);
});

test("local and plus order creation persist financial snapshot and coherent fields", () => {
  const source = read(functionsPath);
  const local = source.slice(source.indexOf("exports.createLocalOrder"), source.indexOf("exports.createPlusOrder"));
  const plus = source.slice(source.indexOf("function plusOrderData"), source.indexOf("function liveBirthContract"));

  assert.match(local, /buildFinancialContract\(\{[\s\S]*paymentMethod: clean\.paymentMethod[\s\S]*subtotal/);
  assert.match(local, /financialSnapshot/);
  assert.match(local, /\.\.\.finance/);
  assert.match(plus, /parsePublicAmountToCents\(clean\.amount\)/);
  assert.match(plus, /buildFinancialContract/);
  assert.match(plus, /\.\.\.finance/);
});

test("public tracking exposes safe total and method but no internal finance", () => {
  const api = loadFinancialInternals();
  const response = api.publicTrackingResponse({
    status: "created",
    trackingNumber: "PDL-G0001",
    publicStatus: "Pedido recibido",
    source: "public_local",
    storeName: "Local Centro",
    items: [{name: "Pizza", quantity: 1}],
    paymentMethod: "cash",
    total: 450000,
    amountToCollect: 450000,
    collectionRequired: true,
  }, "PDL-G0001");

  assert.equal(response.paymentLabel, "Efectivo al recibir");
  assert.equal(response.publicTotal, "$4.500");
  assert.match(response.collectionMessage, /Monto a pagar al recibir/);

  const joined = [publicTrackingModel, publicTrackingAdapter, publicTrackingUi].map(read).join("\n");
  assert.match(joined, /paymentLabel/);
  assert.match(joined, /publicTotal/);
  assert.match(joined, /collectionMessage/);
  assert.doesNotMatch(joined, /cashResponsibleActorId|cashResponsibleRole|collectedAmount|financialSnapshot|debt|cashbox|settlement|rendici[oó]n/i);
});

test("admin store and driver expose controlled financial and cashbox surfaces", () => {
  const admin = [adminModel, adminAdapter, adminUi].map(read).join("\n");
  const store = [storeModel, storeAdapter, storeUi].map(read).join("\n");
  const driver = [driverModel, driverAdapter, driverUi].map(read).join("\n");

  for (const token of ["financialStatus", "paymentMethod", "amountToCollect", "collectionRequired"]) {
    assert.match(admin, new RegExp(token));
    assert.match(store, new RegExp(token));
    assert.match(driver, new RegExp(token));
  }
  assert.match(admin, /financialNotes/);
  assert.match(admin, /Responsable de cobro/);
  assert.match(store, /cobro operativo/);
  assert.match(driver, /Cerrar caja operativa/);
  assert.match(driver, /closeDriverCashbox/);
  assert.doesNotMatch(store, /cashResponsibleActorId|collectedAmount|financialSnapshot/);
  assert.doesNotMatch(`${store}\n${driver}`, /pasarela real|banco real|conciliación bancaria automática/i);
});

test("rules keep order financial writes behind backend only and operation roles intact", () => {
  const source = read(rules);
  const ordersBlock = source.match(/match \/orders\/\{orderId\} \{[\s\S]*?match \/events/)[0];

  assert.match(ordersBlock, /allow create, update, delete: if false/);
  assert.match(source, /operatorRole\(\) in \["store", "driver", "admin"\] && operatorActive\(\)/);
  assert.match(source, /order\.driverId == request\.auth\.uid/);
  assert.match(source, /match \/cashbox_closures\/\{cashboxId\}/);
});
