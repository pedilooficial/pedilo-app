"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const test = require("node:test");
const {spawnSync} = require("node:child_process");

function read(path) {
  return fs.readFileSync(path, "utf8");
}

const inputs = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicInputs.kt";
const local = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicLocal.kt";
const plus = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicPlus.kt";
const conventions = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicConventions.kt";
const tracking = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicShopTracking.kt";
const home = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicHome.kt";
const publicModel = "app/src/main/java/com/pedilo/app/core/model/PublicTrackingState.kt";
const publicApp = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const rules = "firestore.rules";

test("public validation helpers reject placeholders and strict tracking shape", () => {
  const source = read(inputs);

  assert.match(source, /PublicTrackingPattern = Regex\("\^PDL-\[A-Z0-9\]\{4,10\}\$"\)/);
  assert.match(source, /fun isPublicPlaceholder/);
  assert.match(source, /fun hasPublicValue/);
  for (const placeholder of ["tu nombre", "telefono", "dirección", "producto", "paquete"]) {
    assert.match(source, new RegExp(placeholder));
  }
});

test("local public order cannot continue with empty cart placeholders or invalid phone", () => {
  const source = read(local);

  assert.match(source, /enabled = cartItems\.isNotEmpty\(\), onClick = onContinue/);
  assert.match(source, /cartItems\.isNotEmpty\(\) && hasPublicValue\(name\) && isValidPublicPhone\(phone\) && hasPublicValue\(address\)/);
  assert.match(source, /buildLocalOrderDraft/);
  assert.match(source, /publicLocalOrderDraft/);
  assert.doesNotMatch(source, /FirebaseFirestore|collection\("orders"\)|getHttpsCallable/);
});

test("plus buy and pickup shipping reject missing or placeholder public values before confirmation", () => {
  const source = read(plus);

  assert.match(source, /val canAddProduct = hasPublicValue\(productName\) && hasPublicValue\(productDetail\)/);
  assert.match(source, /products\.isNotEmpty\(\) && hasPublicValue\(source\) && hasPublicValue\(contactName\) && isValidPublicPhone\(phone\) && hasPublicValue\(address\)/);
  assert.match(source, /hasPublicValue\(pickupAddress\)/);
  assert.match(source, /hasPublicValue\(destination\)/);
  assert.match(source, /hasPublicValue\(description\)/);
  assert.match(source, /alreadyPaid \|\| hasPublicValue\(amount\)/);
  assert.match(source, /buildPlusOrderDraft/);
  assert.doesNotMatch(source, /FirebaseFirestore|collection\("orders"\)|getHttpsCallable/);
});

test("public tracking UI and model stay public-only", () => {
  const screen = read(tracking);
  const model = read(publicModel);

  assert.match(screen, /GetPublicTrackingUseCase/);
  assert.match(screen, /isValidPublicTrackingNumber\(clean\)/);
  assert.match(screen, /state\.publicStatus/);
  assert.match(screen, /state\.humanMessage/);
  assert.match(screen, /if \(!state\.isClosed && state\.summary\.isNotBlank\(\)\)/);
  assert.doesNotMatch(`${screen}\n${model}`, /responsibleRole|currentResponsibleRole|assignedActorId|assignedActorRole|driverId|events|incidents|audit|payload/i);
});

test("public claim screen persists through backend callable without direct order writes", () => {
  const source = read(conventions);

  assert.match(source, /publicClaimUseCase\(\)/);
  assert.match(source, /PublicClaimDraft/);
  assert.match(source, /Enviar reclamo/);
  assert.match(source, /receiptMessage/);
  assert.doesNotMatch(source, /createIncident|open_incident|collection\("incidents"\)|collection\("orders"\)/);
});

test("public entry remains in public navigation and never writes orders directly", () => {
  const app = read(publicApp);
  const ruleSource = read(rules);
  const ordersBlock = ruleSource.match(/match \/orders\/\{orderId\} \{[\s\S]*?match \/events/)[0];
  const directWrites = spawnSync(
    "grep",
    [
      "-R",
      "collection(\"orders\"\\|collection('orders'",
      "-n",
      "app/src/main/java/com/pedilo/app/ui/publicuser",
    ],
    {encoding: "utf8"},
  );

  assert.match(app, /PublicRoute\.Home/);
  assert.match(app, /PublicHomeScreen/);
  assert.match(app, /PublicPlusChoiceScreen/);
  assert.match(app, /PublicShopScreen/);
  assert.match(ordersBlock, /allow create, update, delete: if false/);
  assert.notEqual(directWrites.status, 0, directWrites.stdout);
});

test("public home explains real ordering flow without fake catalog data", () => {
  const source = read(home);

  assert.match(source, /PublicFlowCard/);
  assert.match(source, /Pedí sin vueltas/);
  assert.match(source, /"Local"/);
  assert.match(source, /"Datos"/);
  assert.match(source, /"Seguí"/);
  assert.match(source, /Cuando el entorno de prueba cargue catálogo autorizado/);
  assert.match(source, /PediloIconKind\.Check/);
  assert.doesNotMatch(source, /Pizzería Roma|Pizza muzzarella|pedido demo|datos demo/i);
});
