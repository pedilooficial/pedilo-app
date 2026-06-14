const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");
const {spawnSync} = require("node:child_process");

function read(path) {
  return fs.readFileSync(path, "utf8");
}

const inputs = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicInputs.kt";
const local = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicLocal.kt";
const plus = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicPlus.kt";
const conventions = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicConventions.kt";
const shop = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicShop.kt";
const subcategory = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicShopSubcategory.kt";
const tracking = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicShopTracking.kt";
const manifest = "app/src/main/AndroidManifest.xml";

test("public phone input sanitizes letters and enforces 8 to 15 digits", () => {
  const source = read(inputs);

  assert.match(source, /fun normalizePublicPhoneInput/);
  assert.match(source, /char\.isDigit\(\)/);
  assert.match(source, /char == '\+'/);
  assert.match(source, /digits in 8\.\.15/);
  assert.match(source, /KeyboardType\.Phone/);
  assert.match(source, /Ingresá un teléfono válido/);
});

test("local and plus flows require valid phone before continuing", () => {
  assert.match(read(local), /isValidPublicPhone\(phone\)/);
  assert.match(read(local), /PublicPhoneInput\("WhatsApp"/);
  assert.match(read(plus), /isValidPublicPhone\(phone\)/);
  assert.match(read(plus), /PublicPhoneInput\("WhatsApp"/);
});

test("claim and tracking inputs normalize tracking and do not use placeholders as values", () => {
  const source = read(conventions);

  assert.match(source, /normalizePublicTrackingInput\(it\)/);
  assert.match(source, /PublicTrackingInput/);
  assert.match(source, /isValidPublicTrackingNumber\(orderNumber\)/);
  assert.doesNotMatch(source, /placeholder = "PDL-123456"/);
});

test("public tracking inputs normalize, limit and validate before consulting", () => {
  const source = read(inputs);

  assert.match(source, /fun normalizePublicTrackingInput/);
  assert.match(source, /\.uppercase\(\)/);
  assert.match(source, /\.take\(MaxTrackingChars\)/);
  assert.match(source, /startsWith\("PDL-"\)/);
  assert.match(read(shop), /normalizePublicTrackingInput\(it\)/);
  assert.match(read(shop), /isValidPublicTrackingNumber\(code\)/);
  assert.match(read(tracking), /isValidPublicTrackingNumber\(clean\)/);
});

test("public inputs use a light cursor and muted placeholders", () => {
  const source = read(inputs);

  assert.match(source, /cursorBrush = SolidColor\(PediloText\)/);
  assert.match(source, /PediloMuted\.copy\(alpha = 0\.68f\)/);
  assert.match(read(shop), /cursorBrush = SolidColor\(PediloText\)/);
  assert.match(read(tracking), /cursorBrush = SolidColor\(PediloText\)/);
});

test("portrait orientation is locked to avoid activity recreation during public flows", () => {
  assert.match(read(manifest), /android:screenOrientation="portrait"/);
});

test("subcategory store card opens from the whole visible card", () => {
  const source = read(subcategory);

  assert.match(source, /\.fillMaxWidth\(\)[\s\S]*\.height\(148\.dp\)[\s\S]*\.clickable\(role = Role\.Button, onClick = \{ onView\(store\.id\) \}\)/);
});

test("public UI has no technical user-facing strings or direct order writes", () => {
  const technical = spawnSync(
    "grep",
    [
      "-R",
      "Firebase\\|backend\\|callable\\|exception\\|stacktrace\\|mock\\|demo\\|de muestra\\|tracking persistente",
      "-n",
      "app/src/main/java/com/pedilo/app/ui/publicuser",
    ],
    {encoding: "utf8"},
  );
  assert.notEqual(technical.status, 0, technical.stdout);

  const orders = spawnSync(
    "grep",
    [
      "-R",
      "collection(\"orders\"\\|collection('orders'",
      "-n",
      "app/src/main/java/com/pedilo/app/ui/publicuser",
    ],
    {encoding: "utf8"},
  );
  assert.notEqual(orders.status, 0, orders.stdout);
});
