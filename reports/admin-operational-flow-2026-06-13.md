# Admin operational flow 2026-06-13

## Referencias consultadas

- Shopify Help Center: `Managing orders`.
- Zendesk ticketing and views references from web search.
- Kitchen display / POS references from web search for queue, status, ready/complete and return-to-workflow patterns.
- Ticket-assignment workflow research: `TaDaa: real time Ticket Assignment Deep learning Auto Advisor`.
- Referencias de operacion delivery/merchant consultadas por busqueda: DoorDash merchant order tablet, Uber Eats Orders app, Square KDS y Toast KDS.

Patrones aplicados: colas de trabajo, filtros por estado, prioridad visible, detalle compacto, accion primaria, resultado inmediato, historial accesible y retorno a la cola.

## Flujo cerrado

1. Admin entra a Operacion.
2. Ve `Que atender ahora` con colas: requieren accion, con problemas, demorados, sin responsable y esperando local.
3. Ve `Siguiente caso para atender` y abre un pedido real/controlado desde una cola.
4. El detalle muestra estado, proximo paso, responsable, ubicacion actual y lectura rapida.
5. La accion primaria sale de `nextAllowedActions.firstOrNull()` y se ejecuta por `operateLiveOrder` con `expectedVersion` cuando existe.
6. Si no hay accion, se muestra `Sin acciones disponibles` con causa humana: `No hay próximo paso habilitado. Revisá estado, responsable o historial.`
7. Historial reciente aparece antes de datos secundarios.
8. Las acciones secundarias quedan bajo `Mas acciones`; las secciones de consulta quedan bajo `Mas datos`.
9. El cierre del detalle ofrece `Volver a mesa`.

## Validacion

- `node --test tests/admin_visual_shell.test.js tests/admin_operation_alignment.test.js tests/admin_operational_actions.test.js`: OK.
- `node --test tests/*.test.js`: OK, 33/33.
- `./gradlew lintDebug`: OK.
- `./gradlew assembleDebug`: OK.
- `adb devices`: `EH423L012409 device`.
- `adb install -r app/build/outputs/apk/debug/app-debug.apk`: OK.

## Evidencia visual

- `reports/admin-reordered-final-desk.png`
- `reports/admin-reordered-final-queue.png`
- `reports/admin-reordered-final-detail.png`
- `reports/admin-reordered-final-detail-more.png`
- `reports/admin-reordered-final-back-to-desk.png`
- `reports/admin-reordered-final-after-return.png`
- `reports/admin-reordered-final-config.png`
- `reports/admin-reordered-final-team-compact.png`
- `reports/admin-redesign-final-desk.png`
- `reports/admin-redesign-final-queue.png`
- `reports/admin-redesign-final-detail-blocked.png`
- `reports/admin-redesign-final-next-case.png`

Pedido usado: `#PDL-9MOV5J`.

Estado anterior visible: `Con problema`, `Retirar y entregar`, cola `Sin responsable`.

Accion ejecutada: no correspondio; el pedido no expuso `nextAllowedActions` y Admin mostro `Sin acciones disponibles` con causa operativa.

Segundo caso revisado: `#PDL-C0JBUH`, tambien sin `nextAllowedActions`.

Historial: `Sin movimientos cargados todavia`.

Vuelta a mesa: validada con `Volver a mesa`.

## Archivos tocados

- `app/src/main/java/com/pedilo/app/ui/admin/AdminApp.kt`
- `app/src/main/java/com/pedilo/app/ui/admin/components/AdminComponents.kt`
- `app/src/main/java/com/pedilo/app/ui/admin/operation/OperationData.kt`
- `tests/admin_visual_shell.test.js`
- `tests/admin_operation_alignment.test.js`
- `tests/admin_operational_actions.test.js`
- `reports/admin-operational-flow-2026-06-13.md`

## Riesgos y pendientes

- En el pedido validado no habia accion backend disponible, por lo que no se ejecuto mutacion.
- Salud/registro queda como soporte tecnico, fuera de la mesa principal.
