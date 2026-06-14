#!/usr/bin/env node
"use strict";

const ENV_ADMIN_UID = "PEDILO_ADMIN_UID";
const ENV_STORE_UID = "PEDILO_STORE_UID";
const ENV_DRIVER_UID = "PEDILO_DRIVER_UID";
const PRODUCT_IDS = ["mila-completa", "ensalada-fresca", "bebida-controlada", "promo-almuerzo"];

async function main() {
  const ids = readControlledIds();
  const admin = loadFirebaseAdmin();

  if (admin.apps.length === 0) {
    admin.initializeApp(firebaseAdminConfig(admin));
  }

  const db = admin.firestore();
  const storeRef = db.collection("stores").doc(ids.storeUid);
  const store = await storeRef.get();
  const products = await storeRef.collection("products").get();
  const productIds = products.docs.map((doc) => doc.id).sort();
  const users = await Promise.all([
    db.collection("users").doc(ids.adminUid).get(),
    db.collection("users").doc(ids.storeUid).get(),
    db.collection("users").doc(ids.driverUid).get(),
  ]);

  console.log(`store:${ids.storeUid}:exists=${store.exists}:visible=${store.get("visible") === true}:acceptsOrders=${store.get("acceptsOrders") === true}`);
  console.log(`products:${productIds.length}:${productIds.join(",")}`);
  console.log(`expected-products-present=${PRODUCT_IDS.every((id) => productIds.includes(id))}`);
  console.log(`users:admin=${isActiveRole(users[0], "admin")}:store=${isActiveRole(users[1], "store")}:driver=${isActiveRole(users[2], "driver")}`);
}

main().catch((error) => {
  console.error(error.message || error);
  process.exit(1);
});

function isActiveRole(snapshot, role) {
  return snapshot.exists && snapshot.get("active") === true && snapshot.get("role") === role;
}

function readControlledIds() {
  return {
    adminUid: cleanDocumentId(process.env[ENV_ADMIN_UID], ENV_ADMIN_UID),
    storeUid: cleanDocumentId(process.env[ENV_STORE_UID], ENV_STORE_UID),
    driverUid: cleanDocumentId(process.env[ENV_DRIVER_UID], ENV_DRIVER_UID),
  };
}

function cleanDocumentId(value, envName) {
  const clean = String(value || "").trim();
  if (!/^[A-Za-z0-9_-]{6,128}$/.test(clean)) {
    throw new Error(`Configurá ${envName} con un UID/Auth doc id válido.`);
  }
  return clean;
}

function loadFirebaseAdmin() {
  try {
    return require("firebase-admin");
  } catch (error) {
    throw new Error("Firebase Admin SDK no está disponible en este entorno local.");
  }
}

function firebaseAdminConfig(admin) {
  if (process.env.FIRESTORE_EMULATOR_HOST) {
    return {
      projectId: process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT || "pedilo-controlled-test",
    };
  }
  return {
    credential: admin.credential.applicationDefault(),
  };
}
