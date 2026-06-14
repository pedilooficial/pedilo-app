const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");
const {spawnSync} = require("node:child_process");

const adapterPath = "app/src/main/java/com/pedilo/app/core/firebase/FirebasePublicCatalogAdapter.kt";
const mapperPath = "app/src/main/java/com/pedilo/app/core/firebase/FirestoreCatalogMappers.kt";
const seedPath = "tools/seed_public_catalog.js";
const verifyPath = "tools/verify_public_catalog.js";
const publicAppPath = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const publicCatalogStatePath = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicCatalogState.kt";
const publicShopSearchPath = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicShopSearch.kt";
const publicLocalPath = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicLocal.kt";
const publicHomePath = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicHome.kt";
const publicShopPath = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicShop.kt";
const publicSubcategoryPath = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicShopSubcategory.kt";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

test("Firebase catalog adapter is read-only and targets catalog collections", () => {
  const source = read(adapterPath);
  assert.match(source, /class FirebasePublicCatalogAdapter/);
  assert.match(source, /PublicCatalogPort/);
  assert.match(source, /collection\(STORES\)[\s\S]*?whereEqualTo\(VISIBLE, true\)[\s\S]*?\.get\(\)[\s\S]*?\.await\(\)/);
  assert.match(source, /collection\(PRODUCTS\)[\s\S]*?whereEqualTo\(VISIBLE, true\)[\s\S]*?whereEqualTo\(AVAILABLE, true\)[\s\S]*?\.get\(\)[\s\S]*?\.await\(\)/);
  assert.doesNotMatch(source, /\.(set|update|delete|add)\(/);
  assert.doesNotMatch(source, /collection\("orders"\)|collection\(ORDERS\)/);
});

test("Firestore catalog mappers filter visibility and optional availability", () => {
  const source = read(mapperPath);
  assert.match(source, /toPublicStoreSummaryOrNull/);
  assert.match(source, /toPublicProductSummaryOrNull/);
  assert.match(source, /optionalBoolean\("visible"\) != true/);
  assert.match(source, /optionalBoolean\("operational"\) == false/);
  assert.match(source, /optionalBoolean\("acceptsOrders"\) == false/);
  assert.match(source, /isKnownSeedCatalogStore\(id, name\)/);
  assert.match(source, /id == "pizzeria-roma"/);
  assert.match(source, /optionalBoolean\("available"\) == false/);
  assert.match(source, /priceCents/);
  assert.doesNotMatch(source, /FirebasePublicCatalogAdapter/);
});

test("controlled environment seed uses real-test ids and requires explicit confirmation", () => {
  const source = read(seedPath);
  assert.match(source, /PEDILO_CONFIRM_CONTROLLED_SEED/);
  assert.match(source, /REQUIRED_CONFIRMATION = "YES"/);
  assert.match(source, /PEDILO_ADMIN_UID/);
  assert.match(source, /PEDILO_STORE_UID/);
  assert.match(source, /PEDILO_DRIVER_UID/);
  assert.match(source, /stores"\)\.doc\(ids\.storeUid\)/);
  assert.match(source, /PRODUCT_IDS = \[\s*"mila-completa",\s*"ensalada-fresca",\s*"bebida-controlada",\s*"promo-almuerzo",\s*\]/);
  for (const index of [0, 1, 2, 3]) {
    assert.match(source, new RegExp(`id: PRODUCT_IDS\\[${index}\\]`));
  }
  assert.doesNotMatch(source, /collection\("orders"\)/);
  assert.doesNotMatch(source, /pizzeria-roma|Pizzería Roma|Pizza muzzarella|napolitana|empanadas|gaseosa/);
});

test("controlled seed script stays in safe mode before loading Firebase Admin", () => {
  const source = read(seedPath);
  assert.match(source, /process\.env\[CONFIRM_ENV\] !== REQUIRED_CONFIRMATION/);
  assert.ok(
    source.indexOf("process.env[CONFIRM_ENV] !== REQUIRED_CONFIRMATION") <
      source.indexOf('require("firebase-admin")'),
  );
  assert.match(source, /no se escribe entorno/);
  assert.match(source, /Los UIDs admin\/store\/driver deben ser distintos/);
  assert.match(source, /driverCapacity: 2/);
  assert.match(source, /FIRESTORE_EMULATOR_HOST/);
  assert.match(source, /pedilo-controlled-test/);
});

test("public UI does not expose removed sample labels", () => {
  const forbidden = ["de" + "mo", "mo" + "ck"];
  for (const term of forbidden) {
    const result = spawnSync("rg", ["-n", "-i", `\\b${term}\\b`, "app/src/main/java/com/pedilo/app/ui/publicuser"], {
      encoding: "utf8",
    });
    assert.notEqual(result.status, 0, result.stdout);
  }
});

test("read-only catalog verifier checks controlled scope without writes", () => {
  const source = read(verifyPath);
  assert.match(source, /PEDILO_ADMIN_UID/);
  assert.match(source, /PEDILO_STORE_UID/);
  assert.match(source, /PEDILO_DRIVER_UID/);
  assert.match(source, /PRODUCT_IDS = \["mila-completa", "ensalada-fresca", "bebida-controlada", "promo-almuerzo"\]/);
  assert.match(source, /collection\("stores"\)\.doc\(ids\.storeUid\)/);
  assert.match(source, /storeRef\.collection\("products"\)\.get\(\)/);
  assert.match(source, /isActiveRole\(users\[0\], "admin"\)/);
  assert.match(source, /isActiveRole\(users\[1\], "store"\)/);
  assert.match(source, /isActiveRole\(users\[2\], "driver"\)/);
  assert.match(source, /FIRESTORE_EMULATOR_HOST/);
  assert.match(source, /pedilo-controlled-test/);
  assert.doesNotMatch(source, /\.(set|update|delete|add)\(/);
  assert.doesNotMatch(source, /collection\("orders"\)|order_tracking|pizzeria-roma|Pizzería Roma/);
});

test("public UI loads real catalog once and keeps failure as a non-crashing state", () => {
  const stateSource = read(publicCatalogStatePath);
  const appSource = read(publicAppPath);

  assert.match(stateSource, /publicCatalogPort/);
  assert.match(stateSource, /adapter\.getVisibleStores\(\)/);
  assert.match(stateSource, /adapter\.getProductsForStore\(store\.id\)/);
  assert.match(stateSource, /return PublicCatalogState\(isLoading = false, loadFailed = true\)/);
  assert.match(stateSource, /hasRealCatalog = stores\.isNotEmpty\(\)/);
  assert.doesNotMatch(stateSource, /\.(set|update|delete|add)\(/);

  assert.match(appSource, /LaunchedEffect\(Unit\)/);
  assert.match(appSource, /loadPublicCatalogState\(\)/);
  assert.match(appSource, /catalogState = catalogState/);
  assert.ok(
    appSource.indexOf("loadPublicCatalogState()") < appSource.indexOf("if (showSplash)"),
    "catalog loading should start before the splash returns",
  );
});

test("shop search and local screens use real catalog states without catalog fallback", () => {
  const searchSource = read(publicShopSearchPath);
  const localSource = read(publicLocalPath);
  const appSource = read(publicAppPath);

  assert.match(searchSource, /val relatedStores = realStoresForQuery\(activeQuery, catalogState\)/);
  assert.match(searchSource, /if \(!catalogState\.hasRealCatalog\) return emptyList\(\)/);
  assert.match(searchSource, /No pudimos cargar los locales/);
  assert.doesNotMatch(searchSource, /storesForQuery|pizzaSearchStores|coherentStores/);
  assert.match(localSource, /catalogState\.productsByStore\[storeId\.orEmpty\(\)\]/);
  assert.match(localSource, /selectedStoreId: String\? = null/);
  assert.match(appSource, /selectedLocalStoreId/);
  assert.match(appSource, /openLocalStore\(storeId: String\)/);
  assert.match(appSource, /selectedStoreForOrder\(selectedLocalStoreId, localCart\)/);
  assert.doesNotMatch(localSource, /localProducts|Pizza muzzarella|Resumen de Pizzería Roma/);
  assert.doesNotMatch(`${appSource}\n${localSource}`, /pizzeria-roma|Pizzería Roma/);
  assert.match(localSource, /No pudimos cargar el local/);
});

test("home, shop, and subcategory screens render real stores or loading/error/empty states", () => {
  const homeSource = read(publicHomePath);
  const shopSource = read(publicShopPath);
  const subcategorySource = read(publicSubcategoryPath);

  for (const source of [homeSource, subcategorySource]) {
    assert.match(source, /PublicCatalogState/);
    assert.match(source, /catalogState\.isLoading/);
    assert.match(source, /catalogState\.loadFailed/);
    assert.match(source, /catalogState\.stores/);
    assert.doesNotMatch(source, /Burger House|Sushi Zen|Verde Vivo|Dulce Hogar|La Esquina|Pizza & Co/);
  }

  assert.match(shopSource, /PublicCatalogState/);
  assert.match(shopSource, /ShopSearchCard/);
  assert.match(shopSource, /TrackingLookupCard/);
  assert.match(shopSource, /CategoryGroupCard/);
  assert.doesNotMatch(shopSource, /RealStoresSection|RealStoreTile|catalogState\.stores/);
  assert.doesNotMatch(shopSource, /Burger House|Sushi Zen|Verde Vivo|Dulce Hogar|La Esquina|Pizza & Co/);
});

test("home offers only render real promo products when present", () => {
  const homeSource = read(publicHomePath);

  assert.match(homeSource, /realPromoProduct/);
  assert.match(homeSource, /product\.id\.contains\("promo"/);
  assert.match(homeSource, /OfferCard\(product = promo/);
  assert.match(homeSource, /Todavía no hay ofertas disponibles/);
  assert.doesNotMatch(homeSource, /Promo imperdible|Oferta destacada|2x1/);
});

test("shop subcategory maps visible catalog into category-specific listings", () => {
  const subcategorySource = read(publicSubcategoryPath);

  assert.match(subcategorySource, /searchTermsForSubcategory/);
  assert.match(subcategorySource, /"pizzas" -> listOf\("pizza", "pizzería", "pizzeria"\)/);
  assert.match(subcategorySource, /catalogState\.productsByStore\[store\.id\]/);
  assert.match(subcategorySource, /RelatedStoreCard/);
});

test("public UI has no hardcoded catalog names from the initial seed", () => {
  const result = spawnSync(
    "rg",
    [
      "-n",
      "Pizzería Roma|Pizza muzzarella|napolitana|empanadas|gaseosa",
      "app/src/main/java/com/pedilo/app/ui/publicuser",
    ],
    {encoding: "utf8"},
  );
  assert.notEqual(result.status, 0, result.stdout);
});
