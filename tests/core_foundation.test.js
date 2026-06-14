const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");
const {spawnSync} = require("node:child_process");

const coreRoot = "app/src/main/java/com/pedilo/app/core";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("clean core foundation has the expected package structure", () => {
  for (const dir of ["model", "result", "port", "usecase"]) {
    assert.ok(fs.statSync(`${coreRoot}/${dir}`).isDirectory(), `Missing ${dir}`);
  }
});

test("core models are pure and define the public base vocabulary", () => {
  const expectedModels = [
    "CustomerContact",
    "DeliveryLocation",
    "PaymentMethod",
    "PublicClaimDraft",
    "PublicClaimReceipt",
    "PublicOrderDraft",
    "PublicOrderItem",
    "PublicOrderStatus",
    "PublicOrderTicket",
    "PublicProductSummary",
    "PublicStoreSummary",
    "PublicTrackingState",
  ];

  for (const model of expectedModels) {
    const source = read(`${coreRoot}/model/${model}.kt`);
    assert.match(source, new RegExp(`(data class|enum class) ${model}`));
    assert.doesNotMatch(source, /firebase|Firebase|Firestore|Functions|com\.google\.firebase/);
    assert.doesNotMatch(source, /androidx\.compose|android\.app|android\.content/);
  }

  const status = read(`${coreRoot}/model/PublicOrderStatus.kt`);
  for (const value of ["RECEIVED", "PREPARING", "ON_THE_WAY", "DELIVERED", "CANCELLED", "UNDER_REVIEW"]) {
    assert.match(status, new RegExp(`\\b${value}\\b`));
  }
});

test("core result types represent success and expected error families", () => {
  const result = read(`${coreRoot}/result/CoreResult.kt`);
  const error = read(`${coreRoot}/result/CoreError.kt`);
  const validation = read(`${coreRoot}/result/ValidationError.kt`);

  assert.match(result, /data class Success<T>/);
  assert.match(result, /data class Failure/);
  assert.match(error, /data class Validation/);
  assert.match(error, /data object IncompleteData/);
  assert.match(error, /data object NotAvailable/);
  assert.match(error, /data object Unknown/);
  assert.match(validation, /TRACKING_NUMBER/);
});

test("ports are interfaces without implementations or platform dependencies", () => {
  const ports = [
    ["PublicCatalogPort", ["getVisibleStores", "getProductsForStore"]],
    ["PublicClaimPort", ["submitPublicClaim"]],
    ["PublicOrderPort", ["createPublicOrder"]],
    ["PublicTrackingPort", ["getPublicTracking"]],
  ];

  for (const [port, methods] of ports) {
    const source = read(`${coreRoot}/port/${port}.kt`);
    assert.match(source, new RegExp(`interface ${port}`));
    for (const method of methods) assert.match(source, new RegExp(`suspend fun ${method}`));
    assert.doesNotMatch(source, /class .*Port|Firebase|Firestore|Functions|androidx\.compose/);
  }
});

test("use cases validate drafts and tracking before delegating to ports", () => {
  const validation = read(`${coreRoot}/usecase/ValidatePublicOrderDraftUseCase.kt`);
  assert.match(validation, /contact\.name\.isBlankOrPlaceholder\(\)/);
  assert.match(validation, /contact\.phone\.isBlankOrPlaceholder\(\)/);
  assert.match(validation, /phone\.count \{ it\.isDigit\(\) \} !in 8\.\.15/);
  assert.match(validation, /deliveryLocation\?\.addressLine\.isNullOrBlankOrPlaceholder\(\)/);
  assert.match(validation, /storeId\.isBlankOrPlaceholder\(\)/);
  assert.match(validation, /storeName\.isBlankOrPlaceholder\(\)/);
  assert.match(validation, /paymentMethod == .*PaymentMethod\.NotSpecified/);
  assert.match(validation, /it\.storeId != draft\.storeId/);
  assert.match(validation, /items\.isEmpty\(\)/);
  assert.match(validation, /quantity <= 0/);
  assert.match(validation, /placeholderValues/);

  const create = read(`${coreRoot}/usecase/CreatePublicOrderUseCase.kt`);
  assert.match(create, /ValidatePublicOrderDraftUseCase/);
  assert.match(create, /orderPort\.createPublicOrder\(draft\)/);

  const tracking = read(`${coreRoot}/usecase/GetPublicTrackingUseCase.kt`);
  assert.match(tracking, /trackingNumber\.trim\(\)/);
  assert.match(tracking, /TRACKING_NUMBER/);
  assert.match(tracking, /trackingPort\.getPublicTracking\(cleanTrackingNumber\)/);

  const claim = read(`${coreRoot}/usecase/SubmitPublicClaimUseCase.kt`);
  assert.match(claim, /publicClaimPort\.submitPublicClaim\(clean\)/);
  assert.match(claim, /Regex\("\^PDL-\[A-Z0-9\]\{4,10\}\$"\)/);
  assert.match(claim, /CoreError\.IncompleteData/);
});

test("pure core source stays free of Firebase, Compose and Android imports", () => {
  const forbidden = spawnSync("rg", [
    "-n",
    "firebase|Firebase|Firestore|Functions|com\\.google\\.firebase|androidx\\.compose|android\\.app|android\\.content",
    `${coreRoot}/model`,
    `${coreRoot}/port`,
    `${coreRoot}/result`,
    `${coreRoot}/usecase`,
  ], {encoding: "utf8"});

  assert.notEqual(forbidden.status, 0, forbidden.stdout);
});
