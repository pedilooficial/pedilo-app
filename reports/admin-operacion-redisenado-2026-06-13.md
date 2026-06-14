# Admin Operacion Redisenado En Limpio - 2026-06-13

## Referencias reales revisadas

- Rappi Aliados: panel de pedidos por columnas, con estados como por aceptar, en preparacion y por entregar, mas acciones directas sobre cada pedido.
- Rappi Aliados: guia de recogida con foco en ID, espera del repartidor, aviso al RT y pedido entregado.
- DiDi Food Manager: postventa con lista de solicitudes, panel de detalle, decision aceptar/rechazar y resultado finalizado.
- DiDi Food tablet: configuracion de aceptacion manual/automatica y condiciones operativas antes de procesar pedidos.

## Patrones aplicados

- Mesa primero: la entrada de Operacion muestra que atender ahora, siguiente caso y colas vivas.
- Colas por razon de trabajo: esperando local, preparando, buscando repartidor, en camino y revision operativa.
- Detalle accionable: el pedido abre con estado, proximo paso, accion principal, datos minimos, historial y salida a mesa.
- Resultado visible: al ejecutar accion, la pantalla muestra resultado, historial reciente y vuelta al flujo.
- Carga contextual: las acciones que requieren motivo abren un dialogo real con campo obligatorio antes de confirmar.

## Cambios principales

- Reconstruida la mesa en `AdminOperationDeskScreen`.
- Reordenado el detalle en `AdminOrderDetailScreen`.
- Ajustado el lenguaje de ramas y colas en `OperationData.kt`.
- Se conserva el backend real existente: acciones via `operateLiveOrder` y recalc via `adminRecalculateOrderActions`.
- No se agregaron botones decorativos ni escrituras directas desde UI a Firestore.

## Validacion en celular

Dispositivo: `EH423L012409`.

Pedidos usados:

- `#PDL-C0JBUH`
- `#PDL-YDEXZY`

Acciones ejecutadas:

- Recalcular acciones permitidas para `#PDL-C0JBUH`.
- Aceptar pedido `#PDL-C0JBUH`.
- Recalcular acciones permitidas para `#PDL-YDEXZY`.
- Abrir incidencia en `#PDL-YDEXZY`.

Datos cargados:

- Motivo operativo: `Cliente_informa_faltante`.

Resultados observados:

- `#PDL-C0JBUH`: resultado `Pedido aceptado.`
- `#PDL-YDEXZY`: resultado `Incidencia abierta.`
- La mesa bajo de 9 a 8 problemas y de 9 a 8 sin responsable despues de aceptar el primer pedido.
- Historial mostro eventos reales de recalc, aceptacion e incidencia con motivo.

## Evidencia visual

- `reports/admin-operacion-redisenado-2026-06-13/01_mesa_operativa.png`
- `reports/admin-operacion-redisenado-2026-06-13/02_siguiente_caso.png`
- `reports/admin-operacion-redisenado-2026-06-13/03_resultado_recalculo.png`
- `reports/admin-operacion-redisenado-2026-06-13/04_resultado_aceptar_pedido.png`
- `reports/admin-operacion-redisenado-2026-06-13/05_carga_motivo.png`
- `reports/admin-operacion-redisenado-2026-06-13/06_resultado_incidencia_historial.png`

## Tests y build

- `node --test tests/admin_operation_alignment.test.js tests/admin_visual_shell.test.js tests/admin_operational_actions.test.js tests/admin_order_operation_mapping.test.js`
- `./gradlew assembleDebug`
- `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- `./gradlew lintDebug`

## Pendientes

- Sin pendientes centrales para Admin Operacion detectados en esta validacion.
