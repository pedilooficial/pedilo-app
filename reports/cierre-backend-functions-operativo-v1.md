# Cierre - Backend / Functions operativo V1

## Bloque

Build operational order action backend V1.

## Que se implemento

- Callable operativa nueva: `operateLiveOrder`.
- Acciones reales de Pedido Vivo por backend:
  - `local_accept`
  - `local_reject`
  - `local_mark_preparing`
  - `local_mark_ready`
  - `driver_take`
  - `driver_mark_picked_up`
  - `driver_mark_delivered`
  - `cancel_order`
  - `open_incident`
  - `resolve_incident`
  - `admin_intervene`
- Validacion de auth y rol activo desde `/users/{uid}`.
- Validacion de actor:
  - Local solo opera pedidos con `storeId == uid`.
  - Repartidor solo toma pedidos disponibles o opera pedidos asignados a su `uid`.
  - Admin puede intervenir dentro del backend.
- Validacion de estado actual mediante `allowedLiveActions`.
- Validacion obligatoria de `expectedVersion`.
- Idempotencia por accion mediante `actionId`.
- Auditoria por accion en `/orders/{orderId}/events/{actionId}`.
- Incidencias abiertas en `/orders/{orderId}/incidents/{actionId}`.
- Actualizacion de estados separados, responsable, actor asignado, `nextAllowedActions`, `version` y `lastOperationEvent`.
- Rechazo de acciones invalidas, roles no permitidos y versiones desactualizadas.

## Archivos tocados

- `functions/index.js`
- `app/src/main/java/com/pedilo/app/core/model/LiveOrderContract.kt`
- `tools/guards/check_architecture.sh`
- `tests/operational_order_actions_backend.test.js`
- `tests/guard_negative.test.js`
- `reports/cierre-backend-functions-operativo-v1.md`

## Backend / Functions / Rules

- Functions:
  - Se agrego `operateLiveOrder`.
  - Se mantuvo `adminOrderAction` existente sin conectar UI nueva.
  - Se actualizo el nacimiento del Pedido Vivo para que `nextAllowedActions` use acciones operativas vivas.
  - Se amplio tracking publico para reconocer estados intermedios sin exponer datos internos.
- Rules:
  - No se modificaron.
  - Las escrituras cliente a `/orders`, `/events` e `/incidents` siguen cerradas.
- Guards:
  - El guard de arquitectura ahora mantiene la prohibicion sobre callables publicas de pedido, pero permite Functions operativas por rol en el mismo archivo.
  - Se agrego prueba negativa para asegurar que una callable publica no vuelva a tocar internos operativos.

## UI

- No se redisenio UI.
- No se creo Local UI.
- No se creo Driver UI.
- No se conectaron botones operativos.
- No se toco deploy.

## Tests

- Nuevo: `tests/operational_order_actions_backend.test.js`.
- Actualizado: `tests/guard_negative.test.js`.

## Validaciones ejecutadas

- `git status --short`
- `node --test tests/*.test.js` - OK, 17/17.
- `bash tools/guards/check_architecture.sh` - OK.
- `cd functions && npm run build` - OK.
- `./gradlew :app:compileDebugKotlin` - OK.
- `./gradlew :app:assembleDebug` - OK.
- `git diff --check` - OK.

## Resultado real

El backend ya tiene una ruta operativa V1 para mutaciones criticas del Pedido Vivo. Cada accion valida rol, actor, estado, version e idempotencia, actualiza el pedido de forma transaccional y deja evento auditado.

## Commit esperado

`Build operational order action backend V1`

## Pendientes tecnicos reales

- Conectar UI Admin/Local/Repartidor a `operateLiveOrder`.
- Definir modelo final de vinculacion `storeId` comercial vs usuario operativo si se separan IDs.
- Completar pagos, cierres, disputas y conciliacion financiera.
- Definir SLA/timeouts ejecutables, no solo declarativos.
- Agregar pruebas con emulador Firebase cuando se autorice una capa de integracion real.
