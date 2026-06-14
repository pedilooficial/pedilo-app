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
    "private fun AdminOperationOverviewBand",
  );

  [
    "Home vivo",
    "Admin Operación",
    "Pedidos reales en operación",
    "Operaciones",
    "Buscar · filtrar · historial",
    "Resolver ahora",
    "Ver más",
  ].forEach((label) => assert.match(source, new RegExp(label)));

  assert.match(desk, /adminLiveBranches\(orders\)/);
  assert.match(desk, /AdminOperationOverviewBand\(orders\)/);
  assert.match(desk, /AdminOperationsEntryCard/);
  assert.match(desk, /AdminLiveBranchCard/);
  assert.match(desk, /onOpenBranch/);
  assert.match(desk, /onOpenOrder/);
  assert.match(source, /verticalScroll\(rememberScrollState\(\)\)/);
  assert.match(source, /height\(if \(branch\.rows\.size > 2\) 230\.dp else 150\.dp\)/);
});

test("ver mas uses OperationBranch and old hierarchy routes are gone", () => {
  const source = readTree(adminDir);
  const appSource = read(admin);

  assert.match(source, /data class OperationBranch\(val list: AdminOperationList\) : AdminRoute/);
  assert.match(source, /AdminOperationBranchScreen/);
  assert.match(source, /route = AdminRoute\.OperationBranch\(list\)/);
  assert.match(source, /returnRoute = AdminRoute\.OperationBranch\(current\.list\)/);
  assert.match(source, /is AdminRoute\.OperationBranch -> AdminRoute\.Operation/);
  assert.match(source, /Rama operativa/);
  assert.match(source, /pedidos reales/);
  assert.match(source, /AdminBranchGroupPanel/);
  assert.match(source, /Debe actuar:/);
  assert.match(source, /Resolución:/);

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
  assert.match(detail, /AdminMoreDataInline/);
  assert.match(detail, /AdminSecondaryActionRow\(title = "Volver"/);

  assert.match(source, /Resumen para resolver/);
  assert.match(source, /Resumen de consulta/);
  assert.match(source, /Datos de la espera/);
  assert.match(source, /Historial reciente/);
  assert.match(source, /Más información del pedido/);
  assert.match(source, /Datos para revisar/);
  assert.match(source, /AdminOrderHeroStage/);
  assert.match(source, /AdminOrderDataSheet/);
  assert.match(source, /AdminResponsibleRail/);
  assert.match(source, /AdminSecondaryActionDock/);
  assert.match(source, /AdminOrderCompactTimeline/);
  assert.match(detail, /Column\(/);
  assert.match(detail, /verticalScroll\(rememberScrollState\(\)\)/);
  assert.doesNotMatch(detail, /LazyColumn\(/);
  assert.doesNotMatch(detail, /AdminQueueHeader\(title = "Acciones secundarias"/);
  assert.match(source, /enum class AdminOrderWorkMode/);
  assert.match(source, /AdminOrderWorkMode\.Problem/);
  assert.match(source, /AdminOrderWorkMode\.Closed/);
  assert.match(source, /AdminOrderWorkMode\.Cancelled/);
  assert.match(source, /AdminOrderWorkMode\.Waiting/);
  assert.match(source, /AdminOrderWorkMode\.Preparing/);
  assert.match(source, /AdminOrderWorkMode\.InTransit/);
  assert.match(source, /Pedido \$number/);
  assert.match(source, /Acción principal/);
  assert.match(source, /AdminPassiveOrderNote/);
  assert.match(source, /private fun adminOrderCurrentNeed/);
  assert.match(source, /private fun adminOrderWorkPresentation/);
  assert.match(source, /private fun adminUsefulSummaryFacts/);
  assert.match(source, /private fun adminOrderPartFacts/);
  assert.match(detail, /allowedActions = detail\?\.nextAllowedActions \?: summary\?\.nextAllowedActions\.orEmpty\(\)/);
  assert.match(detail, /primaryAction = allowedActions\.firstOrNull\(\)/);
  assert.match(detail, /secondaryActions = allowedActions\.drop\(1\)/);
  assert.match(source, /AdminLiveOrderActionRequest/);
  assert.match(source, /adminOrders\.executeLive/);
  assert.match(source, /pending\.action\.adminActionLabel\(\)/);
  assert.match(source, /label = "Motivo"/);
  assert.match(source, /adminOrders\.recalculateActions\(orderId\)/);
  assert.doesNotMatch(detail, /Sin acción necesaria ahora|Revisá historial|Datos para decidir|No requerido ahora|Debe revisar|order_created|consultas|Datos secundarios|Más acciones|Acción o lectura|Solo lectura|Sin acciones disponibles|Volver a mesa|Actualizar acciones del pedido/);
  assert.doesNotMatch(source, /collection\("orders"\).*\.set|collection\("orders"\).*\.add|collection\("orders"\).*\.update|collection\("orders"\).*\.delete|runTransaction|writeBatch/s);
});

test("operations is separate search filter history and opens the same detail", () => {
  const source = read(admin);
  const operations = sliceBetween(
    source,
    "private fun AdminOperationsArchiveScreen",
    "private fun AdminFilterChip",
  );

  [
    "Operaciones",
    "Historial de pedidos",
    "Buscar pedido, local o repartidor",
    "Todos",
    "Hoy",
    "Vivos",
    "Problemas",
    "Cerrados",
    "Búsqueda activa",
  ].forEach((label) => assert.match(operations, new RegExp(label)));

  assert.match(operations, /query by remember/);
  assert.match(operations, /filter by remember/);
  assert.match(operations, /AdminFilterChip/);
  assert.match(operations, /AdminDeskOrderCard/);
  assert.match(source, /AdminRoute\.OperationsArchive -> AdminOperationsArchiveScreen/);
  assert.match(source, /returnRoute = AdminRoute\.OperationsArchive/);
  assert.match(source, /AdminRoute\.OperationsArchive -> AdminRoute\.Operation/);
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
