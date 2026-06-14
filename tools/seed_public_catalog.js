#!/usr/bin/env node
"use strict";

const CONFIRM_ENV = "PEDILO_CONFIRM_CONTROLLED_SEED";
const REQUIRED_CONFIRMATION = "YES";
const ENV_ADMIN_UID = "PEDILO_ADMIN_UID";
const ENV_STORE_UID = "PEDILO_STORE_UID";
const ENV_DRIVER_UID = "PEDILO_DRIVER_UID";

const PRODUCT_IDS = [
  "mila-completa",
  "ensalada-fresca",
  "bebida-controlada",
  "promo-almuerzo",
];

const products = [
  {
    id: PRODUCT_IDS[0],
    name: "Milanesa completa",
    description: "Milanesa con guarnicion, preparada para prueba controlada.",
    priceCents: 820000,
  },
  {
    id: PRODUCT_IDS[1],
    name: "Ensalada fresca",
    description: "Ensalada simple con ingredientes visibles y precio estable.",
    priceCents: 510000,
  },
  {
    id: PRODUCT_IDS[2],
    name: "Bebida controlada",
    description: "Bebida sin alcohol para validar carrito y totales.",
    priceCents: 190000,
  },
  {
    id: PRODUCT_IDS[3],
    name: "Promo almuerzo controlado",
    description: "Producto marcado como promo para validar home y pedido real.",
    priceCents: 960000,
  },
];

async function main() {
  if (process.env[CONFIRM_ENV] !== REQUIRED_CONFIRMATION) {
    console.log(`Modo seguro: no se escribe entorno. Para ejecutar, usar ${CONFIRM_ENV}=${REQUIRED_CONFIRMATION}.`);
    return;
  }

  const ids = readControlledIds();
  const admin = loadFirebaseAdmin();

  if (admin.apps.length === 0) {
    admin.initializeApp(firebaseAdminConfig(admin));
  }

  const db = admin.firestore();
  const now = admin.firestore.FieldValue.serverTimestamp();
  const batch = db.batch();

  batch.set(db.collection("users").doc(ids.adminUid), controlledUser({
    role: "admin",
    email: process.env.PEDILO_ADMIN_EMAIL || "admin-controlado@pedilo.test",
    displayName: "Admin Controlado",
    now,
  }), {merge: true});

  batch.set(db.collection("users").doc(ids.storeUid), controlledUser({
    role: "store",
    email: process.env.PEDILO_STORE_EMAIL || "local-controlado@pedilo.test",
    displayName: "Local Controlado",
    storeId: ids.storeUid,
    now,
  }), {merge: true});

  batch.set(db.collection("users").doc(ids.driverUid), controlledUser({
    role: "driver",
    email: process.env.PEDILO_DRIVER_EMAIL || "driver-controlado@pedilo.test",
    displayName: "Driver Controlado",
    driverCapacity: 2,
    now,
  }), {merge: true});

  const storeRef = db.collection("stores").doc(ids.storeUid);
  batch.set(storeRef, {
    name: process.env.PEDILO_STORE_NAME || "Local Controlado Pédilo",
    category: "Comida controlada",
    mainCategory: "Comida",
    description: "Local autorizado para prueba real controlada de Pédilo.",
    address: process.env.PEDILO_STORE_ADDRESS || "Direccion controlada",
    phone: process.env.PEDILO_STORE_PHONE || "Telefono controlado",
    openingHours: "Prueba coordinada",
    visible: true,
    operational: true,
    acceptsOrders: true,
    isOpen: true,
    controlledTest: true,
    updatedAt: now,
    createdAt: now,
  }, {merge: true});

  for (const product of products) {
    const {id: productId, ...productData} = product;
    batch.set(storeRef.collection("products").doc(productId), {
      ...productData,
      storeId: ids.storeUid,
      visible: true,
      available: true,
      controlledTest: true,
      updatedAt: now,
      createdAt: now,
    }, {merge: true});
  }

  await batch.commit();

  console.log(`Entorno controlado cargado: stores/${ids.storeUid}, ${products.length} productos, usuarios admin/store/driver.`);
}

main().catch((error) => {
  console.error(error.message || error);
  process.exit(1);
});

function controlledUser({role, email, displayName, storeId, driverCapacity, now}) {
  return {
    role,
    email,
    displayName,
    active: true,
    controlledTest: true,
    ...(storeId ? {storeId} : {}),
    ...(driverCapacity ? {driverCapacity, maxActiveOrders: driverCapacity} : {}),
    updatedAt: now,
    createdAt: now,
  };
}

function readControlledIds() {
  const adminUid = cleanDocumentId(process.env[ENV_ADMIN_UID], ENV_ADMIN_UID);
  const storeUid = cleanDocumentId(process.env[ENV_STORE_UID], ENV_STORE_UID);
  const driverUid = cleanDocumentId(process.env[ENV_DRIVER_UID], ENV_DRIVER_UID);
  if (new Set([adminUid, storeUid, driverUid]).size !== 3) {
    throw new Error("Los UIDs admin/store/driver deben ser distintos.");
  }
  return {adminUid, storeUid, driverUid};
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
    throw new Error("Firebase Admin SDK no está disponible en este entorno local. No se escribió entorno controlado.");
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
