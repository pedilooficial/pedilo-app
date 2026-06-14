"use strict";

const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");

const classifierPath =
  "app/src/main/java/com/pedilo/app/core/model/AdminOperationOrderClassification.kt";
const adapterPath = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt";

function read(path) {
  return fs.readFileSync(path, "utf8");
}

function signals(overrides = {}) {
  return {
    status: "",
    publicStatus: "",
    operationalStatus: "",
    responsibleRole: "admin",
    needsAttention: false,
    activeIncident: false,
    source: "",
    requestType: "",
    ...overrides,
  };
}

function normalizedStatus(status) {
  return String(status || "").trim().toLowerCase();
}

function hasRealActiveSignal(s) {
  return (
    normalizedStatus(s.status) === "created" &&
    String(s.publicStatus || "").trim() === "Pedido recibido"
  );
}

function hasRealCancellationSignal(s) {
  return normalizedStatus(s.status) === "cancelled" || normalizedStatus(s.status) === "canceled";
}

function hasRealFinishedSignal(s) {
  return ["delivered", "closed", "archived"].includes(normalizedStatus(s.status));
}

function includes(value, term) {
  return String(value || "").toLowerCase().includes(term);
}

function hasRealDelaySignal(s) {
  return includes(s.publicStatus, "demora") || includes(s.publicStatus, "retras") ||
    includes(s.operationalStatus, "demora") || includes(s.operationalStatus, "retras");
}

function hasRealProblemSignal(s) {
  return s.activeIncident || s.needsAttention || includes(s.publicStatus, "reclamo") ||
    includes(s.publicStatus, "problema") || includes(s.operationalStatus, "local no responde") ||
    includes(s.operationalStatus, "sin respuesta") || hasRealDelaySignal(s) ||
    (hasRealActiveSignal(s) && String(s.responsibleRole || "").trim() === "");
}

function hasNormalActiveSignal(s) {
  return hasRealActiveSignal(s) && !hasRealProblemSignal(s);
}

function primaryPlacement(s) {
  if (hasRealProblemSignal(s)) return "PROBLEM";
  if (hasRealCancellationSignal(s)) return "CANCELLED";
  if (hasRealFinishedSignal(s)) return "FINISHED";
  if (hasNormalActiveSignal(s)) return "ACTIVE";
  return "UNCLASSIFIED";
}

function todayBucket(s) {
  if (hasRealProblemSignal(s)) return "WITH_PROBLEMS";
  if (hasRealCancellationSignal(s)) return "CANCELLED";
  if (hasRealFinishedSignal(s)) return "FINISHED";
  if (hasNormalActiveSignal(s)) return "ACTIVE";
  return "REVIEW";
}

function activeBucket(s) {
  if (!hasNormalActiveSignal(s)) return null;
  if (includes(s.operationalStatus, "preparando")) return "PREPARING";
  if (includes(s.operationalStatus, "esperando repartidor")) return "WAITING_DRIVER";
  if (includes(s.operationalStatus, "en entrega")) return "IN_DELIVERY";
  if (includes(s.operationalStatus, "esperando local")) return "WAITING_STORE";
  return "REVIEW_STATE";
}

function problemBucket(s) {
  if (!hasRealProblemSignal(s)) return null;
  if (includes(s.operationalStatus, "local no responde") || includes(s.operationalStatus, "sin respuesta")) {
    return "STORE_NOT_RESPONDING";
  }
  if (includes(s.publicStatus, "reclamo") || includes(s.publicStatus, "problema")) return "CUSTOMER_CLAIM";
  if (hasRealDelaySignal(s)) return "DELAYED";
  if (hasRealActiveSignal(s) && String(s.responsibleRole || "").trim() === "") return "WITHOUT_RESPONSIBLE";
  return "OPERATIONAL_REVIEW";
}

function operationalIdentity(source, requestType = "") {
  const cleanSource = String(source || "").trim();
  const cleanRequest = String(requestType || "").trim();
  if (cleanSource === "public_local") return "Retiro de local";
  if (cleanSource === "public_plus_buy" || cleanRequest === "buy") return "Compra solicitada";
  if (cleanSource === "public_plus_pickup_shipping" || cleanRequest === "pickup_shipping") {
    return "Retiro solicitado";
  }
  return "Pedido operativo";
}

function operationalFunction(source, requestType = "") {
  switch (operationalIdentity(source, requestType)) {
    case "Compra solicitada":
      return "Comprar y entregar";
    case "Retiro solicitado":
    case "Retiro de local":
      return "Retirar y entregar";
    default:
      return "Revisar pedido";
  }
}

