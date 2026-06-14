# Validate end-to-end live order flow V1

## Flujo validado

- Cliente crea pedido real por `createLocalOrder`.
- El pedido nace con contrato vivo V1, responsable `admin`, `assignedActorId` vacio, `version` inicial y `nextAllowedActions` coherentes.
- Admin puede leer el pedido y su historial desde la mesa operativa.
- Local ve el pedido propio y puede aceptar, marcar en preparacion y marcar listo.
- Al quedar listo, el pedido pasa a responsable `driver` y queda visible para repartidor.
- Driver puede tomar, marcar retirado y marcar entregado.
- El tracking publico acompana el ciclo y ahora traduce `picked_up` como `ON_THE_WAY`.
- Los eventos y la auditoria siguen saliendo por `/orders/{orderId}/events`.

## Fallas encontradas

- El tracking publico seguia traduciendo `picked_up` como `RECEIVED`, lo que dejaba el seguimiento atrasado cuando el driver ya habia retirado el pedido.
- En cierres terminales del flujo vivo, `assignedActorId` y `assignedActorRole` podian quedar arrastrados aunque ya no hubiese actor actual asignado.

## Correcciones aplicadas

- Se ajusto `publicStatusCode` para mapear `picked_up` a `ON_THE_WAY`.
- Se limpio `assignedActorId` y `assignedActorRole` al cerrar pedidos por rechazo local, cancelacion operativa o entrega final.
- Se limpio `driverId` al pasar a `ready_for_pickup` para mantener disponible el pedido sin arrastre de asignacion previa.
- Se agrego un test integrado que recorre el flujo completo real desde nacimiento hasta entrega usando las helpers reales del backend.

## Archivos tocados

- `functions/index.js`
- `tests/live_order_end_to_end_flow.test.js`

## Tests agregados o modificados

- Agregado: `tests/live_order_end_to_end_flow.test.js`

## Validaciones ejecutadas

- `node --test tests/*.test.js`
- `bash tools/guards/check_architecture.sh`
- `cd functions && npm run build`
- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- `git diff --check`

## Commit final

- `Validate end-to-end live order flow V1`

## Riesgos restantes

- La prueba end-to-end valida el contrato operativo real usando las helpers del backend en memoria; no reemplaza una corrida contra Firebase real.
- La lectura de pedidos disponibles para driver sigue dependiendo de que el documento operativo conserve `responsibleRole = "driver"` y `assignedActorId = ""` cuando corresponde.
