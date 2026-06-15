const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const assert = require("node:assert/strict");

const admin = "app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt";
const adminDir = "app/src/main/java/com/pedilo/app/ui/admin";
const publicDir = "app/src/main/java/com/pedilo/app/ui/publicuser";
const publicApp = "app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt";

function read(pathname) {
  return fs.readFileSync(pathname, "utf8");
}

function readTree(dir) {
  const stack = [dir];
  const files = [];
  while (stack.length > 0) {
    const current = stack.pop();
    for (const entry of fs.readdirSync(current, {withFileTypes: true})) {
      const full = path.join(current, entry.name);
      if (entry.isDirectory()) stack.push(full);
      else if (entry.isFile() && entry.name.endsWith(".kt")) files.push(full);
    }
  }
  files.sort();
  return files.map((file) => read(file)).join("\n");
}

function sliceBetween(source, start, end) {
  const startIndex = source.indexOf(start);
  assert.notEqual(startIndex, -1, `missing start marker ${start}`);
  const endIndex = source.indexOf(end, startIndex + start.length);
  assert.notEqual(endIndex, -1, `missing end marker ${end}`);
  return source.slice(startIndex, endIndex);
}

test("admin role opens the rebuilt operation workspace", () => {
  const appSource = read(publicApp);
  const adminSource = read(admin);

  assert.match(appSource, /role == TeamRole\.Admin/);
  assert.match(appSource, /TeamRole\.Admin -> AdminApp\(onSignOutConfirmed = onSignOutConfirmed\)/);
  assert.match(adminSource, /fun AdminApp/);
  assert.match(adminSource, /"Admin Operación"/);
  assert.match(adminSource, /AdminRoute\.Operation -> AdminOperationDeskScreen/);
});

test("admin operation home exposes the new live card contract", () => {
  const source = read(admin);
  const desk = sliceBetween(
    source,
    "private fun AdminOperationDeskScreen",
    "private fun AdminBranchIntentPanel",
  );

  [
    "Pedidos vivos",
    "Admin Operación",
    "Pedidos con problemas",
    "En espera de aceptación",
    "Aceptados",
    "En preparación",
    "En camino",
    "Entregados / cerrados con problemas",
    "Ver más",
  ].forEach((label) => assert.match(source, new RegExp(label)));

  assert.match(desk, /adminLiveBranches\(orders\)/);
  assert.match(desk, /AdminLiveBranchCard/);
  assert.match(desk, /onOpenBranch/);
  assert.doesNotMatch(desk, /onOpenOrder/);
  assert.doesNotMatch(desk, /AdminOperationsEntryCard/);
  assert.match(source, /AdminMainBranchPreview/);
  assert.match(source, /AdminSubBranchCard/);
});