test("kotlin classifier exists as pure core without firebase", () => {
  const source = read(classifierPath);
  assert.match(source, /object AdminOperationOrderClassification/);
  assert.match(source, /enum class AdminOrderPrimaryPlacement/);
  assert.match(source, /fun primaryPlacement/);
  assert.match(source, /enum class AdminTodayOrdersBucket/);
  assert.match(source, /enum class AdminActiveOrdersBucket/);
  assert.doesNotMatch(source, /import.*firebase|import.*Firestore|com\.google\.firebase/);
  assert.doesNotMatch(source, /\.set\(|\.update\(|\.delete\(|writeBatch|runTransaction/);
  assert.match(source, /hasNormalActiveSignal/);
  assert.match(source, /if \(!hasNormalActiveSignal\(signals\)\) return null/);
  assert.match(source, /return AdminActiveOrdersBucket\.REVIEW_STATE/);
  assert.match(source, /return AdminProblemOrdersBucket\.OPERATIONAL_REVIEW/);
  assert.match(source, /AdminOrderPrimaryPlacement\.UNCLASSIFIED -> AdminTodayOrdersBucket\.REVIEW/);
  assert.match(source, /AdminOrderPrimaryPlacement\.PROBLEM -> AdminTodayOrdersBucket\.WITH_PROBLEMS/);
  assert.doesNotMatch(source, /hasRealWaitingStoreSignal\(signals: AdminOperationOrderSignals\): Boolean =\s*hasRealActiveSignal/);
});

test("admin orders adapter stays read-only on orders collection", () => {
  const source = read(adapterPath);
  assert.match(source, /\.get\(\)\.await\(\)/);
  assert.doesNotMatch(source, /\.set\(|\.update\(|\.delete\(|writeBatch|runTransaction/);
});

test("normal active order needs an explicit stage for active sub-bucket", () => {
  const s = signals({
    status: "created",
    publicStatus: "Pedido recibido",
    operationalStatus: "Esperando local",
    source: "public_local",
  });
  assert.equal(todayBucket(s), "ACTIVE");
  assert.equal(activeBucket(s), "WAITING_STORE");
});

test("active order without explicit stage goes only to Revisar estado", () => {
  const s = signals({status: "created", publicStatus: "Pedido recibido"});
  assert.equal(todayBucket(s), "ACTIVE");
  assert.equal(activeBucket(s), "REVIEW_STATE");
  assert.notEqual(activeBucket(s), "WAITING_STORE");
});

test("problem orders leave normal active buckets", () => {
  const withoutResponsible = signals({
    status: "created",
    publicStatus: "Pedido recibido",
    operationalStatus: "Esperando local",
    responsibleRole: "",
  });
  const delayed = signals({
    status: "created",
    publicStatus: "Pedido recibido",
    operationalStatus: "Demorado",
  });

  assert.equal(todayBucket(withoutResponsible), "WITH_PROBLEMS");
  assert.equal(activeBucket(withoutResponsible), null);
  assert.equal(problemBucket(withoutResponsible), "WITHOUT_RESPONSIBLE");
  assert.equal(todayBucket(delayed), "WITH_PROBLEMS");
  assert.equal(activeBucket(delayed), null);
  assert.equal(problemBucket(delayed), "DELAYED");
});

test("today and problem classifications use exclusive priority", () => {
  const conflicted = signals({
    status: "cancelled",
    publicStatus: "Reclamo del cliente",
    operationalStatus: "Local no responde y demorado",
    responsibleRole: "",
  });

  assert.equal(todayBucket(conflicted), "WITH_PROBLEMS");
  assert.equal(primaryPlacement(conflicted), "PROBLEM");
  assert.equal(problemBucket(conflicted), "STORE_NOT_RESPONDING");
});

test("generic real problem goes only to operational review", () => {
  const genericProblem = signals({
    status: "created",
    publicStatus: "Pedido recibido",
    needsAttention: true,
  });
  assert.equal(primaryPlacement(genericProblem), "PROBLEM");
  assert.equal(problemBucket(genericProblem), "OPERATIONAL_REVIEW");

  const specificProblem = signals({
    status: "created",
    publicStatus: "Reclamo del cliente",
    needsAttention: true,
  });
  assert.equal(problemBucket(specificProblem), "CUSTOMER_CLAIM");
  assert.notEqual(problemBucket(specificProblem), "OPERATIONAL_REVIEW");
});

test("every representative order has a findable exclusive operational location", () => {
  const rows = [
    signals({status: "created", publicStatus: "Pedido recibido", operationalStatus: "Preparando"}),
    signals({status: "created", publicStatus: "Pedido recibido"}),
    signals({status: "created", publicStatus: "Pedido recibido", needsAttention: true}),
    signals({status: "cancelled"}),
    signals({status: "delivered"}),
    signals({status: "created"}),
  ];
  const primaryCounts = new Map();
  for (const row of rows) {
    const placement = primaryPlacement(row);
    primaryCounts.set(placement, (primaryCounts.get(placement) || 0) + 1);
    assert.notEqual(todayBucket(row), null);
  }

  assert.equal([...primaryCounts.values()].reduce((sum, count) => sum + count, 0), rows.length);
  for (const row of rows.filter((candidate) => primaryPlacement(candidate) === "ACTIVE")) {
    assert.ok(["WAITING_STORE", "PREPARING", "WAITING_DRIVER", "IN_DELIVERY", "REVIEW_STATE"].includes(activeBucket(row)));
  }
  for (const row of rows.filter((candidate) => primaryPlacement(candidate) === "PROBLEM")) {
    assert.ok(["STORE_NOT_RESPONDING", "CUSTOMER_CLAIM", "DELAYED", "WITHOUT_RESPONSIBLE", "OPERATIONAL_REVIEW"].includes(problemBucket(row)));
  }
});

test("primary placement is exclusive and today remains a separate temporal filter", () => {
  assert.equal(primaryPlacement(signals({
    status: "created",
    publicStatus: "Pedido recibido",
    operationalStatus: "Demorado",
  })), "PROBLEM");
  assert.equal(primaryPlacement(signals({status: "cancelled"})), "CANCELLED");
  assert.equal(primaryPlacement(signals({status: "delivered"})), "FINISHED");
  assert.equal(primaryPlacement(signals({
    status: "created",
    publicStatus: "Pedido recibido",
  })), "ACTIVE");
  assert.equal(primaryPlacement(signals({status: "created"})), "UNCLASSIFIED");
});

test("created + Pedido recibido does not map to problem or cancelled buckets", () => {
  const s = signals({
    status: "created",
    publicStatus: "Pedido recibido",
    source: "public_plus_buy",
    requestType: "buy",
  });
  assert.notEqual(todayBucket(s), "WITH_PROBLEMS");
  assert.notEqual(todayBucket(s), "CANCELLED");
  assert.notEqual(todayBucket(s), "FINISHED");
  assert.notEqual(todayBucket(s), "DELAYED");
});

test("cancelled status maps to Cancelados only with real cancellation signal", () => {
  const s = signals({status: "cancelled", publicStatus: "null", source: "public_app"});
  assert.equal(todayBucket(s), "CANCELLED");
  assert.equal(activeBucket(s), null);
});

test("without finish signal does not map to Finalizados", () => {
  const s = signals({status: "created", publicStatus: "Pedido recibido"});
  assert.notEqual(todayBucket(s), "FINISHED");
  assert.equal(hasRealFinishedSignal(s), false);
});

test("without delay criteria does not map to Demorados", () => {
  const s = signals({status: "created", publicStatus: "Pedido recibido"});
  assert.notEqual(todayBucket(s), "DELAYED");
  assert.equal(hasRealDelaySignal(s), false);
});

test("without problem signal does not map to Con problemas", () => {
  const s = signals({status: "created", publicStatus: "Pedido recibido"});
  assert.notEqual(todayBucket(s), "WITH_PROBLEMS");
  assert.equal(hasRealProblemSignal(s), false);
});

test("operational labels translate public flows into human identities", () => {
  const source = read(classifierPath);

  assert.match(source, /operationalIdentity/);
  assert.match(source, /operationalFunction/);
  assert.equal(operationalIdentity("public_local"), "Retiro de local");
  assert.equal(operationalIdentity("public_plus_buy"), "Compra solicitada");
  assert.equal(operationalIdentity("public_plus_pickup_shipping"), "Retiro solicitado");
  assert.equal(operationalFunction("public_plus_buy"), "Comprar y entregar");
  assert.equal(operationalFunction("public_plus_pickup_shipping"), "Retirar y entregar");
  assert.equal(operationalFunction("public_local"), "Retirar y entregar");
  assert.doesNotMatch(source, /Pedido de local|Botón \+ Comprar|Botón \+ Retiro|Origen técnico/);
});

test("legacy created without publicStatus stays visible for review", () => {
  const s = signals({status: "created", publicStatus: "", source: "public_app"});
  assert.equal(primaryPlacement(s), "UNCLASSIFIED");
  assert.equal(todayBucket(s), "REVIEW");
  assert.equal(activeBucket(s), null);
});

test("real firebase combinations from audit classify conservatively", () => {
  const auditRows = [
    {status: "cancelled", publicStatus: "", source: "", requestType: "", bucket: "CANCELLED"},
    {status: "cancelled", publicStatus: "", source: "public_app", requestType: "", bucket: "CANCELLED"},
    {
      status: "created",
      publicStatus: "Pedido recibido",
      source: "public_local",
      requestType: "",
      bucket: "ACTIVE",
    },
    {status: "created", publicStatus: "", source: "public_app", requestType: "", bucket: "REVIEW"},
    {
      status: "created",
      publicStatus: "Pedido recibido",
      source: "public_plus_pickup_shipping",
      requestType: "pickup_shipping",
      bucket: "ACTIVE",
    },
    {
      status: "created",
      publicStatus: "Pedido recibido",
      source: "public_plus_buy",
      requestType: "buy",
      bucket: "ACTIVE",
    },
  ];

  for (const row of auditRows) {
    const s = signals(row);
    assert.equal(
      todayBucket(s),
      row.bucket,
      `unexpected bucket for ${row.status}/${row.publicStatus}/${row.source}`,
    );
  }
});
