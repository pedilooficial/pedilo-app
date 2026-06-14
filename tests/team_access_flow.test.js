const fs = require("node:fs");
const test = require("node:test");
const assert = require("node:assert/strict");

const app = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";
const home = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicHome.kt";
const screen = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicTeamAccess.kt";
const store = "app/src/main/java/com/pedilo/app/ui/publicuser/TeamSessionStore.kt";
const runtime = "app/src/main/java/com/pedilo/app/core/runtime/TeamAccessRuntime.kt";
const model = "app/src/main/java/com/pedilo/app/core/model/TeamAccess.kt";
const adapter = "app/src/main/java/com/pedilo/app/core/firebase/FirebaseTeamAccessAdapter.kt";

test("team button opens login without changing public home navigation", () => {
  const appSource = fs.readFileSync(app, "utf8");
  const homeSource = fs.readFileSync(home, "utf8");

  assert.match(homeSource, /onTeam: \(\) -> Unit/);
  assert.match(homeSource, /clickable\(role = Role\.Button, onClick = onTeam\)/);
  assert.match(appSource, /data object TeamLogin : PublicRoute/);
  assert.match(appSource, /navigateTo\(PublicRoute\.TeamLogin\)/);
});

test("team access keeps admin store driver routing and sign out confirmation", () => {
  const source = fs.readFileSync(screen, "utf8");
  const modelSource = fs.readFileSync(model, "utf8");
  const appSource = fs.readFileSync(app, "utf8");

  assert.match(modelSource, /Admin\("admin", "Pantalla Admin"\)/);
  assert.match(modelSource, /Local\("store", "Pantalla Local"\)/);
  assert.match(modelSource, /Driver\("driver", "Pantalla Repartidor"\)/);
  assert.match(modelSource, /"store", "local" -> Local/);
  assert.match(modelSource, /"driver", "repartidor" -> Driver/);
  assert.match(source, /role\.screenTitle/);
  assert.match(source, /"Cerrar sesión"/);
  assert.match(source, /"¿Querés cerrar sesión\?"/);
  assert.match(source, /Text\("No"\)/);
  assert.match(source, /Text\("Sí"\)/);
  assert.match(appSource, /TeamRole\.Local -> StoreApp/);
  assert.match(appSource, /TeamRole\.Driver -> DriverApp/);
  assert.doesNotMatch(source, /driverId|WhatsApp|whatsapp/i);
});

test("persisted session redirects to the saved role and back does not clear it", () => {
  const appSource = fs.readFileSync(app, "utf8");
  const storeSource = fs.readFileSync(store, "utf8");

  assert.match(storeSource, /readPersistedSession\(\)/);
  assert.match(storeSource, /putString\(KEY_ROLE, session\.role\.wireName\)/);
  assert.match(storeSource, /fun clear\(\)/);
  assert.match(appSource, /activeTeamSession\?\.let \{ PublicRoute\.TeamRolePlaceholder\(it\.role\) \}/);
  assert.match(appSource, /is PublicRoute\.TeamRolePlaceholder -> null/);
  assert.match(appSource, /teamSessionStore\.clear\(\)/);
});

test("login failure stays closed and exposes pediloapp shop link", () => {
  const appSource = fs.readFileSync(app, "utf8");
  const screenSource = fs.readFileSync(screen, "utf8");

  assert.match(appSource, /No encontramos un acceso activo/);
  assert.match(screenSource, /"¿Querés formar parte de Pédilo\?"/);
  assert.match(screenSource, /"Ir a pediloapp\.shop"/);
  assert.match(screenSource, /https:\/\/pediloapp\.shop/);
});

test("team login uses Firebase Auth and users uid profile for role resolution", () => {
  const runtimeSource = fs.readFileSync(runtime, "utf8");
  const adapterSource = fs.readFileSync(adapter, "utf8");

  assert.match(runtimeSource, /FirebaseTeamAccessAdapter/);
  assert.match(adapterSource, /signInWithEmailAndPassword\(request\.user, request\.secret\)/);
  assert.match(adapterSource, /collection\(USERS\)\.document\(user\.uid\)\.get\(\)\.await\(\)/);
  assert.match(adapterSource, /profile\.getBoolean\(ACTIVE\) != true/);
  assert.match(adapterSource, /TeamRole\.fromWire\(profile\.getString\(ROLE\)\.orEmpty\(\)\)/);
  assert.match(adapterSource, /auth\.signOut\(\)/);
});

test("team access does not hardcode credentials or touch operational domains", () => {
  const joined = [home, screen, store, runtime, model, adapter]
    .map((file) => fs.readFileSync(file, "utf8"))
    .join("\n");
  const appSource = fs.readFileSync(app, "utf8");
  const forbiddenSamples = new RegExp(
    [
      ["admin", "123"].join(""),
      ["contraseña", "123"].join(""),
      ["repartidor", "123"].join(""),
      ["local", "123"].join(""),
    ].join("|"),
  );
  const realFirebaseIdentifiers = new RegExp(
    [
      ["local", "test.com"].join("@"),
      ["repart", "test.com"].join("@"),
      ["javib18", "gmail.com"].join("@"),
      ["9JHn2fgicxO11X41aHe1xA9ub5", "J3"].join(""),
      ["b5cEvThJKMS5EQFj8266DDOMQ1", "w2"].join(""),
      ["a2vodl6GULNrVEwy19A5f8ltWO", "13"].join(""),
    ].join("|"),
  );

  assert.doesNotMatch(joined, forbiddenSamples);
  assert.doesNotMatch(joined, /collection\("orders"\)|collection\('orders'\)|payments|order_tracking|createLocalOrder|createPlusOrder|getPublicOrderTracking/);
  assert.doesNotMatch(joined, realFirebaseIdentifiers);
  assert.match(appSource, /teamAccess\.login/);
  assert.doesNotMatch(appSource, forbiddenSamples);
  assert.doesNotMatch(appSource, realFirebaseIdentifiers);
});
