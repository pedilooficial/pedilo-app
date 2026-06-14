const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");

function readRules() {
  return fs.readFileSync("firestore.rules", "utf8");
}

function matchBlock(rules, start, end) {
  const block = rules.match(new RegExp(`${start}[\\s\\S]*?${end}`));
  assert.ok(block, `Expected rules block matching ${start}`);
  return block[0];
}

test("rules protect operator data and reject direct order writes", () => {
  const rules = readRules();
  assert.match(rules, /match \/users\/\{userId\}/);
  assert.match(rules, /operatorRole\(\) in \["store", "driver", "admin"\]/);
  assert.match(rules, /match \/orders\/\{orderId\}/);
  assert.match(rules, /operatorRole\(\) == "store"\s+&& order\.storeId == request\.auth\.uid/);
  assert.match(rules, /operatorRole\(\) == "driver"\s+&& \(order\.driverId == request\.auth\.uid\s+\|\| order\.assignedActorId == request\.auth\.uid\)/);
  assert.match(rules, /order\.responsibleRole == "driver"/);
  assert.match(rules, /order\.assignedActorId == ""/);
  assert.match(rules, /allow create, update, delete: if false/);
  assert.doesNotMatch(rules, /isOwner/);
  assert.doesNotMatch(rules, new RegExp("cust" + "omer", "i"));
});

test("rules allow public catalog reads and own store product reads only", () => {
  const rules = readRules();

  assert.match(rules, /function storeVisible\(storeId\)/);
  assert.match(rules, /get\(.+stores\/\$\(storeId\).+\)\.data\.visible == true/s);
  assert.match(rules, /function productPubliclyVisible\(product\)/);
  assert.match(rules, /product\.visible == true/);
  assert.match(rules, /!\("available" in product\) \|\| product\.available == true/);

  assert.match(rules, /match \/stores\/\{storeId\}/);
  assert.match(rules, /allow read: if resource\.data\.visible == true/);
  assert.match(rules, /match \/products\/\{productId\}/);
  assert.match(rules, /storeVisible\(storeId\)\s+&& productPubliclyVisible\(resource\.data\)/);
  assert.match(rules, /operatorRole\(\) == "store"/);
  assert.match(rules, /operatorActive\(\)/);
  assert.match(rules, /request\.auth\.uid == storeId/);
  assert.match(rules, /allow create, update, delete: if false/);
});

test("public store reads are allowed only for visible store documents", () => {
  const rules = readRules();
  const storeBlock = matchBlock(rules, "match /stores/\\{storeId\\} \\{", "match /products");

  assert.match(storeBlock, /allow read: if resource\.data\.visible == true/);
  assert.doesNotMatch(storeBlock, /allow read: if true/);
  assert.doesNotMatch(storeBlock, /allow list: if true/);
});

test("product reads require public visibility or matching active store ownership", () => {
  const rules = readRules();
  const productBlock = matchBlock(rules, "match /products/\\{productId\\} \\{", "\\n      \\}");

  assert.match(productBlock, /storeVisible\(storeId\)\s+&& productPubliclyVisible\(resource\.data\)/);
  assert.match(productBlock, /operatorRole\(\) == "store"/);
  assert.match(productBlock, /request\.auth\.uid == storeId/);
  assert.match(rules, /product\.visible == true/);
  assert.match(rules, /!\("available" in product\) \|\| product\.available == true/);
  assert.doesNotMatch(productBlock, /allow read: if true/);
  assert.doesNotMatch(productBlock, /allow list: if true/);
});

test("public writes are denied for stores and products", () => {
  const rules = readRules();
  const storeBlock = matchBlock(rules, "match /stores/\\{storeId\\} \\{", "\\n    \\}");
  const productBlock = matchBlock(rules, "match /products/\\{productId\\} \\{", "\\n      \\}");

  assert.match(storeBlock, /allow create, update, delete: if false/);
  assert.match(productBlock, /allow create, update, delete: if false/);
  assert.doesNotMatch(storeBlock, /allow write: if true/);
  assert.doesNotMatch(productBlock, /allow write: if true/);
});

test("rules do not open public reads for orders or users", () => {
  const rules = readRules();
  const usersBlock = matchBlock(rules, "match /users/\\{userId\\} \\{", "\\n    \\}");
  const ordersBlock = matchBlock(rules, "match /orders/\\{orderId\\} \\{", "match /events");

  assert.doesNotMatch(usersBlock, /allow read: if true/);
  assert.doesNotMatch(ordersBlock, /allow read: if true/);
  assert.match(usersBlock, /signedIn\(\)/);
  assert.match(ordersBlock, /canReadOrder\(resource\.data\)/);
});

test("rules do not expose order tracking or roles collections", () => {
  const rules = readRules();

  assert.doesNotMatch(rules, /match \/order_tracking/);
  assert.doesNotMatch(rules, /match \/roles/);
});
