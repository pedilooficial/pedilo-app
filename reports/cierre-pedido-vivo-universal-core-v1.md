# Cierre - Pedido Vivo Universal core V1

## Bloque

Build Pedido Vivo Universal core V1.

## Que se implemento

- Nacimiento unificado de pedidos publicos como Pedido Vivo Universal.
- Estados separados minimos en cada pedido nuevo:
  - `operationalStatus`
  - `financialStatus`
  - `communicationStatus`
  - `incidentStatus`
  - `archiveStatus`
- Responsable actual inicial:
  - `currentResponsibleRole = admin`
  - `responsibleRole = admin`
  - actor asignado vacio hasta asignacion real.
- `nextAllowedActions` inicial calculado por backend.
- `liveSnapshot` e `initialSnapshot` al crear pedidos de local, compra directa y retiro/envio.
- Evento inicial en `/orders/{orderId}/events/initial` con `type = order_created`.
- `version = 1` al nacer el pedido y control opcional de `expectedVersion` en la callable Admin existente.
- Idempotencia publica basica y conservadora usando hash SHA-256 del payload limpio y doc id deterministico.
- Timeout/fallback declarativo inicial sin duracion final:
  - `timeoutPolicy.code = initial_admin_review`
  - `fallbackPolicy.code = admin_review_required`
- Tracking publico mantiene sufijo con entropia del hash, sin quedar fijo por el prefijo `ord_`.

## Archivos tocados

- `functions/index.js`
- `app/src/main/java/com/pedilo/app/core/model/LiveOrderContract.kt`
- `app/src/main/java/com/pedilo/app/core/model/AdminOrderReadModels.kt`
- `app/src/main/java/com/pedilo/app/core/firebase/FirebaseAdminOrdersAdapter.kt`
- `tests/live_order_birth_contract.test.js`
- `tests/local_order_flow.test.js`
- `tests/plus_order_flow.test.js`
- `reports/cierre-pedido-vivo-universal-core-v1.md`

## Backend / Functions / Rules

- Functions:
  - `createLocalOrder` ahora crea por `idempotencyKey` y usa `liveBirthContract`.
  - `createPlusOrder` ahora crea por `idempotencyKey` y usa `liveBirthContract`.
  - `createOrderWithInitialEvent` crea pedido y evento inicial en transaccion.
  - `adminOrderAction` conserva su alcance actual, pero suma chequeo opcional de version y aumento de version.
- Firestore Rules:
  - No se modificaron.
  - Se validaron por tests: `/orders`, `/events` e `/incidents` siguen cerrados a escrituras cliente.

## UI

- No se agrego UI nueva.
- No se habilitaron acciones operativas Admin.
- No se implemento Local ni Driver operativo.

## Tests

- Nuevo contrato: `tests/live_order_birth_contract.test.js`.
- Ajustes de contrato en:
  - `tests/local_order_flow.test.js`
  - `tests/plus_order_flow.test.js`

## Validaciones ejecutadas

- `git status --short`
- `node --test tests/*.test.js` - OK, 16/16.
- `bash tools/guards/check_architecture.sh` - OK.
- `cd functions && npm run build` - OK.
- `./gradlew :app:compileDebugKotlin` - OK.
- `./gradlew :app:assembleDebug` - OK.
- `git diff --check` - OK.

## Resultado real

El pedido publico nuevo ya no nace flotante: queda creado con tipo, estados separados, responsable inicial, acciones permitidas, snapshot, evento inicial, version, idempotencia y fallback declarativo.

## Commit esperado

`Build Pedido Vivo Universal core V1`

## Riesgos restantes

- La idempotencia publica es conservadora: dos pedidos identicos enviados con el mismo payload limpio pueden deduplicarse.
- Los timeouts quedan declarativos; falta definir duraciones/SLA reales.
- La version queda disponible para concurrencia, pero las acciones operativas completas se abren en bloques siguientes.
- `financialStatus` es inicial; pagos, cierres y disputas no estan implementados en este bloque.

## Siguiente bloque sugerido

Backend / Functions operativo V1: acciones reales separadas por rol Admin, Local y Repartidor, con validacion de rol, estado, actor, version, auditoria e idempotencia por accion.
