const assert = require("node:assert/strict");
const {test} = require("node:test");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const {spawnSync} = require("node:child_process");

function copyProjectForGuard() {
  const tmpRoot = fs.mkdtempSync(path.join(os.tmpdir(), "pedilo-guard-"));
  for (const item of ["app", "functions", "firestore.rules", "firestore.indexes.json", "firebase.json", "README.md", "tools"]) {
    fs.cpSync(item, path.join(tmpRoot, item), {recursive: true});
  }
  return tmpRoot;
}

function runGuard(root) {
  return spawnSync("bash", ["tools/guards/check_architecture.sh"], {
    cwd: root,
    encoding: "utf8",
  });
}

test("architecture guard fails if legacy public identity returns", () => {
  const root = copyProjectForGuard();
  const target = path.join(root, "app/src/main/java/com/pedilo/app/MainActivity.kt");
  fs.appendFileSync(target, "\n// UserRole" + ".Customer\n");
  const result = runGuard(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /forbidden pattern/);
});

test("architecture guard fails if Android writes directly to orders", () => {
  const root = copyProjectForGuard();
  const target = path.join(root, "app/src/main/java/com/pedilo/app/MainActivity.kt");
  fs.appendFileSync(target, "\n// db.collection(\"orders\").document(\"x\").set(mapOf())\n");
  const result = runGuard(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /forbidden pattern|UI must not write directly/);
});

test("architecture guard fails if functions config is missing", () => {
  const root = copyProjectForGuard();
  const target = path.join(root, "firebase.json");
  fs.writeFileSync(target, JSON.stringify({firestore: {rules: "firestore.rules", indexes: "firestore.indexes.json"}}, null, 2));
  const result = runGuard(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /functions deploy config/);
});

test("architecture guard fails if public order functions touch operational internals", () => {
  const root = copyProjectForGuard();
  const target = path.join(root, "functions/index.js");
  const source = fs.readFileSync(target, "utf8");
  fs.writeFileSync(
    target,
    source.replace(
      "const clean = cleanOrderPayload(payload);",
      "const clean = cleanOrderPayload(payload);\n  const forbiddenPublicDriver = \"driverId\";",
    ),
  );
  const result = runGuard(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /public order functions/);
});

test("architecture guard fails if clean core imports platform code", () => {
  const root = copyProjectForGuard();
  const coreDir = path.join(root, "app/src/main/java/com/pedilo/app/core/model");
  fs.mkdirSync(coreDir, {recursive: true});
  fs.writeFileSync(path.join(coreDir, "BrokenCore.kt"), "package com.pedilo.app.core.model\nimport androidx.compose.runtime.Composable\n");
  const result = runGuard(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /core must not import Android or Compose/);
});

test("architecture guard fails if pure core imports Firebase", () => {
  const root = copyProjectForGuard();
  const coreDir = path.join(root, "app/src/main/java/com/pedilo/app/core/model");
  fs.mkdirSync(coreDir, {recursive: true});
  fs.writeFileSync(path.join(coreDir, "BrokenFirebase.kt"), "package com.pedilo.app.core.model\nimport com.google.firebase.firestore.FirebaseFirestore\n");
  const result = runGuard(root);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /pure core must not import Firebase/);
});
