# Cierre - Local UI operativo V1

## Bloque

Build Store operational flow V1.

## Que se implemento

- Rol `store/local` abre `StoreApp` operativo despues del login.
- Local ve solo pedidos propios filtrados por `storeId == auth.uid`.
- Local puede abrir detalle de pedido propio.
- Detalle muestra estado real, productos, total, version y acciones permitidas.
- Acciones visibles salen solo de `nextAllowedActions`.
- Acciones conectadas por `operateLiveOrder`:
  - aceptar pedido;
  - rechazar con motivo;
  - marcar en preparacion;
  - marcar listo;
  - reportar problema/incidencia.
- Se muestra confirmacion antes de ejecutar.
- Las acciones sensibles piden motivo operativo.
- Se muestra resultado o error humano.
- Se refresca el detalle despues de la accion.
- No hay escrituras directas a `/orders` desde UI ni adapter.

## Archivos tocados

- `app/src/main/java/com/pedilo/app/core/model/StoreOrderModels.kt`
- `app/src/main/java/com/pedilo/app/core/port/StoreOrdersPort.kt`
- `app/src/main/java/com/pedilo/app/core/usecase/GetStoreOrdersUseCase.kt`
- `app/src/main/java/com/pedilo/app/core/runtime/StoreRuntime.kt`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebaseStoreOrdersAdapter.kt`
- `app/src/main/java/com/pedilo/app/ui/store/StoreApp.kt`
- `app/src/main/java/com/pedilo/app/ui/publicuser/PublicApp.kt`
- `tests/store_operational_flow.test.js`
- `tests/team_access_flow.test.js`
- `reports/cierre-store-operational-flow-v1.md`

## Backend / Functions / Rules

- No se modifico backend.
- No se modificaron rules.
- Local usa la callable existente `operateLiveOrder`.

## Validaciones ejecutadas

- `node --test tests/*.test.js` - OK, 18/18.
- `bash tools/guards/check_architecture.sh` - OK.
- `cd functions && npm run build` - OK.
- `./gradlew :app:compileDebugKotlin` - OK.
- `./gradlew :app:assembleDebug` - OK.
- `git diff --check` - OK.

## Commit esperado

`Build Store operational flow V1`

## Riesgos restantes

- El vinculo operativo usa `storeId == auth.uid`; si se separa local comercial de usuario store, hay que introducir `storeUserId`/`commercialStoreId`.
- Falta UI Repartidor.
- Falta paginacion/filtros avanzados para locales con mucho volumen.
- Falta prueba con emulador Firebase para validar rules y callable con datos reales.