test("ver mas opens branch then queue before order detail", () => {
  const source = readTree(adminDir);
  const appSource = read(admin);

  assert.match(source, /data class OperationBranch\(val list: AdminOperationList\) : AdminRoute/);
  assert.match(source, /data class OperationQueue\(val list: AdminOperationList\) : AdminRoute/);
  assert.match(source, /AdminOperationBranchScreen/);
  assert.match(source, /AdminOperationQueueScreen/);
  assert.match(source, /route = AdminRoute\.OperationBranch\(list\)/);
  assert.match(source, /route = AdminRoute\.OperationQueue\(queue\)/);
  assert.match(source, /returnRoute = AdminRoute\.OperationQueue\(current\.list\)/);
  assert.match(source, /is AdminRoute\.OperationBranch -> AdminRoute\.Operation/);
  assert.match(source, /is AdminRoute\.OperationQueue -> AdminRoute\.OperationBranch/);
  assert.match(source, /Cola de pedidos/);
  assert.match(source, /Ver pedido/);
  assert.match(source, /Abrir ficha/);
  assert.match(source, /AdminQueueOrderCard/);

  assert.doesNotMatch(appSource, /data class OperationUniverse/);
  assert.doesNotMatch(appSource, /data class OperationView/);
  assert.doesNotMatch(appSource, /data class OperationList\(/);
  assert.doesNotMatch(appSource, /AdminOperationUniverseScreen/);
  assert.doesNotMatch(appSource, /AdminOperationViewScreen/);
  assert.doesNotMatch(appSource, /AdminOperationListScreen/);
  assert.doesNotMatch(source, /operationUniverses|operationEntries|orderDetailEntriesFor/);
  assert.doesNotMatch(source, /AdminOperationUniverseKey|AdminOperationUniverse|AdminOperationView/);
});

test("order detail starts with situation action or read only and real backend actions", () => {
  const source = read(admin);
  const detail = sliceBetween(
    source,
    "private fun AdminOrderDetailScreen",
    "private fun AdminOrderSectionScreen",
  );

  assert.match(detail, /AdminOrderCompactTimeline/);
  assert.match(detail, /AdminSecondaryActionRow\(title = "Volver"/);

  assert.match(source, /Problema actual/);
  assert.match(source, /Estado actual/);
  assert.match(source, /Acciones guiadas/);
  assert.match(source, /Ticket del pedido/);
  assert.match(source, /Historial reciente/);
  assert.match(source, /AdminHumanSituationCard/);
  assert.match(source, /AdminGuidedActionsPanel/);
  assert.match(source, /AdminGuidedActionScreen/);
  assert.match(source, /Canales disponibles/);
  assert.match(source, /Sugerencias/);
  assert.match(source, /Resultado/);
  assert.match(source, /AdminOrderDataSheet/);
  assert.match(source, /AdminOrderCompactTimeline/);
  assert.match(detail, /Column\(/);
  assert.match(detail, /verticalScroll\(rememberScrollState\(\)\)/);
  assert.match(source, /AdminGuidedActionScreen[\s\S]*LazyColumn\(/);
  assert.doesNotMatch(detail, /AdminQueueHeader\(title = "Acciones secundarias"/);
  assert.match(detail, /allowedActions = detail\?\.nextAllowedActions \?: summary\?\.nextAllowedActions\.orEmpty\(\)/);
  assert.match(detail, /adminVisibleGuidedActions/);
  assert.match(detail, /visibleActions/);
  assert.match(source, /AdminLiveOrderActionRequest/);
  assert.match(source, /adminOrders\.executeLive/);
  assert.match(source, /AdminGuidedResultPanel/);
  assert.match(source, /Confirmar resultado/);
  assert.match(source, /adminOrders\.recalculateActions\(orderId\)/);
  assert.doesNotMatch(detail, /Sin acción necesaria ahora|Revisá historial|Datos para decidir|No requerido ahora|Debe revisar|order_created|consultas|Datos secundarios|Más acciones|Acción o lectura|Solo lectura|Sin acciones disponibles|Volver a mesa|Actualizar acciones del pedido|Rama operativa/);
  assert.doesNotMatch(source, /collection\("orders"\).*\.set|collection\("orders"\).*\.add|collection\("orders"\).*\.update|collection\("orders"\).*\.delete|runTransaction|writeBatch/s);
});

test("operation no longer exposes the old archive search workspace", () => {
  const source = read(admin);

  assert.doesNotMatch(source, /AdminOperationsArchiveScreen/);
  assert.doesNotMatch(source, /AdminFilterChip/);
  assert.doesNotMatch(source, /AdminDeskOrderCard/);
  assert.doesNotMatch(source, /AdminRoute\.OperationsArchive/);
  assert.match(source, /AdminOperationBranchScreen/);
  assert.match(source, /AdminOperationQueueScreen/);
  assert.match(source, /AdminGuidedActionScreen/);
});

test("admin bottom navigation remains separated from public navigation", () => {
  const source = readTree(adminDir);
  const bottomBar = read("app/src/main/java/com/pedilo/app/ui/admin/components/AdminComponents.kt");

  assert.match(source, /Operation\("Operación"\)/);
  assert.match(source, /Configuration\("Configuración"\)/);
  assert.match(source, /RoleAccess\("Equipo"\)/);
  assert.match(source, /AdminBottomBar/);
  assert.match(bottomBar, /AdminBottomItem\(AdminOperationTone\.icon, "Operación", AdminOperationTone\.primary/);
  assert.doesNotMatch(bottomBar, /"Inicio"|"\\+"|"Tienda"|"Casa"|"Salir de Pedilo"/);
});

test("public user files do not depend on admin operation internals", () => {
  const publicSource = readTree(publicDir);

  [
    "PublicHome",
    "PublicShop",
    "PublicPlus",
    "PublicShopTracking",
    "PublicLocal",
  ].forEach((name) => assert.match(publicSource, new RegExp(name)));

  assert.doesNotMatch(publicSource, /AdminOperationBranchScreen/);
  assert.doesNotMatch(publicSource, /AdminLiveBranchCard/);
  assert.doesNotMatch(publicSource, /OperationBranch/);
  assert.doesNotMatch(publicSource, /AdminOperationsArchiveScreen/);
});
