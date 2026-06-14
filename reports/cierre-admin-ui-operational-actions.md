# Cierre - Admin UI conectado a acciones operativas

## Bloque

Connect Admin UI to operational order actions.

## Que se implemento

- Admin `Pedido #____` muestra acciones reales desde `nextAllowedActions`.
- Las acciones visibles se renderizan solo si vienen permitidas por backend.
- Admin ejecuta acciones mediante `operateLiveOrder`.
- Se agrega confirmacion previa a la ejecucion.
- Las acciones sensibles piden motivo operativo.
- Se muestra resultado humano o error operativo.
- El detalle del pedido se refresca despues de ejecutar una accion.
- La seccion Historial muestra eventos/auditoria del pedido cuando estan disponibles.

## Acciones Admin conectadas

Admin puede ejecutar desde UI las acciones que backend permita en `nextAllowedActions`, incluyendo:

- `admin_intervene`
- `cancel_order`
- `open_incident`
- `resolve_incident`
- y cualquier otra accion viva permitida por backend para el estado/version actual.

La UI no inventa acciones ni muestra botones fuera de `nextAllowedActions`.

## Archivos tocados

- `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`
- `app/src/main/java/com/pedilo/app/core/model/LiveOrderContract.kt`
- `app/src/main/java/com/pedilo/app/core/model/AdminOrderReadModels.kt`
- `app/src/main/java/com/pedilo/app/core/port/AdminOrdersPort.kt`
- `app/src/main/java/com/pedilo/app/core/usecase/GetAdminOperationOrdersUseCase.kt`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt`
- `tests/admin_operational_actions.test.js`
- `tests/admin_visual_shell.test.js`
- `tests/live_order_birth_contract.test.js`
- `reports/cierre-admin-ui-operational-actions.md`

## Backend / Functions / Rules

- No se modifico backend.
- No se modificaron rules.
- Admin llama la callable existente `operateLiveOrder`.

## Validaciones ejecutadas

- `node --test tests/*.test.js` - OK, 17/17.
- `bash tools/guards/check_architecture.sh` - OK.
- `cd functions && npm run build` - OK.
- `./gradlew :app:compileDebugKotlin` - OK.
- `./gradlew :app:assembleDebug` - OK.
- `git diff --check` - OK.

## Resultado real

Admin quedo conectado al backend operativo V1 para pedidos vivos. Las acciones se toman del contrato del pedido, se confirman, se ejecutan por backend y refrescan el detalle con resultado humano.

## Pendientes tecnicos reales

- Crear Local UI operativa.
- Crear Driver UI operativa.
- Ampliar detalle visual de auditoria si se requiere paginacion o filtros.
- Agregar pruebas de integracion con emulador Firebase cuando se autorice.

## Commit esperado

`Connect Admin UI to operational order actions`
